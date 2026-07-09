package ru.nikit.megastructure.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

final class MegastructureDustRenderer {
	private static final int CELL_SIZE = 6;
	private static final int HORIZONTAL_RADIUS_CELLS = 7;
	private static final int VERTICAL_RADIUS_CELLS = 5;
	private static final float MAXIMUM_DISTANCE = CELL_SIZE * (HORIZONTAL_RADIUS_CELLS + 0.6F);
	private static final float MAXIMUM_DISTANCE_SQUARED = MAXIMUM_DISTANCE * MAXIMUM_DISTANCE;

	private MegastructureDustRenderer() {
	}

	static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!MegastructureAtmosphereRenderer.isActive()
				|| client.world == null
				|| context.matrixStack() == null
				|| context.camera().getSubmersionType() != CameraSubmersionType.NONE
				|| client.getFramebuffer().textureWidth <= 0
				|| client.getFramebuffer().textureHeight <= 0) {
			return;
		}

		String biome = MegastructureAtmosphereRenderer.biomePathAtCamera(client);
		int density = densityForBiome(biome);
		Vec3d camera = context.camera().getPos();
		int cameraCellX = Math.floorDiv((int) Math.floor(camera.x), CELL_SIZE);
		int cameraCellY = Math.floorDiv((int) Math.floor(camera.y), CELL_SIZE);
		int cameraCellZ = Math.floorDiv((int) Math.floor(camera.z), CELL_SIZE);
		double time = client.world.getTime() + context.tickDelta();

		Quaternionf rotation = new Quaternionf(context.camera().getRotation());
		Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(rotation);
		Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(rotation);
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
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_LEQUAL);
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			RenderSystem.setShader(GameRenderer::getPositionColorProgram);

			BufferBuilder buffer = Tessellator.getInstance().getBuffer();
			buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
			int rendered = 0;
			for (int cellX = cameraCellX - HORIZONTAL_RADIUS_CELLS;
					cellX <= cameraCellX + HORIZONTAL_RADIUS_CELLS; cellX++) {
				for (int cellZ = cameraCellZ - HORIZONTAL_RADIUS_CELLS;
						cellZ <= cameraCellZ + HORIZONTAL_RADIUS_CELLS; cellZ++) {
					for (int cellY = cameraCellY - VERTICAL_RADIUS_CELLS;
							cellY <= cameraCellY + VERTICAL_RADIUS_CELLS; cellY++) {
						long hash = hash(cellX, cellY, cellZ);
						if (unsignedPercent(hash) >= density) {
							continue;
						}

						double phase = unit(hash >>> 9) * Math.PI * 2.0;
						double speed = 0.004 + unit(hash >>> 21) * 0.008;
						double falling = positiveModulo(unit(hash >>> 33) * CELL_SIZE - time * speed, CELL_SIZE);
						double driftStrength = 0.20 + unit(hash >>> 45) * 0.55;
						double slowFlow = time * (0.006 + unit(hash >>> 53) * 0.006);
						double driftX = Math.sin(slowFlow + phase) * driftStrength
								+ Math.sin(slowFlow * 0.37 + phase * 1.91) * driftStrength * 0.38;
						double driftZ = Math.cos(slowFlow * 0.83 + phase * 1.31) * driftStrength
								+ Math.sin(slowFlow * 0.29 + phase * 2.17) * driftStrength * 0.32;

						double worldX = cellX * CELL_SIZE + 0.5 + unit(hash) * (CELL_SIZE - 1.0) + driftX;
						double worldY = cellY * CELL_SIZE + falling;
						double worldZ = cellZ * CELL_SIZE + 0.5 + unit(hash >>> 7) * (CELL_SIZE - 1.0) + driftZ;
						float relativeX = (float) (worldX - camera.x);
						float relativeY = (float) (worldY - camera.y);
						float relativeZ = (float) (worldZ - camera.z);
						float distanceSquared = relativeX * relativeX + relativeY * relativeY + relativeZ * relativeZ;
						if (distanceSquared > MAXIMUM_DISTANCE_SQUARED) {
							continue;
						}
						float distance = (float) Math.sqrt(distanceSquared);
						float nearFade = clamp((distance - 1.2F) / 3.0F);
						float farFade = clamp((MAXIMUM_DISTANCE - distance) / 9.0F);
						float cycleFade = clamp((float) Math.min(falling, CELL_SIZE - falling) / 1.1F);
						float alpha = nearFade * farFade * cycleFade
								* (0.12F + (float) unit(hash >>> 17) * 0.18F);
						if (alpha < 0.002F) {
							continue;
						}

						float rareScale = Math.floorMod(hash, 23L) == 0L ? 1.8F : 1.0F;
						float size = (0.025F + (float) unit(hash >>> 29) * 0.050F) * rareScale;
						float warmth = (float) unit(hash >>> 41);
						float red = 0.64F + warmth * 0.14F;
						float green = 0.65F + warmth * 0.10F;
						float blue = 0.63F + warmth * 0.05F;
						quad(buffer, matrix, right, up, relativeX, relativeY, relativeZ,
								size * 2.25F, size * 3.1F, red, green, blue, alpha * 0.24F);
						quad(buffer, matrix, right, up, relativeX, relativeY, relativeZ,
								size * 0.62F, size, red + 0.08F, green + 0.08F, blue + 0.07F, alpha);
						if (Math.floorMod(hash >>> 5, 31L) == 0L) {
							float bendX = (float) Math.cos(slowFlow + phase) * size * 0.72F;
							float bendZ = (float) -Math.sin(slowFlow * 0.83 + phase * 1.31) * size * 0.72F;
							for (int thread = 1; thread <= 3; thread++) {
								float threadFade = (4.0F - thread) / 3.0F;
								float curve = thread * thread * 0.34F;
								quad(buffer, matrix, right, up,
										relativeX - bendX * curve,
										relativeY + size * thread * 1.65F,
										relativeZ - bendZ * curve,
										size * 0.28F,
										size * 0.46F,
										red,
										green,
										blue,
										alpha * 0.34F * threadFade);
							}
						}
						rendered++;
					}
				}
			}
			if (rendered > 0) {
				BufferRenderer.drawWithGlobalProgram(buffer.end());
			} else {
				buffer.end();
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

	private static void quad(
			BufferBuilder buffer,
			Matrix4f matrix,
			Vector3f right,
			Vector3f up,
			float x,
			float y,
			float z,
			float horizontalSize,
			float verticalSize,
			float red,
			float green,
			float blue,
			float alpha
	) {
		vertex(buffer, matrix, x + up.x * verticalSize, y + up.y * verticalSize, z + up.z * verticalSize,
				red, green, blue, alpha);
		vertex(buffer, matrix, x + right.x * horizontalSize, y + right.y * horizontalSize, z + right.z * horizontalSize,
				red, green, blue, alpha);
		vertex(buffer, matrix, x - up.x * verticalSize, y - up.y * verticalSize, z - up.z * verticalSize,
				red, green, blue, alpha);
		vertex(buffer, matrix, x - right.x * horizontalSize, y - right.y * horizontalSize, z - right.z * horizontalSize,
				red, green, blue, alpha);
	}

	private static void vertex(
			BufferBuilder buffer,
			Matrix4f matrix,
			float x,
			float y,
			float z,
			float red,
			float green,
			float blue,
			float alpha
	) {
		buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).next();
	}

	private static int densityForBiome(String biome) {
		if (biome == null) {
			return 0;
		}
		return switch (biome) {
			case "interior_network", "dense_wall", "dead_end_corridors" -> 58;
			case "railway_tunnel", "transit_nexus", "machine_nave", "silent_foundry" -> 54;
			case "abyss_dwelling", "descent_well", "iris_chasm" -> 38;
			case "tank_cluster", "reservoir_hall" -> 32;
			default -> 46;
		};
	}

	private static long hash(int x, int y, int z) {
		long value = 0x44555354564F4C4CL;
		value ^= x * 0x9E3779B97F4A7C15L;
		value ^= y * 0xC2B2AE3D27D4EB4FL;
		value ^= z * 0x165667B19E3779F9L;
		value ^= value >>> 33;
		value *= 0xff51afd7ed558ccdL;
		value ^= value >>> 33;
		value *= 0xc4ceb9fe1a85ec53L;
		return value ^ value >>> 33;
	}

	private static int unsignedPercent(long value) {
		return (int) Long.remainderUnsigned(value, 100L);
	}

	private static double unit(long value) {
		return (value >>> 11) * 0x1.0p-53;
	}

	private static double positiveModulo(double value, double modulus) {
		double result = value % modulus;
		return result < 0.0 ? result + modulus : result;
	}

	private static float clamp(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
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
}
