package ru.nikit.megastructure.task;

public enum GlobalTaskStage {
	FIND_A_PLACE_TO_LIVE("task.megastructure.find_a_place_to_live"),
	PROVIDE_CONDITIONS("task.megastructure.provide_conditions");

	private final String translationKey;

	GlobalTaskStage(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return translationKey;
	}

	public static GlobalTaskStage fromNetworkId(int id) {
		GlobalTaskStage[] stages = values();
		return id >= 0 && id < stages.length ? stages[id] : FIND_A_PLACE_TO_LIVE;
	}
}
