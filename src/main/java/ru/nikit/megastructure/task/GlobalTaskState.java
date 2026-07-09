package ru.nikit.megastructure.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.PersistentState;

/** Persistent server-wide story progress. */
public final class GlobalTaskState extends PersistentState {
	private static final String KEY = "megastructure_global_tasks";
	private static final String PLAYERS_KEY = "PlayersEverSeen";
	private static final String OASIS_VISITORS_KEY = "OasisVisitors";
	private static final String STAGE_KEY = "Stage";
	private static final String IMPORTED_KEY = "ImportedExistingPlayers";

	private final Set<UUID> playersEverSeen = new HashSet<>();
	private final Set<UUID> oasisVisitors = new HashSet<>();
	private GlobalTaskStage stage = GlobalTaskStage.FIND_A_PLACE_TO_LIVE;
	private boolean importedExistingPlayers;

	public static GlobalTaskState get(MinecraftServer server) {
		ServerWorld overworld = server.getOverworld();
		return overworld.getPersistentStateManager().getOrCreate(
				GlobalTaskState::fromNbt,
				GlobalTaskState::new,
				KEY
		);
	}

	public boolean importExistingPlayers(MinecraftServer server) {
		if (importedExistingPlayers) {
			return false;
		}
		boolean changed = false;
		Path playerData = server.getSavePath(WorldSavePath.PLAYERDATA);
		if (Files.isDirectory(playerData)) {
			try (Stream<Path> files = Files.list(playerData)) {
				for (Path path : (Iterable<Path>) files
						.filter(candidate -> candidate.getFileName().toString().endsWith(".dat"))::iterator) {
					String file = path.getFileName().toString();
					UUID uuid = parseUuid(file.substring(0, file.length() - 4));
					if (uuid != null && playersEverSeen.add(uuid)) {
						changed = true;
					}
				}
			} catch (IOException ignored) {
				// The live join event still supplies a correct lower bound when playerdata is unavailable.
			}
		}
		importedExistingPlayers = true;
		markDirty();
		return changed;
	}

	public boolean recordPlayer(ServerPlayerEntity player) {
		boolean changed = playersEverSeen.add(player.getUuid());
		if (changed) {
			markDirty();
		}
		return changed;
	}

	public boolean recordOasisVisit(ServerPlayerEntity player) {
		boolean changed = false;
		if (playersEverSeen.add(player.getUuid())) {
			changed = true;
		}
		if (oasisVisitors.add(player.getUuid())) {
			changed = true;
		}
		if (changed) {
			markDirty();
		}
		return changed;
	}

	public boolean advanceWhenOasisQuotaMet() {
		if (stage != GlobalTaskStage.FIND_A_PLACE_TO_LIVE || oasisVisitors.size() < requiredOasisVisitors()) {
			return false;
		}
		stage = GlobalTaskStage.PROVIDE_CONDITIONS;
		markDirty();
		return true;
	}

	public GlobalTaskStage stage() {
		return stage;
	}

	public int playerCount() {
		return playersEverSeen.size();
	}

	public int oasisVisitorCount() {
		return oasisVisitors.size();
	}

	public int requiredOasisVisitors() {
		return Math.max(1, (playersEverSeen.size() + 3) / 4);
	}

	private static UUID parseUuid(String raw) {
		try {
			return UUID.fromString(raw);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static GlobalTaskState fromNbt(NbtCompound nbt) {
		GlobalTaskState state = new GlobalTaskState();
		readUuidSet(nbt.getList(PLAYERS_KEY, NbtElement.COMPOUND_TYPE), state.playersEverSeen);
		readUuidSet(nbt.getList(OASIS_VISITORS_KEY, NbtElement.COMPOUND_TYPE), state.oasisVisitors);
		state.stage = GlobalTaskStage.fromNetworkId(nbt.getInt(STAGE_KEY));
		state.importedExistingPlayers = nbt.getBoolean(IMPORTED_KEY);
		return state;
	}

	private static void readUuidSet(NbtList entries, Set<UUID> output) {
		for (int index = 0; index < entries.size(); index++) {
			NbtCompound entry = entries.getCompound(index);
			if (entry.containsUuid("Id")) {
				output.add(entry.getUuid("Id"));
			}
		}
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.put(PLAYERS_KEY, writeUuidSet(playersEverSeen));
		nbt.put(OASIS_VISITORS_KEY, writeUuidSet(oasisVisitors));
		nbt.putInt(STAGE_KEY, stage.ordinal());
		nbt.putBoolean(IMPORTED_KEY, importedExistingPlayers);
		return nbt;
	}

	private static NbtList writeUuidSet(Set<UUID> source) {
		NbtList list = new NbtList();
		for (UUID uuid : source) {
			NbtCompound entry = new NbtCompound();
			entry.putUuid("Id", uuid);
			list.add(entry);
		}
		return list;
	}
}
