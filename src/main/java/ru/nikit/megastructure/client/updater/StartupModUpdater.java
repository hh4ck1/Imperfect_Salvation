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
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nikit.megastructure.MegastructureMod;

public final class StartupModUpdater {
	private static final Logger LOGGER = LoggerFactory.getLogger("megastructure/updater");
	private static final String CONFIG_FILE = "imperfect_salvation_updater.properties";
	private static final String MANIFEST_URL_PROPERTY = "imperfect_salvation.update_manifest_url";
	private static final int DEFAULT_TIMEOUT_SECONDS = 8;

	private StartupModUpdater() {
	}

	public static void checkOnClientStartup() {
		Thread updater = new Thread(StartupModUpdater::checkAndRestartIfNeeded, "Imperfect Salvation updater");
		updater.setDaemon(true);
		updater.start();
	}

	private static void checkAndRestartIfNeeded() {
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
				renameCurrentJarIfNeeded(currentJar, manifest);
				return;
			}
			Path downloadedJar = downloadJar(config, manifest);
			verifySha256(downloadedJar, manifest.sha256());
			Path destinationJar = targetJarPath(currentJar, manifest);
			Path script = writeUpdateRestartScript(currentJar, downloadedJar, destinationJar);
			LOGGER.info("Installed update helper for Imperfect Salvation {} -> {}. Restarting Minecraft.",
					currentVersion, manifest.version());
			startHelper(script);
			MinecraftClient.getInstance().scheduleStop();
		} catch (Exception exception) {
			LOGGER.warn("Silent update check failed", exception);
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
		HttpRequest request = HttpRequest.newBuilder(URI.create(config.manifestUrl()))
				.timeout(Duration.ofSeconds(config.timeoutSeconds()))
				.GET()
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		HttpRequest request = HttpRequest.newBuilder(URI.create(manifest.jarUrl()))
				.timeout(Duration.ofSeconds(config.timeoutSeconds() * 2L))
				.GET()
				.build();
		HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			Files.deleteIfExists(target);
			throw new IOException("Jar download returned HTTP " + response.statusCode());
		}
		return target;
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

	private static void renameCurrentJarIfNeeded(Path currentJar, UpdateManifest manifest) throws IOException {
		Path destinationJar = targetJarPath(currentJar, manifest);
		if (currentJar.equals(destinationJar)) {
			return;
		}
		Path script = writeRenameRestartScript(currentJar, destinationJar);
		LOGGER.info("Installed rename helper for Imperfect Salvation jar {} -> {}. Restarting Minecraft.",
				currentJar.getFileName(), destinationJar.getFileName());
		startHelper(script);
		MinecraftClient.getInstance().scheduleStop();
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

		StringBuilder content = new StringBuilder();
		content.append("$ErrorActionPreference = 'Stop'\n");
		content.append("Wait-Process -Id ").append(current.pid()).append(" -ErrorAction SilentlyContinue\n");
		content.append("Start-Sleep -Milliseconds 750\n");
		if (!currentJar.equals(destinationJar)) {
			content.append("Remove-Item -LiteralPath ").append(psQuote(currentJar.toString()))
					.append(" -Force -ErrorAction SilentlyContinue\n");
		}
		content.append("Move-Item -LiteralPath ").append(psQuote(downloadedJar.toString()))
				.append(" -Destination ").append(psQuote(destinationJar.toString())).append(" -Force\n");
		UpdaterRelaunchSupport.appendBestRelaunch(content, javaCommand, arguments, commandLine, gameDir);
		content.append("Remove-Item -LiteralPath $MyInvocation.MyCommand.Path -Force\n");
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

		StringBuilder content = new StringBuilder();
		content.append("$ErrorActionPreference = 'Stop'\n");
		content.append("Wait-Process -Id ").append(current.pid()).append(" -ErrorAction SilentlyContinue\n");
		content.append("Start-Sleep -Milliseconds 750\n");
		content.append("Move-Item -LiteralPath ").append(psQuote(currentJar.toString()))
				.append(" -Destination ").append(psQuote(destinationJar.toString())).append(" -Force\n");
		UpdaterRelaunchSupport.appendBestRelaunch(content, javaCommand, arguments, commandLine, gameDir);
		content.append("Remove-Item -LiteralPath $MyInvocation.MyCommand.Path -Force\n");
		Files.writeString(script, content.toString(), StandardCharsets.UTF_8);
		return script;
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
