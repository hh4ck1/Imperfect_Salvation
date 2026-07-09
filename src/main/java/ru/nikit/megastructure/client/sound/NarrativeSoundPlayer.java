package ru.nikit.megastructure.client.sound;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nikit.megastructure.MegastructureMod;

/**
 * Plays narrative cues only after the client sound registry has loaded their resource definitions.
 * This avoids silently losing a sound when a network packet arrives during a resource reload.
 */
public final class NarrativeSoundPlayer {
	public static final Identifier TASK_APPEARANCE = MegastructureMod.id("task_appearance");
	public static final Identifier SERVER_START = MegastructureMod.id("server_start");

	private static final Logger LOGGER = LoggerFactory.getLogger("megastructure/narrative-sound");
	private static final int INITIAL_DELAY_TICKS = 2;
	private static final int MAX_RESOURCE_WAIT_TICKS = 200;
	private static final Map<Identifier, PendingSound> PENDING = new LinkedHashMap<>();

	private NarrativeSoundPlayer() {
	}

	public static void queue(Identifier soundId, float volume) {
		PENDING.put(soundId, new PendingSound(soundId, volume, INITIAL_DELAY_TICKS));
	}

	public static void clear() {
		PENDING.clear();
	}

	public static void tick(MinecraftClient client) {
		if (PENDING.isEmpty() || client.getSoundManager() == null) {
			return;
		}

		Iterator<PendingSound> iterator = PENDING.values().iterator();
		while (iterator.hasNext()) {
			PendingSound pending = iterator.next();
			if (pending.delayTicks > 0) {
				pending.delayTicks--;
				continue;
			}
			if (client.getSoundManager().get(pending.id) == null) {
				pending.resourceWaitTicks++;
				if (pending.resourceWaitTicks >= MAX_RESOURCE_WAIT_TICKS) {
					LOGGER.warn(
							"Narrative sound {} is absent from the active resource pack after {} ticks; skipping playback.",
							pending.id,
							MAX_RESOURCE_WAIT_TICKS
					);
					iterator.remove();
				}
				continue;
			}

			client.getSoundManager().play(new PositionedSoundInstance(
					pending.id,
					SoundCategory.MASTER,
					pending.volume,
					1.0F,
					SoundInstance.createRandom(),
					false,
					0,
					SoundInstance.AttenuationType.NONE,
					0.0D,
					0.0D,
					0.0D,
					true
			));
			iterator.remove();
		}
	}

	private static final class PendingSound {
		private final Identifier id;
		private final float volume;
		private int delayTicks;
		private int resourceWaitTicks;

		private PendingSound(Identifier id, float volume, int delayTicks) {
			this.id = id;
			this.volume = volume;
			this.delayTicks = delayTicks;
		}
	}
}
