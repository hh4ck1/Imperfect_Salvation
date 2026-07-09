package ru.nikit.megastructure.mixin;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FurnaceMinecartEntity.class)
public abstract class FurnaceMinecartSpeedMixin {
	@Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
	private void megastructure$raisePoweredRailSpeed(CallbackInfoReturnable<Double> callbackInfo) {
			AbstractMinecartEntity cart = (AbstractMinecartEntity) (Object) this;
			if (cart.getWorld().getBiome(cart.getBlockPos()).getKey()
					.map(key -> key.getValue().getNamespace().equals("megastructure"))
					.orElse(false)) {
			callbackInfo.setReturnValue(1.35);
		}
	}
}
