package ru.nikit.megastructure.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import ru.nikit.megastructure.client.task.GlobalTaskAnnouncementOverlay;
import ru.nikit.megastructure.client.startup.ServerStartClientState;
import ru.nikit.megastructure.client.sound.NarrativeSoundPlayer;
import ru.nikit.megastructure.client.task.GlobalTaskKeyBindings;
import ru.nikit.megastructure.client.task.GlobalTasksClientState;
import ru.nikit.megastructure.client.updater.StartupModUpdater;
import ru.nikit.megastructure.survival.PrimitiveSurvivalContent;
import ru.nikit.megastructure.traversal.TraversalContent;

public final class MegastructureClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		StartupModUpdater.checkOnStartup(() -> System.exit(0));
		BlockRenderLayerMap.INSTANCE.putBlock(TraversalContent.ROPE_BLOCK, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(TraversalContent.ANCHOR_BLOCK, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(PrimitiveSurvivalContent.LOOSE_STONE_BLOCK, RenderLayer.getCutout());
		EntityRendererRegistry.register(
				TraversalContent.GRAPPLING_HOOK,
				context -> new FlyingItemEntityRenderer<>(context, 1.0F, true)
		);
		HandledScreens.register(PrimitiveSurvivalContent.LOOSE_STONE_KNAPPING, LooseStoneKnappingScreen::new);
		// Register before GameOptions freezes Fabric's keybinding registry.
		GlobalTaskKeyBindings.register();
		GlobalTasksClientState.registerNetworking();
		ServerStartClientState.registerNetworking();
		ClientTickEvents.END_CLIENT_TICK.register(ServerStartClientState::tick);
		ClientTickEvents.END_CLIENT_TICK.register(NarrativeSoundPlayer::tick);
		ClientTickEvents.END_CLIENT_TICK.register(GlobalTaskKeyBindings::tick);
		ClientTickEvents.END_CLIENT_TICK.register(RenderSafetyGuard::repairInvalidFramebuffer);
		ClientTickEvents.END_CLIENT_TICK.register(OasisSporeParticleSpawner::tick);
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
				ScreenEvents.beforeRender(screen).register((activeScreen, context, mouseX, mouseY, tickDelta) -> {
					if (ServerStartClientState.isBlackoutActive()) {
						context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0xFF000000);
					}
				})
		);
		HudRenderCallback.EVENT.register((context, tickDelta) -> {
			if (ServerStartClientState.shouldRenderHudBlackout()) {
				context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0xFF000000);
			}
			GlobalTaskAnnouncementOverlay.render(context, tickDelta);
		});
		WorldRenderEvents.START.register(context -> RenderSafetyGuard.repairInvalidFramebuffer(
				net.minecraft.client.MinecraftClient.getInstance()
		));
		WorldRenderEvents.LAST.register(MegastructureAtmosphereRenderer::render);
		WorldRenderEvents.LAST.register(MinecartLinkRenderer::render);
		WorldRenderEvents.LAST.register(MegastructureDustRenderer::render);
		WorldRenderEvents.AFTER_ENTITIES.register(VulkanVisualEffectsRenderer::renderIrisWorldPipeline);
		WorldRenderEvents.LAST.register(VulkanVisualEffectsRenderer::render);
		if (Boolean.getBoolean("megastructure.blackhole.enabled")) {
			WorldRenderEvents.LAST.register(BlackHoleReactorRenderer::render);
		}
		WorldRenderEvents.LAST.register(UnloadedEdgeFogRenderer::render);
	}
}
