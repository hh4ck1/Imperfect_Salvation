package ru.nikit.megastructure;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nikit.megastructure.survival.PrimitiveSurvivalContent;
import ru.nikit.megastructure.task.GlobalTaskManager;
import ru.nikit.megastructure.task.GlobalTaskSounds;
import ru.nikit.megastructure.startup.ServerStartManager;
import ru.nikit.megastructure.survival.MegastructureMobSystem;
import ru.nikit.megastructure.world.DistrictBiomeSource;
import ru.nikit.megastructure.world.LoadedChunkBlockUpdater;
import ru.nikit.megastructure.world.MegastructureChunkGenerator;
import ru.nikit.megastructure.world.VanillaOreFeatures;
import ru.nikit.megastructure.traversal.TraversalContent;
import ru.nikit.megastructure.client.updater.StartupModUpdater;

public final class MegastructureMod implements ModInitializer {
	public static final String MOD_ID = "megastructure";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			StartupModUpdater.checkOnStartup(() -> System.exit(0));
		}
		GlobalTaskSounds.register();
		VanillaOreFeatures.register();
		GlobalTaskManager.register();
		ServerStartManager.register();
		TraversalContent.register();
		PrimitiveSurvivalContent.register();
		MegastructureMobSystem.register();
		BlackHoleReactorSystem.register();
		LoadedChunkBlockUpdater.register();
		Registry.register(Registries.BIOME_SOURCE, id("district"), DistrictBiomeSource.CODEC);
		Registry.register(Registries.CHUNK_GENERATOR, id("megastructure"), MegastructureChunkGenerator.CODEC);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(net.minecraft.server.command.CommandManager.literal("edemstart")
					.executes(context -> ServerStartManager.confirmLaunch(context.getSource())));
			dispatcher.register(net.minecraft.server.command.CommandManager.literal("megastructure")
					.then(net.minecraft.server.command.CommandManager.literal("locate_oasis")
							.requires(source -> source.hasPermissionLevel(2))
							.executes(context -> locateOasis(context.getSource(), 64))
							.then(net.minecraft.server.command.CommandManager.argument(
									"radius",
									IntegerArgumentType.integer(1, 256)
							).executes(context -> locateOasis(
									context.getSource(),
									IntegerArgumentType.getInteger(context, "radius")
							)))
					));
		});
		ServerWorldEvents.LOAD.register((server, world) -> {
			if (!world.getRegistryKey().equals(World.OVERWORLD) || world.getTime() > 1L) {
				return;
			}
			if (world.getChunkManager().getChunkGenerator() instanceof MegastructureChunkGenerator generator) {
				world.setSpawnPos(new BlockPos(0, generator.spawnRailY() + 1, 2), 0.0F);
			}
		});
	}

	private static int locateOasis(net.minecraft.server.command.ServerCommandSource source, int radius) {
		try {
			if (!(source.getWorld().getChunkManager().getChunkGenerator() instanceof MegastructureChunkGenerator generator)) {
				source.sendError(Text.literal("This world does not use the megastructure generator."));
				return 0;
			}
			BlockPos origin = BlockPos.ofFloored(source.getPosition());
			return generator.findNearestOasis(origin, radius)
					.map(location -> {
						BlockPos pos = location.pos();
						String teleport = "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
						MutableText coordinates = Text.literal("[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]")
								.styled(style -> style
										.withColor(Formatting.GREEN)
										.withUnderline(true)
										.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, teleport))
										.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Insert " + teleport))));
						MutableText message = Text.literal(
								"Nearest oasis: " + location.host() + " / " + location.profile() + " at "
						).append(coordinates).append(Text.literal(", distance " + location.distance() + " blocks."));
						source.sendFeedback(
								() -> message,
								false
						);
						return 1;
					})
					.orElseGet(() -> {
						source.sendError(Text.literal("No oasis found within " + radius + " districts."));
						return 0;
					});
		} catch (RuntimeException exception) {
			LOGGER.error("Failed to locate oasis within {} districts", radius, exception);
			source.sendError(Text.literal("Failed to locate oasis: " + exception.getClass().getSimpleName()
					+ (exception.getMessage() == null ? "" : " - " + exception.getMessage())));
			return 0;
		}
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
