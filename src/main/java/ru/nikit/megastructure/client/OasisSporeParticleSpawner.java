package ru.nikit.megastructure.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import ru.nikit.megastructure.world.MegastructureChunkGenerator;

import java.util.Optional;

final class OasisSporeParticleSpawner {
	private static final int MAXIMUM_SCORE = 180;
	private static long lastScanTick = Long.MIN_VALUE;
	private static float targetStrength;
	private static float strength;

	private OasisSporeParticleSpawner() {
	}

	static void tick(MinecraftClient client) {
		if (client.world == null || client.player == null || client.isPaused()) {
			strength *= 0.86F;
			return;
		}

		updateStrength(client.world, client.player);
		strength += (targetStrength - strength) * 0.28F;
		if (strength < 0.018F) {
			return;
		}

		Random random = client.player.getRandom();
		int count = 34 + Math.round(strength * 112.0F);
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double radius = 1.5 + random.nextDouble() * 32.0;
			double x = client.player.getX() + Math.cos(angle) * radius;
			double y = client.player.getEyeY() - 4.8 + random.nextDouble() * 18.5;
			double z = client.player.getZ() + Math.sin(angle) * radius;
			double velocityX = (random.nextDouble() - 0.5) * 0.026;
			double velocityY = 0.010 + random.nextDouble() * 0.040;
			double velocityZ = (random.nextDouble() - 0.5) * 0.026;
			client.world.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, velocityX, velocityY, velocityZ);
		}
	}

	private static void updateStrength(ClientWorld world, PlayerEntity player) {
		long tick = world.getTime();
		if (tick - lastScanTick < 5) {
			return;
		}
		lastScanTick = tick;

		BlockPos origin = player.getBlockPos();
		int score = 0;
		score += scan(world, origin, 12, 5, 8, 2, 1, MAXIMUM_SCORE);
		if (score < MAXIMUM_SCORE) {
			score += scan(world, origin, 42, 16, 28, 4, 3, MAXIMUM_SCORE - score);
		}
		if (score < MAXIMUM_SCORE) {
			score += scan(world, origin, 92, 72, 30, 8, 5, MAXIMUM_SCORE - score);
		}
		float blockStrength = score < 3 ? 0.0F : Math.min(1.0F, Math.max(0.72F, score / 72.0F));
		float hintStrength = 0.0F;
		Optional<MegastructureChunkGenerator.OasisRenderHint> nearest =
				MegastructureChunkGenerator.findNearestOasisRenderHint(player.getX(), player.getY(), player.getZ(), 3);
		if (nearest.isPresent()) {
			MegastructureChunkGenerator.OasisRenderHint hint = nearest.get();
			double dx = player.getX() - hint.basinX();
			double dz = player.getZ() - hint.basinZ();
			double horizontal = Math.sqrt(dx * dx + dz * dz);
			double vertical = Math.abs(player.getY() - hint.basinY());
			float nearFade = 1.0F - Math.max(
					(float) (horizontal / Math.max(hint.horizontalRadius() * 1.8, 1.0)),
					(float) (vertical / Math.max(hint.verticalRadius() * 1.8, 1.0))
			);
			if (nearFade > 0.0F) {
				hintStrength = Math.max(hintStrength, Math.min(0.48F, nearFade * 0.72F));
			}
			if (horizontal <= hint.horizontalRadius() * 1.35 && vertical <= hint.verticalRadius() * 1.35) {
				hintStrength = Math.max(hintStrength, 0.94F);
			}
		}
		targetStrength = Math.max(blockStrength, hintStrength);
	}

	private static int scan(
			ClientWorld world,
			BlockPos origin,
			int horizontalRadius,
			int below,
			int above,
			int horizontalStep,
			int verticalStep,
			int maximumScore
	) {
		int score = 0;
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		for (int dx = -horizontalRadius; dx <= horizontalRadius && score < maximumScore; dx += horizontalStep) {
			for (int dz = -horizontalRadius; dz <= horizontalRadius && score < maximumScore; dz += horizontalStep) {
				for (int dy = -below; dy <= above && score < maximumScore; dy += verticalStep) {
					mutable.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
					score += oasisScore(world.getBlockState(mutable));
				}
			}
		}
		return Math.min(score, maximumScore);
	}

	private static int oasisScore(BlockState state) {
		if (state.isOf(Blocks.MOSS_BLOCK) || state.isOf(Blocks.MOSS_CARPET)) {
			return 24;
		}
		if (state.isOf(Blocks.VINE)
				|| state.isOf(Blocks.AZALEA_LEAVES)
				|| state.isOf(Blocks.OAK_LEAVES)
				|| state.isOf(Blocks.BIRCH_LEAVES)
				|| state.isOf(Blocks.AZALEA)
				|| state.isOf(Blocks.FLOWERING_AZALEA)) {
			return 18;
		}
		if (state.isOf(Blocks.OAK_LOG)
				|| state.isOf(Blocks.BIRCH_LOG)
				|| state.isOf(Blocks.OAK_WOOD)
				|| state.isOf(Blocks.ROOTED_DIRT)
				|| state.isOf(Blocks.CLAY)) {
			return 12;
		}
		if (state.isOf(Blocks.WATER)) {
			return 7;
		}
		Identifier id = Registries.BLOCK.getId(state.getBlock());
		if ("neepmeat".equals(id.getNamespace())) {
			String path = id.getPath();
			if (path.contains("blood_bubble") || path.contains("contaminated")) {
				return 22;
			}
			if (path.contains("meat") || path.contains("vascular")) {
				return 14;
			}
		}
		return 0;
	}
}
