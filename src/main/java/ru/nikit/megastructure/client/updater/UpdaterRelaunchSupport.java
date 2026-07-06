package ru.nikit.megastructure.client.updater;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
		if (javaCommand.isPresent() && processArguments.isPresent()) {
			appendArgumentListRelaunch(content, javaCommand.get(), List.of(processArguments.get()), gameDir);
			return;
		}
		if (processCommandLine.isPresent()) {
			appendCommandLineRelaunch(content, processCommandLine.get(), gameDir);
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
		content.append("$argsList = @(\n");
		for (String argument : arguments) {
			content.append("\t").append(psQuote(argument)).append(",\n");
		}
		content.append(")\n");
		content.append("Start-Process -FilePath ").append(psQuote(javaCommand))
				.append(" -ArgumentList $argsList -WorkingDirectory ").append(psQuote(gameDir.toString())).append("\n");
	}

	static void appendCommandLineRelaunch(StringBuilder content, String commandLine, Path gameDir) {
		content.append("$commandLine = ").append(psQuote(commandLine)).append("\n");
		content.append("Start-Process -FilePath 'cmd.exe' -ArgumentList @('/d', '/c', 'start \"\" ' + $commandLine)")
				.append(" -WorkingDirectory ").append(psQuote(gameDir.toString())).append("\n");
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
		arguments.addAll(splitCommandLine(sunCommand));
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

	private static String psQuote(String value) {
		return "'" + value.replace("'", "''") + "'";
	}

	record RelaunchCommand(String javaCommand, List<String> arguments) {
	}
}
