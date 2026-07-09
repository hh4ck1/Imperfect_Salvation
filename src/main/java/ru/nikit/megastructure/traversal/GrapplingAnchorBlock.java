package ru.nikit.megastructure.traversal;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class GrapplingAnchorBlock extends Block {
	public static final DirectionProperty FACING = Properties.FACING;
	private static final VoxelShape NORTH_SHAPE = VoxelShapes.union(
			Block.createCuboidShape(3.0, 3.0, 0.0, 13.0, 13.0, 3.0),
			Block.createCuboidShape(1.0, 6.0, 3.0, 15.0, 10.0, 8.0),
			Block.createCuboidShape(6.0, 1.0, 3.0, 10.0, 15.0, 8.0),
			Block.createCuboidShape(7.0, 7.0, 0.0, 9.0, 9.0, 16.0)
	);
	private static final VoxelShape SOUTH_SHAPE = VoxelShapes.union(
			Block.createCuboidShape(3.0, 3.0, 13.0, 13.0, 13.0, 16.0),
			Block.createCuboidShape(1.0, 6.0, 8.0, 15.0, 10.0, 13.0),
			Block.createCuboidShape(6.0, 1.0, 8.0, 10.0, 15.0, 13.0),
			Block.createCuboidShape(7.0, 7.0, 0.0, 9.0, 9.0, 16.0)
	);
	private static final VoxelShape WEST_SHAPE = VoxelShapes.union(
			Block.createCuboidShape(0.0, 3.0, 3.0, 3.0, 13.0, 13.0),
			Block.createCuboidShape(3.0, 6.0, 1.0, 8.0, 10.0, 15.0),
			Block.createCuboidShape(3.0, 1.0, 6.0, 8.0, 15.0, 10.0),
			Block.createCuboidShape(0.0, 7.0, 7.0, 16.0, 9.0, 9.0)
	);
	private static final VoxelShape EAST_SHAPE = VoxelShapes.union(
			Block.createCuboidShape(13.0, 3.0, 3.0, 16.0, 13.0, 13.0),
			Block.createCuboidShape(8.0, 6.0, 1.0, 13.0, 10.0, 15.0),
			Block.createCuboidShape(8.0, 1.0, 6.0, 13.0, 15.0, 10.0),
			Block.createCuboidShape(0.0, 7.0, 7.0, 16.0, 9.0, 9.0)
	);
	private static final VoxelShape DOWN_SHAPE = VoxelShapes.union(
			Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 3.0, 13.0),
			Block.createCuboidShape(1.0, 3.0, 6.0, 15.0, 8.0, 10.0),
			Block.createCuboidShape(6.0, 3.0, 1.0, 10.0, 8.0, 15.0),
			Block.createCuboidShape(7.0, 0.0, 7.0, 9.0, 16.0, 9.0)
	);
	private static final VoxelShape UP_SHAPE = VoxelShapes.union(
			Block.createCuboidShape(3.0, 13.0, 3.0, 13.0, 16.0, 13.0),
			Block.createCuboidShape(1.0, 8.0, 6.0, 15.0, 13.0, 10.0),
			Block.createCuboidShape(6.0, 8.0, 1.0, 10.0, 13.0, 15.0),
			Block.createCuboidShape(7.0, 0.0, 7.0, 9.0, 16.0, 9.0)
	);

	GrapplingAnchorBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(FACING, Direction.DOWN));
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return switch (state.get(FACING)) {
			case NORTH -> NORTH_SHAPE;
			case SOUTH -> SOUTH_SHAPE;
			case WEST -> WEST_SHAPE;
			case EAST -> EAST_SHAPE;
			case UP -> UP_SHAPE;
			case DOWN -> DOWN_SHAPE;
		};
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
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
		return GrapplingRopeItem.interactWithAnchor(world, pos, player, hand);
	}

	@Override
	public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient) {
			int segments = GrapplingRopeItem.removeRopeBelow(world, pos);
			if (!player.isCreative()) {
				GrapplingRopeItem.giveModules(
						player,
						Math.max(1, (segments + GrapplingRopeItem.MODULE_LENGTH - 1) / GrapplingRopeItem.MODULE_LENGTH)
				);
			}
		}
		super.onBreak(world, pos, state, player);
	}
}
