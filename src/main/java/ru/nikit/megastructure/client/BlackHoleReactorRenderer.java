package ru.nikit.megastructure.client;

import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.nikit.megastructure.world.MegastructureChunkGenerator;

final class BlackHoleReactorRenderer {
	private static boolean nativeFailureLogged;

	private BlackHoleReactorRenderer() {
	}

	static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null
				|| client.player == null
				|| context.matrixStack() == null
				|| context.camera().getSubmersionType() != CameraSubmersionType.NONE) {
			return;
		}

		Vec3d camera = context.camera().getPos();
		Optional<MegastructureChunkGenerator.BlackHoleCoreHint> nearest =
				MegastructureChunkGenerator.findNearestBlackHoleCore(camera.x, camera.y, camera.z, 8);
		if (nearest.isEmpty()) {
			return;
		}

		MegastructureChunkGenerator.BlackHoleCoreHint core = nearest.get();
		Vec3d center = new Vec3d(core.x() + 0.5, core.y() + 0.5, core.z() + 0.5);
		double distance = camera.distanceTo(center);
		double renderRange = Math.max(1800.0, core.influenceRadius() * 6.0);
		if (distance > renderRange) {
			return;
		}
		if (!hasVisibleCoreSample(client, context, camera, center, core, distance)) {
			return;
		}

		double time = client.world.getTime() + context.tickDelta();
		float influence = 1.0F - clamp((float) (distance / Math.max(core.influenceRadius(), 1.0F)), 0.0F, 1.0F);
		float inside = 1.0F - clamp((float) (distance / (core.eventHorizonRadius() * 2.55F)), 0.0F, 1.0F);
		float intensity = clamp(0.62F + influence * 0.92F + inside * 1.35F, 0.34F, 2.35F);
		float slowPulse = smoothUnitNoise(core.seed(), time * 0.085, 991);
		float fastPulse = smoothUnitNoise(core.seed(), time * 0.31, 139);
		float fluctuation = clamp(0.30F + slowPulse * 0.44F + fastPulse * 0.18F + inside * 0.16F, 0.24F, 1.18F);

		boolean rendered = BlackHoleNativeBridge.render(context, center, core, time, intensity, fluctuation, inside);
		if (!rendered && !nativeFailureLogged) {
			nativeFailureLogged = true;
			System.err.println("Megastructure black-hole Vulkan renderer is unavailable; no OpenGL fallback is used.");
		}
	}

	private static boolean hasVisibleCoreSample(
			MinecraftClient client,
			WorldRenderContext context,
			Vec3d camera,
			Vec3d center,
			MegastructureChunkGenerator.BlackHoleCoreHint core,
			double distance
	) {
		if (client.world == null) {
			return false;
		}
		double horizon = Math.max(core.eventHorizonRadius(), 1.0);
		if (distance <= horizon * 2.75) {
			return true;
		}

		Vec3d right = new Vec3d(1.0, 0.0, 0.0).rotateX(context.camera().getPitch() * ((float) Math.PI / 180.0F))
				.rotateY(-context.camera().getYaw() * ((float) Math.PI / 180.0F));
		Vec3d up = new Vec3d(0.0, 1.0, 0.0).rotateX(context.camera().getPitch() * ((float) Math.PI / 180.0F))
				.rotateY(-context.camera().getYaw() * ((float) Math.PI / 180.0F));
		double shadow = horizon * 2.60;
		double disk = horizon * 5.55;
		Vec3d[] samples = {
				center,
				center.add(right.multiply(shadow)),
				center.subtract(right.multiply(shadow)),
				center.add(up.multiply(shadow)),
				center.subtract(up.multiply(shadow)),
				center.add(disk, 0.0, 0.0),
				center.add(-disk, 0.0, 0.0),
				center.add(0.0, 0.0, disk),
				center.add(0.0, 0.0, -disk)
		};
		for (Vec3d sample : samples) {
			if (hasLineOfSight(client, camera, sample)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasLineOfSight(MinecraftClient client, Vec3d camera, Vec3d target) {
		HitResult hit = client.world.raycast(new RaycastContext(
				camera,
				target,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				client.player
		));
		return hit.getType() == HitResult.Type.MISS || hit.getPos().squaredDistanceTo(target) < 1.44;
	}

	private static float smoothUnitNoise(long seed, double time, int salt) {
		double base = Math.floor(time);
		float t = (float) (time - base);
		t = t * t * (3.0F - 2.0F * t);
		float a = unitNoise(seed, base, salt);
		float b = unitNoise(seed, base + 1.0, salt);
		return a + (b - a) * t;
	}

	private static float unitNoise(long seed, double timeBucket, int salt) {
		long bits = Double.doubleToLongBits(timeBucket);
		long value = seed ^ bits ^ (long) salt * 0x9E3779B97F4A7C15L;
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		value ^= value >>> 31;
		return (value >>> 40) / (float) (1L << 24);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
