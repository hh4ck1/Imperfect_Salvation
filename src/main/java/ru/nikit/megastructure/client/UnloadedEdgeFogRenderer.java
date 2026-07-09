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
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

final class UnloadedEdgeFogRenderer {
	private static final int NORTH = 0;
	private static final int SOUTH = 1;
	private static final int WEST = 2;
	private static final int EAST = 3;

	private UnloadedEdgeFogRenderer() {
	}

	static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null
				|| context.matrixStack() == null
				|| context.camera().getSubmersionType() != CameraSubmersionType.NONE) {
			return;
		}
		Vec3d camera = context.camera().getPos();
		int cameraChunkX = Math.floorDiv(MathHelper.floor(camera.x), 16);
		int cameraChunkZ = Math.floorDiv(MathHelper.floor(camera.z), 16);
		int viewDistance = client.options.getViewDistance().getValue();
		int scanRadius = Math.min(48, Math.max(8, viewDistance + 3));

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
			for (int chunkX = cameraChunkX - scanRadius; chunkX <= cameraChunkX + scanRadius; chunkX++) {
				for (int chunkZ = cameraChunkZ - scanRadius; chunkZ <= cameraChunkZ + scanRadius; chunkZ++) {
					if (!isChunkLoaded(client.world, chunkX, chunkZ)) {
						continue;
					}
					if (!isChunkLoaded(client.world, chunkX, chunkZ - 1)) {
						edge(buffer, matrix, camera, client.world, chunkX, chunkZ, NORTH);
					}
					if (!isChunkLoaded(client.world, chunkX, chunkZ + 1)) {
						edge(buffer, matrix, camera, client.world, chunkX, chunkZ, SOUTH);
					}
					if (!isChunkLoaded(client.world, chunkX - 1, chunkZ)) {
						edge(buffer, matrix, camera, client.world, chunkX, chunkZ, WEST);
					}
					if (!isChunkLoaded(client.world, chunkX + 1, chunkZ)) {
						edge(buffer, matrix, camera, client.world, chunkX, chunkZ, EAST);
					}
				}
			}
			BufferRenderer.drawWithGlobalProgram(buffer.end());
		} finally {
			RenderSystem.depthMask(depthWrite);
			RenderSystem.depthFunc(depthFunction);
			RenderSystem.blendFuncSeparate(blendSourceRgb, blendDestinationRgb, blendSourceAlpha, blendDestinationAlpha);
			setCapability(GL11.GL_BLEND, blendEnabled);
			setCapability(GL11.GL_DEPTH_TEST, depthEnabled);
			setCapability(GL11.GL_CULL_FACE, cullEnabled);
			if (previousShader != null) {
				RenderSystem.setShader(() -> previousShader);
			}
			matrices.pop();
		}
	}

	private static boolean isChunkLoaded(ClientWorld world, int chunkX, int chunkZ) {
		return world.isChunkLoaded(chunkX, chunkZ);
	}

	private static void edge(
			BufferBuilder buffer,
			Matrix4f matrix,
			Vec3d camera,
			ClientWorld world,
			int chunkX,
			int chunkZ,
			int direction
	) {
		double x0 = chunkX * 16.0;
		double z0 = chunkZ * 16.0;
		double x1 = x0 + 16.0;
		double z1 = z0 + 16.0;
		double y0 = world.getBottomY() - camera.y;
		double y1 = world.getTopY() - camera.y;
		for (int layer = 0; layer < 5; layer++) {
			double inset = layer * 3.0;
			float alpha = 0.62F - layer * 0.085F;
			float shade = 0.052F + layer * 0.012F;
			if (direction == NORTH) {
				vertical(buffer, matrix, x0 - camera.x, y0, z0 + inset - camera.z,
						x1 - camera.x, y1, z0 + inset - camera.z, shade, alpha);
			} else if (direction == SOUTH) {
				vertical(buffer, matrix, x1 - camera.x, y0, z1 - inset - camera.z,
						x0 - camera.x, y1, z1 - inset - camera.z, shade, alpha);
			} else if (direction == WEST) {
				vertical(buffer, matrix, x0 + inset - camera.x, y0, z1 - camera.z,
						x0 + inset - camera.x, y1, z0 - camera.z, shade, alpha);
			} else {
				vertical(buffer, matrix, x1 - inset - camera.x, y0, z0 - camera.z,
						x1 - inset - camera.x, y1, z1 - camera.z, shade, alpha);
			}
		}
	}

	private static void vertical(
			BufferBuilder buffer,
			Matrix4f matrix,
			double ax,
			double y0,
			double az,
			double bx,
			double y1,
			double bz,
			float shade,
			float alpha
	) {
		vertex(buffer, matrix, ax, y0, az, shade, shade + 0.006F, shade + 0.008F, alpha);
		vertex(buffer, matrix, bx, y0, bz, shade, shade + 0.006F, shade + 0.008F, alpha);
		vertex(buffer, matrix, bx, y1, bz, shade * 0.75F, shade * 0.76F, shade * 0.78F, alpha * 0.96F);
		vertex(buffer, matrix, ax, y1, az, shade * 0.75F, shade * 0.76F, shade * 0.78F, alpha * 0.96F);
	}

	private static void vertex(
			BufferBuilder buffer,
			Matrix4f matrix,
			double x,
			double y,
			double z,
			float red,
			float green,
			float blue,
			float alpha
	) {
		buffer.vertex(matrix, (float) x, (float) y, (float) z).color(red, green, blue, alpha).next();
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
