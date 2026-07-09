package ru.nikit.megastructure.survival;

import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import ru.nikit.megastructure.world.MegastructureChunkGenerator;

public final class MegastructureMobSystem {
	private static final int SPAWN_INTERVAL_TICKS = 80;
	private static final float MEGASTRUCTURE_SPAWN_RATE = 0.15F;
	private static final int LOCAL_MOB_CAP = 18;
	private static final int MIN_SPAWN_DISTANCE = 18;
	private static final int MAX_SPAWN_DISTANCE = 56;
	private static final EntityType<?>[] HOSTILES = {
			EntityType.ZOMBIE,
			EntityType.SKELETON,
			EntityType.SPIDER,
			EntityType.CREEPER
	};

	private MegastructureMobSystem() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(MegastructureMobSystem::tickWorld);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity.getWorld().isClient) {
				return;
			}
			if (entity.getType() == EntityType.SPIDER || entity.getType() == EntityType.CAVE_SPIDER) {
				entity.dropStack(new ItemStack(Items.ROTTEN_FLESH), 0.0F);
			} else if (entity.getType() == EntityType.CREEPER) {
				entity.dropStack(new ItemStack(PrimitiveSurvivalContent.EDIBLE_MOSS), 0.0F);
			}
		});
	}

	private static void tickWorld(ServerWorld world) {
		if (world.getDifficulty() == Difficulty.PEACEFUL
				|| world.getTime() % SPAWN_INTERVAL_TICKS != 0L
				|| !(world.getChunkManager().getChunkGenerator() instanceof MegastructureChunkGenerator)) {
			return;
		}
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.isSpectator()) {
				continue;
			}
			Random random = world.getRandom();
			if (random.nextFloat() >= MEGASTRUCTURE_SPAWN_RATE || countNearbyMobs(world, player) >= LOCAL_MOB_CAP) {
				continue;
			}
			trySpawnNearPlayer(world, player, random);
		}
	}

	private static int countNearbyMobs(ServerWorld world, ServerPlayerEntity player) {
		Box box = player.getBoundingBox().expand(64.0D);
		List<Entity> mobs = world.getOtherEntities(null, box, entity -> entity instanceof MobEntity);
		return mobs.size();
	}

	private static void trySpawnNearPlayer(ServerWorld world, ServerPlayerEntity player, Random random) {
		for (int attempt = 0; attempt < 12; attempt++) {
			BlockPos origin = player.getBlockPos().add(
					randomSignedRange(random, MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE),
					random.nextInt(19) - 10,
					randomSignedRange(random, MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE)
			);
			BlockPos spawnPos = findSpawnPos(world, origin);
			if (spawnPos == null || spawnPos.getSquaredDistance(player.getBlockPos()) < MIN_SPAWN_DISTANCE * MIN_SPAWN_DISTANCE) {
				continue;
			}
			Entity entity = HOSTILES[random.nextInt(HOSTILES.length)].create(world);
			if (!(entity instanceof MobEntity mob)) {
				continue;
			}
			mob.refreshPositionAndAngles(
					spawnPos.getX() + 0.5D,
					spawnPos.getY(),
					spawnPos.getZ() + 0.5D,
					random.nextFloat() * 360.0F,
					0.0F
			);
			if (!world.isSpaceEmpty(mob)) {
				mob.discard();
				continue;
			}
			mob.initialize(world, world.getLocalDifficulty(spawnPos), SpawnReason.NATURAL, null, null);
			world.spawnEntity(mob);
			return;
		}
	}

	private static int randomSignedRange(Random random, int min, int max) {
		int value = min + random.nextInt(max - min + 1);
		return random.nextBoolean() ? value : -value;
	}

	private static BlockPos findSpawnPos(ServerWorld world, BlockPos origin) {
		for (int offset = 8; offset >= -12; offset--) {
			BlockPos pos = origin.add(0, offset, 0);
			if (canSpawnAt(world, pos)) {
				return pos;
			}
		}
		return null;
	}

	private static boolean canSpawnAt(ServerWorld world, BlockPos pos) {
		if (pos.getY() <= world.getBottomY() || pos.getY() >= world.getTopY() - 2) {
			return false;
		}
		BlockPos floor = pos.down();
		BlockState floorState = world.getBlockState(floor);
		return floorState.isSolidBlock(world, floor)
				&& world.getBlockState(pos).isAir()
				&& world.getBlockState(pos.up()).isAir();
	}
}
