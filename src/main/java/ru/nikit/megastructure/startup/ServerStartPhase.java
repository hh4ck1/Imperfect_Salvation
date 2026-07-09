package ru.nikit.megastructure.startup;

/** Lifecycle of the server-wide launch gate shown before the story begins. */
public enum ServerStartPhase {
	WAITING,
	INTRODUCTION,
	STARTED;

	public static ServerStartPhase fromNetworkId(int id) {
		ServerStartPhase[] phases = values();
		return id >= 0 && id < phases.length ? phases[id] : WAITING;
	}
}
