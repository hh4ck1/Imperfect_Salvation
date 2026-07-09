package ru.nikit.megastructure.world;

import java.util.function.UnaryOperator;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

final class NeepMeatCompat {
	private static final String MOD_ID = "neepmeat";
	private static final boolean LOADED = FabricLoader.getInstance().isModLoaded(MOD_ID);

	private NeepMeatCompat() {
	}

	static BlockState block(String path, BlockState fallback) {
		return block(path, fallback, UnaryOperator.identity());
	}

	static BlockState block(String path, BlockState fallback, UnaryOperator<BlockState> transform) {
		if (!LOADED) {
			return fallback;
		}
		return Registries.BLOCK.getOrEmpty(new Identifier(MOD_ID, path))
				.map(block -> block.getDefaultState())
				.map(transform)
				.orElse(fallback);
	}
}
