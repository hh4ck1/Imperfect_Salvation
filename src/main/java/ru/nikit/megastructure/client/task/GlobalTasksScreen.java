package ru.nikit.megastructure.client.task;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nikit.megastructure.task.GlobalTaskStage;

public final class GlobalTasksScreen extends Screen {
	private static final int PANEL_WIDTH = 520;
	private static final int PANEL_HEIGHT = 164;

	public GlobalTasksScreen() {
		super(Text.translatable("screen.megastructure.global_tasks"));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xD9000000);
		int panelWidth = Math.min(PANEL_WIDTH, width - 36);
		int panelHeight = Math.min(PANEL_HEIGHT, height - 28);
		int left = (width - panelWidth) / 2;
		int top = (height - panelHeight) / 2;
		int right = left + panelWidth;
		int bottom = top + panelHeight;
		context.fillGradient(left, top, right, bottom, 0xF2050504, 0xF217140C);
		context.fill(left, top, right, top + 2, 0xFFE3BD3B);
		context.fill(left, bottom - 2, right, bottom, 0xFF5E4D17);
		context.fill(left + 8, top + 8, right - 8, bottom - 8, 0x72000000);
		for (int y = top + 14; y < bottom - 12; y += 8) {
			context.fill(left + 12, y, right - 12, y + 1, 0x442C281A);
		}

		GlobalTaskStage stage = GlobalTasksClientState.stage();
		Text task = Text.translatable(stage.translationKey()).formatted(Formatting.YELLOW, Formatting.BOLD);
		drawCenteredScaled(context, task, width / 2, top + panelHeight / 2 - 11, panelWidth - 72, 2.0F, 0xFFFFD84E);
		Text progress = Text.literal(GlobalTasksClientState.oasisVisitors() + " / " + GlobalTasksClientState.requiredOasisVisitors())
				.formatted(Formatting.GRAY);
		context.drawCenteredTextWithShadow(textRenderer, progress, width / 2, bottom - 30, 0xFF9B978B);
		context.fill(left + 28, bottom - 16, right - 28, bottom - 14, 0xFF211E14);
		int denominator = Math.max(1, GlobalTasksClientState.requiredOasisVisitors());
		int filled = Math.min(right - left - 56, (right - left - 56) * GlobalTasksClientState.oasisVisitors() / denominator);
		context.fill(left + 28, bottom - 16, left + 28 + filled, bottom - 14, 0xFFE3BD3B);
	}

	private void drawCenteredScaled(DrawContext context, Text text, int centerX, int y, int maxWidth, float preferredScale, int color) {
		int textWidth = Math.max(1, textRenderer.getWidth(text));
		float scale = Math.min(preferredScale, maxWidth / (float) textWidth);
		scale = Math.max(0.72F, scale);
		context.getMatrices().push();
		context.getMatrices().translate(centerX, y, 0.0F);
		context.getMatrices().scale(scale, scale, 1.0F);
		context.drawCenteredTextWithShadow(textRenderer, text, 0, 0, color);
		context.getMatrices().pop();
	}
}
