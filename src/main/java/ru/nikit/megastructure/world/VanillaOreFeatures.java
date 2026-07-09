package ru.nikit.megastructure.world;

import java.util.List;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.OrePlacedFeatures;
import net.minecraft.world.gen.feature.PlacedFeature;
import ru.nikit.megastructure.MegastructureMod;

/**
 * Reuses Minecraft's own placed ore features instead of approximating ores during chunk filling.
 * Their placement modifiers, vein sizes and height distributions stay exactly vanilla; the companion
 * {@code minecraft:stone_ore_replaceables} tag extension only makes the features recognise the
 * megastructure's smooth-stone structural mass as a valid replacement target.
 */
public final class VanillaOreFeatures {
	private static final TagKey<Biome> MEGASTRUCTURE_ORE_BIOMES = TagKey.of(
			RegistryKeys.BIOME,
			MegastructureMod.id("has_vanilla_ores")
	);

	private static final List<RegistryKey<PlacedFeature>> DEFAULT_OVERWORLD_ORES = List.of(
			OrePlacedFeatures.ORE_COAL_UPPER,
			OrePlacedFeatures.ORE_COAL_LOWER,
			OrePlacedFeatures.ORE_IRON_UPPER,
			OrePlacedFeatures.ORE_IRON_MIDDLE,
			OrePlacedFeatures.ORE_IRON_SMALL,
			OrePlacedFeatures.ORE_GOLD,
			OrePlacedFeatures.ORE_GOLD_LOWER,
			OrePlacedFeatures.ORE_REDSTONE,
			OrePlacedFeatures.ORE_REDSTONE_LOWER,
			OrePlacedFeatures.ORE_DIAMOND,
			OrePlacedFeatures.ORE_DIAMOND_LARGE,
			OrePlacedFeatures.ORE_DIAMOND_BURIED,
			OrePlacedFeatures.ORE_LAPIS,
			OrePlacedFeatures.ORE_LAPIS_BURIED,
			OrePlacedFeatures.ORE_COPPER
	);

	private VanillaOreFeatures() {
	}

	public static void register() {
		for (RegistryKey<PlacedFeature> feature : DEFAULT_OVERWORLD_ORES) {
			BiomeModifications.addFeature(
					BiomeSelectors.tag(MEGASTRUCTURE_ORE_BIOMES),
					GenerationStep.Feature.UNDERGROUND_ORES,
					feature
			);
		}
	}
}
