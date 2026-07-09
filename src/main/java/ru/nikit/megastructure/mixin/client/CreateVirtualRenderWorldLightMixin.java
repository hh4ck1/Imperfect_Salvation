package ru.nikit.megastructure.mixin.client;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld", remap = false)
public abstract class CreateVirtualRenderWorldLightMixin {
	private static final boolean STARLIGHT_LOADED = FabricLoader.getInstance().isModLoaded("starlight");

	@Inject(method = "runLightEngine", at = @At("HEAD"), cancellable = true, remap = false)
	private void megastructure$skipStarlightVirtualLightEngine(CallbackInfo callbackInfo) {
		if (STARLIGHT_LOADED) {
			callbackInfo.cancel();
		}
	}
}
