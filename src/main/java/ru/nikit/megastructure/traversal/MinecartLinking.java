package ru.nikit.megastructure.traversal;

import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class MinecartLinking {
	private static final String SELECTED_CART = "MegastructureSelectedCart";
	private static final double MAXIMUM_LINK_DISTANCE_SQUARED = 12.0 * 12.0;

	private MinecartLinking() {
	}

	static void register() {
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (!(entity instanceof AbstractMinecartEntity cart)) {
				return ActionResult.PASS;
			}
			ItemStack held = player.getStackInHand(hand);
			if (held.isOf(TraversalContent.ROPE_COIL)) {
				return useCoil(player, world, held, cart);
			}
			if (held.isEmpty() && player.isSneaking()) {
				return detachCoil(player, world, cart);
			}
			return ActionResult.PASS;
		});
	}

	private static ActionResult useCoil(
			PlayerEntity player,
			World world,
			ItemStack coil,
			AbstractMinecartEntity cart
	) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		LinkedMinecart linkedCart = (LinkedMinecart) cart;
		NbtCompound nbt = coil.getOrCreateNbt();
		if (!nbt.containsUuid(SELECTED_CART)) {
			if (!linkedCart.megastructure$hasFreeLink()) {
				player.sendMessage(Text.translatable("message.megastructure.minecart_full"), true);
				return ActionResult.CONSUME;
			}
			nbt.putUuid(SELECTED_CART, cart.getUuid());
			player.sendMessage(Text.translatable("message.megastructure.minecart_selected"), true);
			world.playSound(null, cart.getBlockPos(), SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 0.65F, 0.9F);
			return ActionResult.CONSUME;
		}

		UUID selectedUuid = nbt.getUuid(SELECTED_CART);
		if (selectedUuid.equals(cart.getUuid())) {
			nbt.remove(SELECTED_CART);
			player.sendMessage(Text.translatable("message.megastructure.minecart_selection_cleared"), true);
			return ActionResult.CONSUME;
		}
		Entity selectedEntity = ((ServerWorld) world).getEntity(selectedUuid);
		if (!(selectedEntity instanceof AbstractMinecartEntity selectedCart)
				|| selectedCart.squaredDistanceTo(cart) > MAXIMUM_LINK_DISTANCE_SQUARED) {
			nbt.remove(SELECTED_CART);
			player.sendMessage(Text.translatable("message.megastructure.minecart_too_far"), true);
			return ActionResult.CONSUME;
		}
		LinkedMinecart first = (LinkedMinecart) selectedCart;
		if (first.megastructure$getLinks().contains(cart.getUuid())) {
			nbt.remove(SELECTED_CART);
			player.sendMessage(Text.translatable("message.megastructure.minecarts_already_linked"), true);
			return ActionResult.CONSUME;
		}
		if (!first.megastructure$hasFreeLink() || !linkedCart.megastructure$hasFreeLink()) {
			nbt.remove(SELECTED_CART);
			player.sendMessage(Text.translatable("message.megastructure.minecart_full"), true);
			return ActionResult.CONSUME;
		}
		if (!first.megastructure$addLink(cart.getUuid()) || !linkedCart.megastructure$addLink(selectedCart.getUuid())) {
			first.megastructure$removeLink(cart.getUuid());
			linkedCart.megastructure$removeLink(selectedCart.getUuid());
			nbt.remove(SELECTED_CART);
			return ActionResult.CONSUME;
		}
		nbt.remove(SELECTED_CART);
		if (!player.isCreative()) {
			coil.decrement(1);
		}
		player.getItemCooldownManager().set(TraversalContent.ROPE_COIL, 6);
		player.sendMessage(Text.translatable("message.megastructure.minecarts_linked"), true);
		world.playSound(null, cart.getBlockPos(), SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 0.9F, 0.65F);
		return ActionResult.CONSUME;
	}

	private static ActionResult detachCoil(PlayerEntity player, World world, AbstractMinecartEntity cart) {
		LinkedMinecart linkedCart = (LinkedMinecart) cart;
		List<UUID> links = linkedCart.megastructure$getLinks();
		if (links.isEmpty()) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		UUID partnerUuid = links.get(0);
		linkedCart.megastructure$removeLink(partnerUuid);
		Entity partner = ((ServerWorld) world).getEntity(partnerUuid);
		if (partner instanceof LinkedMinecart linkedPartner) {
			linkedPartner.megastructure$removeLink(cart.getUuid());
		}
		GrapplingRopeItem.giveItem(player, new ItemStack(TraversalContent.ROPE_COIL));
		player.getItemCooldownManager().set(TraversalContent.ROPE_COIL, 6);
		player.sendMessage(Text.translatable("message.megastructure.minecarts_unlinked"), true);
		world.playSound(null, cart.getBlockPos(), SoundEvents.BLOCK_WOOL_BREAK, SoundCategory.PLAYERS, 0.8F, 0.8F);
		return ActionResult.CONSUME;
	}

	public static void dropLinkedCoils(AbstractMinecartEntity cart) {
		if (!(cart.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		LinkedMinecart linkedCart = (LinkedMinecart) cart;
		List<UUID> links = linkedCart.megastructure$getLinks();
		for (UUID link : links) {
			cart.dropStack(new ItemStack(TraversalContent.ROPE_COIL), 0.2F);
			Entity entity = serverWorld.getEntity(link);
			if (entity instanceof LinkedMinecart linkedPartner) {
				linkedPartner.megastructure$removeLink(cart.getUuid());
			}
			linkedCart.megastructure$removeLink(link);
		}
	}
}
