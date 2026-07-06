package ru.nikit.megastructure.client.updater;

import java.nio.file.Path;
import java.util.List;

public final class StartupModUpdaterSelfTest {
	private StartupModUpdaterSelfTest() {
	}

	public static void main(String[] args) {
		assertCommandLineSplitKeepsQuotedArguments();
		assertRuntimeFallbackCanBuildRelaunchCommand();
		assertPowershellRelaunchUsesArgumentArray();
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

	private static void assertPowershellRelaunchUsesArgumentArray() {
		StringBuilder script = new StringBuilder();
		UpdaterRelaunchSupport.appendArgumentListRelaunch(
				script,
				"C:\\Java\\bin\\javaw.exe",
				List.of("-Xmx2G", "-cp", "a;b", "net.fabricmc.loader.impl.launch.knot.KnotClient", "--username", "Project Eden"),
				Path.of("C:\\Games\\Project Imperfect Salvation")
		);
		String content = script.toString();
		require(content.contains("$argsList = @("), "script should use PowerShell argument array");
		require(content.contains("'Project Eden'"), "script should quote spaced argument");
		require(content.contains("-WorkingDirectory 'C:\\Games\\Project Imperfect Salvation'"), "working directory missing");
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
