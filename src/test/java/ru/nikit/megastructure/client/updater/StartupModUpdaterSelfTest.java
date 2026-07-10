package ru.nikit.megastructure.client.updater;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class StartupModUpdaterSelfTest {
	private StartupModUpdaterSelfTest() {
	}

	public static void main(String[] args) {
		assertCommandLineSplitKeepsQuotedArguments();
		assertUnquotedMinecraftPathsAreRegrouped();
		assertRuntimeFallbackCanBuildRelaunchCommand();
		assertPowershellRelaunchUsesArgumentArray();
		assertOriginalCommandLineHasRelaunchPriority();
		assertTransientHttpStatusesAreRetried();
		assertWindowsArgumentLineQuotesSpacedPaths();
		assertUnixArgumentLineQuotesSpacedPaths();
		assertUnixRelaunchUsesQuotedArgv();
		System.out.println("StartupModUpdater self-test passed");
	}

	private static void assertCommandLineSplitKeepsQuotedArguments() {
		List<String> parsed = UpdaterRelaunchSupport.splitCommandLine(
				"net.fabricmc.loader.impl.launch.knot.KnotClient --username \"Project Eden\" --gameDir \"C:\\Games\\Project Imperfect Salvation\""
		);
		require(parsed.size() == 5, "quoted command should split into five arguments");
		require(parsed.get(0).equals("net.fabricmc.loader.impl.launch.knot.KnotClient"), "main class mismatch");
		require(parsed.get(2).equals("Project Eden"), "quoted username mismatch");
		require(parsed.get(4).equals("C:\\Games\\Project Imperfect Salvation"), "quoted gameDir mismatch");
	}

	private static void assertRuntimeFallbackCanBuildRelaunchCommand() {
		UpdaterRelaunchSupport.RelaunchCommand command = UpdaterRelaunchSupport.runtimeRelaunchCommand()
				.orElseThrow(() -> new AssertionError("runtime fallback command is not available"));
		require(!command.javaCommand().isBlank(), "java command is blank");
		require(!command.arguments().isEmpty(), "runtime fallback arguments are empty");
	}

	private static void assertUnquotedMinecraftPathsAreRegrouped() {
		List<String> parsed = UpdaterRelaunchSupport.splitJavaCommand(
				"net.fabricmc.loader.impl.launch.knot.KnotClient --version Fabric 1.20.1 --gameDir C:\\Users\\nikit\\Desktop\\Project Imperfect Salvation --assetsDir C:\\Users\\nikit\\Desktop\\Project Imperfect Salvation\\assets --width 925"
		);
		require(parsed.contains("Fabric 1.20.1"), "unquoted version should be regrouped");
		require(parsed.contains("C:\\Users\\nikit\\Desktop\\Project Imperfect Salvation"), "unquoted gameDir should be regrouped");
		require(parsed.contains("C:\\Users\\nikit\\Desktop\\Project Imperfect Salvation\\assets"), "unquoted assetsDir should be regrouped");
		require(parsed.contains("--width"), "following option should be preserved");
		require(parsed.contains("925"), "following option value should be preserved");
	}

	private static void assertPowershellRelaunchUsesArgumentArray() {
		StringBuilder script = new StringBuilder();
		UpdaterRelaunchSupport.appendArgumentListRelaunch(
				script,
				"C:\\Java\\bin\\javaw.exe",
				List.of("-Xmx2G", "-cp", "a;b", "net.fabricmc.loader.impl.launch.knot.KnotClient", "--username", "Project Eden"),
				Path.of("C:\\Games\\Project Imperfect Salvation")
		);
		String content = script.toString();
		require(content.contains("$argumentLine = "), "script should use one quoted argument line");
		require(content.contains("\"Project Eden\""), "script should quote spaced argument");
		require(content.contains("-WorkingDirectory 'C:\\Games\\Project Imperfect Salvation'"), "working directory missing");
		require(!content.contains("$argsList = @("), "PowerShell argument array should not be used");
	}

	private static void assertOriginalCommandLineHasRelaunchPriority() {
		StringBuilder script = new StringBuilder();
		try {
			UpdaterRelaunchSupport.appendBestRelaunch(
					script,
					Optional.of("C:\\Java\\bin\\javaw.exe"),
					Optional.of(new String[]{"-cp", "fallback", "FallbackMain"}),
					Optional.of("\"C:\\Games\\Java Runtime\\bin\\javaw.exe\" -Xmx2G -cp \"real cp\" net.fabricmc.loader.impl.launch.knot.KnotClient --gameDir \"C:\\Games\\Project Imperfect Salvation\""),
					Path.of("C:\\Games\\Project Imperfect Salvation")
			);
		} catch (Exception exception) {
			throw new AssertionError("appendBestRelaunch should not fail", exception);
		}
		String content = script.toString();
		require(content.contains("$commandLine = "), "exact command line should be used when available");
		require(content.contains("start \"\" "), "cmd start relaunch should be used for exact command line");
		require(content.contains("original command line"), "script should log exact command-line relaunch");
		require(!content.contains("$argsList = @("), "argument array fallback should not override exact command line");
	}

	private static void assertTransientHttpStatusesAreRetried() {
		require(UpdaterHttpSupport.shouldRetryHttp(408), "request timeout should be retried");
		require(UpdaterHttpSupport.shouldRetryHttp(429), "rate limit should be retried");
		require(UpdaterHttpSupport.shouldRetryHttp(500), "server error should be retried");
		require(!UpdaterHttpSupport.shouldRetryHttp(404), "not found should not be retried");
		require(!UpdaterHttpSupport.shouldRetryHttp(200), "success should not be retried");
	}

	private static void assertWindowsArgumentLineQuotesSpacedPaths() {
		String commandLine = UpdaterRelaunchSupport.windowsCommandLine(List.of(
				"-Xmx2G",
				"-cp",
				"C:\\Games\\Project Imperfect Salvation\\libraries\\fabric loader.jar",
				"net.fabricmc.loader.impl.launch.knot.KnotClient",
				"--gameDir",
				"C:\\Games\\Project Imperfect Salvation",
				"--username",
				"Project Eden"
		));
		require(commandLine.contains("\"C:\\Games\\Project Imperfect Salvation\\libraries\\fabric loader.jar\""),
				"classpath path with spaces should be quoted");
		require(commandLine.contains("\"C:\\Games\\Project Imperfect Salvation\""),
				"gameDir path with spaces should be quoted");
		require(commandLine.endsWith("\"Project Eden\""), "final spaced argument should be quoted");
	}

	private static void assertUnixArgumentLineQuotesSpacedPaths() {
		String commandLine = UpdaterRelaunchSupport.unixCommandLine(List.of(
				"-Xmx2G",
				"-cp",
				"/home/user/Project Imperfect Salvation/libraries/fabric loader.jar",
				"net.fabricmc.loader.impl.launch.knot.KnotClient",
				"--gameDir",
				"/home/user/Project Imperfect Salvation",
				"--username",
				"Project Eden's Host"
		));
		require(commandLine.contains("'/home/user/Project Imperfect Salvation/libraries/fabric loader.jar'"),
				"unix classpath path with spaces should be quoted");
		require(commandLine.contains("'/home/user/Project Imperfect Salvation'"),
				"unix gameDir path with spaces should be quoted");
		require(commandLine.endsWith("'Project Eden'\"'\"'s Host'"), "unix single quote should be escaped");
	}

	private static void assertUnixRelaunchUsesQuotedArgv() {
		StringBuilder script = new StringBuilder();
		UpdaterRelaunchSupport.appendUnixArgumentListRelaunch(
				script,
				"/home/user/Java Runtime/bin/java",
				List.of("-Xmx2G", "-cp", "/home/user/Project Imperfect Salvation/libraries/fabric loader.jar",
						"net.fabricmc.loader.impl.launch.knot.KnotClient", "--username", "Project Eden"),
				Path.of("/home/user/Project Imperfect Salvation")
		);
		String content = script.toString();
		require(content.contains("cd '/home/user/Project Imperfect Salvation'"), "unix working directory missing");
		require(content.contains("nohup '/home/user/Java Runtime/bin/java'"), "unix java command should be quoted");
		require(content.contains("'/home/user/Project Imperfect Salvation/libraries/fabric loader.jar'"),
				"unix argument path should be quoted");
		require(content.contains("pid=$!"), "unix relaunch should log background pid");
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
