package ru.nikit.megastructure.world;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

final class RetrofittedRoomChestState extends PersistentState {
	private static final String KEY = "megastructure_retrofitted_room_chests_v1";
	private static final String CHUNKS_KEY = "Chunks";

	private final Set<Long> processedChunks = new HashSet<>();

	static RetrofittedRoomChestState get(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(
				RetrofittedRoomChestState::fromNbt,
				RetrofittedRoomChestState::new,
				KEY
		);
	}

	boolean markProcessed(int chunkX, int chunkZ) {
		boolean changed = processedChunks.add(ChunkPos.toLong(chunkX, chunkZ));
		if (changed) {
			markDirty();
		}
		return changed;
	}

	private static RetrofittedRoomChestState fromNbt(NbtCompound nbt) {
		RetrofittedRoomChestState state = new RetrofittedRoomChestState();
		for (long chunk : nbt.getLongArray(CHUNKS_KEY)) {
			state.processedChunks.add(chunk);
		}
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		long[] chunks = new long[processedChunks.size()];
		int index = 0;
		for (long chunk : processedChunks) {
			chunks[index++] = chunk;
		}
		nbt.putLongArray(CHUNKS_KEY, chunks);
		return nbt;
	}
}
