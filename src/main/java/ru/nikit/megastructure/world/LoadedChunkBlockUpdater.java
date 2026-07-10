package ru.nikit.megastructure.world;

import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

public final class LoadedChunkBlockUpdater {
	private static final int CHUNKS_PER_TICK = 1;
	private static final String NEEPMEAT = "neepmeat";
	private static final String RUSTY_METAL_LIGHT = "rusty_metal_light";
	private static final String CEILING_LIGHT = "ceiling_light";
	private static final String VASCULAR_CONDUIT = "vascular_conduit";
	private static final String ENCASED_VASCULAR_CONDUIT = "encased_vascular_conduit";
	private static final Queue<PendingChunk> PENDING_CHUNKS = new ConcurrentLinkedQueue<>();

	private record PendingChunk(ServerWorld world, int chunkX, int chunkZ, MegastructureChunkGenerator generator) {
	}

	private LoadedChunkBlockUpdater() {
	}

	public static void register() {
		ServerChunkEvents.CHUNK_LOAD.register(LoadedChunkBlockUpdater::queueLoadedChunk);
		ServerTickEvents.END_WORLD_TICK.register(LoadedChunkBlockUpdater::processQueuedChunks);
	}

	private static void queueLoadedChunk(ServerWorld world, WorldChunk chunk) {
		if (!(world.getChunkManager().getChunkGenerator() instanceof MegastructureChunkGenerator generator)) {
			return;
		}
		PENDING_CHUNKS.offer(new PendingChunk(world, chunk.getPos().x, chunk.getPos().z, generator));
	}

	private static void processQueuedChunks(ServerWorld tickWorld) {
		for (int i = 0; i < CHUNKS_PER_TICK; i++) {
			PendingChunk pending = PENDING_CHUNKS.poll();
			if (pending == null) {
				return;
			}
			if (pending.world().isChunkLoaded(pending.chunkX(), pending.chunkZ())) {
				WorldChunk chunk = pending.world().getChunkManager().getWorldChunk(
						pending.chunkX(),
						pending.chunkZ(),
						false
				);
				if (chunk != null) {
					updateLoadedChunk(pending.world(), chunk, pending.generator());
				}
			}
		}
	}

