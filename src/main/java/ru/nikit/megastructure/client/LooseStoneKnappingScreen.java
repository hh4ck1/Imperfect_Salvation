package ru.nikit.megastructure.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import ru.nikit.megastructure.survival.LooseStoneKnappingScreenHandler;

public final class LooseStoneKnappingScreen extends HandledScreen<LooseStoneKnappingScreenHandler> {
	private long lastStrikeMs;

	public LooseStoneKnappingScreen(LooseStoneKnappingScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		backgroundWidth = 196;
		backgroundHeight = 128;
		playerInventoryTitleX = -1000;
		playerInventoryTitleY = -1000;
	}

	@Override
	protected void init() {
		super.init();
		titleX = x + 12;
		titleY = y + 10;
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.fillGradient(x, y, x + backgroundWidth, y + backgroundHeight, 0xF0141414, 0xF0262218);
		context.fill(x + 14, y + 36, x + 182, y + 60, 0xFF1D1B18);
		context.fill(x + 20, y + 44, x + 176, y + 52, 0xFF0D0D0D);

		int targetStart = x + 20 + Math.round(handler.targetStart() * 156.0F / 1000.0F);
		int targetEnd = x + 20 + Math.round((handler.targetStart() + handler.targetWidth()) * 156.0F / 1000.0F);
		context.fill(targetStart, y + 43, targetEnd, y + 53, 0xFF786A44);
		context.fill(targetStart + 2, y + 45, targetEnd - 2, y + 51, 0xFFB39B57);

		int cursorX = x + 20 + Math.round(currentCursor() * 156.0F);
		context.fill(cursorX - 1, y + 40, cursorX + 1, y + 56, 0xFFE1E1E1);

		int phase = handler.phase();
		for (int i = 0; i < 5; i++) {
			int left = x + 20 + i * 30;
			int color = i < phase ? 0xFF8E8A77 : 0xFF2E2B26;
			context.fill(left, y + 72, left + 22, y + 80, color);
		}

		for (int i = 0; i < 3; i++) {
			int color = i < handler.misses() ? 0xFFA34332 : 0xFF3A241F;
			context.fill(x + 150 + i * 10, y + 72, x + 156 + i * 10, y + 80, color);
		}

		float strikeFade = Math.max(0.0F, 1.0F - (Util.getMeasuringTimeMs() - lastStrikeMs) / 220.0F);
		int hammerBaseX = x + 150;
		int hammerBaseY = y + 28;
		int lift = Math.round(strikeFade * 10.0F);
		context.fill(hammerBaseX, hammerBaseY - lift, hammerBaseX + 18, hammerBaseY + 4 - lift, 0xFF7A7061);
		context.fill(hammerBaseX + 8, hammerBaseY + 4 - lift, hammerBaseX + 12, hammerBaseY + 22 - lift, 0xFF4C3422);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context);
		super.render(context, mouseX, mouseY, delta);
		context.drawText(textRenderer, title, titleX, titleY, 0xE0D7C0, false);
		context.drawText(textRenderer, handler.progressText(), x + 20, y + 88, 0xD5C9A8, false);
		context.drawText(textRenderer, Text.translatable("screen.megastructure.loose_stone_knapping.misses", handler.misses(), 3), x + 20, y + 100, 0xBCB4A0, false);
		context.drawText(textRenderer, Text.translatable("screen.megastructure.loose_stone_knapping.hit_hint"), x + 20, y + 24, 0x9F9889, false);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && strike()) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 32 && strike()) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private boolean strike() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.interactionManager == null) {
			return false;
		}
		lastStrikeMs = Util.getMeasuringTimeMs();
		int packed = 1000 + Math.round(currentCursor() * 1000.0F);
		client.interactionManager.clickButton(handler.syncId, packed);
		return true;
	}

	private float currentCursor() {
		double cycleMs = Math.max(700.0D, 1320.0D - handler.phase() * 110.0D);
		double time = Util.getMeasuringTimeMs() % cycleMs;
		double half = cycleMs / 2.0D;
		double normalized = time <= half ? time / half : 1.0D - (time - half) / half;
		return (float) normalized;
	}
}
