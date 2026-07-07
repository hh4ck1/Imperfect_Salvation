package ru.nikit.megastructure.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowableFluid.class)
public abstract class FlowableFluidMossCarpetMixin {
	@Inject(method = "canFill", at = @At("HEAD"), cancellable = true)
	private void megastructure$keepMossCarpetFromWater(
			BlockView world,
			BlockPos pos,
			BlockState state,
			Fluid fluid,
			CallbackInfoReturnable<Boolean> callbackInfo
	) {
		if (state.isOf(Blocks.MOSS_CARPET) && (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER)) {
			callbackInfo.setReturnValue(false);
		}
	}
}
