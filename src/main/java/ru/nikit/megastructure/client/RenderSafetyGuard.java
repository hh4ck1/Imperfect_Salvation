package ru.nikit.megastructure.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;

final class RenderSafetyGuard {
	private static final BbsBridge BBS = BbsBridge.load();
	private static boolean reportedRepair;

	private RenderSafetyGuard() {
	}

	static void repairInvalidFramebuffer(MinecraftClient client) {
		if (client == null || client.getWindow() == null || client.getFramebuffer() == null) {
			return;
		}
		int width = client.getWindow().getFramebufferWidth();
		int height = client.getWindow().getFramebufferHeight();
		if (width > 0 && height > 0) {
			RenderSystem.viewport(0, 0, width, height);
		}
		if (!BBS.hasInvalidCustomSize()) {
			return;
		}
		BBS.disableCustomSize();
		if (width > 0 && height > 0) {
			client.getFramebuffer().resize(width, height, MinecraftClient.IS_SYSTEM_MAC);
			RenderSystem.viewport(0, 0, width, height);
		}
		if (!reportedRepair) {
			reportedRepair = true;
			System.err.println("Megastructure repaired an invalid 0x0 BBS world framebuffer.");
		}
	}

	private record BbsBridge(
			Method isCustomSize,
			Method getVideoWidth,
			Method getVideoHeight,
			Method setCustomSize
	) {
		private static BbsBridge load() {
			try {
				Class<?> type = Class.forName("mchorse.bbs_mod.client.BBSRendering");
				return new BbsBridge(
						type.getMethod("isCustomSize"),
						type.getMethod("getVideoWidth"),
						type.getMethod("getVideoHeight"),
						type.getMethod("setCustomSize", boolean.class)
				);
			} catch (ReflectiveOperationException ignored) {
				return new BbsBridge(null, null, null, null);
			}
		}

		private boolean hasInvalidCustomSize() {
			if (isCustomSize == null) {
				return false;
			}
			try {
				return (boolean) isCustomSize.invoke(null)
						&& ((int) getVideoWidth.invoke(null) <= 0 || (int) getVideoHeight.invoke(null) <= 0);
			} catch (ReflectiveOperationException ignored) {
				return false;
			}
		}

		private void disableCustomSize() {
			try {
				setCustomSize.invoke(null, false);
			} catch (ReflectiveOperationException ignored) {
				// BBS is optional; failure leaves vanilla rendering untouched.
			}
		}
	}
}
