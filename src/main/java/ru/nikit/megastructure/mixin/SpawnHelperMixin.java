package ru.nikit.megastructure.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.nikit.megastructure.world.MegastructureChunkGenerator;

@Mixin(SpawnHelper.class)
public abstract class SpawnHelperMixin {
	private static final float MEGASTRUCTURE_MONSTER_SPAWN_RATE = 0.25F;
	private static final int MEGASTRUCTURE_MONSTER_MOB_LIMIT = 50;

	@Inject(
			method = "spawnEntitiesInChunk(Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/SpawnHelper$Checker;Lnet/minecraft/world/SpawnHelper$Runner;)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private static void megastructure$reduceVanillaMonsterSpawns(
			SpawnGroup group,
			ServerWorld world,
			WorldChunk chunk,
			SpawnHelper.Checker checker,
			SpawnHelper.Runner runner,
			CallbackInfo ci
	) {
		if (group != SpawnGroup.MONSTER
				|| !(world.getChunkManager().getChunkGenerator() instanceof MegastructureChunkGenerator)) {
			return;
		}
		if (hasReachedMegastructureMonsterLimit(world)
				|| world.getRandom().nextFloat() >= MEGASTRUCTURE_MONSTER_SPAWN_RATE) {
			ci.cancel();
		}
	}

	private static boolean hasReachedMegastructureMonsterLimit(ServerWorld world) {
		int monsters = 0;
		for (Entity entity : world.iterateEntities()) {
			if (entity.isAlive() && entity.getType().getSpawnGroup() == SpawnGroup.MONSTER) {
				monsters++;
				if (monsters >= MEGASTRUCTURE_MONSTER_MOB_LIMIT) {
					return true;
				}
			}
		}
		return false;
	}
}
