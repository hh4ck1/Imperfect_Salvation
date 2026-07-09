package ru.nikit.megastructure.client.task;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import ru.nikit.megastructure.task.GlobalTaskManager;
import ru.nikit.megastructure.task.GlobalTaskStage;

public final class GlobalTasksClientState {
	private static GlobalTaskStage stage = GlobalTaskStage.FIND_A_PLACE_TO_LIVE;
	private static int playersEverSeen;
	private static int oasisVisitors;

	private GlobalTasksClientState() {
	}

	public static void registerNetworking() {
		ClientPlayNetworking.registerGlobalReceiver(GlobalTaskManager.TASK_SYNC, (client, handler, buffer, responseSender) -> {
			int stageId = buffer.readVarInt();
			int playerCount = buffer.readVarInt();
			int visitorCount = buffer.readVarInt();
			boolean announce = buffer.readBoolean();
			int durationMs = buffer.readVarInt();
			client.execute(() -> apply(stageId, playerCount, visitorCount, announce, durationMs));
		});
	}

	private static void apply(int stageId, int playerCount, int visitorCount, boolean announce, int durationMs) {
		stage = GlobalTaskStage.fromNetworkId(stageId);
		playersEverSeen = Math.max(0, playerCount);
		oasisVisitors = Math.max(0, visitorCount);
		if (announce && stage == GlobalTaskStage.PROVIDE_CONDITIONS) {
			GlobalTaskAnnouncementOverlay.show(stage, durationMs);
		}
	}

	public static GlobalTaskStage stage() {
		return stage;
	}

	public static int playersEverSeen() {
		return playersEverSeen;
	}

	public static int oasisVisitors() {
		return oasisVisitors;
	}

	public static int requiredOasisVisitors() {
		return Math.max(1, (playersEverSeen + 3) / 4);
	}
}
