package ru.nikit.megastructure.client.updater;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class UpdaterRelaunchSupport {
	private UpdaterRelaunchSupport() {
	}

	static void appendBestRelaunch(
			StringBuilder content,
			Optional<String> javaCommand,
			Optional<String[]> processArguments,
			Optional<String> processCommandLine,
			Path gameDir
	) throws IOException {
		if (processCommandLine.isPresent()) {
			appendCommandLineRelaunch(content, processCommandLine.get(), gameDir);
			return;
		}
		if (javaCommand.isPresent() && processArguments.isPresent()) {
			appendArgumentListRelaunch(content, javaCommand.get(), List.of(processArguments.get()), gameDir);
			return;
		}
		RelaunchCommand runtimeCommand = runtimeRelaunchCommand()
				.orElseThrow(() -> new IOException("Current Java launch command is not available"));
		appendArgumentListRelaunch(content, runtimeCommand.javaCommand(), runtimeCommand.arguments(), gameDir);
	}

	static void appendArgumentListRelaunch(
			StringBuilder content,
			String javaCommand,
			List<String> arguments,
			Path gameDir
	) {
		content.append("$argumentLine = ").append(psQuote(windowsCommandLine(arguments))).append("\n");
		content.append("$startedProcess = Start-Process -FilePath ").append(psQuote(javaCommand))
				.append(" -ArgumentList $argumentLine -WorkingDirectory ").append(psQuote(gameDir.toString()))
				.append(" -PassThru\n");
		content.append("Write-Step ('relaunch requested with quoted argument line, pid=' + $startedProcess.Id)\n");
	}

	static void appendCommandLineRelaunch(StringBuilder content, String commandLine, Path gameDir) {
		content.append("$commandLine = ").append(psQuote(commandLine)).append("\n");
		content.append("$startedProcess = Start-Process -FilePath 'cmd.exe' ")
				.append("-ArgumentList @('/d', '/s', '/c', 'start \"\" ' + $commandLine)")
				.append(" -WorkingDirectory ").append(psQuote(gameDir.toString())).append(" -PassThru\n");
		content.append("Write-Step ('relaunch requested with original command line, pid=' + $startedProcess.Id)\n");
	}

	static Optional<RelaunchCommand> runtimeRelaunchCommand() {
		String javaCommand = javaExecutablePath();
		String classPath = System.getProperty("java.class.path", "").trim();
		String sunCommand = System.getProperty("sun.java.command", "").trim();
		if (javaCommand.isBlank() || sunCommand.isBlank()) {
			return Optional.empty();
		}
		RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = new ArrayList<>();
		arguments.addAll(runtime.getInputArguments());
		if (!classPath.isBlank()) {
			arguments.add("-cp");
			arguments.add(classPath);
		}
		arguments.addAll(splitJavaCommand(sunCommand));
		return arguments.isEmpty() ? Optional.empty() : Optional.of(new RelaunchCommand(javaCommand, arguments));
	}

	private static String javaExecutablePath() {
		String javaHome = System.getProperty("java.home", "").trim();
		String executableName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
				? "javaw.exe"
				: "java";
		if (!javaHome.isBlank()) {
			Path candidate = Path.of(javaHome, "bin", executableName);
			if (Files.isRegularFile(candidate)) {
				return candidate.toAbsolutePath().normalize().toString();
			}
		}
		return executableName;
	}

	static List<String> splitCommandLine(String commandLine) {
		List<String> arguments = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		char quote = 0;
		for (int i = 0; i < commandLine.length(); i++) {
			char character = commandLine.charAt(i);
			if ((character == '"' || character == '\'') && (!quoted || quote == character)) {
				quoted = !quoted;
				quote = quoted ? character : 0;
				continue;
			}
			if (Character.isWhitespace(character) && !quoted) {
				if (!current.isEmpty()) {
					arguments.add(current.toString());
					current.setLength(0);
				}
				continue;
			}
			current.append(character);
		}
		if (!current.isEmpty()) {
			arguments.add(current.toString());
		}
		return arguments;
	}

	static List<String> splitJavaCommand(String commandLine) {
		return normalizeMinecraftCommandArguments(splitCommandLine(commandLine));
	}

	static String windowsCommandLine(List<String> arguments) {
		return arguments.stream()
				.map(UpdaterRelaunchSupport::windowsQuoteArgument)
				.collect(Collectors.joining(" "));
	}

	private static String windowsQuoteArgument(String argument) {
		if (argument.isEmpty()) {
			return "\"\"";
		}
		boolean needsQuotes = argument.chars().anyMatch(character ->
				Character.isWhitespace(character) || character == '"'
		);
		if (!needsQuotes) {
			return argument;
		}
		StringBuilder quoted = new StringBuilder("\"");
		int backslashes = 0;
		for (int i = 0; i < argument.length(); i++) {
			char character = argument.charAt(i);
			if (character == '\\') {
				backslashes++;
				continue;
			}
			if (character == '"') {
				quoted.append("\\".repeat(backslashes * 2 + 1));
				quoted.append('"');
				backslashes = 0;
				continue;
			}
			quoted.append("\\".repeat(backslashes));
			backslashes = 0;
			quoted.append(character);
		}
		quoted.append("\\".repeat(backslashes * 2));
		quoted.append('"');
		return quoted.toString();
	}

	private static List<String> normalizeMinecraftCommandArguments(List<String> rawArguments) {
		Set<String> multiTokenOptions = new HashSet<>(List.of("--version", "--gameDir", "--assetsDir"));
		List<String> normalized = new ArrayList<>(rawArguments.size());
		for (int i = 0; i < rawArguments.size(); i++) {
			String argument = rawArguments.get(i);
			normalized.add(argument);
			if (!multiTokenOptions.contains(argument) || i + 1 >= rawArguments.size()) {
				continue;
			}
			StringBuilder value = new StringBuilder();
			int cursor = i + 1;
			while (cursor < rawArguments.size() && !rawArguments.get(cursor).startsWith("--")) {
				if (!value.isEmpty()) {
					value.append(' ');
				}
				value.append(rawArguments.get(cursor));
				cursor++;
			}
			if (!value.isEmpty()) {
				normalized.add(value.toString());
				i = cursor - 1;
			}
		}
		return normalized;
	}

	private static String psQuote(String value) {
		return "'" + value.replace("'", "''") + "'";
	}

	record RelaunchCommand(String javaCommand, List<String> arguments) {
	}
}
