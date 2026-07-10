package ru.nikit.megastructure.survival;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class MegastructureMobSystem {
	private MegastructureMobSystem() {
	}

	public static void register() {
		registerVanillaSpawns();
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

	private static void registerVanillaSpawns() {
		BiomeModifications.addSpawn(MegastructureMobSystem::isMegastructureBiome, SpawnGroup.MONSTER, EntityType.SPIDER, 100, 4, 4);
		BiomeModifications.addSpawn(MegastructureMobSystem::isMegastructureBiome, SpawnGroup.MONSTER, EntityType.ZOMBIE, 95, 4, 4);
		BiomeModifications.addSpawn(MegastructureMobSystem::isMegastructureBiome, SpawnGroup.MONSTER, EntityType.SKELETON, 100, 4, 4);
		BiomeModifications.addSpawn(MegastructureMobSystem::isMegastructureBiome, SpawnGroup.MONSTER, EntityType.CREEPER, 100, 4, 4);
	}

	private static boolean isMegastructureBiome(BiomeSelectionContext context) {
		return "megastructure".equals(context.getBiomeKey().getValue().getNamespace());
	}
}
