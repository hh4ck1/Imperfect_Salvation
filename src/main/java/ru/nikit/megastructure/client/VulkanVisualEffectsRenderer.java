package ru.nikit.megastructure.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import ru.nikit.megastructure.world.MegastructureChunkGenerator;

final class VulkanVisualEffectsRenderer {
	private static final int TEXTURE_SIZE = 512;
	private static final ByteBuffer PIXELS = BufferUtils.createByteBuffer(TEXTURE_SIZE * TEXTURE_SIZE * 4);
	private static final boolean IRIS_PRESENT = FabricLoader.getInstance().isModLoaded("iris");
	private static int texture;
	private static NativeImageBackedTexture pipelineTexture;
	private static Identifier pipelineTextureId;
	private static boolean nativeFailureLogged;

	@FunctionalInterface
	private interface VertexWriter {
		void vertex(Matrix4f matrix, double x, double y, double z, float[] tint, float alpha, float u, float v);
	}

	private record Basis(Vec3d right, Vec3d up, Vec3d forward) {
	}

	private VulkanVisualEffectsRenderer() {
	}

	static void render(WorldRenderContext context) {
		if (IRIS_PRESENT) {
			return;
		}
		renderWorldPass(context);
	}

	static void renderIrisWorldPipeline(WorldRenderContext context) {
		if (!IRIS_PRESENT) {
			return;
		}
		renderWorldPass(context);
	}

	private static void renderWorldPass(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null
				|| client.player == null
				|| context.matrixStack() == null
				|| context.camera().getSubmersionType() != CameraSubmersionType.NONE
				|| client.getFramebuffer().textureWidth <= 0
				|| client.getFramebuffer().textureHeight <= 0) {
			return;
		}

		Vec3d camera = context.camera().getPos();
		Optional<MegastructureChunkGenerator.VulkanEffectHint> nearest =
				MegastructureChunkGenerator.findNearestVulkanEffectHint(camera.x, camera.y, camera.z, 2);
		if (nearest.isEmpty()) {
			return;
		}

		MegastructureChunkGenerator.VulkanEffectHint hint = nearest.get();
		Vec3d center = new Vec3d(hint.x() + 0.5, hint.y() + 0.5, hint.z() + 0.5);
		double distance = camera.distanceTo(center);
		double renderRange = Math.max(700.0, hint.radius() * 3.8);
		if (distance > renderRange) {
			return;
		}

		double time = client.world.getTime() + context.tickDelta();
		float proximity = clamp(1.0F - (float) (distance / renderRange), 0.0F, 1.0F);
		float intensity = clamp(0.74F + proximity * 1.15F, 0.55F, 2.0F);
		PIXELS.clear();
		boolean generated = BlackHoleNativeBridge.renderVisualField(
				PIXELS,
				TEXTURE_SIZE,
				TEXTURE_SIZE,
				hint.seed(),
				time * 0.05,
				intensity,
				hint.kind()
		);
		if (!generated) {
			if (!nativeFailureLogged) {
				nativeFailureLogged = true;
				System.err.println("Megastructure Vulkan visual-field renderer is unavailable.");
			}
			return;
		}

