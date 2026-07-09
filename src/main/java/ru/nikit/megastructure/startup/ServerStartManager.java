package ru.nikit.megastructure.startup;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.nikit.megastructure.MegastructureMod;
import ru.nikit.megastructure.task.GlobalTaskManager;

/** Coordinates the locked pre-launch client screen and the one permitted launch confirmation. */
public final class ServerStartManager {
	// The supplied server_start recording lasts 25.077551 seconds.
	public static final int INTRO_DURATION_TICKS = 502;
	public static final int FIRST_TASK_DELAY_TICKS = 200;
	public static final Identifier START_SYNC = MegastructureMod.id("server_start_sync");

	private ServerStartManager() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerStartState state = ServerStartState.get(server);
			sendSync(handler.player, server, state, false, false);
		});
		ServerTickEvents.END_SERVER_TICK.register(ServerStartManager::tickServer);
	}

	public static int confirmLaunch(ServerCommandSource source) {
		if (!source.hasPermissionLevel(2)) {
			source.sendError(Text.literal("Operator permissions are required to launch the server."));
			return 0;
		}

		MinecraftServer server = source.getServer();
		ServerStartState state = ServerStartState.get(server);
		if (!state.beginIntroduction(server.getOverworld().getTime())) {
			source.sendError(Text.literal("The server launch has already been confirmed."));
			return 0;
		}

		broadcast(server, state, true, false);
		source.sendFeedback(() -> Text.literal("Server launch confirmed."), true);
		return 1;
	}

	private static void tickServer(MinecraftServer server) {
		ServerStartState state = ServerStartState.get(server);
		long gameTime = server.getOverworld().getTime();
		if (state.announceFirstTaskIfReady(gameTime)) {
			broadcast(server, state, false, true);
		}
		if (!state.finishIntroductionIfReady(gameTime)) {
			return;
		}
		broadcast(server, state, false, false);
		GlobalTaskManager.syncAll(server);
	}

	private static void broadcast(MinecraftServer server, ServerStartState state, boolean playIntro, boolean announceFirstTask) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			sendSync(player, server, state, playIntro, announceFirstTask);
		}
	}

	private static void sendSync(
			ServerPlayerEntity player,
			MinecraftServer server,
			ServerStartState state,
			boolean playIntro,
			boolean announceFirstTask
	) {
		PacketByteBuf buffer = PacketByteBufs.create();
		buffer.writeVarInt(state.phase().ordinal());
		buffer.writeVarInt(state.remainingIntroductionTicks(server.getOverworld().getTime()));
		buffer.writeBoolean(playIntro);
		buffer.writeBoolean(announceFirstTask);
		ServerPlayNetworking.send(player, START_SYNC, buffer);
	}
}
