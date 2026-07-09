package ru.nikit.megastructure.survival;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

public final class MossConsumptionState extends PersistentState {
	private static final String KEY = "megastructure_moss_consumption";
	private static final String PLAYERS_KEY = "Players";

	private final Map<UUID, Integer> eatenByPlayer = new HashMap<>();

	public static void increment(ServerPlayerEntity player) {
		MossConsumptionState state = get(player.getServerWorld());
		state.eatenByPlayer.merge(player.getUuid(), 1, Integer::sum);
		state.markDirty();
	}

	private static MossConsumptionState get(ServerWorld world) {
		ServerWorld overworld = world.getServer().getOverworld();
		return overworld.getPersistentStateManager().getOrCreate(
				MossConsumptionState::fromNbt,
				MossConsumptionState::new,
				KEY
		);
	}

	private static MossConsumptionState fromNbt(NbtCompound nbt) {
		MossConsumptionState state = new MossConsumptionState();
		NbtList players = nbt.getList(PLAYERS_KEY, NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < players.size(); i++) {
			NbtCompound entry = players.getCompound(i);
			if (entry.containsUuid("Player")) {
				state.eatenByPlayer.put(entry.getUuid("Player"), entry.getInt("Count"));
			}
		}
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtList players = new NbtList();
		for (Map.Entry<UUID, Integer> entry : eatenByPlayer.entrySet()) {
			NbtCompound player = new NbtCompound();
			player.putUuid("Player", entry.getKey());
			player.putInt("Count", entry.getValue());
			players.add(player);
		}
		nbt.put(PLAYERS_KEY, players);
		return nbt;
	}
}
