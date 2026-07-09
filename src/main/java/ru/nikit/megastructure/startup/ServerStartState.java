package ru.nikit.megastructure.startup;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/** Persisted launch gate. It survives restarts, unlike a purely client-side menu. */
public final class ServerStartState extends PersistentState {
	private static final String KEY = "megastructure_server_start";
	private static final String PHASE_KEY = "Phase";
	private static final String INTRO_STARTED_AT_KEY = "IntroductionStartedAt";
	private static final String FIRST_TASK_ANNOUNCED_KEY = "FirstTaskAnnounced";

	private ServerStartPhase phase = ServerStartPhase.WAITING;
	private long introductionStartedAt = -1L;
	private boolean firstTaskAnnounced;

	public static ServerStartState get(MinecraftServer server) {
		ServerWorld overworld = server.getOverworld();
		return overworld.getPersistentStateManager().getOrCreate(
				ServerStartState::fromNbt,
				ServerStartState::new,
				KEY
		);
	}

	public boolean beginIntroduction(long gameTime) {
		if (phase != ServerStartPhase.WAITING) {
			return false;
		}
		phase = ServerStartPhase.INTRODUCTION;
		introductionStartedAt = gameTime;
		markDirty();
		return true;
	}

	public boolean announceFirstTaskIfReady(long gameTime) {
		// The 10-second delay starts only after the full server_start recording has ended.
		// The state may already be STARTED at that point, so do not tie this check to INTRODUCTION.
		long firstTaskAt = ServerStartManager.INTRO_DURATION_TICKS + ServerStartManager.FIRST_TASK_DELAY_TICKS;
		if (phase == ServerStartPhase.WAITING || firstTaskAnnounced
				|| elapsedIntroductionTicks(gameTime) < firstTaskAt) {
			return false;
		}
		firstTaskAnnounced = true;
		markDirty();
		return true;
	}

	public boolean finishIntroductionIfReady(long gameTime) {
		if (phase != ServerStartPhase.INTRODUCTION || remainingIntroductionTicks(gameTime) > 0) {
			return false;
		}
		phase = ServerStartPhase.STARTED;
		markDirty();
		return true;
	}

	public int remainingIntroductionTicks(long gameTime) {
		if (phase != ServerStartPhase.INTRODUCTION) {
			return 0;
		}
		long remaining = Math.max(0L, ServerStartManager.INTRO_DURATION_TICKS - elapsedIntroductionTicks(gameTime));
		return (int) Math.min(Integer.MAX_VALUE, remaining);
	}

	private long elapsedIntroductionTicks(long gameTime) {
		return Math.max(0L, gameTime - introductionStartedAt);
	}

	public ServerStartPhase phase() {
		return phase;
	}

	public boolean hasStarted() {
		return phase == ServerStartPhase.STARTED;
	}

	private static ServerStartState fromNbt(NbtCompound nbt) {
		ServerStartState state = new ServerStartState();
		state.phase = ServerStartPhase.fromNetworkId(nbt.getInt(PHASE_KEY));
		state.introductionStartedAt = nbt.contains(INTRO_STARTED_AT_KEY)
				? nbt.getLong(INTRO_STARTED_AT_KEY)
				: -1L;
		state.firstTaskAnnounced = nbt.getBoolean(FIRST_TASK_ANNOUNCED_KEY);
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putInt(PHASE_KEY, phase.ordinal());
		nbt.putLong(INTRO_STARTED_AT_KEY, introductionStartedAt);
		nbt.putBoolean(FIRST_TASK_ANNOUNCED_KEY, firstTaskAnnounced);
		return nbt;
	}
}
