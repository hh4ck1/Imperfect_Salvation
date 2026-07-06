package ru.nikit.megastructure.client.updater;

import java.nio.file.Path;
import java.util.List;

public final class StartupModUpdaterSelfTest {
	private StartupModUpdaterSelfTest() {
	}

	public static void main(String[] args) {
		assertCommandLineSplitKeepsQuotedArguments();
		assertUnquotedMinecraftPathsAreRegrouped();
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
		require(content.contains("$argsList = @("), "script should use PowerShell argument array");
		require(content.contains("'Project Eden'"), "script should quote spaced argument");
		require(content.contains("-WorkingDirectory 'C:\\Games\\Project Imperfect Salvation'"), "working directory missing");
		require(!content.contains("'Project Eden',\n)"), "last argument should not have a trailing comma");
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
