package ru.nikit.megastructure.survival;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public final class EdibleMossItem extends Item {
	public EdibleMossItem(Settings settings) {
		super(settings);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		ItemStack result = super.finishUsing(stack, world, user);
		if (!world.isClient && user instanceof ServerPlayerEntity player) {
			MossConsumptionState.increment(player);
		}
		return result;
	}
}
