package ru.nikit.megastructure.survival;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

public final class LooseStoneItem extends BlockItem {
	public LooseStoneItem(LooseStoneBlock block, Settings settings) {
		super(block, settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		if (world.getBlockState(context.getBlockPos()).isOf(PrimitiveSurvivalContent.LOOSE_STONE_BLOCK)
				&& context.getPlayer() != null) {
			return PrimitiveSurvivalContent.LOOSE_STONE_BLOCK.beginKnapping(
					world,
					context.getBlockPos(),
					context.getPlayer(),
					context.getHand()
			);
		}
		return super.useOnBlock(context);
	}
}
