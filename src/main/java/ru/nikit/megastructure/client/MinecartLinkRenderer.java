package ru.nikit.megastructure.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import ru.nikit.megastructure.traversal.LinkedMinecart;

final class MinecartLinkRenderer {
	private static final double RENDER_RADIUS = 96.0;
	private static final int SEGMENTS = 10;

	private MinecartLinkRenderer() {
	}

	static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || context.matrixStack() == null) {
			return;
		}
		Vec3d camera = context.camera().getPos();
		List<AbstractMinecartEntity> carts = client.world.getEntitiesByClass(
				AbstractMinecartEntity.class,
				new Box(camera, camera).expand(RENDER_RADIUS),
				cart -> !((LinkedMinecart) cart).megastructure$getLinks().isEmpty()
		);
		if (carts.isEmpty()) {
			return;
		}
		Map<UUID, AbstractMinecartEntity> byUuid = new HashMap<>();
		for (AbstractMinecartEntity cart : carts) {
			byUuid.put(cart.getUuid(), cart);
		}

		MatrixStack matrices = context.matrixStack();
		matrices.push();
		Matrix4f matrix = matrices.peek().getPositionMatrix();
		boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		int blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
		int blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
		int blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
		int blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
		ShaderProgram previousShader = RenderSystem.getShader();
		try {
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			RenderSystem.setShader(GameRenderer::getPositionColorProgram);
			BufferBuilder buffer = Tessellator.getInstance().getBuffer();
			buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
			int links = 0;
			for (AbstractMinecartEntity cart : carts) {
				for (UUID uuid : ((LinkedMinecart) cart).megastructure$getLinks()) {
					if (cart.getUuid().compareTo(uuid) >= 0) {
						continue;
					}
					AbstractMinecartEntity partner = byUuid.get(uuid);
					if (partner == null) {
						continue;
					}
					drawLink(buffer, matrix, camera, cart.getPos(), partner.getPos());
					links++;
				}
			}
			if (links > 0) {
				BufferRenderer.drawWithGlobalProgram(buffer.end());
			} else {
				buffer.end();
			}
		} finally {
			RenderSystem.depthMask(depthWrite);
			RenderSystem.blendFuncSeparate(
					blendSourceRgb, blendDestinationRgb, blendSourceAlpha, blendDestinationAlpha);
			setCapability(GL11.GL_BLEND, blendEnabled);
			setCapability(GL11.GL_DEPTH_TEST, depthEnabled);
			setCapability(GL11.GL_CULL_FACE, cullEnabled);
			if (previousShader != null) {
				RenderSystem.setShader(() -> previousShader);
			}
			matrices.pop();
		}
	}

	private static void drawLink(
			BufferBuilder buffer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d first,
			Vec3d second
	) {
		Vec3d start = first.add(0.0, 0.62, 0.0);
		Vec3d end = second.add(0.0, 0.62, 0.0);
		double distance = start.distanceTo(end);
		double sag = Math.min(0.42, 0.10 + distance * 0.035);
		for (int segment = 0; segment < SEGMENTS; segment++) {
			double firstT = segment / (double) SEGMENTS;
			double secondT = (segment + 1) / (double) SEGMENTS;
			Vec3d a = ropePoint(start, end, firstT, sag);
			Vec3d b = ropePoint(start, end, secondT, sag);
			Vec3d tangent = b.subtract(a);
			Vec3d midpoint = a.add(b).multiply(0.5);
			Vec3d facing = camera.subtract(midpoint);
			Vec3d side = tangent.crossProduct(facing);
			if (side.lengthSquared() < 1.0E-5) {
				side = new Vec3d(0.0, 1.0, 0.0);
			} else {
				side = side.normalize();
			}
			drawRibbon(buffer, matrix, camera, a, b, side.multiply(0.032), 0.19F, 0.12F, 0.07F, 0.95F);
			Vec3d offset = side.multiply(0.016);
			drawRibbon(buffer, matrix, camera, a.add(offset), b.add(offset), side.multiply(0.010),
					0.48F, 0.31F, 0.15F, 0.82F);
		}
	}

	private static Vec3d ropePoint(Vec3d start, Vec3d end, double t, double sag) {
		return start.lerp(end, t).add(0.0, -Math.sin(Math.PI * t) * sag, 0.0);
	}

	private static void drawRibbon(
			BufferBuilder buffer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d a,
			Vec3d b,
			Vec3d side,
			float red,
			float green,
			float blue,
			float alpha
	) {
		vertex(buffer, matrix, a.add(side).subtract(camera), red, green, blue, alpha);
		vertex(buffer, matrix, b.add(side).subtract(camera), red, green, blue, alpha);
		vertex(buffer, matrix, b.subtract(side).subtract(camera), red, green, blue, alpha);
		vertex(buffer, matrix, a.subtract(side).subtract(camera), red, green, blue, alpha);
	}

	private static void vertex(
			BufferBuilder buffer,
			Matrix4f matrix,
			Vec3d position,
			float red,
			float green,
			float blue,
			float alpha
	) {
		buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
				.color(red, green, blue, alpha).next();
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
