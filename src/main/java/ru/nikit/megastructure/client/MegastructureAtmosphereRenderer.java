package ru.nikit.megastructure.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public final class MegastructureAtmosphereRenderer {
	private static final float[] SHELL_RATIOS = {0.24F, 0.36F, 0.50F, 0.66F, 0.82F, 0.96F};
	private static final int LONGITUDE_SEGMENTS = 32;
	private static final int LATITUDE_SEGMENTS = 12;
	private static float red = AtmosphereProfile.NETWORK.red;
	private static float green = AtmosphereProfile.NETWORK.green;
	private static float blue = AtmosphereProfile.NETWORK.blue;
	private static float alpha = 0.0F;
	private static float reach = AtmosphereProfile.NETWORK.reach;

	private MegastructureAtmosphereRenderer() {
	}

	public static boolean isActive() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.world != null
				&& client.player != null
				&& client.options.getPerspective().isFirstPerson()
				&& profileAtCamera(client) != null;
	}

	static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		AtmosphereProfile target = profileAtCamera(client);
		boolean firstPerson = client.options.getPerspective().isFirstPerson();
		float frameBlend = Math.min(1.0F, context.tickDelta() * 0.08F + 0.035F);
		float targetAlpha = target != null && firstPerson ? target.layerAlpha : 0.0F;
		alpha += (targetAlpha - alpha) * frameBlend;
		if (target != null) {
			red += (target.red - red) * frameBlend;
			green += (target.green - green) * frameBlend;
			blue += (target.blue - blue) * frameBlend;
			reach += (target.reach - reach) * frameBlend;
		}
		if (alpha < 0.001F || context.matrixStack() == null) {
			return;
		}

		int viewDistance = client.options.getViewDistance().getValue() * 16;
		if (client.getFramebuffer().textureWidth <= 0 || client.getFramebuffer().textureHeight <= 0) {
			return;
		}
		float maximumRadius = Math.max(96.0F, (viewDistance - 20.0F) * reach);
		MatrixStack matrices = context.matrixStack();
		matrices.push();
		Matrix4f matrix = matrices.peek().getPositionMatrix();
		boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
		int blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
		int blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
		int blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
		int blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
		ShaderProgram previousShader = RenderSystem.getShader();

		try {
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.depthMask(false);
			RenderSystem.enableDepthTest();
			RenderSystem.disableCull();
			RenderSystem.setShader(GameRenderer::getPositionColorProgram);

			for (int shell = SHELL_RATIOS.length - 1; shell >= 0; shell--) {
				float radius = maximumRadius * SHELL_RATIOS[shell];
				float progression = (shell + 1.0F) / SHELL_RATIOS.length;
				float shellAlpha = alpha * (0.72F + progression * 0.46F);
				drawShell(matrix, radius, shellAlpha);
			}
		} finally {
			RenderSystem.depthMask(depthWrite);
			RenderSystem.depthFunc(depthFunction);
			RenderSystem.blendFuncSeparate(
					blendSourceRgb,
					blendDestinationRgb,
					blendSourceAlpha,
					blendDestinationAlpha
			);
			setCapability(GL11.GL_BLEND, blendEnabled);
			setCapability(GL11.GL_DEPTH_TEST, depthEnabled);
			setCapability(GL11.GL_CULL_FACE, cullEnabled);
			if (previousShader != null) {
				RenderSystem.setShader(() -> previousShader);
			}
			matrices.pop();
		}
	}

	private static void setCapability(int capability, boolean enabled) {
		if (capability == GL11.GL_BLEND) {
			if (enabled) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
		} else if (capability == GL11.GL_DEPTH_TEST) {
			if (enabled) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
		} else if (capability == GL11.GL_CULL_FACE) {
			if (enabled) RenderSystem.enableCull(); else RenderSystem.disableCull();
		}
	}

	private static void drawShell(Matrix4f matrix, float radius, float shellAlpha) {
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
		for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
			float v0 = (float) latitude / LATITUDE_SEGMENTS;
			float v1 = (float) (latitude + 1) / LATITUDE_SEGMENTS;
			float phi0 = (v0 - 0.5F) * (float) Math.PI;
			float phi1 = (v1 - 0.5F) * (float) Math.PI;
			for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
				float u0 = (float) longitude / LONGITUDE_SEGMENTS;
				float u1 = (float) (longitude + 1) / LONGITUDE_SEGMENTS;
				float theta0 = u0 * (float) (Math.PI * 2.0);
				float theta1 = u1 * (float) (Math.PI * 2.0);
				vertex(buffer, matrix, radius, phi0, theta0, shellAlpha);
				vertex(buffer, matrix, radius, phi1, theta0, shellAlpha);
				vertex(buffer, matrix, radius, phi1, theta1, shellAlpha);
				vertex(buffer, matrix, radius, phi0, theta1, shellAlpha);
			}
		}
		BufferRenderer.drawWithGlobalProgram(buffer.end());
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, float radius, float phi, float theta, float baseAlpha) {
		float horizontal = (float) Math.cos(phi);
		float x = radius * horizontal * (float) Math.cos(theta);
		float y = radius * (float) Math.sin(phi);
		float z = radius * horizontal * (float) Math.sin(theta);
		float verticalDensity = 0.78F + 0.28F * Math.abs((float) Math.sin(phi));
		buffer.vertex(matrix, x, y, z).color(red, green, blue, baseAlpha * verticalDensity).next();
	}

	private static AtmosphereProfile profileAtCamera(MinecraftClient client) {
		if (client.world == null || client.player == null) {
			return null;
		}
		String path = biomePathAtCamera(client);
		if (path == null) {
			return null;
		}
		return switch (path) {
			case "titan_tower_hall", "ring_vault", "reservoir_hall", "colossus_lift",
					"reactor_cathedral", "suspended_city", "upper_rim_city",
					"crown_spire", "globe_monument", "atom_storm_array" -> AtmosphereProfile.TITAN;
			case "abyss_dwelling", "descent_well", "iris_chasm", "orbital_web_core",
					"void_altar", "black_hole_reactor" -> AtmosphereProfile.ABYSS;
			case "primary_rift", "ventilation_canyon" -> AtmosphereProfile.RIFT;
			case "railway_tunnel", "transit_nexus" -> AtmosphereProfile.RAIL;
			case "interior_network", "dense_wall", "dead_end_corridors" -> AtmosphereProfile.SERVICE;
			case "tank_cluster", "column_forest", "monolith_hall" -> AtmosphereProfile.OASIS;
			default -> AtmosphereProfile.NETWORK;
		};
	}

	static String biomePathAtCamera(MinecraftClient client) {
		if (client.world == null) {
			return null;
		}
		BlockPos position = BlockPos.ofFloored(client.gameRenderer.getCamera().getPos());
		RegistryEntry<Biome> biome = client.world.getBiome(position);
		return biome.getKey()
				.filter(key -> key.getValue().getNamespace().equals("megastructure"))
				.map(key -> key.getValue().getPath())
				.orElse(null);
	}
}
