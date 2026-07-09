package ru.nikit.megastructure.traversal;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class GrapplingRopeItem extends Item {
	public static final int MODULE_LENGTH = 30;
	private static final int MAX_TOTAL_LENGTH = 240;

	GrapplingRopeItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (player.getItemCooldownManager().isCoolingDown(this)) {
			return TypedActionResult.fail(stack);
		}
		world.playSound(
				null,
				player.getX(),
				player.getY(),
				player.getZ(),
				SoundEvents.ENTITY_FISHING_BOBBER_THROW,
				SoundCategory.PLAYERS,
				0.65F,
				0.78F + world.getRandom().nextFloat() * 0.18F
		);
		if (!world.isClient) {
			GrapplingHookEntity hook = new GrapplingHookEntity(world, player, stack.copyWithCount(1));
			hook.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, 1.65F, 0.35F);
			world.spawnEntity(hook);
			if (!player.isCreative()) {
				stack.decrement(1);
			}
		}
		player.incrementStat(Stats.USED.getOrCreateStat(this));
		player.getItemCooldownManager().set(this, 6);
		return TypedActionResult.success(stack, world.isClient());
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		PlayerEntity player = context.getPlayer();
		if (player != null && player.getItemCooldownManager().isCoolingDown(this)) {
			return ActionResult.FAIL;
		}
		BlockPos anchor = context.getBlockPos().offset(context.getSide());
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (!deploy(world, anchor, context.getSide().getOpposite(), MODULE_LENGTH)) {
			return ActionResult.FAIL;
		}
		if (player != null && !player.isCreative()) {
			context.getStack().decrement(1);
		}
		world.playSound(null, anchor, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 0.8F, 0.72F);
		if (player != null) {
			player.getItemCooldownManager().set(this, 6);
		}
		return ActionResult.CONSUME;
	}

	public static boolean deploy(World world, BlockPos anchor, Direction attachedTo, int length) {
		BlockState anchorState = world.getBlockState(anchor);
		if (!anchorState.isAir() && !anchorState.isReplaceable()) {
			return false;
		}
		world.setBlockState(anchor, TraversalContent.ANCHOR_BLOCK.getDefaultState()
				.with(GrapplingAnchorBlock.FACING, attachedTo), Block.NOTIFY_ALL);
		int placed = extendRope(world, anchor, length);
		if (placed == 0) {
			world.setBlockState(anchor, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
			return false;
		}
		return true;
	}

	public static ActionResult interactWithAnchor(World world, BlockPos anchor, PlayerEntity player, Hand hand) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		ItemStack held = player.getStackInHand(hand);
		if (held.isOf(TraversalContent.GRAPPLING_ROPE)) {
			int added = extendRope(world, anchor, MODULE_LENGTH);
			if (added <= 0) {
				return ActionResult.FAIL;
			}
			if (!player.isCreative()) {
				held.decrement(1);
			}
			world.playSound(null, anchor, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 0.8F, 0.68F);
			return ActionResult.CONSUME;
		}
		int segments = removeRopeBelow(world, anchor);
		world.setBlockState(anchor, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
		giveModules(player, Math.max(1, (segments + MODULE_LENGTH - 1) / MODULE_LENGTH));
		world.playSound(null, anchor, SoundEvents.ITEM_BUNDLE_DROP_CONTENTS, SoundCategory.PLAYERS, 0.7F, 1.1F);
		return ActionResult.CONSUME;
	}

	static int extendRope(World world, BlockPos anchor, int requested) {
		BlockPos.Mutable cursor = anchor.mutableCopy().move(0, -1, 0);
		int existing = 0;
		while (world.getBlockState(cursor).isOf(TraversalContent.ROPE_BLOCK) && existing < MAX_TOTAL_LENGTH) {
			existing++;
			cursor.move(0, -1, 0);
		}
		if (existing >= MAX_TOTAL_LENGTH) {
			return 0;
		}
		int placed = 0;
		int limit = Math.min(requested, MAX_TOTAL_LENGTH - existing);
		while (placed < limit && cursor.getY() > world.getBottomY()) {
			BlockState state = world.getBlockState(cursor);
			if (!state.isAir() && !state.isReplaceable()) {
				break;
			}
			world.setBlockState(cursor, TraversalContent.ROPE_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
			placed++;
			cursor.move(0, -1, 0);
		}
		return placed;
	}

	static BlockPos findAnchor(World world, BlockPos rope) {
		BlockPos.Mutable cursor = rope.mutableCopy();
		for (int i = 0; i <= MAX_TOTAL_LENGTH; i++) {
			BlockState state = world.getBlockState(cursor);
			if (state.isOf(TraversalContent.ANCHOR_BLOCK)) {
				return cursor.toImmutable();
			}
			if (!state.isOf(TraversalContent.ROPE_BLOCK)) {
				return null;
			}
			cursor.move(0, 1, 0);
		}
		return null;
	}

	static int removeRopeBelow(World world, BlockPos anchor) {
		BlockPos.Mutable cursor = anchor.mutableCopy().move(0, -1, 0);
		int removed = 0;
		while (world.getBlockState(cursor).isOf(TraversalContent.ROPE_BLOCK) && removed < MAX_TOTAL_LENGTH) {
			world.setBlockState(cursor, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
			removed++;
			cursor.move(0, -1, 0);
		}
		return removed;
	}

	static void giveModules(PlayerEntity player, int count) {
		giveItem(player, new ItemStack(TraversalContent.GRAPPLING_ROPE, count));
	}

	static void giveItem(PlayerEntity player, ItemStack returned) {
		if (!player.getInventory().insertStack(returned) && !returned.isEmpty()) {
			player.dropItem(returned, false);
		}
	}
}
