package ru.nikit.megastructure.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MegastructureSettings(
	int seaLevel,
	int floorY,
	int ceilingY,
	int spawnPlatformY,
	int primaryRiftWidth,
	int riftMinWidth,
	int riftMaxWidth,
	int cellSize,
	int motifCellSize,
	int oreRate
) {
	public static final MegastructureSettings DEFAULT = new MegastructureSettings(40, -384, 767, 96, 80, 20, 100, 96, 768, 42);

	public static final Codec<MegastructureSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("sea_level", DEFAULT.seaLevel()).forGetter(MegastructureSettings::seaLevel),
			Codec.INT.optionalFieldOf("floor_y", DEFAULT.floorY()).forGetter(MegastructureSettings::floorY),
			Codec.INT.optionalFieldOf("ceiling_y", DEFAULT.ceilingY()).forGetter(MegastructureSettings::ceilingY),
			Codec.INT.optionalFieldOf("spawn_platform_y", DEFAULT.spawnPlatformY()).forGetter(MegastructureSettings::spawnPlatformY),
			Codec.INT.optionalFieldOf("primary_rift_width", DEFAULT.primaryRiftWidth()).forGetter(MegastructureSettings::primaryRiftWidth),
			Codec.INT.optionalFieldOf("rift_min_width", DEFAULT.riftMinWidth()).forGetter(MegastructureSettings::riftMinWidth),
			Codec.INT.optionalFieldOf("rift_max_width", DEFAULT.riftMaxWidth()).forGetter(MegastructureSettings::riftMaxWidth),
			Codec.INT.optionalFieldOf("cell_size", DEFAULT.cellSize()).forGetter(MegastructureSettings::cellSize),
			Codec.INT.optionalFieldOf("motif_cell_size", DEFAULT.motifCellSize()).forGetter(MegastructureSettings::motifCellSize),
			Codec.INT.optionalFieldOf("ore_rate", DEFAULT.oreRate()).forGetter(MegastructureSettings::oreRate)
	).apply(instance, MegastructureSettings::new));
}
