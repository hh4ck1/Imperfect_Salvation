package ru.nikit.megastructure.traversal;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class RopeBlock extends Block {
	private static final VoxelShape SHAPE = Block.createCuboidShape(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);

	RopeBlock(Settings settings) {
		super(settings);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public ActionResult onUse(
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			BlockHitResult hit
	) {
		BlockPos anchor = GrapplingRopeItem.findAnchor(world, pos);
		return anchor == null ? ActionResult.PASS : GrapplingRopeItem.interactWithAnchor(world, anchor, player, hand);
	}

	@Override
	public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient) {
			BlockPos anchor = GrapplingRopeItem.findAnchor(world, pos);
			if (anchor != null) {
				int segments = GrapplingRopeItem.removeRopeBelow(world, anchor);
				world.setBlockState(anchor, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
				if (!player.isCreative()) {
					GrapplingRopeItem.giveModules(
							player,
							Math.max(1, (segments + GrapplingRopeItem.MODULE_LENGTH - 1)
									/ GrapplingRopeItem.MODULE_LENGTH)
					);
				}
			}
		}
		super.onBreak(world, pos, state, player);
	}
}
