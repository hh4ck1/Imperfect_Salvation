package ru.nikit.megastructure.survival;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.FoodComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.MiningToolItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.BlockSoundGroup;
import ru.nikit.megastructure.MegastructureMod;

public final class PrimitiveSurvivalContent {
	public static final LooseStoneBlock LOOSE_STONE_BLOCK = new LooseStoneBlock(
			AbstractBlock.Settings.create()
					.mapColor(MapColor.STONE_GRAY)
					.nonOpaque()
					.noCollision()
					.strength(0.25F)
					.sounds(BlockSoundGroup.STONE)
	);
	public static final LooseStoneItem LOOSE_STONE = new LooseStoneItem(
			LOOSE_STONE_BLOCK,
			new net.minecraft.item.Item.Settings().maxCount(32)
	);
	public static final MiningToolItem KNAPPED_DIGGING_STONE = new MiningToolItem(
			0,
			-2.6F,
			CrudeStoneToolMaterial.INSTANCE,
			net.minecraft.registry.tag.BlockTags.PICKAXE_MINEABLE,
			new net.minecraft.item.Item.Settings()
	);
	public static final Item EDIBLE_MOSS = new EdibleMossItem(
			new Item.Settings().food(FoodComponents.BAKED_POTATO)
	);
	public static final BlockEntityType<LooseStoneBlockEntity> LOOSE_STONE_BLOCK_ENTITY = FabricBlockEntityTypeBuilder
			.create(LooseStoneBlockEntity::new, LOOSE_STONE_BLOCK)
			.build();
	public static final ScreenHandlerType<LooseStoneKnappingScreenHandler> LOOSE_STONE_KNAPPING = new ExtendedScreenHandlerType<>(
			LooseStoneKnappingScreenHandler::new
	);

	private PrimitiveSurvivalContent() {
	}

	public static void register() {
		Registry.register(Registries.BLOCK, MegastructureMod.id("loose_stone"), LOOSE_STONE_BLOCK);
		Registry.register(Registries.ITEM, MegastructureMod.id("loose_stone"), LOOSE_STONE);
		Registry.register(Registries.ITEM, MegastructureMod.id("knapped_digging_stone"), KNAPPED_DIGGING_STONE);
		Registry.register(Registries.ITEM, MegastructureMod.id("edible_moss"), EDIBLE_MOSS);
		Registry.register(Registries.BLOCK_ENTITY_TYPE, MegastructureMod.id("loose_stone"), LOOSE_STONE_BLOCK_ENTITY);
		Registry.register(Registries.SCREEN_HANDLER, MegastructureMod.id("loose_stone_knapping"), LOOSE_STONE_KNAPPING);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
			entries.add(LOOSE_STONE);
			entries.add(KNAPPED_DIGGING_STONE);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> entries.add(EDIBLE_MOSS));
	}
}
