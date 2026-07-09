package ru.nikit.megastructure.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import ru.nikit.megastructure.mixin.client.ScreenAccessor;

public final class VulkanServerListButton {
	private static final int BUTTON_WIDTH = 132;
	private static final int BUTTON_HEIGHT = 20;
	private static final int MARGIN = 8;

	private VulkanServerListButton() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof MultiplayerScreen) {
				addButton(screen, scaledWidth);
			}
		});
	}

	private static void addButton(Screen screen, int scaledWidth) {
		ButtonWidget button = ButtonWidget.builder(buttonText(), widget -> {
			boolean enabled = !VulkanClientConfig.isVulkanEnabled();
			VulkanClientConfig.setVulkanEnabled(enabled);
			widget.setMessage(buttonText());
		}).dimensions(
				Math.max(MARGIN, scaledWidth - BUTTON_WIDTH - MARGIN),
				MARGIN,
				BUTTON_WIDTH,
				BUTTON_HEIGHT
		).build();
		((ScreenAccessor) screen).megastructure$addDrawableChild(button);
	}

	private static Text buttonText() {
		return Text.literal("Vulkan: " + (VulkanClientConfig.isVulkanEnabled() ? "ON" : "OFF"));
	}
}
