package ru.nikit.megastructure.task;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import ru.nikit.megastructure.MegastructureMod;
import ru.nikit.megastructure.world.MegastructureChunkGenerator;
import ru.nikit.megastructure.startup.ServerStartState;

public final class GlobalTaskManager {
	public static final int TASK_APPEARANCE_DURATION_MS = 4_190;
	public static final net.minecraft.util.Identifier TASK_SYNC = MegastructureMod.id("global_task_sync");
	private static final int OASIS_CHECK_INTERVAL_TICKS = 20;

	private GlobalTaskManager() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			GlobalTaskState state = GlobalTaskState.get(server);
			state.importExistingPlayers(server);
			boolean newPlayer = state.recordPlayer(handler.player);
			sendSync(handler.player, state, false);
			if (newPlayer) {
				broadcast(server, state, false);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(GlobalTaskManager::tickServer);
	}

	private static void tickServer(MinecraftServer server) {
		if (!ServerStartState.get(server).hasStarted() || server.getTicks() % OASIS_CHECK_INTERVAL_TICKS != 0) {
			return;
		}
		GlobalTaskState state = GlobalTaskState.get(server);
		boolean visitChanged = false;
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (!(player.getServerWorld().getChunkManager().getChunkGenerator() instanceof MegastructureChunkGenerator generator)) {
				continue;
			}
			if (generator.isInsideOasis(player.getBlockPos())) {
				visitChanged |= state.recordOasisVisit(player);
			}
		}
		boolean advanced = state.advanceWhenOasisQuotaMet();
		if (visitChanged || advanced) {
			broadcast(server, state, advanced);
		}
	}

	public static void syncAll(MinecraftServer server) {
		broadcast(server, GlobalTaskState.get(server), false);
	}

	private static void broadcast(MinecraftServer server, GlobalTaskState state, boolean announce) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			sendSync(player, state, announce);
		}
	}

	private static void sendSync(ServerPlayerEntity player, GlobalTaskState state, boolean announce) {
		PacketByteBuf buffer = PacketByteBufs.create();
		buffer.writeVarInt(state.stage().ordinal());
		buffer.writeVarInt(state.playerCount());
		buffer.writeVarInt(state.oasisVisitorCount());
		buffer.writeBoolean(announce);
		buffer.writeVarInt(TASK_APPEARANCE_DURATION_MS);
		ServerPlayNetworking.send(player, TASK_SYNC, buffer);
	}
}
