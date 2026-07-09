package ru.nikit.megastructure.client;

import java.nio.ByteBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;

public final class StartupTerminalVulkanRenderer {
	private static final int TEXTURE_SIZE = 512;
	private static final long SEED = 0x4544454E5F435254L;
	private static final ByteBuffer PIXELS = BufferUtils.createByteBuffer(TEXTURE_SIZE * TEXTURE_SIZE * 4);
	private static NativeImageBackedTexture texture;
	private static Identifier textureId;
	private static boolean unavailableLogged;

	private StartupTerminalVulkanRenderer() {
	}

	public static boolean render(DrawContext context, int width, int height, int elapsedTicks) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || width <= 0 || height <= 0) {
			return false;
		}

		PIXELS.clear();
		boolean generated = BlackHoleNativeBridge.renderVisualField(
				PIXELS,
				TEXTURE_SIZE,
				TEXTURE_SIZE,
				SEED,
				elapsedTicks * 0.012,
				0.04F,
				6
		);
		if (!generated) {
			if (!unavailableLogged) {
				unavailableLogged = true;
				System.err.println("Imperfect_salvation Vulkan terminal renderer is unavailable.");
			}
			return false;
		}

		ensureTexture(client);
		if (texture == null || textureId == null || texture.getImage() == null) {
			return false;
		}

		NativeImage image = texture.getImage();
		PIXELS.rewind();
		for (int y = 0; y < TEXTURE_SIZE; y++) {
			for (int x = 0; x < TEXTURE_SIZE; x++) {
				int r = PIXELS.get() & 0xFF;
				int g = PIXELS.get() & 0xFF;
				int b = PIXELS.get() & 0xFF;
				int a = PIXELS.get() & 0xFF;
				image.setColor(x, y, crtPixel(x, y, r, g, b, a, elapsedTicks));
			}
		}
		texture.upload();

		context.drawTexture(textureId, 0, 0, 0.0F, 0.0F, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
		return true;
	}

	private static void ensureTexture(MinecraftClient client) {
		if (texture != null && textureId != null) {
			return;
		}
		texture = new NativeImageBackedTexture(TEXTURE_SIZE, TEXTURE_SIZE, false);
		textureId = client.getTextureManager().registerDynamicTexture("imperfect_salvation_startup_terminal_vulkan", texture);
	}

	private static int crtPixel(int x, int y, int r, int g, int b, int a, int elapsedTicks) {
		double uvx = (x + 0.5) / TEXTURE_SIZE;
		double uvy = (y + 0.5) / TEXTURE_SIZE;
		double px = uvx * 2.0 - 1.0;
		double py = uvy * 2.0 - 1.0;
		double r2 = px * px + py * py;

		double outside = smoothstep(0.92, 1.26, r2);
		double cornerPull = Math.pow(Math.abs(px), 4.0) + Math.pow(Math.abs(py), 4.0);
		double cornerShadow = smoothstep(0.62, 1.22, cornerPull);
		double glassDome = Math.max(0.0, 1.0 - r2) * 0.09;
		int sourceNoise = ((r * 3) ^ (g * 5) ^ (b * 7) ^ (a * 11) ^ (x * 13) ^ (y * 17) ^ elapsedTicks) & 15;
		double noise = (sourceNoise - 7.5) / 255.0;
		double brightness = Math.max(0.0, glassDome + noise);

		int green = clamp255((int) Math.round(4 + brightness * 42.0));
		int red = 0;
		int blue = 0;
		int alpha = clamp255((int) Math.round((0.16 + cornerShadow * 0.58 + outside * 0.84) * 255.0));
		return (alpha << 24) | (blue << 16) | (green << 8) | red;
	}

	private static double smoothstep(double edge0, double edge1, double x) {
		double t = Math.max(0.0, Math.min(1.0, (x - edge0) / (edge1 - edge0)));
		return t * t * (3.0 - 2.0 * t);
	}

	private static int clamp255(int value) {
		return Math.max(0, Math.min(255, value));
	}
}
