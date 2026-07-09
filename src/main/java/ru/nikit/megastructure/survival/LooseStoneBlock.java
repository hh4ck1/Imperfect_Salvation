package ru.nikit.megastructure.survival;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public final class LooseStoneBlock extends Block implements BlockEntityProvider {
	public static final IntProperty PHASE = IntProperty.of("phase", 0, 4);
	private static final VoxelShape PHASE_0_SHAPE = Block.createCuboidShape(4.0D, 0.0D, 4.0D, 12.0D, 3.0D, 11.0D);
	private static final VoxelShape PHASE_1_SHAPE = Block.createCuboidShape(3.0D, 0.0D, 4.0D, 12.0D, 3.0D, 12.0D);
	private static final VoxelShape PHASE_2_SHAPE = Block.createCuboidShape(3.0D, 0.0D, 3.0D, 13.0D, 3.0D, 12.0D);
	private static final VoxelShape PHASE_3_SHAPE = Block.createCuboidShape(3.0D, 0.0D, 3.0D, 13.0D, 4.0D, 13.0D);
	private static final VoxelShape PHASE_4_SHAPE = Block.createCuboidShape(2.0D, 0.0D, 3.0D, 13.0D, 4.0D, 13.0D);

	public LooseStoneBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(PHASE, 0));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(PHASE);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return switch (state.get(PHASE)) {
			case 1 -> PHASE_1_SHAPE;
			case 2 -> PHASE_2_SHAPE;
			case 3 -> PHASE_3_SHAPE;
			case 4 -> PHASE_4_SHAPE;
			default -> PHASE_0_SHAPE;
		};
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP);
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
		ItemStack held = player.getStackInHand(hand);
		if (held.isOf(PrimitiveSurvivalContent.LOOSE_STONE)) {
			return beginKnapping(world, pos, player, hand);
		}
		if (!held.isEmpty()) {
			return ActionResult.PASS;
		}
		if (state.get(PHASE) != 0) {
			return ActionResult.SUCCESS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		world.removeBlock(pos, false);
		giveItem(player, new ItemStack(PrimitiveSurvivalContent.LOOSE_STONE));
		world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 0.65F, 1.15F);
		return ActionResult.CONSUME;
	}

	public ActionResult beginKnapping(World world, BlockPos pos, PlayerEntity player, Hand hand) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		boolean consumeHammer = !player.isCreative();
		if (consumeHammer) {
			player.getStackInHand(hand).decrement(1);
		}
		player.openHandledScreen(new LooseStoneKnappingScreenHandler.OpeningDataFactory(pos, consumeHammer));
		world.playSound(null, pos, SoundEvents.BLOCK_STONE_HIT, SoundCategory.BLOCKS, 0.55F, 0.78F);
		return ActionResult.CONSUME;
	}

	@Override
	public void neighborUpdate(
			BlockState state,
			World world,
			BlockPos pos,
			Block sourceBlock,
			BlockPos sourcePos,
			boolean notify
	) {
		super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
		if (!canPlaceAt(state, world, pos)) {
			world.breakBlock(pos, false);
		}
	}

	@Override
	public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient && !player.isCreative()) {
			dropStack(world, pos, new ItemStack(PrimitiveSurvivalContent.LOOSE_STONE));
		}
		super.onBreak(world, pos, state, player);
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new LooseStoneBlockEntity(pos, state);
	}

	private static void giveItem(PlayerEntity player, ItemStack returned) {
		if (!player.getInventory().insertStack(returned) && !returned.isEmpty()) {
			player.dropItem(returned, false);
		}
	}
}
