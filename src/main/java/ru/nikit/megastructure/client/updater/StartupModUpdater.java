package ru.nikit.megastructure.client.updater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.lang.ProcessHandle.Info;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nikit.megastructure.MegastructureMod;

public final class StartupModUpdater {
	private static final String CONFIG_FILE = "imperfect_salvation_updater.properties";
	private static final String MANIFEST_URL_PROPERTY = "imperfect_salvation.update_manifest_url";
	private static final int DEFAULT_TIMEOUT_SECONDS = 8;
	private static final int MAX_HTTP_ATTEMPTS = 4;
	private static final String USER_AGENT = "Imperfect-Salvation-Updater/1.0";

	private StartupModUpdater() {
	}

	public static void checkOnStartup(Runnable shutdown) {
		Thread updater = new Thread(() -> checkAndRestartIfNeeded(shutdown), "Imperfect Salvation updater");
		updater.setDaemon(true);
		updater.start();
	}

	private static void checkAndRestartIfNeeded(Runnable shutdown) {
		try {
			UpdaterConfig config = UpdaterConfig.load();
			if (!config.enabled() || config.manifestUrl().isBlank()) {
				return;
			}
			ModContainer mod = FabricLoader.getInstance()
					.getModContainer(MegastructureMod.MOD_ID)
					.orElseThrow(() -> new IllegalStateException("Mod container is not available"));
			Path currentJar = currentJarPath(mod)
					.orElseThrow(() -> new IllegalStateException("Current mod jar path is not available"));
			String currentVersion = mod.getMetadata().getVersion().getFriendlyString();
			UpdateManifest manifest = fetchManifest(config);
			if (!isNewerVersion(manifest.version(), currentVersion, config.allowDowngrade())) {
				if (renameCurrentJarIfNeeded(currentJar, manifest)) {
					shutdown.run();
				}
				return;
			}
			Path downloadedJar = downloadJar(config, manifest);
			verifySha256(downloadedJar, manifest.sha256());
			Path destinationJar = targetJarPath(currentJar, manifest);
			Path script = writeUpdateRestartScript(currentJar, downloadedJar, destinationJar);
			logger().info("Installed update helper for Imperfect Salvation {} -> {}. Restarting Minecraft.",
					currentVersion, manifest.version());
			startHelper(script);
			shutdown.run();
		} catch (Exception exception) {
			logger().warn("Silent update check failed", exception);
		}
	}

	private static Optional<Path> currentJarPath(ModContainer mod) {
		ModOrigin origin = mod.getOrigin();
		if (origin.getKind() != ModOrigin.Kind.PATH) {
			return Optional.empty();
		}
		return origin.getPaths().stream()
				.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar"))
				.findFirst()
				.map(Path::toAbsolutePath)
				.map(Path::normalize);
	}

