package ru.nikit.megastructure.traversal;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import ru.nikit.megastructure.MegastructureMod;

public final class TraversalContent {
	public static final RopeBlock ROPE_BLOCK = new RopeBlock(
			AbstractBlock.Settings.create().noCollision().nonOpaque().strength(0.35F).sounds(BlockSoundGroup.WOOL)
	);
	public static final GrapplingAnchorBlock ANCHOR_BLOCK = new GrapplingAnchorBlock(
			AbstractBlock.Settings.create().noCollision().nonOpaque().strength(2.0F).sounds(BlockSoundGroup.CHAIN)
	);
	public static final GrapplingRopeItem GRAPPLING_ROPE = new GrapplingRopeItem(
			new Item.Settings().maxCount(16)
	);
	public static final RopeCoilItem ROPE_COIL = new RopeCoilItem(new Item.Settings().maxCount(16));
	public static final EntityType<GrapplingHookEntity> GRAPPLING_HOOK = EntityType.Builder
			.<GrapplingHookEntity>create(GrapplingHookEntity::new, SpawnGroup.MISC)
			.disableSaving()
			.setDimensions(0.28F, 0.28F)
			.maxTrackingRange(96)
			.trackingTickInterval(1)
			.build("grappling_hook");

	private TraversalContent() {
	}

	public static void register() {
		Registry.register(Registries.BLOCK, MegastructureMod.id("rope"), ROPE_BLOCK);
		Registry.register(Registries.BLOCK, MegastructureMod.id("grappling_anchor"), ANCHOR_BLOCK);
		Registry.register(Registries.ITEM, MegastructureMod.id("grappling_rope"), GRAPPLING_ROPE);
		Registry.register(Registries.ITEM, MegastructureMod.id("rope_coil"), ROPE_COIL);
		Registry.register(Registries.ENTITY_TYPE, MegastructureMod.id("grappling_hook"), GRAPPLING_HOOK);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
			entries.add(ROPE_COIL);
			entries.add(GRAPPLING_ROPE);
		});
		MinecartLinking.register();
	}
}
