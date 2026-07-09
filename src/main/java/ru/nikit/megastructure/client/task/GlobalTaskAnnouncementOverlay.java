package ru.nikit.megastructure.client.task;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import ru.nikit.megastructure.client.sound.NarrativeSoundPlayer;
import ru.nikit.megastructure.task.GlobalTaskStage;

public final class GlobalTaskAnnouncementOverlay {
	private static long endsAtMs;
	private static GlobalTaskStage announcedStage;

	private GlobalTaskAnnouncementOverlay() {
	}

	public static void show(GlobalTaskStage stage, int durationMs) {
		announcedStage = stage;
		endsAtMs = Util.getMeasuringTimeMs() + Math.max(1_000, durationMs);
		NarrativeSoundPlayer.queue(NarrativeSoundPlayer.TASK_APPEARANCE, 1.0F);
	}

	public static void render(DrawContext context, float tickDelta) {
		if (announcedStage == null || Util.getMeasuringTimeMs() >= endsAtMs) {
			announcedStage = null;
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		int width = client.getWindow().getScaledWidth();
		int height = client.getWindow().getScaledHeight();
		context.fill(0, 0, width, height, 0xF5000000);
		Text heading = Text.translatable("screen.megastructure.global_tasks.new_task");
		Text task = Text.translatable(announcedStage.translationKey());
		int centerX = width / 2;
		int headingY = height / 2 - 28;
		int taskY = height / 2 + 2;
		context.drawCenteredTextWithShadow(client.textRenderer, heading, centerX, headingY, 0xFFE6C344);
		context.getMatrices().push();
		context.getMatrices().translate(centerX, taskY, 0.0F);
		context.getMatrices().scale(2.0F, 2.0F, 1.0F);
		context.drawCenteredTextWithShadow(client.textRenderer, task, 0, 0, 0xFFFFDA43);
		context.getMatrices().pop();
	}
}