	private static UpdateManifest fetchManifest(UpdaterConfig config) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		URI manifestUri = URI.create(config.manifestUrl());
		HttpResponse<String> response = sendStringWithRetries(
				client,
				manifestUri,
				Duration.ofSeconds(config.timeoutSeconds())
		);
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Manifest returned HTTP " + response.statusCode());
		}
		JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
		return new UpdateManifest(
				requiredString(json, "version"),
				requiredString(json, "jar_url"),
				requiredString(json, "sha256"),
				optionalString(json, "file_name")
		);
	}

	private static Path downloadJar(UpdaterConfig config, UpdateManifest manifest) throws IOException, InterruptedException {
		Path updateDir = FabricLoader.getInstance().getGameDir().resolve(".imperfect_salvation_updates");
		Files.createDirectories(updateDir);
		Path target = updateDir.resolve("Imperfect_salvation-" + sanitizeFilePart(manifest.version()) + ".jar.tmp");
		Files.deleteIfExists(target);
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		URI jarUri = URI.create(manifest.jarUrl());
		HttpResponse<Path> response = sendFileWithRetries(
				client,
				jarUri,
				target,
				Duration.ofSeconds(config.timeoutSeconds() * 2L)
		);
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			Files.deleteIfExists(target);
			throw new IOException("Jar download returned HTTP " + response.statusCode());
		}
		return target;
	}

	private static HttpResponse<String> sendStringWithRetries(
			HttpClient client,
			URI uri,
			Duration timeout
	) throws IOException, InterruptedException {
		HttpResponse<String> response = null;
		for (int attempt = 1; attempt <= MAX_HTTP_ATTEMPTS; attempt++) {
			response = client.send(
					httpRequest(uri, timeout).build(),
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
			);
			if (!UpdaterHttpSupport.shouldRetryHttp(response.statusCode()) || attempt == MAX_HTTP_ATTEMPTS) {
				return response;
			}
			sleepBeforeRetry(attempt, uri, response.statusCode());
		}
		return response;
	}

	private static HttpResponse<Path> sendFileWithRetries(
			HttpClient client,
			URI uri,
			Path target,
			Duration timeout
	) throws IOException, InterruptedException {
		HttpResponse<Path> response = null;
		for (int attempt = 1; attempt <= MAX_HTTP_ATTEMPTS; attempt++) {
			Files.deleteIfExists(target);
			response = client.send(
					httpRequest(uri, timeout).build(),
					HttpResponse.BodyHandlers.ofFile(target)
			);
			if (!UpdaterHttpSupport.shouldRetryHttp(response.statusCode()) || attempt == MAX_HTTP_ATTEMPTS) {
				return response;
			}
			Files.deleteIfExists(target);
			sleepBeforeRetry(attempt, uri, response.statusCode());
		}
		return response;
	}

	private static HttpRequest.Builder httpRequest(URI uri, Duration timeout) {
		return HttpRequest.newBuilder(uri)
				.timeout(timeout)
				.header("User-Agent", USER_AGENT)
				.header("Accept", "application/octet-stream, application/json;q=0.9, */*;q=0.8")
				.GET();
	}

	private static void sleepBeforeRetry(int attempt, URI uri, int statusCode) throws InterruptedException {
		long delayMillis = 600L * attempt * attempt;
		logger().warn("Update download from {} returned HTTP {}. Retrying in {} ms.",
				uri, statusCode, delayMillis);
		Thread.sleep(delayMillis);
	}

	private static void verifySha256(Path file, String expectedHash) throws IOException {
		if (expectedHash.isBlank()) {
			throw new IOException("Manifest does not contain sha256");
		}
		String normalizedExpected = expectedHash.toLowerCase(Locale.ROOT).replace(" ", "");
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] actual = digest.digest(Files.readAllBytes(file));
			String actualHash = HexFormat.of().formatHex(actual);
			if (!actualHash.equals(normalizedExpected)) {
				Files.deleteIfExists(file);
				throw new IOException("Downloaded jar SHA-256 mismatch");
			}
		} catch (NoSuchAlgorithmException exception) {
			throw new IOException("SHA-256 is not available", exception);
		}
	}

	private static boolean renameCurrentJarIfNeeded(Path currentJar, UpdateManifest manifest) throws IOException {
		Path destinationJar = targetJarPath(currentJar, manifest);
		if (currentJar.equals(destinationJar)) {
			return false;
		}
		Path script = writeRenameRestartScript(currentJar, destinationJar);
		logger().info("Installed rename helper for Imperfect Salvation jar {} -> {}. Restarting Minecraft.",
				currentJar.getFileName(), destinationJar.getFileName());
		startHelper(script);
		return true;
	}

	private static Logger logger() {
		return LoggerHolder.LOGGER;
	}

	private static final class LoggerHolder {
		private static final Logger LOGGER = LoggerFactory.getLogger("megastructure/updater");
	}

	private static Path targetJarPath(Path currentJar, UpdateManifest manifest) {
		String targetFileName = manifest.fileName().isBlank()
				? fileNameFromUrl(manifest.jarUrl(), manifest.version())
				: manifest.fileName();
		return currentJar.getParent().resolve(sanitizeFileName(targetFileName)).toAbsolutePath().normalize();
	}

	private static Path writeUpdateRestartScript(Path currentJar, Path downloadedJar, Path destinationJar) throws IOException {
		ProcessHandle current = ProcessHandle.current();
		Info info = current.info();
		Optional<String> javaCommand = info.command();
		Optional<String[]> arguments = info.arguments();
		Optional<String> commandLine = info.commandLine();
		Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
		Path script = gameDir.resolve(".imperfect_salvation_updates")
				.resolve("apply-update-" + current.pid() + ".ps1");
		Path log = gameDir.resolve(".imperfect_salvation_updates")
				.resolve("apply-update-" + current.pid() + ".log");

		StringBuilder content = new StringBuilder();
		content.append("$ErrorActionPreference = 'Stop'\n");
		appendScriptLogHeader(content, log);
		content.append("try {\n");
		appendProcessRelease(content, current.pid());
		content.append("Start-Sleep -Milliseconds 750\n");
		if (!currentJar.equals(destinationJar)) {
			content.append("Write-Step 'removing old jar'\n");
			content.append("Remove-Item -LiteralPath ").append(psQuote(currentJar.toString()))
					.append(" -Force -ErrorAction SilentlyContinue\n");
		}
		content.append("Write-Step 'moving downloaded jar into mods folder'\n");
		content.append("Move-Item -LiteralPath ").append(psQuote(downloadedJar.toString()))
				.append(" -Destination ").append(psQuote(destinationJar.toString())).append(" -Force\n");
		content.append("Write-Step 'starting Minecraft again'\n");
		UpdaterRelaunchSupport.appendBestRelaunch(content, javaCommand, arguments, commandLine, gameDir);
		content.append("Write-Step 'update helper completed'\n");
		content.append("Remove-Item -LiteralPath $MyInvocation.MyCommand.Path -Force\n");
		appendScriptCatch(content);
		Files.writeString(script, content.toString(), StandardCharsets.UTF_8);
		return script;
	}

	private static Path writeRenameRestartScript(Path currentJar, Path destinationJar) throws IOException {
		ProcessHandle current = ProcessHandle.current();
		Info info = current.info();
		Optional<String> javaCommand = info.command();
		Optional<String[]> arguments = info.arguments();
		Optional<String> commandLine = info.commandLine();
		Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
		Path script = gameDir.resolve(".imperfect_salvation_updates")
				.resolve("rename-update-" + current.pid() + ".ps1");
		Path log = gameDir.resolve(".imperfect_salvation_updates")
				.resolve("rename-update-" + current.pid() + ".log");

		StringBuilder content = new StringBuilder();
		content.append("$ErrorActionPreference = 'Stop'\n");
		appendScriptLogHeader(content, log);
		content.append("try {\n");
		appendProcessRelease(content, current.pid());
		content.append("Start-Sleep -Milliseconds 750\n");
		content.append("Write-Step 'renaming current jar'\n");
		content.append("Move-Item -LiteralPath ").append(psQuote(currentJar.toString()))
				.append(" -Destination ").append(psQuote(destinationJar.toString())).append(" -Force\n");
		content.append("Write-Step 'starting Minecraft again'\n");
		UpdaterRelaunchSupport.appendBestRelaunch(content, javaCommand, arguments, commandLine, gameDir);
		content.append("Write-Step 'rename helper completed'\n");
		content.append("Remove-Item -LiteralPath $MyInvocation.MyCommand.Path -Force\n");
		appendScriptCatch(content);
		Files.writeString(script, content.toString(), StandardCharsets.UTF_8);
		return script;
	}

	private static void appendScriptLogHeader(StringBuilder content, Path log) {
		content.append("$logPath = ").append(psQuote(log.toString())).append("\n");
		content.append("function Write-Step($message) { ")
				.append("Add-Content -LiteralPath $logPath -Value ((Get-Date -Format o) + ' ' + $message) ")
				.append("}\n");
		content.append("Write-Step 'helper started'\n");
	}

	private static void appendProcessRelease(StringBuilder content, long pid) {
		content.append("Write-Step 'waiting for Minecraft process ").append(pid).append("'\n");
		content.append("$targetProcess = Get-Process -Id ").append(pid).append(" -ErrorAction SilentlyContinue\n");
		content.append("if ($null -ne $targetProcess) {\n");
		content.append("\tif (-not $targetProcess.WaitForExit(12000)) {\n");
		content.append("\t\tWrite-Step 'Minecraft process did not exit in time; forcing stop'\n");
		content.append("\t\tStop-Process -Id ").append(pid).append(" -Force -ErrorAction SilentlyContinue\n");
		content.append("\t\tStart-Sleep -Milliseconds 1250\n");
		content.append("\t} else {\n");
		content.append("\t\tWrite-Step 'Minecraft process exited cleanly'\n");
		content.append("\t}\n");
		content.append("} else {\n");
		content.append("\tWrite-Step 'Minecraft process already exited'\n");
		content.append("}\n");
	}

	private static void appendScriptCatch(StringBuilder content) {
		content.append("}\n");
		content.append("catch {\n");
		content.append("\tAdd-Content -LiteralPath $logPath -Value ((Get-Date -Format o) + ' ERROR ' + $_.Exception.Message)\n");
		content.append("\tAdd-Content -LiteralPath $logPath -Value ($_.ScriptStackTrace | Out-String)\n");
		content.append("\texit 1\n");
		content.append("}\n");
	}

	private static void startHelper(Path script) throws IOException {
		new ProcessBuilder(
				"powershell.exe",
				"-WindowStyle",
				"Hidden",
				"-NoProfile",
				"-ExecutionPolicy",
				"Bypass",
				"-File",
				script.toString()
		).start();
	}

	private static boolean isNewerVersion(String candidate, String current, boolean allowDowngrade) {
		int comparison = compareVersions(candidate, current);
		return allowDowngrade ? comparison != 0 : comparison > 0;
	}

	private static int compareVersions(String left, String right) {
		List<String> leftParts = versionParts(left);
		List<String> rightParts = versionParts(right);
		int length = Math.max(leftParts.size(), rightParts.size());
		for (int i = 0; i < length; i++) {
			String leftPart = i < leftParts.size() ? leftParts.get(i) : "0";
			String rightPart = i < rightParts.size() ? rightParts.get(i) : "0";
			int result = compareVersionPart(leftPart, rightPart);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}

	private static int compareVersionPart(String left, String right) {
		boolean leftNumber = left.chars().allMatch(Character::isDigit);
		boolean rightNumber = right.chars().allMatch(Character::isDigit);
		if (leftNumber && rightNumber) {
			return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
		}
		return left.compareToIgnoreCase(right);
	}

	private static List<String> versionParts(String version) {
		String[] raw = version.split("[^A-Za-z0-9]+");
		List<String> parts = new ArrayList<>(raw.length);
		for (String part : raw) {
			if (!part.isBlank()) {
				parts.add(part);
			}
		}
		return parts;
	}

	private static String requiredString(JsonObject json, String key) throws IOException {
		if (!json.has(key) || !json.get(key).isJsonPrimitive() || json.get(key).getAsString().isBlank()) {
			throw new IOException("Manifest is missing required field: " + key);
		}
		return json.get(key).getAsString().trim();
	}

	private static String optionalString(JsonObject json, String key) {
		if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
			return "";
		}
		return json.get(key).getAsString().trim();
	}

	private static String fileNameFromUrl(String url, String version) {
		int slash = url.lastIndexOf('/');
		String fileName = slash >= 0 ? url.substring(slash + 1) : "";
		int query = fileName.indexOf('?');
		if (query >= 0) {
			fileName = fileName.substring(0, query);
		}
		if (fileName.isBlank() || !fileName.endsWith(".jar")) {
			return "Imperfect_salvation-" + sanitizeFilePart(version) + ".jar";
		}
		return fileName;
	}

	private static String sanitizeFileName(String value) {
		String sanitized = value.replaceAll("[^A-Za-z0-9._+-]", "_");
		return sanitized.endsWith(".jar") ? sanitized : sanitized + ".jar";
	}

	private static String sanitizeFilePart(String value) {
		return value.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	private static String psQuote(String value) {
		return "'" + value.replace("'", "''") + "'";
	}

	private record UpdaterConfig(
			boolean enabled,
			boolean allowDowngrade,
			int timeoutSeconds,
			String manifestUrl
	) {
		static UpdaterConfig load() throws IOException {
			Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
			if (!Files.exists(configPath)) {
				writeDefaultConfig(configPath);
				return new UpdaterConfig(true, false, DEFAULT_TIMEOUT_SECONDS, "");
			}
			Properties properties = new Properties();
			try (var reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
				properties.load(reader);
			}
			String manifestUrl = System.getProperty(MANIFEST_URL_PROPERTY, properties.getProperty("manifest_url", "")).trim();
			boolean enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
			boolean allowDowngrade = Boolean.parseBoolean(properties.getProperty("allow_downgrade", "false"));
			int timeoutSeconds = Math.max(2, parseInt(properties.getProperty("timeout_seconds"), DEFAULT_TIMEOUT_SECONDS));
			return new UpdaterConfig(enabled, allowDowngrade, timeoutSeconds, manifestUrl);
		}

		private static void writeDefaultConfig(Path configPath) throws IOException {
			Files.createDirectories(configPath.getParent());
			String content = """
					# Imperfect Salvation startup updater.
					# Set manifest_url to enable silent startup updates.
					# Manifest JSON fields: version, jar_url, sha256, optional file_name.
					enabled=true
					manifest_url=
					timeout_seconds=8
					allow_downgrade=false
					""";
			Files.writeString(configPath, content, StandardCharsets.UTF_8);
		}

		private static int parseInt(String value, int fallback) {
			try {
				return Integer.parseInt(value.trim());
			} catch (NumberFormatException exception) {
				return fallback;
			}
		}
	}

	private record UpdateManifest(String version, String jarUrl, String sha256, String fileName) {
	}
}
