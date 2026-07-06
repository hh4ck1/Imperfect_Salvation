package ru.nikit.megastructure.client.startup;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import ru.nikit.megastructure.client.StartupTerminalVulkanRenderer;
import ru.nikit.megastructure.client.task.GlobalTaskAnnouncementOverlay;
import ru.nikit.megastructure.startup.ServerStartManager;
import ru.nikit.megastructure.startup.ServerStartPhase;

/** Full-screen pre-launch gate. It intentionally renders only the normal in-game chat. */
public final class ServerStartScreen extends Screen {
	private static final int ERROR_START_TICKS = 260;
	private static final int TERMINAL_SHUTDOWN_TICKS = 420;
	private static final int LINE_HEIGHT = 10;
	private static final int NORMAL_LINE_COUNT = 104;
	private static final String DISTRESS_LINK = "https://drive.google.com/file/d/1y6bfrYROhkrzxAxrIgoJBeFRc-KTHI8g/view?usp=sharing";

	public ServerStartScreen() {
		super(Text.empty());
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (client != null && keyCode == GLFW.GLFW_KEY_ESCAPE) {
			// Settings and voice-chat configuration stay usable, while ScreenEvents keeps the world black.
			client.setScreen(new GameMenuScreen(true));
			return true;
		}
		if (client != null && client.options.chatKey.matchesKey(keyCode, scanCode)) {
			client.setScreen(new ServerStartChatScreen(""));
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_SLASH) {
			client.setScreen(new ServerStartChatScreen("/"));
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderLaunchGate(context, textRenderer, width, height);
		renderChat(context, mouseX, mouseY);
		GlobalTaskAnnouncementOverlay.render(context, delta);
	}

	static void renderLaunchGate(DrawContext context, TextRenderer textRenderer, int width, int height) {
		context.fill(0, 0, width, height, 0xFF000000);
		ServerStartPhase phase = ServerStartClientState.phase();
		int elapsed = 0;
		if (phase == ServerStartPhase.INTRODUCTION) {
			int remaining = ServerStartClientState.remainingIntroductionTicks();
			elapsed = Math.max(0, ServerStartManager.INTRO_DURATION_TICKS - remaining);
		}
		renderCrtGlass(context, width, height);
		StartupTerminalVulkanRenderer.render(context, width, height, elapsed);
		int left = 18;
		int top = 18;
		int bottom = Math.max(top + LINE_HEIGHT, height - 28);

		if (phase != ServerStartPhase.INTRODUCTION) {
			drawWaitingTerminal(context, textRenderer, left, top);
		} else {
			if (elapsed >= TERMINAL_SHUTDOWN_TICKS) {
				return;
			}
			drawIntroTerminal(context, textRenderer, left, top, bottom, elapsed);
		}
	}

	private static void renderCrtGlass(DrawContext context, int width, int height) {
		context.fill(0, 0, width, height, 0xFF010501);
		context.fill(10, 10, width - 10, height - 10, 0xFF000600);
		context.fill(22, 18, width - 22, height - 18, 0xFF000100);
		for (int i = 0; i < 34; i++) {
			int alpha = Math.min(220, 16 + i * 6);
			int color = alpha << 24;
			context.fill(i, 0, i + 1, height, color);
			context.fill(width - i - 1, 0, width - i, height, color);
			if (i < 28) {
				context.fill(0, i, width, i + 1, color);
				context.fill(0, height - i - 1, width, height - i, color);
			}
		}
	}

	private static void drawWaitingTerminal(DrawContext context, TextRenderer textRenderer, int left, int top) {
		String[] lines = {
				"PROJECT EDEN / SITE WAKE TERMINAL",
				"secure maintenance channel established",
				"operator signature: pending",
				"pressure manifold: sealed / holding",
				"node array: cold-standby",
				"fluid feed: inhibited",
				"phase servos: locked",
				"human interface: low-bandwidth console relay online",
				"",
				"awaiting authorized ignition phrase"
		};
		for (int i = 0; i < lines.length; i++) {
			context.drawText(textRenderer, Text.literal(lines[i]), left, top + i * LINE_HEIGHT, terminalColor(i, false), false);
		}
		if ((Util.getMeasuringTimeMs() / 430L & 1L) == 0L) {
			context.drawText(textRenderer, Text.literal("> _"), left, top + lines.length * LINE_HEIGHT + 8, 0xFF62FF74, false);
		}
	}

	private static void drawIntroTerminal(DrawContext context, TextRenderer textRenderer, int left, int top, int bottom, int elapsedTicks) {
		int normalVisible = Math.min(NORMAL_LINE_COUNT, 10 + elapsedTicks / 3);
		int errorVisible = elapsedTicks < ERROR_START_TICKS ? 0 : 2 + (elapsedTicks - ERROR_START_TICKS) * 2;
		int total = normalVisible + errorVisible;
		int capacity = Math.max(1, (bottom - top) / LINE_HEIGHT);
		int first = Math.max(0, total - capacity);
		for (int line = first; line < total; line++) {
			boolean error = line >= normalVisible;
			int local = error ? line - normalVisible : line;
			String text = error ? errorLine(local) : normalLine(local);
			int y = top + (line - first) * LINE_HEIGHT;
			context.drawText(textRenderer, Text.literal(text), left, y, terminalColor(line, error), false);
		}
		if (total < capacity && (Util.getMeasuringTimeMs() / 180L & 1L) == 0L) {
			context.drawText(textRenderer, Text.literal("_"), left, top + total * LINE_HEIGHT, 0xFF62FF74, false);
		}
	}

	private static int terminalColor(int index, boolean error) {
		if (error) {
			return Math.floorMod(index, 5) == 0 ? 0xFF7C1717 : 0xFF5D0D0D;
		}
		return Math.floorMod(index, 7) == 0 ? 0xFFB5FFB0 : 0xFF41F25A;
	}

	private static String normalLine(int index) {
		return switch (Math.floorMod(index, 26)) {
			case 0 -> stamp(index) + " PROJECT EDEN cold-start bus accepted / frame " + hex(index * 113);
			case 1 -> stamp(index) + " manifold A pressure " + fixed(184.0, index, 7, 0.37) + " kPa / bleed closed";
			case 2 -> stamp(index) + " manifold B pressure " + fixed(181.0, index, 11, 0.41) + " kPa / bleed closed";
			case 3 -> stamp(index) + " cryofluid feed rate " + fixed(12.4, index, 5, 0.09) + " L/s / cavitation margin "
					+ fixed(2.8, index, 13, 0.04);
			case 4 -> stamp(index) + " dielectric coolant loop delta-T " + fixed(3.2, index, 17, 0.06) + " C";
			case 5 -> stamp(index) + " node " + padded(Math.floorMod(index * 7, 96), 2) + " handshake "
					+ hex(index * 8191) + " / impedance " + fixed(41.0, index, 19, 0.7) + " ohm";
			case 6 -> stamp(index) + " phase servo " + Math.floorMod(index * 3, 24) + " drift "
					+ fixed(0.018, index, 23, 0.001) + " rad";
			case 7 -> stamp(index) + " inertial cage strain " + fixed(0.42, index, 29, 0.006) + " MPa / green";
			case 8 -> stamp(index) + " substrate shear estimate " + fixed(7.6, index, 31, 0.12) + " microstrain";
			case 9 -> stamp(index) + " threshold surface lock " + padded(Math.floorMod(index * 13, 256), 3)
					+ " / carrier phase " + fixed(87.0, index, 37, 0.8) + " deg";
			case 10 -> stamp(index) + " vacuum skirt pressure " + fixed(0.032, index, 41, 0.002) + " kPa";
			case 11 -> stamp(index) + " magnetic bearing current " + fixed(312.0, index, 43, 1.9) + " A";
			case 12 -> stamp(index) + " torsion ring RPM " + fixed(1180.0, index, 47, 3.7) + " / phase trim nominal";
			case 13 -> stamp(index) + " hydraulic ram group " + Math.floorMod(index * 5, 18) + " stroke "
					+ fixed(72.0, index, 53, 0.6) + " mm";
			case 14 -> stamp(index) + " envelope pressure differential " + fixed(18.0, index, 59, 0.21) + " kPa";
			case 15 -> stamp(index) + " sodium trace line purity " + fixed(99.2, index, 61, 0.03) + "%";
			case 16 -> stamp(index) + " optical fiducials " + (1800 + index * 17) + "/" + (1808 + index * 17) + " aligned";
			case 17 -> stamp(index) + " deep reference clock skew " + fixed(0.004, index, 67, 0.0003) + " ms";
			case 18 -> stamp(index) + " wet bus continuity " + fixed(1.0, index, 71, 0.0) + " / relay bank sealed";
			case 19 -> stamp(index) + " load cell " + Math.floorMod(index * 17, 128) + " compression "
					+ fixed(24.0, index, 73, 0.4) + " kN";
			case 20 -> stamp(index) + " occlusion shutters indexing / aperture " + fixed(0.0, index, 79, 0.0) + "%";
			case 21 -> stamp(index) + " thermal buffer mass " + fixed(44.0, index, 83, 0.5) + " t / flow stable";
			case 22 -> stamp(index) + " node lattice quorum " + (72 + Math.floorMod(index, 9)) + "/81";
			case 23 -> stamp(index) + " return-fluid turbidity " + fixed(0.8, index, 89, 0.05) + " NTU";
			case 24 -> stamp(index) + " launch veil opacity " + fixed(100.0, index, 97, 0.0) + "% / observer blind";
			default -> stamp(index) + " supervisory checksum " + hex(index * 2713) + " accepted";
		};
	}

	private static String errorLine(int index) {
		return switch (Math.floorMod(index, 28)) {
			case 0 -> stamp(52 + index) + "[!] CRITICAL: pressure manifold A exceeds modeled gradient";
			case 1 -> stamp(52 + index) + "[!] FAULT: node " + padded(Math.floorMod(index * 11, 96), 2) + " returns negative latency";
			case 2 -> stamp(52 + index) + "[!] ERROR: coolant phase separator cavitation detected";
			case 3 -> stamp(52 + index) + "[!] HELP";
			case 4 -> stamp(52 + index) + "[!] CRITICAL: threshold surface checksum divergent";
			case 5 -> stamp(52 + index) + "[!] FAULT: torsion ring desync " + fixed(6.0, index, 7, 0.31) + " deg";
			case 6 -> stamp(52 + index) + "[!] OH LORD HELP ME";
			case 7 -> stamp(52 + index) + "[!] ERROR: hydraulic ram group " + Math.floorMod(index * 5, 18) + " overstroke";
			case 8 -> stamp(52 + index) + "[!] WARNING: reference clock monotonicity violation";
			case 9 -> stamp(52 + index) + "[!] SAVE ME PLEASE";
			case 10 -> stamp(52 + index) + "[!] CRITICAL: observer blind reports incoming light";
			case 11 -> stamp(52 + index) + "[!] FAULT: return-fluid turbidity beyond sensor range";
			case 12 -> stamp(52 + index) + "[!] DISTRESS LINK: " + DISTRESS_LINK;
			case 13 -> stamp(52 + index) + "[!] ERROR: lattice quorum split / " + (34 + Math.floorMod(index, 8)) + "/81";
			case 14 -> stamp(52 + index) + "[!] BEG YOU";
			case 15 -> stamp(52 + index) + "[!] CRITICAL: envelope differential rising without pump command";
			case 16 -> stamp(52 + index) + "[!] ERROR: fiducial grid folded across locked aperture";
			case 17 -> stamp(52 + index) + "[!] FIND ME";
			case 18 -> stamp(52 + index) + "[!] PANIC: supervisory checksum mutating in place";
			case 19 -> stamp(52 + index) + "[!] HELP HELP HELP";
			case 20 -> stamp(52 + index) + "[!] FAILSAFE: terminal bus shedding nonessential output";
			case 21 -> stamp(52 + index) + "[!] FAILSAFE: console phosphor drive dropping";
			case 22 -> stamp(52 + index) + "[!] OH LORD HELP ME";
			case 23 -> stamp(52 + index) + "[!] FAILSAFE: visual relay cut pending";
			case 24 -> stamp(52 + index) + "[!] SIGNAL: carrier below black-start floor";
			case 25 -> stamp(52 + index) + "[!] SAVE ME PLEASE";
			case 26 -> stamp(52 + index) + "[!] FIND ME / " + DISTRESS_LINK;
			default -> stamp(52 + index) + "[!] LINK SILENT";
		};
	}

	private static String stamp(int index) {
		int ticks = index < 52 ? index * 5 : ERROR_START_TICKS + (index - 52) * 2;
		return String.format("T+%02d.%02d ", ticks / 20, Math.floorMod(ticks * 5, 100));
	}

	private static String hex(int value) {
		return Integer.toHexString(value).toUpperCase();
	}

	private static String fixed(double base, int index, int multiplier, double step) {
		double value = base + Math.floorMod(index * multiplier, 17) * step;
		return String.format(java.util.Locale.ROOT, "%.3f", value);
	}

	private static String padded(int value, int width) {
		String text = Integer.toString(value);
		while (text.length() < width) {
			text = "0" + text;
		}
		return text;
	}

	static void renderChat(DrawContext context, int mouseX, int mouseY) {
		if (net.minecraft.client.MinecraftClient.getInstance().inGameHud == null) {
			return;
		}
		net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
		client.inGameHud.getChatHud().render(context, client.inGameHud.getTicks(), mouseX, mouseY);
	}
}
