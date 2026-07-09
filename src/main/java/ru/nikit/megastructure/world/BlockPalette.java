package ru.nikit.megastructure.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.RailShape;
import net.minecraft.block.enums.SlabType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

final class BlockPalette {
	static final BlockState AIR = Blocks.AIR.getDefaultState();
	static final BlockState MASS = NeepMeatCompat.block("grey_rough_concrete", Blocks.SMOOTH_STONE.getDefaultState());
	static final BlockState MASS_STONE_VARIANT = Blocks.STONE.getDefaultState();
	static final BlockState MASS_ANDESITE_VARIANT = Blocks.ANDESITE.getDefaultState();
	static final BlockState MASS_GRANITE_VARIANT = Blocks.GRANITE.getDefaultState();
	static final BlockState DARK_STONE = NeepMeatCompat.block("rusty_metal", Blocks.POLISHED_DEEPSLATE.getDefaultState());
	static final BlockState LIGHT_STONE = NeepMeatCompat.block("white_rough_concrete", Blocks.SMOOTH_STONE.getDefaultState());
	static final BlockState FOUNDATION = NeepMeatCompat.block("polished_metal", Blocks.POLISHED_ANDESITE.getDefaultState());
	static final BlockState PLATFORM = NeepMeatCompat.block("grey_rough_concrete", Blocks.SMOOTH_STONE.getDefaultState());
	static final BlockState GLOBE_SHELL = NeepMeatCompat.block("yellow_rough_concrete", Blocks.YELLOW_TERRACOTTA.getDefaultState());
	static final BlockState GLOBE_PANEL = NeepMeatCompat.block("yellow_tiles", Blocks.YELLOW_CONCRETE.getDefaultState());
	static final BlockState GLOBE_RIB = NeepMeatCompat.block("smooth_tile_orange", Blocks.ORANGE_TERRACOTTA.getDefaultState());
	static final BlockState GLOBE_SUPPORT = NeepMeatCompat.block("rusty_metal_sheet", Blocks.BROWN_TERRACOTTA.getDefaultState());
	static final BlockState WALKWAY = Blocks.STONE_BRICKS.getDefaultState();
	static final BlockState WALL_PANEL = NeepMeatCompat.block("dirty_white_tiles", Blocks.ANDESITE.getDefaultState());
	static final BlockState CRACKED_PANEL = Blocks.CRACKED_STONE_BRICKS.getDefaultState();
	static final BlockState STAIN = Blocks.TUFF.getDefaultState();
	static final BlockState PIPE = NeepMeatCompat.block("rusty_column", Blocks.POLISHED_DEEPSLATE.getDefaultState());
	static final BlockState RUST_PIPE = NeepMeatCompat.block("rusty_vent", Blocks.COPPER_BLOCK.getDefaultState());
	static final BlockState GRATE = NeepMeatCompat.block("rusty_metal_sheet", Blocks.POLISHED_DEEPSLATE.getDefaultState());
	static final BlockState LAMP = NeepMeatCompat.block(
			"rusty_metal_light",
			Blocks.SEA_LANTERN.getDefaultState(),
			state -> {
				if (state.contains(Properties.LIT)) {
					state = state.with(Properties.LIT, true);
				}
				return state;
			}
	);
	static final BlockState WATER = Blocks.WATER.getDefaultState();
	static final BlockState LAVA = Blocks.LAVA.getDefaultState();
	static final BlockState MOSS = Blocks.MOSS_BLOCK.getDefaultState();
	static final BlockState MOSS_CARPET = Blocks.MOSS_CARPET.getDefaultState();
	static final BlockState ROOTED_DIRT = NeepMeatCompat.block("contaminated_dirt", Blocks.ROOTED_DIRT.getDefaultState());
	static final BlockState CLAY = Blocks.CLAY.getDefaultState();
	static final BlockState COBWEB = Blocks.COBWEB.getDefaultState();
	static final BlockState OAK_LOG = NeepMeatCompat.block("blood_bubble_log", Blocks.OAK_LOG.getDefaultState());
	static final BlockState BIRCH_LOG = Blocks.BIRCH_LOG.getDefaultState();
	static final BlockState OAK_WOOD = NeepMeatCompat.block("blood_bubble_wood", Blocks.OAK_WOOD.getDefaultState());
	static final BlockState OAK_PLANKS = NeepMeatCompat.block("blood_bubble_planks", Blocks.OAK_PLANKS.getDefaultState());
	static final BlockState OAK_SLAB = Blocks.OAK_SLAB.getDefaultState().with(Properties.SLAB_TYPE, SlabType.BOTTOM);
	static final BlockState QUARTZ_BLOCK = Blocks.QUARTZ_BLOCK.getDefaultState();
	static final BlockState QUARTZ_PILLAR = Blocks.QUARTZ_PILLAR.getDefaultState();
	static final BlockState QUARTZ_SLAB = Blocks.QUARTZ_SLAB.getDefaultState().with(Properties.SLAB_TYPE, SlabType.BOTTOM);
	static final BlockState LIGHT_SLAB = Blocks.SMOOTH_STONE_SLAB.getDefaultState().with(Properties.SLAB_TYPE, SlabType.BOTTOM);
	static final BlockState OAK_LEAVES = NeepMeatCompat.block("blood_bubble_leaves", Blocks.OAK_LEAVES.getDefaultState().with(Properties.PERSISTENT, true));
	static final BlockState BIRCH_LEAVES = Blocks.BIRCH_LEAVES.getDefaultState().with(Properties.PERSISTENT, true);
	static final BlockState AZALEA_LEAVES = Blocks.AZALEA_LEAVES.getDefaultState().with(Properties.PERSISTENT, true);
	static final BlockState RAIL_Z = rail(RailShape.NORTH_SOUTH);
	static final BlockState RAIL_X = rail(RailShape.EAST_WEST);
	static final BlockState RAIL_NORTH_EAST = rail(RailShape.NORTH_EAST);
	static final BlockState RAIL_NORTH_WEST = rail(RailShape.NORTH_WEST);
	static final BlockState RAIL_SOUTH_EAST = rail(RailShape.SOUTH_EAST);
	static final BlockState RAIL_SOUTH_WEST = rail(RailShape.SOUTH_WEST);
	static final BlockState RAIL_ASCENDING_EAST = rail(RailShape.ASCENDING_EAST);
	static final BlockState RAIL_ASCENDING_WEST = rail(RailShape.ASCENDING_WEST);
	static final BlockState RAIL_ASCENDING_NORTH = rail(RailShape.ASCENDING_NORTH);
	static final BlockState RAIL_ASCENDING_SOUTH = rail(RailShape.ASCENDING_SOUTH);
	static final BlockState IRON_ORE = Blocks.IRON_ORE.getDefaultState();
	static final BlockState COPPER_ORE = Blocks.COPPER_ORE.getDefaultState();
	static final BlockState COAL_ORE = Blocks.COAL_ORE.getDefaultState();
	static final BlockState REDSTONE_ORE = Blocks.REDSTONE_ORE.getDefaultState();

	private BlockPalette() {
	}

	private static BlockState rail(RailShape shape) {
		BlockState state = Blocks.RAIL.getDefaultState();
		return state.contains(Properties.RAIL_SHAPE) ? state.with(Properties.RAIL_SHAPE, shape) : state;
	}

	static BlockState stairs(Direction facing) {
		return Blocks.STONE_STAIRS.getDefaultState()
				.with(Properties.HORIZONTAL_FACING, facing)
				.with(Properties.BLOCK_HALF, BlockHalf.BOTTOM);
	}

	static BlockState vine(Direction attachedTo) {
		BlockState state = Blocks.VINE.getDefaultState();
		return switch (attachedTo) {
			case NORTH -> state.with(Properties.NORTH, true);
			case SOUTH -> state.with(Properties.SOUTH, true);
			case EAST -> state.with(Properties.EAST, true);
			case WEST -> state.with(Properties.WEST, true);
			default -> state;
		};
	}
}
