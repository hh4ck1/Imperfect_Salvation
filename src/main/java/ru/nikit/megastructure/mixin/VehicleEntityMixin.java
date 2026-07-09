package ru.nikit.megastructure.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.nikit.megastructure.traversal.MinecartLinking;

@Mixin(AbstractMinecartEntity.class)
public abstract class VehicleEntityMixin {
	@Inject(method = "dropItems", at = @At("HEAD"))
	private void megastructure$dropMinecartLinkCoils(DamageSource damageSource, CallbackInfo callbackInfo) {
		MinecartLinking.dropLinkedCoils((AbstractMinecartEntity) (Object) this);
	}
}
