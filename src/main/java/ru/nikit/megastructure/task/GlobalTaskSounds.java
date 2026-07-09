package ru.nikit.megastructure.task;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import ru.nikit.megastructure.MegastructureMod;

public final class GlobalTaskSounds {
	public static final SoundEvent TASK_APPEARANCE = SoundEvent.of(MegastructureMod.id("task_appearance"));
	public static final SoundEvent SERVER_START = SoundEvent.of(MegastructureMod.id("server_start"));

	private GlobalTaskSounds() {
	}

	public static void register() {
		Registry.register(Registries.SOUND_EVENT, MegastructureMod.id("task_appearance"), TASK_APPEARANCE);
		Registry.register(Registries.SOUND_EVENT, MegastructureMod.id("server_start"), SERVER_START);
	}
}
