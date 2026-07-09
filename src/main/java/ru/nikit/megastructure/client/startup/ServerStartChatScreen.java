package ru.nikit.megastructure.client.startup;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import ru.nikit.megastructure.client.task.GlobalTaskAnnouncementOverlay;

/** Chat entry screen that preserves the black launch-gate background. */
public final class ServerStartChatScreen extends ChatScreen {
	public ServerStartChatScreen(String defaultText) {
		super(defaultText);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (client != null && keyCode == GLFW.GLFW_KEY_ESCAPE) {
			client.setScreen(new ServerStartScreen());
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ServerStartScreen.renderLaunchGate(context, textRenderer, width, height);
		ServerStartScreen.renderChat(context, mouseX, mouseY);
		GlobalTaskAnnouncementOverlay.render(context, delta);
		super.render(context, mouseX, mouseY, delta);
	}
}
