package ru.nikit.megastructure.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public abstract class AbstractBlockSaplingDropMixin {
	private static final float MEGASTRUCTURE_SAPLING_KEEP_CHANCE = 0.1F;

	@Inject(method = "getDroppedStacks", at = @At("RETURN"), cancellable = true)
	private void megastructure$reduceLeafSaplingDrops(BlockState state, LootContextParameterSet.Builder builder,
			CallbackInfoReturnable<List<ItemStack>> callbackInfo) {
		if (!state.isIn(BlockTags.LEAVES)) {
			return;
		}

		List<ItemStack> originalDrops = callbackInfo.getReturnValue();
		if (originalDrops.isEmpty()) {
			return;
		}

		List<ItemStack> filteredDrops = null;
		for (int i = 0; i < originalDrops.size(); i++) {
			ItemStack stack = originalDrops.get(i);
			if (!stack.isIn(ItemTags.SAPLINGS)) {
				if (filteredDrops != null) {
					filteredDrops.add(stack);
				}
				continue;
			}

			if (filteredDrops == null) {
				filteredDrops = new ArrayList<>(originalDrops.size());
				filteredDrops.addAll(originalDrops.subList(0, i));
			}

			int keptCount = megastructure$countKeptSaplings(stack.getCount());
			if (keptCount > 0) {
				ItemStack keptStack = stack.copy();
				keptStack.setCount(keptCount);
				filteredDrops.add(keptStack);
			}
		}

		if (filteredDrops != null) {
			callbackInfo.setReturnValue(filteredDrops);
		}
	}

	private static int megastructure$countKeptSaplings(int count) {
		int keptCount = 0;
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < count; i++) {
			if (random.nextFloat() < MEGASTRUCTURE_SAPLING_KEEP_CHANCE) {
				keptCount++;
			}
		}
		return keptCount;
	}
}
