package ru.nikit.megastructure.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.nikit.megastructure.client.MegastructureAtmosphereRenderer;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
	@Inject(method = "applyFog", at = @At("TAIL"))
	private static void megastructure$deferTerrainFog(
			Camera camera,
			BackgroundRenderer.FogType fogType,
			float viewDistance,
			boolean thickFog,
			float tickDelta,
			CallbackInfo callbackInfo
	) {
		if (fogType == BackgroundRenderer.FogType.FOG_TERRAIN && MegastructureAtmosphereRenderer.isActive()) {
			RenderSystem.setShaderFogStart(viewDistance * 1.08F);
			RenderSystem.setShaderFogEnd(viewDistance * 1.22F);
		}
	}
}
