package ru.nikit.megastructure.client.task;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.nikit.megastructure.client.startup.ServerStartClientState;

public final class GlobalTaskKeyBindings {
	private static final String CATEGORY = "key.category.megastructure";
	private static KeyBinding openGlobalTasks;

	private GlobalTaskKeyBindings() {
	}

	/**
	 * Must run from the client entrypoint. Fabric freezes the keybinding registry after
	 * {@code GameOptions} is created, so registering lazily from the first client tick crashes.
	 */
	public static void register() {
		if (openGlobalTasks != null) {
			return;
		}
		openGlobalTasks = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.megastructure.global_tasks",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_J,
				CATEGORY
		));
	}

	public static void tick(MinecraftClient client) {
		if (!ServerStartClientState.hasStarted()) {
			return;
		}
		KeyBinding keyBinding = openGlobalTasks;
		if (keyBinding == null) {
			return;
		}
		while (keyBinding.wasPressed()) {
			if (client.currentScreen == null) {
				client.setScreen(new GlobalTasksScreen());
			}
		}
	}
}
