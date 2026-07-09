package ru.nikit.megastructure.client.startup;

import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import ru.nikit.megastructure.startup.ServerStartManager;
import ru.nikit.megastructure.startup.ServerStartPhase;
import ru.nikit.megastructure.task.GlobalTaskManager;
import ru.nikit.megastructure.task.GlobalTaskStage;
import ru.nikit.megastructure.client.sound.NarrativeSoundPlayer;
import ru.nikit.megastructure.client.task.GlobalTaskAnnouncementOverlay;

/** Client mirror of the server's pre-launch lifecycle. */
public final class ServerStartClientState {
	private static ServerStartPhase phase = ServerStartPhase.WAITING;
	private static int remainingIntroductionTicks;
	private static boolean receivedState;

	private ServerStartClientState() {
	}

	public static void registerNetworking() {
		ClientPlayNetworking.registerGlobalReceiver(ServerStartManager.START_SYNC, (client, handler, buffer, responseSender) -> {
			int phaseId = buffer.readVarInt();
			int remainingTicks = buffer.readVarInt();
			boolean playIntro = buffer.readBoolean();
			boolean announceFirstTask = buffer.readBoolean();
			client.execute(() -> apply(ServerStartPhase.fromNetworkId(phaseId), remainingTicks, playIntro, announceFirstTask));
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
	}

	private static void apply(
			ServerStartPhase incomingPhase,
			int remainingTicks,
			boolean playIntro,
			boolean announceFirstTask
	) {
		phase = incomingPhase;
		remainingIntroductionTicks = Math.max(0, remainingTicks);
		receivedState = true;
		MinecraftClient client = MinecraftClient.getInstance();
		if (playIntro && incomingPhase == ServerStartPhase.INTRODUCTION) {
			NarrativeSoundPlayer.queue(NarrativeSoundPlayer.SERVER_START, 1.0F);
		}
		if (announceFirstTask) {
			GlobalTaskAnnouncementOverlay.show(GlobalTaskStage.FIND_A_PLACE_TO_LIVE, GlobalTaskManager.TASK_APPEARANCE_DURATION_MS);
		}
	}

	public static boolean hasStarted() {
		return receivedState && phase == ServerStartPhase.STARTED;
	}

	public static boolean isBlackoutActive() {
		return receivedState && phase != ServerStartPhase.STARTED;
	}

	public static boolean shouldRenderHudBlackout() {
		if (!isBlackoutActive()) {
			return false;
		}
		Screen screen = MinecraftClient.getInstance().currentScreen;
		return !(screen instanceof ServerStartScreen) && !(screen instanceof ServerStartChatScreen);
	}

	public static ServerStartPhase phase() {
		return phase;
	}

	public static int remainingIntroductionTicks() {
		return remainingIntroductionTicks;
	}

	public static void tick(MinecraftClient client) {
		if (client.player == null || client.world == null || !receivedState) {
			return;
		}
		if (remainingIntroductionTicks > 0) {
			remainingIntroductionTicks--;
		}
		if (phase == ServerStartPhase.STARTED) {
			if (client.currentScreen instanceof ServerStartScreen || client.currentScreen instanceof ServerStartChatScreen) {
				client.setScreen(null);
			}
			return;
		}

		blockVanillaGameplayKeys(client);
		Screen currentScreen = client.currentScreen;
		if (currentScreen == null || isXaeroMapScreen(currentScreen)) {
			client.setScreen(new ServerStartScreen());
		}
	}

	private static void blockVanillaGameplayKeys(MinecraftClient client) {
		setReleased(
				client.options.forwardKey,
				client.options.backKey,
				client.options.leftKey,
				client.options.rightKey,
				client.options.jumpKey,
				client.options.sneakKey,
				client.options.sprintKey,
				client.options.attackKey,
				client.options.useKey,
				client.options.pickItemKey,
				client.options.dropKey
		);
	}

	private static void setReleased(KeyBinding... bindings) {
		for (KeyBinding binding : bindings) {
			binding.setPressed(false);
		}
	}

	private static boolean isXaeroMapScreen(Screen screen) {
		String className = screen.getClass().getName().toLowerCase(Locale.ROOT);
		return className.startsWith("xaero.") && (className.contains("map") || className.contains("worldmap"));
	}

	private static void reset() {
		phase = ServerStartPhase.WAITING;
		remainingIntroductionTicks = 0;
		receivedState = false;
		NarrativeSoundPlayer.clear();
	}
}
