package ru.nikit.megastructure.world;

import net.minecraft.util.math.BlockPos;

public record OasisLocation(BlockPos pos, String host, String profile, int distance) {
}