	private static void updateLoadedChunk(ServerWorld world, WorldChunk chunk, MegastructureChunkGenerator generator) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int startX = chunk.getPos().getStartX();
		int startZ = chunk.getPos().getStartZ();
		for (int localX = 0; localX < 16; localX++) {
			int x = startX + localX;
			for (int localZ = 0; localZ < 16; localZ++) {
				int z = startZ + localZ;
				for (int y = chunk.getBottomY(); y < chunk.getTopY(); y++) {
					mutable.set(x, y, z);
					BlockState state = chunk.getBlockState(mutable);
					if (state.isAir()) {
						continue;
					}

					BlockPos pos = mutable.toImmutable();
					state = refreshNaturalBlockState(world, pos, state);
					if (state.isAir()) {
						continue;
					}
					FluidState fluidState = state.getFluidState();
					if (!fluidState.isEmpty()) {
						scheduleFluid(world, pos, state, fluidState);
					}
					if (state.getBlock() instanceof FallingBlock) {
						scheduleFallingBlock(world, pos, state);
					}
					if (isNeepMeatBlock(state, RUSTY_METAL_LIGHT)) {
						state = switchLightOn(world, pos, state);
					} else if (isNeepMeatBlock(state, CEILING_LIGHT)) {
						state = replaceLegacyLamp(world, pos);
					}
					if (isVascularConduit(state)) {
						refreshVascularConduit(world, pos, state);
					}
				}
			}
		}
		retrofitRoomLootChests(world, chunk, generator);
	}

	private static void retrofitRoomLootChests(ServerWorld world, WorldChunk chunk, MegastructureChunkGenerator generator) {
		RetrofittedRoomChestState retrofitState = RetrofittedRoomChestState.get(world);
		if (!retrofitState.markProcessed(chunk.getPos().x, chunk.getPos().z)) {
			return;
		}

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int startX = chunk.getPos().getStartX();
		int startZ = chunk.getPos().getStartZ();
		int minY = Math.max(chunk.getBottomY(), generator.getMinimumY() + 1);
		int maxY = Math.min(chunk.getTopY(), generator.getMinimumY() + generator.getWorldHeight() - 1);
		for (int localX = 0; localX < 16; localX++) {
			int x = startX + localX;
			for (int localZ = 0; localZ < 16; localZ++) {
				int z = startZ + localZ;
				int district = generator.districtAt(x, z);
				for (int y = minY; y < maxY; y++) {
					mutable.set(x, y, z);
					if (!world.getBlockState(mutable).isAir()) {
						continue;
					}
					BlockState chestState = generator.roomLootChestState(district, x, y, z, true);
					if (chestState == null) {
						continue;
					}
					world.setBlockState(mutable, chestState, Block.NOTIFY_ALL);
					BlockEntity blockEntity = world.getBlockEntity(mutable);
					if (blockEntity instanceof ChestBlockEntity chest) {
						generator.fillExplorationLootChest(chest, x, y, z);
					}
				}
			}
		}
	}

	private static BlockState refreshNaturalBlockState(ServerWorld world, BlockPos pos, BlockState state) {
		BlockState refreshed = state;
		for (Direction direction : Direction.values()) {
			BlockPos neighborPos = pos.offset(direction);
			if (!world.isChunkLoaded(neighborPos.getX() >> 4, neighborPos.getZ() >> 4)) {
				continue;
			}
			refreshed = refreshed.getStateForNeighborUpdate(
					direction,
					world.getBlockState(neighborPos),
					world,
					pos,
					neighborPos
			);
			if (refreshed.isAir()) {
				world.setBlockState(pos, refreshed, Block.NOTIFY_ALL);
				return refreshed;
			}
		}

		if (!refreshed.equals(state)) {
			world.setBlockState(pos, refreshed, Block.NOTIFY_ALL);
			return refreshed;
		}
		if (shouldPulseNeighbors(world, pos, refreshed)) {
			world.updateNeighbors(pos, refreshed.getBlock());
		}
		return refreshed;
	}

	private static boolean shouldPulseNeighbors(ServerWorld world, BlockPos pos, BlockState state) {
		return !state.getFluidState().isEmpty()
				|| state.getBlock() instanceof FallingBlock
				|| !state.isFullCube(world, pos)
				|| !state.isOpaqueFullCube(world, pos);
	}

	private static void scheduleFluid(ServerWorld world, BlockPos pos, BlockState state, FluidState fluidState) {
		Fluid fluid = fluidState.getFluid();
		if (fluid == Fluids.EMPTY) {
			return;
		}
		world.scheduleFluidTick(pos, fluid, fluid.getTickRate(world));
		world.updateNeighbors(pos, state.getBlock());
	}

	private static void scheduleFallingBlock(ServerWorld world, BlockPos pos, BlockState state) {
		world.scheduleBlockTick(pos, state.getBlock(), 2);
		world.updateNeighbors(pos, state.getBlock());
	}

	private static BlockState switchLightOn(ServerWorld world, BlockPos pos, BlockState state) {
		if (!state.contains(Properties.LIT) || state.get(Properties.LIT)) {
			return state;
		}
		BlockState lit = state.with(Properties.LIT, true);
		world.setBlockState(pos, lit, Block.NOTIFY_ALL);
		return lit;
	}

	private static BlockState replaceLegacyLamp(ServerWorld world, BlockPos pos) {
		BlockState replacement = BlockPalette.LAMP;
		world.setBlockState(pos, replacement, Block.NOTIFY_ALL);
		return replacement;
	}

	private static void refreshVascularConduit(ServerWorld world, BlockPos pos, BlockState state) {
		BlockState refreshed = state;
		for (Direction direction : Direction.values()) {
			BlockPos neighborPos = pos.offset(direction);
			if (!world.isChunkLoaded(neighborPos.getX() >> 4, neighborPos.getZ() >> 4)) {
				continue;
			}
			refreshed = refreshed.getStateForNeighborUpdate(
					direction,
					world.getBlockState(neighborPos),
					world,
					pos,
					neighborPos
			);
		}

		if (!refreshed.equals(state)) {
			world.setBlockState(pos, refreshed, Block.NOTIFY_ALL);
		}
		refreshVascularBlockEntity(world, pos, refreshed);
		world.updateListeners(pos, state, refreshed, Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
		world.updateNeighbors(pos, refreshed.getBlock());
	}

	private static void refreshVascularBlockEntity(ServerWorld world, BlockPos pos, BlockState state) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null) {
			state.getBlock().onPlaced(world, pos, state, null, ItemStack.EMPTY);
			blockEntity = world.getBlockEntity(pos);
		}
		if (blockEntity == null) {
			return;
		}

		blockEntity.markDirty();
		invokeIfPresent(blockEntity, "onNeighbourUpdate");
		if (hasNullNetwork(blockEntity)) {
			state.getBlock().onPlaced(world, pos, state, null, ItemStack.EMPTY);
			blockEntity.markDirty();
		}
	}

	private static boolean hasNullNetwork(BlockEntity blockEntity) {
		try {
			Method method = blockEntity.getClass().getMethod("getNetwork");
			return method.invoke(blockEntity) == null;
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}

	private static void invokeIfPresent(Object target, String methodName) {
		try {
			Method method = target.getClass().getMethod(methodName);
			method.invoke(target);
		} catch (ReflectiveOperationException ignored) {
			// NeepMeat's encased conduit exposes this on its block entity; plain conduits do not need it.
		}
	}

	private static boolean isVascularConduit(BlockState state) {
		return isNeepMeatBlock(state, VASCULAR_CONDUIT) || isNeepMeatBlock(state, ENCASED_VASCULAR_CONDUIT);
	}

	private static boolean isNeepMeatBlock(BlockState state, String path) {
		Identifier id = Registries.BLOCK.getId(state.getBlock());
		return NEEPMEAT.equals(id.getNamespace()) && path.equals(id.getPath());
	}
}