		if (IRIS_PRESENT) {
			uploadPipelineTexture(client);
			drawBufferedWorldEffect(context, camera, center, hint, proximity);
		} else {
			ensureTexture();
			uploadTexture();
			drawWorldEffect(context, camera, center, hint, proximity);
		}
	}

	private static void drawBufferedWorldEffect(
			WorldRenderContext context,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.VulkanEffectHint hint,
			float proximity
	) {
		if (pipelineTextureId == null || context.consumers() == null) {
			return;
		}
		Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
		VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getEntityTranslucentEmissive(pipelineTextureId));
		VertexWriter writer = (mat, x, y, z, tint, alpha, u, v) -> consumer.vertex(mat, (float) x, (float) y, (float) z)
				.color(tint[0], tint[1], tint[2], alpha)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(0.0F, 1.0F, 0.0F)
				.next();
		float alpha = 0.44F + proximity * 0.34F;
		float[] tint = shaderBoostTint(tintForKind(hint.kind()));
		drawEffectGeometry(writer, matrix, camera, center, hint, tint, alpha);
	}

	private static void drawWorldEffect(
			WorldRenderContext context,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.VulkanEffectHint hint,
			float proximity
	) {
		MatrixStack matrices = context.matrixStack();
		matrices.push();
		Matrix4f matrix = matrices.peek().getPositionMatrix();

		boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
		int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		int blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
		int blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
		int blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
		int blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
		ShaderProgram previousShader = RenderSystem.getShader();

		try {
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_LEQUAL);
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
			RenderSystem.setShaderTexture(0, texture);

			BufferBuilder buffer = Tessellator.getInstance().getBuffer();
			buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
			VertexWriter writer = (mat, x, y, z, tint, alpha, u, v) -> buffer.vertex(mat, (float) x, (float) y, (float) z)
					.color(tint[0], tint[1], tint[2], alpha)
					.texture(u, v)
					.next();
			float alpha = 0.36F + proximity * 0.28F;
			float[] tint = tintForKind(hint.kind());
			drawEffectGeometry(writer, matrix, camera, center, hint, tint, alpha);
			BufferRenderer.drawWithGlobalProgram(buffer.end());

		} finally {
			RenderSystem.depthMask(depthWrite);
			RenderSystem.depthFunc(depthFunction);
			RenderSystem.blendFuncSeparate(blendSourceRgb, blendDestinationRgb, blendSourceAlpha, blendDestinationAlpha);
			setCapability(GL11.GL_BLEND, blendEnabled);
			setCapability(GL11.GL_DEPTH_TEST, depthEnabled);
			setCapability(GL11.GL_CULL_FACE, cullEnabled);
			RenderSystem.activeTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
			RenderSystem.activeTexture(activeTexture);
			if (previousShader != null) {
				RenderSystem.setShader(() -> previousShader);
			}
			matrices.pop();
		}
	}

	private static void drawEffectGeometry(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.VulkanEffectHint hint,
			float[] tint,
			float alpha
	) {
		switch (hint.kind()) {
			case 0 -> drawRift(writer, matrix, camera, center, hint, tint, alpha);
			case 1 -> drawVoid(writer, matrix, camera, center, hint, tint, alpha);
			case 2 -> drawAtom(writer, matrix, camera, center, hint, tint, alpha);
			case 3 -> drawVascular(writer, matrix, camera, center, hint, tint, alpha);
			case 4 -> drawReservoir(writer, matrix, camera, center, hint, tint, alpha);
			case 5 -> drawFoundry(writer, matrix, camera, center, hint, tint, alpha);
			case 6 -> drawTransit(writer, matrix, camera, center, hint, tint, alpha);
			default -> drawRift(writer, matrix, camera, center, hint, tint, alpha);
		}
	}

	private static void drawRift(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.VulkanEffectHint hint,
			float[] tint,
			float alpha
	) {
		float width = hint.radius() * 1.25F;
		float height = hint.height() * 0.55F;
		Basis main = basis(
				new Vec3d(0.96, 0.0, 0.28).normalize(),
				new Vec3d(0.0, 1.0, 0.0),
				new Vec3d(-0.28, 0.0, 0.96).normalize()
		);
		Basis cross = basis(
				new Vec3d(-0.35, 0.0, 0.94).normalize(),
				new Vec3d(0.0, 1.0, 0.0),
				new Vec3d(-0.94, 0.0, -0.35).normalize()
		);
		for (int shell = 0; shell < 4; shell++) {
			float scale = 1.0F + shell * 0.18F;
			float shellAlpha = alpha * (0.24F - shell * 0.035F);
			ellipsoidShell(writer, matrix, camera, center.add(shell * 4.0, 0.0, -shell * 3.0),
					width * 0.42F * scale, height * (0.88F + shell * 0.03F), width * 0.13F * scale,
					main, 28, 12, tint, shellAlpha);
			ellipsoidShell(writer, matrix, camera, center.add(-shell * 3.0, 0.0, shell * 5.0),
					width * 0.23F * scale, height * 0.82F, width * 0.34F * scale,
					cross, 24, 10, tint, shellAlpha * 0.76F);
		}
		for (int layer = -3; layer <= 3; layer++) {
			float normalized = Math.abs(layer) / 3.0F;
			torusY(writer, matrix, camera, center.add(0.0, layer * height * 0.18F, 0.0),
					width * (0.34F + normalized * 0.08F), width * 0.035F,
					36, 8, tint, alpha * (0.10F + (1.0F - normalized) * 0.05F));
		}
	}

	private static void drawVoid(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.VulkanEffectHint hint,
			float[] tint,
			float alpha
	) {
		float radius = hint.radius();
		for (int shell = 0; shell < 5; shell++) {
			float scale = 0.72F + shell * 0.16F;
			ellipsoidShell(writer, matrix, camera, center,
					radius * scale, hint.height() * (0.22F + shell * 0.025F), radius * scale,
					worldBasis(), 32, 14, tint, alpha * (0.18F - shell * 0.022F));
		}
		torusY(writer, matrix, camera, center, radius * 1.24F, radius * 0.08F, 52, 10, tint, alpha * 0.34F);
		torusAroundAxis(writer, matrix, camera, center, new Vec3d(1.0, 0.0, 0.0),
				radius * 0.82F, radius * 0.035F, 38, 8, tint, alpha * 0.16F);
		torusAroundAxis(writer, matrix, camera, center, new Vec3d(0.0, 0.0, 1.0),
				radius * 0.82F, radius * 0.035F, 38, 8, tint, alpha * 0.14F);
	}

	private static void drawAtom(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.VulkanEffectHint hint,
			float[] tint,
			float alpha
	) {
		if (hint.volumetric()) {
			drawVolumetricGlobe(writer, matrix, camera, center, hint, tint, alpha);
			return;
		}
		float radius = hint.radius();
		ellipsoidShell(writer, matrix, camera, center, radius * 0.68F, radius * 0.68F, radius * 0.68F,
				worldBasis(), 32, 16, tint, alpha * 0.22F);
		torusY(writer, matrix, camera, center, radius * 1.15F, radius * 0.045F, 56, 8, tint, alpha * 0.58F);
		torusAroundAxis(writer, matrix, camera, center, new Vec3d(0.94, 0.34, 0.0),
				radius * 1.06F, radius * 0.038F, 50, 8, tint, alpha * 0.44F);
		torusAroundAxis(writer, matrix, camera, center, new Vec3d(-0.26, 0.82, 0.51),
				radius * 0.92F, radius * 0.034F, 44, 8, tint, alpha * 0.34F);
	}

	private static void drawVolumetricGlobe(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.VulkanEffectHint hint,
			float[] tint,
			float alpha
	) {
		float radius = hint.radius();
		float halfHeight = hint.height() * 0.40F;
		float[] warmTint = new float[]{
				Math.min(1.0F, tint[0] * 1.04F),
				Math.min(1.0F, tint[1] * 1.06F),
				Math.min(1.0F, tint[2] * 0.82F)
		};

		for (int shell = 0; shell < 4; shell++) {
			float scale = 0.86F + shell * 0.12F;
			float shellAlpha = alpha * (0.18F - shell * 0.025F);
			ellipsoidShell(writer, matrix, camera, center,
					radius * scale, halfHeight * (0.88F - shell * 0.04F), radius * scale,
					worldBasis(), 38, 18, warmTint, shellAlpha);
		}

		for (int layer = -5; layer <= 5; layer++) {
			float normalized = Math.abs(layer) / 5.0F;
			float layerRadius = radius * (0.42F + (1.0F - normalized * normalized) * 0.68F);
			float layerAlpha = alpha * (0.10F + (1.0F - normalized) * 0.05F);
			torusY(writer, matrix, camera, center.add(0.0, layer * 18.0, 0.0),
					layerRadius, radius * 0.018F, 44, 6, warmTint, layerAlpha);
		}

		torusY(writer, matrix, camera, center, radius * 1.24F, radius * 0.045F, 58, 8, tint, alpha * 0.30F);
		torusAroundAxis(writer, matrix, camera, center, new Vec3d(0.82, 0.32, 0.48),
				radius * 1.08F, radius * 0.03F, 50, 8, tint, alpha * 0.18F);
		torusAroundAxis(writer, matrix, camera, center, new Vec3d(-0.48, 0.38, 0.79),
				radius * 0.96F, radius * 0.026F, 46, 8, tint, alpha * 0.15F);
	}

	private static void drawVascular(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.VulkanEffectHint hint,
			float[] tint,
			float alpha
	) {
		float width = hint.radius() * 0.92F;
		for (int branch = -3; branch <= 3; branch++) {
			float offset = branch * 18.0F;
			Vec3d root = center.add(-width * 0.62F, -hint.height() * 0.24F, offset);
			Vec3d tip = center.add(width * 0.62F, hint.height() * 0.24F, -offset * 0.42F);
			tubeBetween(writer, matrix, camera, root, tip, width * (0.020F + Math.abs(branch) * 0.0025F),
					10, tint, alpha * (0.30F - Math.abs(branch) * 0.022F));
			tubeBetween(writer, matrix, camera,
					center.add(offset * 0.65F, -hint.height() * 0.26F, -width * 0.48F),
					center.add(-offset * 0.35F, hint.height() * 0.22F, width * 0.52F),
					width * 0.018F, 10, tint, alpha * 0.22F);
		}
		ellipsoidShell(writer, matrix, camera, center, width * 0.46F, hint.height() * 0.23F, width * 0.46F,
				worldBasis(), 28, 12, tint, alpha * 0.12F);
	}

	private static void drawReservoir(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.VulkanEffectHint hint,
			float[] tint,
			float alpha
	) {
		float radius = hint.radius();
		for (int shell = 0; shell < 4; shell++) {
			ellipsoidShell(writer, matrix, camera, center.add(0.0, shell * 10.0, 0.0),
					radius * (0.78F + shell * 0.08F), hint.height() * (0.10F + shell * 0.018F), radius * (0.66F + shell * 0.06F),
					worldBasis(), 34, 10, tint, alpha * (0.20F - shell * 0.025F));
		}
		torusY(writer, matrix, camera, center.add(0.0, hint.height() * 0.10F, 0.0),
				radius * 0.92F, radius * 0.025F, 46, 8, tint, alpha * 0.20F);
	}

	private static void drawFoundry(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.VulkanEffectHint hint,
			float[] tint,
			float alpha
	) {
		float width = hint.radius() * 1.1F;
		for (int layer = 0; layer < 5; layer++) {
			Vec3d shifted = center.add(0.0, layer * 34.0, (layer - 2) * 14.0);
			ellipsoidShell(writer, matrix, camera, shifted,
					width * (0.34F + layer * 0.035F), hint.height() * (0.12F + layer * 0.03F), width * (0.16F + layer * 0.03F),
					basis(new Vec3d(1.0, 0.0, 0.12 * layer).normalize(), new Vec3d(0.0, 1.0, 0.0), new Vec3d(-0.12 * layer, 0.0, 1.0).normalize()),
					26, 10, tint, alpha * (0.26F - layer * 0.025F));
		}
	}

	private static void drawTransit(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.VulkanEffectHint hint,
			float[] tint,
			float alpha
	) {
		float length = hint.radius() * 1.75F;
		float height = hint.height() * 0.18F;
		for (int layer = -2; layer <= 2; layer++) {
			Vec3d shifted = center.add(0.0, layer * 12.0, 0.0);
			tubeBetween(writer, matrix, camera,
					shifted.add(-length, -height * 0.22F, 0.0),
					shifted.add(length, height * 0.22F, 0.0),
					height * (0.08F + Math.abs(layer) * 0.008F), 12, tint, alpha * 0.22F);
			torusY(writer, matrix, camera, shifted, length * 0.36F, height * 0.035F, 34, 6, tint, alpha * 0.13F);
		}
	}

	private static Basis worldBasis() {
		return basis(new Vec3d(1.0, 0.0, 0.0), new Vec3d(0.0, 1.0, 0.0), new Vec3d(0.0, 0.0, 1.0));
	}

	private static Basis basis(Vec3d right, Vec3d up, Vec3d forward) {
		return new Basis(right.normalize(), up.normalize(), forward.normalize());
	}

	private static void ellipsoidShell(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			float radiusX,
			float radiusY,
			float radiusZ,
			Basis basis,
			int slices,
			int stacks,
			float[] tint,
			float alpha
	) {
		for (int stack = 0; stack < stacks; stack++) {
			double v0 = (double) stack / stacks;
			double v1 = (double) (stack + 1) / stacks;
			double theta0 = -Math.PI * 0.5 + Math.PI * v0;
			double theta1 = -Math.PI * 0.5 + Math.PI * v1;
			for (int slice = 0; slice < slices; slice++) {
				double u0 = (double) slice / slices;
				double u1 = (double) (slice + 1) / slices;
				double phi0 = Math.PI * 2.0 * u0;
				double phi1 = Math.PI * 2.0 * u1;
				Vec3d a = ellipsoidPoint(center, radiusX, radiusY, radiusZ, theta0, phi0, basis);
				Vec3d b = ellipsoidPoint(center, radiusX, radiusY, radiusZ, theta0, phi1, basis);
				Vec3d c = ellipsoidPoint(center, radiusX, radiusY, radiusZ, theta1, phi1, basis);
				Vec3d d = ellipsoidPoint(center, radiusX, radiusY, radiusZ, theta1, phi0, basis);
				quadUv(writer, matrix, camera, a, b, c, d, tint, alpha, (float) u0, (float) v0, (float) u1, (float) v1);
			}
		}
	}

	private static Vec3d ellipsoidPoint(
			Vec3d center,
			float radiusX,
			float radiusY,
			float radiusZ,
			double theta,
			double phi,
			Basis basis
	) {
		double cosTheta = Math.cos(theta);
		double localX = Math.cos(phi) * cosTheta * radiusX;
		double localY = Math.sin(theta) * radiusY;
		double localZ = Math.sin(phi) * cosTheta * radiusZ;
		return localPoint(center, basis, localX, localY, localZ);
	}

	private static void torusY(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			float majorRadius,
			float minorRadius,
			int segments,
			int tubeSegments,
			float[] tint,
			float alpha
	) {
		torusAroundAxis(writer, matrix, camera, center, new Vec3d(0.0, 1.0, 0.0), majorRadius, minorRadius, segments, tubeSegments, tint, alpha);
	}

	private static void torusAroundAxis(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			Vec3d axis,
			float majorRadius,
			float minorRadius,
			int segments,
			int tubeSegments,
			float[] tint,
			float alpha
	) {
		Vec3d normalizedAxis = axis.normalize();
		Vec3d helper = Math.abs(normalizedAxis.y) > 0.92 ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(0.0, 1.0, 0.0);
		Vec3d radialA = normalizedAxis.crossProduct(helper).normalize();
		Vec3d radialB = normalizedAxis.crossProduct(radialA).normalize();
		for (int segment = 0; segment < segments; segment++) {
			double u0 = (double) segment / segments;
			double u1 = (double) (segment + 1) / segments;
			double angle0 = Math.PI * 2.0 * u0;
			double angle1 = Math.PI * 2.0 * u1;
			Vec3d ring0 = radialA.multiply(Math.cos(angle0)).add(radialB.multiply(Math.sin(angle0)));
			Vec3d ring1 = radialA.multiply(Math.cos(angle1)).add(radialB.multiply(Math.sin(angle1)));
			Vec3d tubeNormal0 = normalizedAxis.crossProduct(ring0).normalize();
			Vec3d tubeNormal1 = normalizedAxis.crossProduct(ring1).normalize();
			Vec3d center0 = center.add(ring0.multiply(majorRadius));
			Vec3d center1 = center.add(ring1.multiply(majorRadius));
			for (int tube = 0; tube < tubeSegments; tube++) {
				double v0 = (double) tube / tubeSegments;
				double v1 = (double) (tube + 1) / tubeSegments;
				double tubeAngle0 = Math.PI * 2.0 * v0;
				double tubeAngle1 = Math.PI * 2.0 * v1;
				Vec3d a = center0.add(ring0.multiply(Math.cos(tubeAngle0) * minorRadius)).add(tubeNormal0.multiply(Math.sin(tubeAngle0) * minorRadius));
				Vec3d b = center1.add(ring1.multiply(Math.cos(tubeAngle0) * minorRadius)).add(tubeNormal1.multiply(Math.sin(tubeAngle0) * minorRadius));
				Vec3d c = center1.add(ring1.multiply(Math.cos(tubeAngle1) * minorRadius)).add(tubeNormal1.multiply(Math.sin(tubeAngle1) * minorRadius));
				Vec3d d = center0.add(ring0.multiply(Math.cos(tubeAngle1) * minorRadius)).add(tubeNormal0.multiply(Math.sin(tubeAngle1) * minorRadius));
				quadUv(writer, matrix, camera, a, b, c, d, tint, alpha, (float) u0, (float) v0, (float) u1, (float) v1);
			}
		}
	}

	private static void tubeBetween(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d start,
			Vec3d end,
			float radius,
			int segments,
			float[] tint,
			float alpha
	) {
		Vec3d axis = end.subtract(start).normalize();
		Vec3d helper = Math.abs(axis.y) > 0.92 ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(0.0, 1.0, 0.0);
		Vec3d side = axis.crossProduct(helper).normalize();
		Vec3d binormal = axis.crossProduct(side).normalize();
		for (int segment = 0; segment < segments; segment++) {
			double u0 = (double) segment / segments;
			double u1 = (double) (segment + 1) / segments;
			double angle0 = Math.PI * 2.0 * u0;
			double angle1 = Math.PI * 2.0 * u1;
			Vec3d offset0 = side.multiply(Math.cos(angle0) * radius).add(binormal.multiply(Math.sin(angle0) * radius));
			Vec3d offset1 = side.multiply(Math.cos(angle1) * radius).add(binormal.multiply(Math.sin(angle1) * radius));
			quadUv(writer, matrix, camera,
					start.add(offset0), end.add(offset0), end.add(offset1), start.add(offset1),
					tint, alpha, (float) u0, 1.0F, (float) u1, 0.0F);
		}
	}

	private static Vec3d localPoint(Vec3d center, Basis basis, double x, double y, double z) {
		return center
				.add(basis.right.multiply(x))
				.add(basis.up.multiply(y))
				.add(basis.forward.multiply(z));
	}

	private static void quadUv(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d a,
			Vec3d b,
			Vec3d c,
			Vec3d d,
			float[] tint,
			float alpha,
			float u0,
			float v0,
			float u1,
			float v1
	) {
		writer.vertex(matrix, a.x - camera.x, a.y - camera.y, a.z - camera.z, tint, alpha, u0, v1);
		writer.vertex(matrix, b.x - camera.x, b.y - camera.y, b.z - camera.z, tint, alpha, u1, v1);
		writer.vertex(matrix, c.x - camera.x, c.y - camera.y, c.z - camera.z, tint, alpha, u1, v0);
		writer.vertex(matrix, d.x - camera.x, d.y - camera.y, d.z - camera.z, tint, alpha, u0, v0);
	}

	private static void verticalX(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			float halfWidth,
			float halfHeight,
			float[] tint,
			float alpha
	) {
		double x0 = center.x - halfWidth - camera.x;
		double x1 = center.x + halfWidth - camera.x;
		double y0 = center.y - halfHeight - camera.y;
		double y1 = center.y + halfHeight - camera.y;
		double z = center.z - camera.z;
		quad(writer, matrix, x0, y0, z, x1, y0, z, x1, y1, z, x0, y1, z, tint, alpha);
	}

	private static void verticalZ(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			float halfWidth,
			float halfHeight,
			float[] tint,
			float alpha
	) {
		double x = center.x - camera.x;
		double y0 = center.y - halfHeight - camera.y;
		double y1 = center.y + halfHeight - camera.y;
		double z0 = center.z - halfWidth - camera.z;
		double z1 = center.z + halfWidth - camera.z;
		quad(writer, matrix, x, y0, z1, x, y0, z0, x, y1, z0, x, y1, z1, tint, alpha);
	}

	private static void verticalDiagonal(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			double directionX,
			double directionZ,
			float halfWidth,
			float halfHeight,
			float[] tint,
			float alpha
	) {
		double x0 = center.x - directionX * halfWidth - camera.x;
		double x1 = center.x + directionX * halfWidth - camera.x;
		double y0 = center.y - halfHeight - camera.y;
		double y1 = center.y + halfHeight - camera.y;
		double z0 = center.z - directionZ * halfWidth - camera.z;
		double z1 = center.z + directionZ * halfWidth - camera.z;
		quad(writer, matrix, x0, y0, z0, x1, y0, z1, x1, y1, z1, x0, y1, z0, tint, alpha);
	}

	private static void horizontal(
			VertexWriter writer,
			Matrix4f matrix,
			Vec3d camera,
			Vec3d center,
			float halfSize,
			float[] tint,
			float alpha
	) {
		double x0 = center.x - halfSize - camera.x;
		double x1 = center.x + halfSize - camera.x;
		double y = center.y - camera.y;
		double z0 = center.z - halfSize - camera.z;
		double z1 = center.z + halfSize - camera.z;
		quad(writer, matrix, x0, y, z0, x1, y, z0, x1, y, z1, x0, y, z1, tint, alpha);
	}

	private static void quad(
			VertexWriter writer,
			Matrix4f matrix,
			double ax,
			double ay,
			double az,
			double bx,
			double by,
			double bz,
			double cx,
			double cy,
			double cz,
			double dx,
			double dy,
			double dz,
			float[] tint,
			float alpha
	) {
		writer.vertex(matrix, ax, ay, az, tint, alpha, 0.0F, 1.0F);
		writer.vertex(matrix, bx, by, bz, tint, alpha, 1.0F, 1.0F);
		writer.vertex(matrix, cx, cy, cz, tint, alpha, 1.0F, 0.0F);
		writer.vertex(matrix, dx, dy, dz, tint, alpha, 0.0F, 0.0F);
	}

	private static float[] tintForKind(int kind) {
		return switch (kind) {
			case 1 -> new float[]{0.45F, 0.62F, 1.0F};
			case 2 -> new float[]{1.0F, 0.58F, 0.18F};
			case 3 -> new float[]{1.0F, 0.20F, 0.13F};
			case 4 -> new float[]{0.34F, 0.98F, 0.86F};
			case 5 -> new float[]{1.0F, 0.38F, 0.12F};
			case 6 -> new float[]{0.42F, 0.76F, 1.0F};
			default -> new float[]{0.32F, 0.82F, 1.0F};
		};
	}

	private static float[] shaderBoostTint(float[] tint) {
		float lift = 0.18F;
		return new float[]{
				Math.min(1.0F, tint[0] * 1.18F + lift),
				Math.min(1.0F, tint[1] * 1.18F + lift),
				Math.min(1.0F, tint[2] * 1.18F + lift)
		};
	}

	private static void uploadPipelineTexture(MinecraftClient client) {
		if (pipelineTexture == null || pipelineTextureId == null) {
			pipelineTexture = new NativeImageBackedTexture(TEXTURE_SIZE, TEXTURE_SIZE, false);
			pipelineTextureId = client.getTextureManager().registerDynamicTexture(
					"megastructure_vulkan_visual_field",
					pipelineTexture
			);
		}
		NativeImage image = pipelineTexture.getImage();
		if (image == null) {
			return;
		}
		PIXELS.rewind();
		for (int y = 0; y < TEXTURE_SIZE; y++) {
			for (int x = 0; x < TEXTURE_SIZE; x++) {
				int r = PIXELS.get() & 0xFF;
				int g = PIXELS.get() & 0xFF;
				int b = PIXELS.get() & 0xFF;
				int a = PIXELS.get() & 0xFF;
				image.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r);
			}
		}
		pipelineTexture.upload();
	}

	private static void ensureTexture() {
		if (texture != 0) {
			return;
		}
		int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		texture = GL11.glGenTextures();
		RenderSystem.activeTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		PIXELS.clear();
		GL11.glTexImage2D(
				GL11.GL_TEXTURE_2D,
				0,
				GL11.GL_RGBA8,
				TEXTURE_SIZE,
				TEXTURE_SIZE,
				0,
				GL11.GL_RGBA,
				GL11.GL_UNSIGNED_BYTE,
				PIXELS
		);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
		RenderSystem.activeTexture(activeTexture);
	}

	private static void uploadTexture() {
		int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		int unpackAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
		int unpackRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
		int unpackSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
		int unpackSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
		PIXELS.clear();
		RenderSystem.activeTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
		GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
		GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
		GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
		GL11.glTexSubImage2D(
				GL11.GL_TEXTURE_2D,
				0,
				0,
				0,
				TEXTURE_SIZE,
				TEXTURE_SIZE,
				GL11.GL_RGBA,
				GL11.GL_UNSIGNED_BYTE,
				PIXELS
		);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, unpackAlignment);
		GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, unpackRowLength);
		GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);
		GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, unpackSkipRows);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
		RenderSystem.activeTexture(activeTexture);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
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
