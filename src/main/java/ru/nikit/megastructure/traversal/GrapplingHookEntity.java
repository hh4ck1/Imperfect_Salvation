package ru.nikit.megastructure.traversal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public final class GrapplingHookEntity extends ThrownItemEntity {
	public GrapplingHookEntity(EntityType<? extends GrapplingHookEntity> type, World world) {
		super(type, world);
	}

	GrapplingHookEntity(World world, LivingEntity owner, ItemStack stack) {
		super(TraversalContent.GRAPPLING_HOOK, owner, world);
		setItem(stack);
	}

	@Override
	protected Item getDefaultItem() {
		return TraversalContent.GRAPPLING_ROPE;
	}

	@Override
	protected float getGravity() {
		return 0.022F;
	}

	@Override
	protected void onBlockHit(BlockHitResult hit) {
		super.onBlockHit(hit);
		if (getWorld().isClient) {
			return;
		}
		boolean deployed = GrapplingRopeItem.deploy(
				getWorld(),
				hit.getBlockPos().offset(hit.getSide()),
				hit.getSide().getOpposite(),
				GrapplingRopeItem.MODULE_LENGTH
		);
		if (!deployed) {
			returnToOwner();
		}
		discard();
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && (age > 100 || getY() < getWorld().getBottomY() - 8)) {
			returnToOwner();
			discard();
		}
	}

	private void returnToOwner() {
		Entity owner = getOwner();
		ItemStack returned = getStack().copyWithCount(1);
		if (owner instanceof PlayerEntity player && player.isAlive()) {
			if (!player.getInventory().insertStack(returned) && !returned.isEmpty()) {
				player.dropItem(returned, false);
			}
		} else {
			dropStack(returned);
		}
	}
}
