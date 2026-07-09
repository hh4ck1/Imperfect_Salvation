package ru.nikit.megastructure.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

final class DynamicWorldgenPalette {
	record TreeMaterial(BlockState log, BlockState leaves) {
	}

	private static volatile List<TreeMaterial> treeMaterials;
	private static volatile List<Item> corridorLootItems;
	private static volatile List<BlockState> oreStates;

	private DynamicWorldgenPalette() {
	}

	static TreeMaterial treeMaterial(long seed, int index) {
		List<TreeMaterial> materials = treeMaterials;
		if (materials == null) {
			materials = buildTreeMaterials();
			treeMaterials = materials;
		}
		if (materials.isEmpty()) {
			return new TreeMaterial(BlockPalette.OAK_LOG, BlockPalette.OAK_LEAVES);
		}
		return materials.get(Math.floorMod((int) MegastructureMath.hash(seed, index, materials.size(), 2901), materials.size()));
	}

	static Item corridorLootItem(long hash) {
		List<Item> items = corridorLootItems;
		if (items == null) {
			items = buildCorridorLootItems();
			corridorLootItems = items;
		}
		if (items.isEmpty()) {
			return Items.WHEAT_SEEDS;
		}
		return items.get(Math.floorMod((int) hash, items.size()));
	}

	static ItemStack corridorLootStack(long hash) {
		int roll = Math.floorMod((int) hash, 10000);
		if (roll < 100) {
			return new ItemStack(Items.LAVA_BUCKET);
		}
		if (roll < 520) {
			return new ItemStack(Items.BLAZE_ROD, MegastructureMath.range(hash >>> 9, 1, 2));
		}
		Item item;
		if (roll < 3000) {
			item = supportLootItem(hash >>> 5);
		} else if (roll < 5600) {
			item = neepMeatLootItem(hash >>> 7);
		} else {
			item = corridorLootItem(hash >>> 11);
		}
		return new ItemStack(item, lootCount(item, hash >>> 17));
	}

	static BlockState ore(long hash) {
		List<BlockState> ores = oreStates;
		if (ores == null) {
			ores = buildOreStates();
			oreStates = ores;
		}
		if (ores.isEmpty()) {
			return Blocks.IRON_ORE.getDefaultState();
		}
		return ores.get(Math.floorMod((int) hash, ores.size()));
	}

	private static List<TreeMaterial> buildTreeMaterials() {
		List<Block> logs = new ArrayList<>();
		List<Block> leaves = new ArrayList<>();
		for (Block block : Registries.BLOCK) {
			BlockState state = block.getDefaultState();
			if (state.isIn(BlockTags.LOGS)) {
				logs.add(block);
			} else if (state.isIn(BlockTags.LEAVES)) {
				leaves.add(block);
			}
		}
		logs.sort(Comparator.comparing(block -> Registries.BLOCK.getId(block).toString()));
		leaves.sort(Comparator.comparing(block -> Registries.BLOCK.getId(block).toString()));

		List<TreeMaterial> materials = new ArrayList<>();
		for (Block log : logs) {
			Block leaf = bestLeafFor(log, leaves);
			if (leaf != null) {
				materials.add(new TreeMaterial(log.getDefaultState(), persistentLeaves(leaf.getDefaultState())));
			}
		}
		return materials;
	}

	private static Block bestLeafFor(Block log, List<Block> leaves) {
		Identifier logId = Registries.BLOCK.getId(log);
		String logKey = treeKey(logId.getPath());
		Block fallbackSameNamespace = null;
		for (Block leaf : leaves) {
			Identifier leafId = Registries.BLOCK.getId(leaf);
			if (!leafId.getNamespace().equals(logId.getNamespace())) {
				continue;
			}
			if (fallbackSameNamespace == null) {
				fallbackSameNamespace = leaf;
			}
			String leafKey = treeKey(leafId.getPath());
			if (!logKey.isEmpty() && (leafKey.contains(logKey) || logKey.contains(leafKey))) {
				return leaf;
			}
		}
		if (fallbackSameNamespace != null) {
			return fallbackSameNamespace;
		}
		return leaves.isEmpty() ? null : leaves.get(0);
	}

