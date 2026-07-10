package ru.nikit.megastructure.mixin.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld", remap = false)
public abstract class CreateVirtualRenderWorldLightMixin {
	private static final boolean STARLIGHT_LOADED = FabricLoader.getInstance().isModLoaded("starlight");
	private static boolean reportedStarlightVirtualWorldFailure;

	@Redirect(
			method = "runLightEngine",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/class_3568;method_51471(Lnet/minecraft/class_1923;)V", remap = false),
			remap = false
	)
	private void megastructure$prepareCreateContraptionLightSafely(LightingProvider lightingProvider, ChunkPos chunkPos) {
		try {
			lightingProvider.setRetainData(chunkPos, false);
		} catch (RuntimeException exception) {
			handleCreateVirtualLightFailure(exception);
		}
	}

	@Redirect(
			method = "runLightEngine",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/class_3568;method_15516()I", remap = false),
			remap = false
	)
	private int megastructure$runCreateContraptionLightSafely(LightingProvider lightingProvider) {
		try {
			return lightingProvider.doLightUpdates();
		} catch (RuntimeException exception) {
			handleCreateVirtualLightFailure(exception);
			return 0;
		}
	}

	private static void handleCreateVirtualLightFailure(RuntimeException exception) {
		if (!STARLIGHT_LOADED) {
			throw exception;
		}
		if (!reportedStarlightVirtualWorldFailure) {
			reportedStarlightVirtualWorldFailure = true;
			System.err.println("Imperfect_salvation suppressed a Starlight light update crash inside a Create virtual render world: "
					+ exception.getClass().getSimpleName()
					+ (exception.getMessage() == null ? "" : " - " + exception.getMessage()));
		}
	}
}
