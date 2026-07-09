package ru.nikit.megastructure.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MossBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MossBlock.class)
public abstract class MossBlockBonemealMixin {
	private static final BlockState[] MEGASTRUCTURE_MOSS_FLOWERS = {
			Blocks.DANDELION.getDefaultState(),
			Blocks.POPPY.getDefaultState(),
			Blocks.BLUE_ORCHID.getDefaultState(),
			Blocks.ALLIUM.getDefaultState(),
			Blocks.AZURE_BLUET.getDefaultState(),
			Blocks.RED_TULIP.getDefaultState(),
			Blocks.ORANGE_TULIP.getDefaultState(),
			Blocks.WHITE_TULIP.getDefaultState(),
			Blocks.PINK_TULIP.getDefaultState(),
			Blocks.OXEYE_DAISY.getDefaultState(),
			Blocks.CORNFLOWER.getDefaultState(),
			Blocks.LILY_OF_THE_VALLEY.getDefaultState()
	};

	@Inject(method = "grow", at = @At("TAIL"))
	private void megastructure$growGrassAndFlowersOnMoss(
			ServerWorld world,
			Random random,
			BlockPos pos,
			BlockState state,
			CallbackInfo callbackInfo
	) {
		BlockPos origin = pos.up();
		for (int attempt = 0; attempt < 128; attempt++) {
			BlockPos target = origin;
			for (int walk = 0; walk < attempt / 16; walk++) {
				target = target.add(
						random.nextInt(3) - 1,
						(random.nextInt(3) - 1) * random.nextInt(3) / 2,
						random.nextInt(3) - 1
				);
				if (!world.getBlockState(target.down()).isOf(Blocks.MOSS_BLOCK)
						|| world.getBlockState(target).isFullCube(world, target)) {
					target = null;
					break;
				}
			}
			if (target == null || !world.getBlockState(target).isAir()) {
				continue;
			}

			BlockState growth = random.nextInt(8) == 0
					? MEGASTRUCTURE_MOSS_FLOWERS[random.nextInt(MEGASTRUCTURE_MOSS_FLOWERS.length)]
					: Blocks.GRASS.getDefaultState();
			if (growth.canPlaceAt(world, target)) {
				world.setBlockState(target, growth, 3);
			}
		}
	}
}