	private static String treeKey(String path) {
		String key = path.toLowerCase(Locale.ROOT);
		for (String suffix : new String[]{"_stripped_log", "_stripped_stem", "_stripped_wood", "_stripped_hyphae",
				"_log", "_stem", "_wood", "_hyphae", "_leaves", "_leaf", "_wart_block"}) {
			key = key.replace(suffix, "");
		}
		return key;
	}

	private static BlockState persistentLeaves(BlockState state) {
		if (state.contains(Properties.PERSISTENT)) {
			state = state.with(Properties.PERSISTENT, true);
		}
		return state;
	}

	private static List<Item> buildCorridorLootItems() {
		List<Item> items = new ArrayList<>(List.of(
				Items.WHEAT_SEEDS,
				Items.BEETROOT_SEEDS,
				Items.MELON_SEEDS,
				Items.PUMPKIN_SEEDS,
				Items.KELP,
				Items.CACTUS,
				Items.SUGAR_CANE,
				Items.BAMBOO,
				Items.COCOA_BEANS,
				Items.NETHER_WART,
				Items.BROWN_MUSHROOM,
				Items.RED_MUSHROOM,
				Items.DANDELION,
				Items.POPPY,
				Items.BLUE_ORCHID,
				Items.ALLIUM,
				Items.AZURE_BLUET,
				Items.RED_TULIP,
				Items.ORANGE_TULIP,
				Items.WHITE_TULIP,
				Items.PINK_TULIP,
				Items.OXEYE_DAISY,
				Items.CORNFLOWER,
				Items.LILY_OF_THE_VALLEY,
				Items.SUNFLOWER,
				Items.LILAC,
				Items.ROSE_BUSH,
				Items.PEONY,
				Items.SLIME_BALL,
				Items.PRISMARINE_SHARD,
				Items.PRISMARINE_CRYSTALS,
				Items.AMETHYST_SHARD,
				Items.GLOW_BERRIES,
				Items.SWEET_BERRIES,
				Items.BONE_MEAL,
				Items.STRING,
				Items.PAPER,
				Items.REDSTONE,
				Items.LAPIS_LAZULI,
				Items.QUARTZ,
				Items.COPPER_INGOT,
				Items.IRON_NUGGET
		));
		for (Item item : Registries.ITEM) {
			Identifier id = Registries.ITEM.getId(item);
			String path = id.getPath().toLowerCase(Locale.ROOT);
			if (id.getNamespace().equals("minecraft")) {
				continue;
			}
			if (isUnsafeLoot(path)) {
				continue;
			}
			if (path.contains("seed")
					|| path.contains("sapling")
					|| path.contains("spore")
					|| path.contains("crystal")
					|| path.contains("shard")
					|| path.contains("gem")
					|| path.contains("dust")
					|| path.contains("fiber")
					|| path.contains("berry")) {
				items.add(item);
			}
		}
		items.sort(Comparator.comparing(item -> Registries.ITEM.getId(item).toString()));
		return items;
	}

	private static Item supportLootItem(long hash) {
		Item[] items = {
				Items.KELP,
				Items.CACTUS,
				Items.SUGAR_CANE,
				Items.BAMBOO,
				Items.COCOA_BEANS,
				Items.NETHER_WART,
				Items.BROWN_MUSHROOM,
				Items.RED_MUSHROOM,
				Items.DANDELION,
				Items.POPPY,
				Items.BLUE_ORCHID,
				Items.ALLIUM,
				Items.AZURE_BLUET,
				Items.RED_TULIP,
				Items.ORANGE_TULIP,
				Items.WHITE_TULIP,
				Items.PINK_TULIP,
				Items.OXEYE_DAISY,
				Items.CORNFLOWER,
				Items.LILY_OF_THE_VALLEY,
				Items.SUNFLOWER,
				Items.LILAC,
				Items.ROSE_BUSH,
				Items.PEONY,
				Items.SLIME_BALL,
				Items.PRISMARINE_SHARD,
				Items.PRISMARINE_CRYSTALS,
				Items.AMETHYST_SHARD,
				Items.GLOW_BERRIES,
				Items.SWEET_BERRIES
		};
		return items[Math.floorMod((int) hash, items.length)];
	}

	private static Item neepMeatLootItem(long hash) {
		List<Item> items = new ArrayList<>();
		addItemIfPresent(items, "neepmeat:asbestos");
		addItemIfPresent(items, "neepmeat:asbestos_dust");
		addItemIfPresent(items, "neepmeat:asbestos_fabric");
		addItemIfPresent(items, "neepmeat:blood_bubble");
		addItemIfPresent(items, "neepmeat:blood_bubble_sapling");
		addItemIfPresent(items, "neepmeat:fluid_pipe");
		addItemIfPresent(items, "neepmeat:data_cable");
		addItemIfPresent(items, "neepmeat:vascular_conduit");
		addItemIfPresent(items, "neepmeat:vascular_sensor");
		addItemIfPresent(items, "neepmeat:rusty_metal_sheet");
		addItemIfPresent(items, "neepmeat:mesh_pane");
		addItemIfPresent(items, "neepmeat:meat_scrap");
		addItemIfPresent(items, "neepmeat:meat_steel_ball");
		addItemIfPresent(items, "neepmeat:whisper_wheat_seeds");
		if (items.isEmpty()) {
			return Items.ROTTEN_FLESH;
		}
		items.sort(Comparator.comparing(item -> Registries.ITEM.getId(item).toString()));
		return items.get(Math.floorMod((int) hash, items.size()));
	}

	private static void addItemIfPresent(List<Item> items, String id) {
		Registries.ITEM.getOrEmpty(new Identifier(id)).ifPresent(items::add);
	}

	private static int lootCount(Item item, long hash) {
		int maxStack = item.getMaxCount();
		if (maxStack <= 1) {
			return 1;
		}
		String path = Registries.ITEM.getId(item).getPath().toLowerCase(Locale.ROOT);
		int max = 8;
		if (path.contains("seed") || path.contains("sapling") || path.contains("wart")) {
			max = 3;
		} else if (path.contains("pipe") || path.contains("cable") || path.contains("conduit")
				|| path.contains("sheet") || path.contains("asbestos") || path.contains("kelp")) {
			max = 10;
		} else if (path.contains("rod") || path.contains("crystal") || path.contains("shard")) {
			max = 4;
		}
		return MegastructureMath.range(hash, 1, Math.min(maxStack, max));
	}

	private static List<BlockState> buildOreStates() {
		List<BlockState> ores = new ArrayList<>();
		for (Block block : Registries.BLOCK) {
			Identifier id = Registries.BLOCK.getId(block);
			String path = id.getPath().toLowerCase(Locale.ROOT);
			if (!looksLikeOre(path) || isUnsafeOre(path)) {
				continue;
			}
			BlockState state = block.getDefaultState();
			if (!state.isAir()) {
				ores.add(state);
			}
		}
		ores.sort(Comparator.comparing(state -> Registries.BLOCK.getId(state.getBlock()).toString()));
		return ores;
	}

	private static boolean looksLikeOre(String path) {
		return path.endsWith("_ore")
				|| (path.startsWith("deepslate_") && path.endsWith("_ore"))
				|| (path.startsWith("raw_") && path.endsWith("_ore"));
	}

	private static boolean isUnsafeOre(String path) {
		return path.contains("ancient_debris")
				|| path.contains("netherite")
				|| path.contains("nether_gold")
				|| path.contains("nether_quartz")
				|| path.contains("raw_ore_block");
	}

	private static boolean isUnsafeLoot(String path) {
		return path.contains("netherite")
				|| path.contains("diamond")
				|| path.contains("creative")
				|| path.contains("spawn_egg")
				|| path.contains("sword")
				|| path.contains("pickaxe")
				|| path.contains("axe")
				|| path.contains("shovel")
				|| path.contains("hoe")
				|| path.contains("helmet")
				|| path.contains("chestplate")
				|| path.contains("leggings")
				|| path.contains("boots");
	}
}
