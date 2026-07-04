package ru.nikit.megastructure.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Identifier;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import ru.nikit.megastructure.survival.PrimitiveSurvivalContent;

public final class MegastructureChunkGenerator extends ChunkGenerator {
	public static final Codec<MegastructureChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
			MegastructureSettings.CODEC.optionalFieldOf("settings", MegastructureSettings.DEFAULT).forGetter(generator -> generator.settings)
	).apply(instance, MegastructureChunkGenerator::new));

	private static final long SHAPE_SEED = 0x4D4547415354524CL;
	static final int DISTRICT_SIZE = 1024;
	static final int DISTRICT_NETWORK = 0;
	static final int DISTRICT_DEAD_END = 1;
	static final int DISTRICT_MONOLITH_HALL = 2;
	static final int DISTRICT_COLUMN_FOREST = 3;
	static final int DISTRICT_CYLINDER = 4;
	static final int DISTRICT_ABYSS = 5;
	static final int DISTRICT_DESCENT = 6;
	static final int DISTRICT_BLOCK_TOWERS = 7;
	static final int DISTRICT_TANK_CLUSTER = 8;
	static final int DISTRICT_SCAFFOLD = 9;
	static final int DISTRICT_INDUSTRIAL_WALL = 10;
	static final int DISTRICT_DENSE_WALL = 11;
	static final int DISTRICT_TRANSIT_NEXUS = 12;
	static final int DISTRICT_REACTOR_CATHEDRAL = 13;
	static final int DISTRICT_HANGING_ARCHIVE = 14;
	static final int DISTRICT_VENTILATION_CANYON = 15;
	static final int DISTRICT_INVERTED_PYRAMID = 16;
	static final int DISTRICT_RING_VAULT = 17;
	static final int DISTRICT_MACHINE_NAVE = 18;
	static final int DISTRICT_FRACTURED_HABITAT = 19;
	static final int DISTRICT_CONDUIT_BASILICA = 20;
	static final int DISTRICT_RESERVOIR_HALL = 21;
	static final int DISTRICT_SUSPENDED_CITY = 22;
	static final int DISTRICT_IRIS_CHASM = 23;
	static final int DISTRICT_MACHINE_ROOT_VAULT = 24;
	static final int DISTRICT_TILTED_STACKS = 25;
	static final int DISTRICT_SILENT_FOUNDRY = 26;
	static final int DISTRICT_COLOSSUS_LIFT = 27;
	static final int DISTRICT_FOLDED_CITY = 28;
	static final int DISTRICT_UPPER_RIM_CITY = 29;
	static final int DISTRICT_ORBITAL_WEB_CORE = 30;
	static final int DISTRICT_CROWN_SPIRE = 31;
	static final int DISTRICT_GLOBE_MONUMENT = 32;
	static final int DISTRICT_VOID_ALTAR = 33;
	static final int DISTRICT_ATOM_STORM_ARRAY = 34;
	static final int DISTRICT_BLACK_HOLE_REACTOR = 35;
	private static final int[] TREE_DIRECTIONS_X = {100, 81, 31, -31, -81, -100, -81, -31, 31, 81};
	private static final int[] TREE_DIRECTIONS_Z = {0, 59, 95, 95, 59, 0, -59, -95, -95, -59};
	private static final int[] SPAWN_GIANT_DISTRICTS = {
			DISTRICT_BLOCK_TOWERS,
			DISTRICT_TRANSIT_NEXUS,
			DISTRICT_REACTOR_CATHEDRAL,
			DISTRICT_RING_VAULT,
			DISTRICT_MACHINE_NAVE,
			DISTRICT_RESERVOIR_HALL,
			DISTRICT_SUSPENDED_CITY,
			DISTRICT_IRIS_CHASM,
			DISTRICT_MACHINE_ROOT_VAULT,
			DISTRICT_SILENT_FOUNDRY,
			DISTRICT_COLOSSUS_LIFT,
			DISTRICT_FOLDED_CITY,
			DISTRICT_UPPER_RIM_CITY,
			DISTRICT_ORBITAL_WEB_CORE,
			DISTRICT_CROWN_SPIRE,
			DISTRICT_GLOBE_MONUMENT,
			DISTRICT_VOID_ALTAR,
			DISTRICT_ATOM_STORM_ARRAY
	};
	private static final int[][][] SPAWN_STATION_ROOMS = {
			{{-108, -42, 24, 58, 10, 0}, {34, 96, -58, -24, 9, 1}},
			{{-112, -70, 24, 54, 10, 0}, {-58, -16, -54, -24, 9, 1},
					{8, 50, 24, 54, 11, 2}, {62, 108, -54, -24, 10, 3}},
			{{-116, 8, 24, 62, 12, 1}, {30, 116, -62, -24, 10, 2}},
			{{-104, -16, 24, 58, 11, 3}, {16, 104, 24, 58, 11, 0},
					{-104, -16, -58, -24, 11, 2}, {16, 104, -58, -24, 11, 1}},
			{{-96, -52, 24, 48, 8, 0}, {-40, 4, -48, -24, 8, 3},
					{16, 60, 24, 48, 8, 2}, {72, 116, -48, -24, 8, 1}},
			{{-118, -34, 24, 60, 9, 2}, {24, 82, -56, -24, 12, 1}}
	};
	private static final int[] SPAWN_HALL_LENGTHS = {128, 120, 116, 136, 112, 124};
	private static final int[] SPAWN_HALL_WIDTHS = {29, 34, 31, 38, 27, 32};
	private static final int[] SPAWN_HALL_HEIGHTS = {13, 15, 14, 16, 12, 14};
	private static final int[] SPAWN_PLATFORM_LENGTHS = {124, 116, 112, 132, 108, 120};
	private static final int[] SPAWN_PLATFORM_WIDTHS = {28, 32, 30, 36, 26, 30};

	static final long DISTRICT_SEED = SHAPE_SEED;
	static final int DISTRICT_SPAN = DISTRICT_SIZE;
	private static volatile long activeWorldVariantSeed = SHAPE_SEED;

	private final MegastructureSettings settings;
	private final ConcurrentHashMap<Long, Optional<OasisDescriptor>> oasisCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, Integer> ruinGroundCache = new ConcurrentHashMap<>();
	private volatile long worldVariantSeed = SHAPE_SEED;
	private volatile boolean worldVariantInitialized;

	private record OasisDescriptor(
			int district,
			int districtX,
			int districtZ,
			int centerX,
			int centerZ,
			int floorY,
			int sourceY,
			int hostRadius,
			int pipeWallDistance,
			int basinX,
			int basinZ,
			int basinRadius,
			int side,
			int profile,
			int origin,
			int treeCount,
			boolean rootedDirt,
			long seed
	) {
	}

	private record RiftBridgeRoomLink(int entryX, int bridgeZ, int bridgeY, long seed) {
	}

	public record OasisRenderHint(
			int basinX,
			int basinY,
			int basinZ,
			int horizontalRadius,
			int verticalRadius,
			long seed
	) {
	}

	public record BlackHoleCoreHint(
			int x,
			int y,
			int z,
			int eventHorizonRadius,
			int influenceRadius,
			long seed
	) {
	}

	public record VulkanEffectHint(
			int x,
			int y,
			int z,
			int radius,
			int height,
			int kind,
			long seed,
			boolean volumetric
	) {
	}

	public MegastructureChunkGenerator(BiomeSource biomeSource, MegastructureSettings settings) {
		super(biomeSource);
		this.settings = settings;
	}

	@Override
	protected Codec<? extends ChunkGenerator> getCodec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<Chunk> populateBiomes(
			Executor executor,
			NoiseConfig noiseConfig,
			Blender blender,
			StructureAccessor structureAccessor,
			Chunk chunk
	) {
		initializeWorldVariant(noiseConfig);
		return super.populateBiomes(executor, noiseConfig, blender, structureAccessor, chunk);
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
		initializeWorldVariant(noiseConfig);
		return CompletableFuture.supplyAsync(() -> populateNoiseChunk(chunk), executor);
	}

	private Chunk populateNoiseChunk(Chunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int minY = Math.max(chunk.getBottomY(), settings.floorY());
		int maxY = Math.min(chunk.getTopY(), settings.ceilingY() + 1);

		for (int localX = 0; localX < 16; localX++) {
			int x = chunkPos.getStartX() + localX;
			for (int localZ = 0; localZ < 16; localZ++) {
				int z = chunkPos.getStartZ() + localZ;
				for (int y = minY; y < maxY; y++) {
					BlockState state = stateAt(x, y, z);
					if (!state.isAir()) {
						chunk.setBlockState(mutable.set(x, y, z), state, false);
					}
				}
			}
		}
		return chunk;
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
	}

	@Override
	public void carve(
			ChunkRegion chunkRegion,
			long seed,
			NoiseConfig noiseConfig,
			BiomeAccess biomeAccess,
			StructureAccessor structureAccessor,
			Chunk chunk,
			GenerationStep.Carver carverStep
	) {
	}

	@Override
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
		super.generateFeatures(world, chunk, structureAccessor);
		populateCorridorLootChests(world, chunk);
	}

	@Override
	public void populateEntities(ChunkRegion region) {
	}

	@Override
	public int getWorldHeight() {
		return settings.ceilingY() - settings.floorY() + 1;
	}

	@Override
	public int getSeaLevel() {
		return settings.seaLevel();
	}

	@Override
	public int getMinimumY() {
		return settings.floorY();
	}

	@Override
	public int getSpawnHeight(HeightLimitView world) {
		return spawnRailY() + 1;
	}

	public int spawnRailY() {
		return settings.spawnPlatformY() + 1;
	}

	public Optional<OasisLocation> findNearestOasis(BlockPos origin, int radiusDistricts) {
		int originDistrictX = MegastructureMath.floorDiv(origin.getX(), DISTRICT_SIZE);
		int originDistrictZ = MegastructureMath.floorDiv(origin.getZ(), DISTRICT_SIZE);
		OasisDescriptor nearest = null;
		long nearestDistance2 = Long.MAX_VALUE;
		for (int offsetX = -radiusDistricts; offsetX <= radiusDistricts; offsetX++) {
			for (int offsetZ = -radiusDistricts; offsetZ <= radiusDistricts; offsetZ++) {
				int districtX = originDistrictX + offsetX;
				int districtZ = originDistrictZ + offsetZ;
				Optional<OasisDescriptor> candidate = oasisCache.computeIfAbsent(
						packDistrict(districtX, districtZ),
						ignored -> createOasisDescriptor(districtX, districtZ)
				);
				if (candidate.isEmpty()) {
					continue;
				}
				OasisDescriptor oasis = candidate.get();
				long dx = (long) oasis.basinX() - origin.getX();
				long dz = (long) oasis.basinZ() - origin.getZ();
				long distance2 = dx * dx + dz * dz;
				if (distance2 < nearestDistance2) {
					nearest = oasis;
					nearestDistance2 = distance2;
				}
			}
		}
		if (nearest == null) {
			return Optional.empty();
		}
		int viewingOffset = nearest.basinRadius() + 8;
		int viewingX = nearest.basinX() + (nearest.side() <= 1 ? 0 : viewingOffset);
		int viewingZ = nearest.basinZ() + (nearest.side() <= 1 ? viewingOffset : 0);
		BlockPos position = new BlockPos(viewingX, nearest.floorY() + 2, viewingZ);
		return Optional.of(new OasisLocation(
				position,
				districtName(nearest.district()),
				oasisCompositionName(nearest),
				(int) Math.round(Math.sqrt(nearestDistance2))
		));
	}

	/**
	 * Server-side oasis test used by global story progression. It operates on the same deterministic
	 * descriptors as world generation and does not rely on biome names or already-loaded vegetation.
	 */
	public boolean isInsideOasis(BlockPos position) {
		if (isInsidePrimaryRailOasis(position)) {
			return true;
		}
		int originDistrictX = MegastructureMath.floorDiv(position.getX(), DISTRICT_SIZE);
		int originDistrictZ = MegastructureMath.floorDiv(position.getZ(), DISTRICT_SIZE);
		for (int offsetX = -1; offsetX <= 1; offsetX++) {
			for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
				int districtX = originDistrictX + offsetX;
				int districtZ = originDistrictZ + offsetZ;
				Optional<OasisDescriptor> candidate = oasisCache.computeIfAbsent(
						packDistrict(districtX, districtZ),
						ignored -> createOasisDescriptor(districtX, districtZ)
				);
				if (candidate.isPresent() && isInsideOasis(candidate.get(), position)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isInsideOasis(OasisDescriptor oasis, BlockPos position) {
		int horizontalReach = oasis.basinRadius() + (isGiantOasisHostDistrict(oasis.district()) ? 260 : 112);
		long dx = (long) position.getX() - oasis.basinX();
		long dz = (long) position.getZ() - oasis.basinZ();
		if (dx * dx + dz * dz > (long) horizontalReach * horizontalReach) {
			return false;
		}
		int minY = oasis.floorY() - 8;
		int maxY = Math.max(oasis.sourceY() + 24, oasis.floorY() + oasisVerticalReach(oasis));
		return position.getY() >= minY && position.getY() <= maxY;
	}

	private boolean isInsidePrimaryRailOasis(BlockPos position) {
		int center = position.getX() >= 0 ? primaryRailOasisCenter(true) : primaryRailOasisCenter(false);
		int baseY = primaryRailYAt(center);
		if (position.getY() < baseY || position.getY() > baseY + 64 || Math.abs(position.getX() - center) > 132 || Math.abs(position.getZ()) > 88) {
			return false;
		}
		long seed = MegastructureMath.hash(worldVariantSeed, center, 0, 2053);
		int side = Math.floorMod(seed, 2) == 0 ? 1 : -1;
		int basinX = center + MegastructureMath.range(seed >>> 8, -34, 34);
		int basinZ = side * MegastructureMath.range(seed >>> 18, 34, 46);
		int basinRadius = MegastructureMath.range(seed >>> 26, 22, 31);
		int reach = basinRadius + 48;
		long dx = (long) position.getX() - basinX;
		long dz = (long) position.getZ() - basinZ;
		return dx * dx + dz * dz <= (long) reach * reach;
	}

	public static Optional<OasisRenderHint> findNearestOasisRenderHint(double x, double y, double z, int radiusDistricts) {
		int originDistrictX = MegastructureMath.floorDiv((int) Math.floor(x), DISTRICT_SIZE);
		int originDistrictZ = MegastructureMath.floorDiv((int) Math.floor(z), DISTRICT_SIZE);
		OasisRenderHint nearest = null;
		double nearestScore = Double.MAX_VALUE;
		for (int offsetX = -radiusDistricts; offsetX <= radiusDistricts; offsetX++) {
			for (int offsetZ = -radiusDistricts; offsetZ <= radiusDistricts; offsetZ++) {
				Optional<OasisRenderHint> hint = createOasisRenderHint(originDistrictX + offsetX, originDistrictZ + offsetZ);
				if (hint.isEmpty()) {
					continue;
				}
				OasisRenderHint oasis = hint.get();
				double dx = oasis.basinX() - x;
				double dz = oasis.basinZ() - z;
				double dy = Math.max(0.0, Math.abs(oasis.basinY() - y) - oasis.verticalRadius() * 0.72);
				double score = dx * dx + dz * dz + dy * dy * 0.18;
				if (score < nearestScore) {
					nearest = oasis;
					nearestScore = score;
				}
			}
		}
		return Optional.ofNullable(nearest);
	}

	public static Optional<BlackHoleCoreHint> findNearestBlackHoleCore(double x, double y, double z, int radiusDistricts) {
		int originDistrictX = MegastructureMath.floorDiv((int) Math.floor(x), DISTRICT_SIZE);
		int originDistrictZ = MegastructureMath.floorDiv((int) Math.floor(z), DISTRICT_SIZE);
		BlackHoleCoreHint nearest = null;
		double nearestDistance2 = Double.MAX_VALUE;
		for (int offsetX = -radiusDistricts; offsetX <= radiusDistricts; offsetX++) {
			for (int offsetZ = -radiusDistricts; offsetZ <= radiusDistricts; offsetZ++) {
				int districtX = originDistrictX + offsetX;
				int districtZ = originDistrictZ + offsetZ;
				int centerX = districtX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				int centerZ = districtZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				if (districtTypeAt(centerX, centerZ) != DISTRICT_BLACK_HOLE_REACTOR) {
					continue;
				}
				BlackHoleCoreHint hint = blackHoleCoreHintForDistrict(districtX, districtZ);
				double distance2 = squaredDistance(x, y, z, hint.x(), hint.y(), hint.z());
				if (distance2 < nearestDistance2) {
					nearest = hint;
					nearestDistance2 = distance2;
				}
			}
		}
		return Optional.ofNullable(nearest);
	}

	public static Optional<VulkanEffectHint> findNearestVulkanEffectHint(double x, double y, double z, int radiusDistricts) {
		int blockX = (int) Math.floor(x);
		int blockZ = (int) Math.floor(z);
		int originDistrictX = MegastructureMath.floorDiv(blockX, DISTRICT_SIZE);
		int originDistrictZ = MegastructureMath.floorDiv(blockZ, DISTRICT_SIZE);
		VulkanEffectHint nearest = null;
		double nearestScore = Double.MAX_VALUE;
		for (int offsetX = -radiusDistricts; offsetX <= radiusDistricts; offsetX++) {
			for (int offsetZ = -radiusDistricts; offsetZ <= radiusDistricts; offsetZ++) {
				int districtX = originDistrictX + offsetX;
				int districtZ = originDistrictZ + offsetZ;
				int centerX = districtX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				int centerZ = districtZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				int district = districtTypeAt(centerX, centerZ, activeWorldVariantSeed);
				int kind = vulkanEffectKindForDistrict(district);
				if (kind < 0) {
					continue;
				}
				int baseY = clientOasisHostFloorY(district, centerX, centerZ);
				int centerY = baseY + vulkanEffectYOffset(district);
				int radius = vulkanEffectRadiusForDistrict(district);
				int height = vulkanEffectHeightForDistrict(district);
				double dx = centerX - x;
				double dz = centerZ - z;
				double dy = Math.max(0.0, Math.abs(centerY - y) - height * 0.45);
				double score = dx * dx + dz * dz + dy * dy * 0.22;
				if (score < nearestScore) {
					long seed = MegastructureMath.hash(activeWorldVariantSeed, districtX, districtZ, 4099 + kind);
					nearest = new VulkanEffectHint(centerX, centerY, centerZ, radius, height, kind, seed,
							district == DISTRICT_GLOBE_MONUMENT);
					nearestScore = score;
				}
			}
		}
		return Optional.ofNullable(nearest);
	}

	private static BlackHoleCoreHint blackHoleCoreHintForDistrict(int districtX, int districtZ) {
		int centerX = districtX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
		int centerZ = districtZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
		int baseY = clientDistrictBaseY(centerX, centerZ, 1543, 54, 300);
		long seed = MegastructureMath.hash(activeWorldVariantSeed, districtX, districtZ, 3559);
		return new BlackHoleCoreHint(centerX, baseY + 214, centerZ, 24, 224, seed);
	}

	private boolean isBlackHoleCoreExclusion(int x, int y, int z) {
		int district = districtType(x, z);
		if (district != DISTRICT_BLACK_HOLE_REACTOR) {
			return false;
		}
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		BlackHoleCoreHint core = blackHoleCoreHintForDistrict(districtX, districtZ);
		long dx = (long) x - core.x();
		long dy = (long) y - core.y();
		long dz = (long) z - core.z();
		long horizontal2 = dx * dx + dz * dz;
		long sphere2 = horizontal2 + dy * dy;
		return sphere2 <= 144L * 144L
				|| horizontal2 <= 92L * 92L
				|| (Math.abs(dy) <= 28 && horizontal2 <= 204L * 204L);
	}

	private static double squaredDistance(double x, double y, double z, double targetX, double targetY, double targetZ) {
		double dx = x - targetX;
		double dy = y - targetY;
		double dz = z - targetZ;
		return dx * dx + dy * dy + dz * dz;
	}

	private static Optional<OasisRenderHint> createOasisRenderHint(int districtX, int districtZ) {
		int centerX = districtX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
		int centerZ = districtZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
		int district = districtTypeAt(centerX, centerZ, activeWorldVariantSeed);
		if (!isOasisHostDistrict(district)) {
			return Optional.empty();
		}
		long key = packDistrict(districtX, districtZ);
		long seed = MegastructureMath.hash(activeWorldVariantSeed, districtX, districtZ, 1701);
		long guaranteedOasisKey = findGuaranteedOasisKey(activeWorldVariantSeed);
		boolean guaranteed = key == guaranteedOasisKey;
		if (!guaranteed && Math.floorMod(seed, 40) != 0) {
			return Optional.empty();
		}
		if (!guaranteed && hasNearbyHigherPriorityOasisStatic(districtX, districtZ, seed)) {
			return Optional.empty();
		}

		int side = Math.floorMod((int) (seed >>> 8), 4);
		int radius = clientOasisHostRadius(district, side);
		int basinRadius = isGiantOasisHostDistrict(district)
				? MegastructureMath.range(seed >>> 20, 52, 88)
				: MegastructureMath.range(seed >>> 20, 16, 34);
		int basinDistance = isGiantOasisHostDistrict(district)
				? MegastructureMath.range(seed >>> 34, 48, Math.max(64, radius - 72))
				: Math.max(34, Math.min(radius + 24, radius - 12 + basinRadius));
		int basinX = centerX + sideOffsetX(side, basinDistance);
		int basinZ = centerZ + sideOffsetZ(side, basinDistance);
		int floorY = clientOasisHostFloorY(district, centerX, centerZ);
		int horizontalRadius = isGiantOasisHostDistrict(district)
				? Math.max(260, basinRadius + 230)
				: Math.max(112, basinRadius + 96);
		int verticalRadius = isGiantOasisHostDistrict(district) ? 300 : 150;
		return Optional.of(new OasisRenderHint(
				basinX,
				floorY + Math.max(8, basinRadius / 3),
				basinZ,
				horizontalRadius,
				verticalRadius,
				seed
		));
	}

	private static boolean hasNearbyHigherPriorityOasisStatic(int districtX, int districtZ, long priority) {
		for (int offsetX = -2; offsetX <= 2; offsetX++) {
			for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
				if (offsetX == 0 && offsetZ == 0) {
					continue;
				}
				int neighborX = districtX + offsetX;
				int neighborZ = districtZ + offsetZ;
				long neighborKey = packDistrict(neighborX, neighborZ);
				int blockX = neighborX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				int blockZ = neighborZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				if (!isOasisHostDistrict(districtTypeAt(blockX, blockZ, activeWorldVariantSeed))) {
					continue;
				}
				long neighborPriority = MegastructureMath.hash(activeWorldVariantSeed, neighborX, neighborZ, 1701);
				boolean neighborCandidate = neighborKey == findGuaranteedOasisKey(activeWorldVariantSeed) || Math.floorMod(neighborPriority, 40) == 0;
				if (neighborCandidate && Long.compareUnsigned(neighborPriority, priority) < 0) {
					return true;
				}
			}
		}
		return false;
	}

	private static int clientOasisHostFloorY(int district, int x, int z) {
		MegastructureSettings settings = MegastructureSettings.DEFAULT;
		return switch (district) {
			case DISTRICT_TANK_CLUSTER -> settings.floorY() + MegastructureMath.range(clientDistrictHash(x, z, 941), 54, 612);
			case DISTRICT_RESERVOIR_HALL -> clientDistrictBaseY(x, z, 1459, 48, 420);
			case DISTRICT_REACTOR_CATHEDRAL -> clientDistrictBaseY(x, z, 1403, 48, 360);
			case DISTRICT_RING_VAULT -> clientDistrictBaseY(x, z, 1433, 52, 360);
			case DISTRICT_INDUSTRIAL_WALL -> settings.floorY() + MegastructureMath.range(clientDistrictHash(x, z, 1301) >>> 8, 40, 220) - 1;
			case DISTRICT_SCAFFOLD -> settings.floorY() + MegastructureMath.range(clientDistrictHash(x, z, 947), 48, 650) - 1;
			case DISTRICT_COLUMN_FOREST -> settings.floorY() + MegastructureMath.range(clientDistrictHash(x, z, 823), 64, 704) - 1;
			case DISTRICT_MONOLITH_HALL -> settings.floorY() + MegastructureMath.range(clientDistrictHash(x, z, 811), 96, 624) - 5;
			case DISTRICT_FRACTURED_HABITAT -> clientDistrictBaseY(x, z, 1447, 54, 380);
			case DISTRICT_CONDUIT_BASILICA -> clientDistrictBaseY(x, z, 1451, 60, 360);
			case DISTRICT_HANGING_ARCHIVE -> clientDistrictBaseY(x, z, 1409, 64, 340);
			case DISTRICT_BLOCK_TOWERS -> settings.floorY() + 18;
			case DISTRICT_MACHINE_NAVE -> clientDistrictBaseY(x, z, 1439, 70, 480);
			case DISTRICT_SUSPENDED_CITY -> clientDistrictBaseY(x, z, 1463, 52, 330);
			case DISTRICT_SILENT_FOUNDRY -> clientDistrictBaseY(x, z, 1487, 54, 360);
			case DISTRICT_COLOSSUS_LIFT -> clientDistrictBaseY(x, z, 1493, 42, 280);
			case DISTRICT_FOLDED_CITY -> clientDistrictBaseY(x, z, 1499, 56, 360);
			case DISTRICT_UPPER_RIM_CITY -> clientDistrictBaseY(x, z, 1505, 42, 300);
			case DISTRICT_ORBITAL_WEB_CORE -> clientDistrictBaseY(x, z, 1511, 60, 360);
			case DISTRICT_CROWN_SPIRE -> clientDistrictBaseY(x, z, 1521, 34, 240);
			case DISTRICT_GLOBE_MONUMENT -> clientDistrictBaseY(x, z, 1527, 60, 320);
			case DISTRICT_VOID_ALTAR -> clientDistrictBaseY(x, z, 1531, 52, 340);
			case DISTRICT_ATOM_STORM_ARRAY -> clientDistrictBaseY(x, z, 1537, 48, 320);
			default -> settings.spawnPlatformY();
		};
	}

	private static int clientOasisHostRadius(int district, int side) {
		boolean xSide = side <= 1;
		return switch (district) {
			case DISTRICT_TANK_CLUSTER -> 170;
			case DISTRICT_RESERVOIR_HALL -> 276;
			case DISTRICT_REACTOR_CATHEDRAL -> xSide ? 174 : 318;
			case DISTRICT_RING_VAULT -> 246;
			case DISTRICT_INDUSTRIAL_WALL -> 96;
			case DISTRICT_SCAFFOLD -> 152;
			case DISTRICT_COLUMN_FOREST -> 150;
			case DISTRICT_MONOLITH_HALL -> xSide ? 160 : 142;
			case DISTRICT_FRACTURED_HABITAT -> xSide ? 268 : 238;
			case DISTRICT_CONDUIT_BASILICA -> xSide ? 190 : 364;
			case DISTRICT_HANGING_ARCHIVE -> 238;
			case DISTRICT_BLOCK_TOWERS -> 500;
			case DISTRICT_MACHINE_NAVE -> xSide ? 372 : 174;
			case DISTRICT_SUSPENDED_CITY -> xSide ? 330 : 270;
			case DISTRICT_SILENT_FOUNDRY -> xSide ? 404 : 238;
			case DISTRICT_COLOSSUS_LIFT -> 276;
			case DISTRICT_FOLDED_CITY -> xSide ? 352 : 292;
			case DISTRICT_UPPER_RIM_CITY -> 452;
			case DISTRICT_ORBITAL_WEB_CORE -> 340;
			case DISTRICT_CROWN_SPIRE -> 472;
			case DISTRICT_GLOBE_MONUMENT -> 340;
			case DISTRICT_VOID_ALTAR -> 320;
			case DISTRICT_ATOM_STORM_ARRAY -> 374;
			default -> 128;
		};
	}

	private static long clientDistrictHash(int x, int z, int salt) {
		return MegastructureMath.hash(
				activeWorldVariantSeed,
				MegastructureMath.floorDiv(x, DISTRICT_SIZE),
				MegastructureMath.floorDiv(z, DISTRICT_SIZE),
				salt
		);
	}

	private static int clientDistrictBaseY(int x, int z, int salt, int minOffset, int maxOffset) {
		MegastructureSettings settings = MegastructureSettings.DEFAULT;
		int safeMaximum = Math.min(maxOffset, Math.max(minOffset, settings.ceilingY() - settings.floorY() - 360));
		return settings.floorY() + MegastructureMath.range(clientDistrictHash(x, z, salt), minOffset, safeMaximum);
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		initializeWorldVariant(noiseConfig);
		for (int y = Math.min(settings.ceilingY(), world.getTopY() - 1); y >= Math.max(settings.floorY(), world.getBottomY()); y--) {
			BlockState state = stateAt(x, y, z);
			if (heightmap.getBlockPredicate().test(state)) {
				return y + 1;
			}
		}
		return world.getBottomY();
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		initializeWorldVariant(noiseConfig);
		int minY = Math.max(settings.floorY(), world.getBottomY());
		int maxY = Math.min(settings.ceilingY() + 1, world.getTopY());
		BlockState[] states = new BlockState[maxY - minY];
		for (int y = minY; y < maxY; y++) {
			states[y - minY] = stateAt(x, y, z);
		}
		return new VerticalBlockSample(minY, states);
	}

	@Override
	public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
		initializeWorldVariant(noiseConfig);
		text.add("Megastructure: " + motifName(pos.getX(), pos.getZ()));
		int districtX = MegastructureMath.floorDiv(pos.getX(), DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(pos.getZ(), DISTRICT_SIZE);
		oasisCache.computeIfAbsent(packDistrict(districtX, districtZ), ignored -> createOasisDescriptor(districtX, districtZ))
				.ifPresent(oasis -> text.add("Oasis overlay: " + oasisCompositionName(oasis)));
	}

	private String oasisProfileName(int profile) {
		return switch (profile) {
			case 1 -> "cistern garden";
			case 2 -> "hanging seep";
			case 3 -> "monolith spring";
			case 4 -> "terraced deluge";
			case 5 -> "root cathedral";
			case 6 -> "broken aqueduct";
			case 7 -> "drowned gallery";
			case 8 -> "hanging delta";
			default -> "pipefall";
		};
	}

	private String oasisCompositionName(OasisDescriptor oasis) {
		StringBuilder name = new StringBuilder(oasisOriginName(oasis.origin()))
				.append(" / ").append(oasisProfileName(oasis.profile()));
		for (int feature = 4; feature <= 8; feature++) {
			if (feature != oasis.profile() && oasisSceneFeatureEnabled(oasis, feature)) {
				name.append(" + ").append(oasisProfileName(feature));
			}
		}
		int basinLobes = 1 + MegastructureMath.range(oasis.seed() >>> 42, 0, 3);
		return name.append(" / ").append(basinLobes).append("-lobe catchment").toString();
	}

	private String oasisOriginName(int origin) {
		return switch (origin) {
			case 1 -> "condensation canopy";
			case 2 -> "structural seam seep";
			case 3 -> "pressure spring";
			case 4 -> "abandoned filtration bed";
			default -> "ruptured conduit";
		};
	}

	private void initializeWorldVariant(NoiseConfig noiseConfig) {
		if (worldVariantInitialized) {
			return;
		}
		synchronized (this) {
			if (worldVariantInitialized) {
				return;
			}
			worldVariantSeed = noiseConfig
					.getOrCreateRandomDeriver(new Identifier("megastructure", "spawn_variant"))
					.split(0, 0, 0)
					.nextLong();
			activeWorldVariantSeed = worldVariantSeed;
			if (biomeSource instanceof DistrictBiomeSource districtBiomeSource) {
				districtBiomeSource.setWorldVariantSeed(worldVariantSeed);
			}
			worldVariantInitialized = true;
		}
	}

	private BlockState stateAt(int x, int y, int z) {
		if (y < settings.floorY() || y > settings.ceilingY()) {
			return BlockPalette.AIR;
		}
		if (isBlackHoleCoreExclusion(x, y, z)) {
			return BlockPalette.AIR;
		}

		BlockState spawnPrecinct = spawnPrecinctState(x, y, z);
		if (spawnPrecinct != null) {
			return spawnPrecinct;
		}
		if (isSpawnPrecinctAir(x, y, z)) {
			return BlockPalette.AIR;
		}

		boolean rift = isPrimaryRift(x, z);
		BlockState railway = railwayState(x, y, z);
		if (railway != null) {
			return railway;
		}
		BlockState connectorNetwork = connectorNetworkState(x, y, z);
		if (connectorNetwork != null) {
			return connectorNetwork;
		}
		if (rift) {
			BlockState riftBridge = riftSuspensionBridgeState(x, y, z);
			return riftBridge == null ? BlockPalette.AIR : riftBridge;
		}

		BlockState riftAccess = riftBridgeAccessState(x, y, z);
		if (riftAccess != null) {
			return riftAccess;
		}
		BlockState bridgeLink = riftBridgeRoomLinkState(x, y, z);
		if (bridgeLink != null) {
			return bridgeLink;
		}
		BlockState districtAccess = districtAccessState(x, y, z);
		if (districtAccess != null) {
			return districtAccess;
		}

		int district = districtType(x, z);
		BlockState oasis = oasisOverlayState(district, x, y, z);
		if (oasis != null) {
			return oasis;
		}
		boolean air = isRailwayAir(x, y, z) || districtAir(district, x, y, z);

		BlockState structural = structuralOverlay(district, x, y, z, air);
		if (structural != null) {
			return structural;
		}

		BlockState lava = lavaReservoirState(district, x, y, z, air);
		if (lava != null) {
			return lava;
		}

		BlockState looseStone = looseStoneScatterState(district, x, y, z, air);
		if (looseStone != null) {
			return looseStone;
		}

		BlockState chest = corridorLootChestState(district, x, y, z, air);
		if (chest != null) {
			return chest;
		}

		if (air) {
			return BlockPalette.AIR;
		}

		BlockState ore = wallOreState(district, x, y, z);
		if (ore != null) {
			return ore;
		}

		BlockState detail = wallDetail(x, y, z);
		if (detail != null) {
			return detail;
		}

		if (y <= settings.floorY() + 3 || y >= settings.ceilingY() - 4) {
			return BlockPalette.FOUNDATION;
		}

		return massState(x, y, z);
	}

	private int districtType(int x, int z) {
		return districtTypeAt(x, z, worldVariantSeed);
	}

	static int districtTypeAt(int x, int z) {
		return districtTypeAt(x, z, activeWorldVariantSeed);
	}

	public static int districtTypeAt(int x, int z, long worldVariantSeed) {
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		if (districtX == 0 && districtZ == 0) {
			return DISTRICT_DENSE_WALL;
		}
		if (districtX == spawnLandmarkDistrictX(worldVariantSeed)
				&& districtZ == spawnLandmarkDistrictZ(worldVariantSeed)) {
			return spawnLandmarkDistrictType(worldVariantSeed);
		}
		long hash = MegastructureMath.hash(SHAPE_SEED ^ worldVariantSeed, districtX, districtZ, 501);
		int roll = Math.floorMod((int) hash, 360);
		if (roll < 20) {
			return DISTRICT_NETWORK;
		}
		if (roll < 32) {
			return DISTRICT_DEAD_END;
		}
		if (roll < 44) {
			return DISTRICT_MONOLITH_HALL;
		}
		if (roll < 56) {
			return DISTRICT_COLUMN_FOREST;
		}
		if (roll < 68) {
			return DISTRICT_CYLINDER;
		}
		if (roll < 80) {
			return DISTRICT_ABYSS;
		}
		if (roll < 92) {
			return DISTRICT_DESCENT;
		}
		if (roll < 104) {
			return DISTRICT_BLOCK_TOWERS;
		}
		if (roll < 116) {
			return DISTRICT_TANK_CLUSTER;
		}
		if (roll < 128) {
			return DISTRICT_SCAFFOLD;
		}
		if (roll < 140) {
			return DISTRICT_INDUSTRIAL_WALL;
		}
		if (roll < 150) {
			return DISTRICT_DENSE_WALL;
		}
		if (roll < 157) {
			return DISTRICT_TRANSIT_NEXUS;
		}
		if (roll < 164) {
			return DISTRICT_REACTOR_CATHEDRAL;
		}
		if (roll < 171) {
			return DISTRICT_HANGING_ARCHIVE;
		}
		if (roll < 178) {
			return DISTRICT_VENTILATION_CANYON;
		}
		if (roll < 185) {
			return DISTRICT_INVERTED_PYRAMID;
		}
		if (roll < 192) {
			return DISTRICT_RING_VAULT;
		}
		if (roll < 199) {
			return DISTRICT_MACHINE_NAVE;
		}
		if (roll < 206) {
			return DISTRICT_FRACTURED_HABITAT;
		}
		if (roll < 213) {
			return DISTRICT_CONDUIT_BASILICA;
		}
		if (roll < 220) {
			return DISTRICT_RESERVOIR_HALL;
		}
		if (roll < 230) {
			return DISTRICT_SUSPENDED_CITY;
		}
		if (roll < 240) {
			return DISTRICT_IRIS_CHASM;
		}
		if (roll < 250) {
			return DISTRICT_MACHINE_ROOT_VAULT;
		}
		if (roll < 260) {
			return DISTRICT_TILTED_STACKS;
		}
		if (roll < 270) {
			return DISTRICT_SILENT_FOUNDRY;
		}
		if (roll < 280) {
			return DISTRICT_COLOSSUS_LIFT;
		}
		if (roll < 290) {
			return DISTRICT_FOLDED_CITY;
		}
		if (roll < 300) {
			return DISTRICT_UPPER_RIM_CITY;
		}
		if (roll < 310) {
			return DISTRICT_ORBITAL_WEB_CORE;
		}
		if (roll < 322) {
			return DISTRICT_CROWN_SPIRE;
		}
		if (roll < 333) {
			return DISTRICT_GLOBE_MONUMENT;
		}
		if (roll < 344) {
			return DISTRICT_VOID_ALTAR;
		}
		if (roll < 356) {
			return DISTRICT_ATOM_STORM_ARRAY;
		}
		return Math.floorMod(roll, 2) == 0 ? DISTRICT_VOID_ALTAR : DISTRICT_ATOM_STORM_ARRAY;
	}

	private static int vulkanEffectKindForDistrict(int district) {
		return switch (district) {
			case DISTRICT_IRIS_CHASM -> 0;
			case DISTRICT_VOID_ALTAR, DISTRICT_ORBITAL_WEB_CORE, DISTRICT_ABYSS, DISTRICT_DESCENT -> 1;
			case DISTRICT_ATOM_STORM_ARRAY, DISTRICT_REACTOR_CATHEDRAL, DISTRICT_GLOBE_MONUMENT -> 2;
			default -> -1;
		};
	}

	private static int vulkanEffectYOffset(int district) {
		return switch (district) {
			case DISTRICT_TANK_CLUSTER -> 96;
			case DISTRICT_RESERVOIR_HALL -> 128;
			case DISTRICT_REACTOR_CATHEDRAL -> 150;
			case DISTRICT_ATOM_STORM_ARRAY -> 178;
			case DISTRICT_VOID_ALTAR -> 148;
			case DISTRICT_ORBITAL_WEB_CORE -> 186;
			case DISTRICT_GLOBE_MONUMENT -> 178;
			case DISTRICT_TRANSIT_NEXUS -> 90;
			case DISTRICT_SILENT_FOUNDRY, DISTRICT_INDUSTRIAL_WALL -> 82;
			default -> 132;
		};
	}

	private static int vulkanEffectRadiusForDistrict(int district) {
		return switch (district) {
			case DISTRICT_ATOM_STORM_ARRAY -> 360;
			case DISTRICT_VOID_ALTAR -> 300;
			case DISTRICT_ORBITAL_WEB_CORE -> 340;
			case DISTRICT_GLOBE_MONUMENT -> 330;
			case DISTRICT_REACTOR_CATHEDRAL -> 280;
			case DISTRICT_RESERVOIR_HALL -> 260;
			case DISTRICT_TANK_CLUSTER -> 190;
			case DISTRICT_VENTILATION_CANYON, DISTRICT_IRIS_CHASM -> 360;
			case DISTRICT_TRANSIT_NEXUS -> 260;
			default -> 230;
		};
	}

	private static int vulkanEffectHeightForDistrict(int district) {
		return switch (district) {
			case DISTRICT_ATOM_STORM_ARRAY, DISTRICT_ORBITAL_WEB_CORE, DISTRICT_VOID_ALTAR -> 520;
			case DISTRICT_VENTILATION_CANYON, DISTRICT_IRIS_CHASM -> 640;
			case DISTRICT_RESERVOIR_HALL, DISTRICT_REACTOR_CATHEDRAL, DISTRICT_GLOBE_MONUMENT -> 420;
			case DISTRICT_TRANSIT_NEXUS -> 260;
			default -> 340;
		};
	}

	private static int spawnLandmarkDistrictX(long seed) {
		return Math.floorMod((int) MegastructureMath.hash(seed, 0, 0, 1921), 3) == 1 ? 0 : -1;
	}

	private static int spawnLandmarkDistrictZ(long seed) {
		return Math.floorMod((int) MegastructureMath.hash(seed, 0, 0, 1921), 3) == 0 ? 0 : -1;
	}

	private static int spawnLandmarkDistrictType(long seed) {
		long hash = MegastructureMath.hash(seed, 0, 0, 1931);
		return SPAWN_GIANT_DISTRICTS[Math.floorMod((int) hash, SPAWN_GIANT_DISTRICTS.length)];
	}

	static boolean isDistrictBiomeFootprintAt(int district, int x, int z) {
		int dx = MegastructureMath.floorMod(x, DISTRICT_SIZE) - DISTRICT_SIZE / 2;
		int dz = MegastructureMath.floorMod(z, DISTRICT_SIZE) - DISTRICT_SIZE / 2;
		long dist2 = (long) dx * dx + (long) dz * dz;
		return switch (district) {
			case DISTRICT_NETWORK, DISTRICT_DEAD_END, DISTRICT_DENSE_WALL -> true;
			case DISTRICT_MONOLITH_HALL -> Math.abs(dx) <= 170 && Math.abs(dz) <= 152;
			case DISTRICT_COLUMN_FOREST -> Math.abs(dx) <= 176 && Math.abs(dz) <= 176;
			case DISTRICT_CYLINDER -> dist2 <= 150L * 150L;
			case DISTRICT_ABYSS -> dist2 <= 168L * 168L;
			case DISTRICT_DESCENT -> Math.abs(dx) <= 72 && Math.abs(dz) <= 180;
			case DISTRICT_BLOCK_TOWERS -> dist2 <= 500L * 500L;
			case DISTRICT_TANK_CLUSTER -> dist2 <= 190L * 190L;
			case DISTRICT_SCAFFOLD -> Math.abs(dx) <= 184 && Math.abs(dz) <= 184;
			case DISTRICT_INDUSTRIAL_WALL -> (Math.abs(dx) <= 472 && Math.abs(dz) <= 184)
					|| (Math.abs(dz) <= 472 && Math.abs(dx) <= 184);
			case DISTRICT_TRANSIT_NEXUS -> Math.abs(dx) <= 268 && Math.abs(dz) <= 204;
			case DISTRICT_REACTOR_CATHEDRAL -> Math.abs(dx) <= 292 && Math.abs(dz) <= 332;
			case DISTRICT_HANGING_ARCHIVE -> Math.abs(dx) <= 252 && Math.abs(dz) <= 252;
			case DISTRICT_VENTILATION_CANYON -> (Math.abs(dx) <= 480 && Math.abs(dz) <= 96)
					|| (Math.abs(dz) <= 480 && Math.abs(dx) <= 96);
			case DISTRICT_INVERTED_PYRAMID -> Math.abs(dx) <= 292 && Math.abs(dz) <= 292;
			case DISTRICT_RING_VAULT -> dist2 <= 260L * 260L;
			case DISTRICT_MACHINE_NAVE -> Math.abs(dx) <= 388 && Math.abs(dz) <= 190;
			case DISTRICT_FRACTURED_HABITAT -> Math.abs(dx) <= 284 && Math.abs(dz) <= 254;
			case DISTRICT_CONDUIT_BASILICA -> Math.abs(dx) <= 308 && Math.abs(dz) <= 380;
			case DISTRICT_RESERVOIR_HALL -> dist2 <= 292L * 292L;
			case DISTRICT_SUSPENDED_CITY -> Math.abs(dx) <= 344 && Math.abs(dz) <= 284;
			case DISTRICT_IRIS_CHASM -> (Math.abs(dx) <= 452 && Math.abs(dz) <= 164)
					|| (Math.abs(dz) <= 452 && Math.abs(dx) <= 164);
			case DISTRICT_MACHINE_ROOT_VAULT -> dist2 <= 292L * 292L;
			case DISTRICT_TILTED_STACKS -> Math.abs(dx) <= 324 && Math.abs(dz) <= 286;
			case DISTRICT_SILENT_FOUNDRY -> Math.abs(dx) <= 420 && Math.abs(dz) <= 252;
			case DISTRICT_COLOSSUS_LIFT -> Math.abs(dx) <= 286 && Math.abs(dz) <= 286;
			case DISTRICT_FOLDED_CITY -> Math.abs(dx) <= 366 && Math.abs(dz) <= 306;
			case DISTRICT_UPPER_RIM_CITY -> dist2 <= 470L * 470L;
			case DISTRICT_ORBITAL_WEB_CORE -> dist2 <= 360L * 360L;
			case DISTRICT_CROWN_SPIRE -> dist2 <= 486L * 486L;
			case DISTRICT_GLOBE_MONUMENT -> dist2 <= 360L * 360L;
			case DISTRICT_VOID_ALTAR -> dist2 <= 340L * 340L;
			case DISTRICT_ATOM_STORM_ARRAY -> dist2 <= 390L * 390L;
			case DISTRICT_BLACK_HOLE_REACTOR -> dist2 <= 430L * 430L;
			default -> true;
		};
	}

	private static boolean isOasisHostDistrict(int district) {
		return district == DISTRICT_TANK_CLUSTER
				|| district == DISTRICT_RESERVOIR_HALL
				|| district == DISTRICT_REACTOR_CATHEDRAL
				|| district == DISTRICT_RING_VAULT
				|| district == DISTRICT_INDUSTRIAL_WALL
				|| district == DISTRICT_SCAFFOLD
				|| district == DISTRICT_COLUMN_FOREST
				|| district == DISTRICT_MONOLITH_HALL
				|| district == DISTRICT_FRACTURED_HABITAT
				|| district == DISTRICT_CONDUIT_BASILICA
				|| district == DISTRICT_HANGING_ARCHIVE
				|| isGiantOasisHostDistrict(district);
	}

	private static boolean isGiantOasisHostDistrict(int district) {
		return district == DISTRICT_BLOCK_TOWERS
				|| district == DISTRICT_MACHINE_NAVE
				|| district == DISTRICT_SUSPENDED_CITY
				|| district == DISTRICT_SILENT_FOUNDRY
				|| district == DISTRICT_COLOSSUS_LIFT
				|| district == DISTRICT_FOLDED_CITY
				|| district == DISTRICT_UPPER_RIM_CITY
				|| district == DISTRICT_ORBITAL_WEB_CORE
				|| district == DISTRICT_CROWN_SPIRE
				|| district == DISTRICT_GLOBE_MONUMENT
				|| district == DISTRICT_ATOM_STORM_ARRAY;
	}

	private static long findGuaranteedOasisKey(long variantSeed) {
		for (int targetDistance = 3; targetDistance <= 6; targetDistance++) {
			long bestKey = packDistrict(targetDistance, 0);
			long bestPriority = -1L;
			boolean found = false;
			for (int districtX = -targetDistance; districtX <= targetDistance; districtX++) {
				for (int districtZ = -targetDistance; districtZ <= targetDistance; districtZ++) {
					if (Math.max(Math.abs(districtX), Math.abs(districtZ)) != targetDistance) {
						continue;
					}
					int x = districtX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
					int z = districtZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
					if (!isOasisHostDistrict(districtTypeAt(x, z, variantSeed))) {
						continue;
					}
					long priority = MegastructureMath.hash(variantSeed, districtX, districtZ, 1703);
					if (!found || Long.compareUnsigned(priority, bestPriority) < 0) {
						found = true;
						bestPriority = priority;
						bestKey = packDistrict(districtX, districtZ);
					}
				}
			}
			if (found) {
				return bestKey;
			}
		}
		return packDistrict(4, 4);
	}

	private static long packDistrict(int districtX, int districtZ) {
		return ((long) districtX << 32) ^ (districtZ & 0xFFFFFFFFL);
	}

	private BlockState oasisOverlayState(int district, int x, int y, int z) {
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long key = packDistrict(districtX, districtZ);
		Optional<OasisDescriptor> optional = oasisCache.computeIfAbsent(key, ignored -> createOasisDescriptor(districtX, districtZ));
		if (optional.isEmpty()) {
			return null;
		}
		OasisDescriptor oasis = optional.get();
		if (oasis.district() != district) {
			return null;
		}
		if (y < oasis.floorY() || y > Math.max(oasis.sourceY() + 20, oasis.floorY() + oasisVerticalReach(oasis))) {
			return null;
		}
		if (isOasisProtectedRoute(x, y, z)) {
			return null;
		}

		int basinDx = x - oasis.basinX();
		int basinDz = z - oasis.basinZ();
		int basinDist2 = basinDx * basinDx + basinDz * basinDz;
		BlockState scene = oasisSceneState(oasis, x, y, z);
		if (scene != null) {
			return scene;
		}

		BlockState origin = oasisOriginState(oasis, x, y, z);
		if (origin != null) {
			return origin;
		}

		BlockState water = oasisWaterState(district, oasis, x, y, z);
		if (water != null) {
			return water;
		}

		BlockState pipe = oasisPipeState(oasis, x, y, z);
		if (pipe != null) {
			return pipe;
		}

		BlockState erosion = oasisHydraulicErosionState(district, oasis, x, y, z);
		if (erosion != null) {
			return erosion;
		}

		BlockState tree = oasisTreeState(oasis, x, y, z);
		if (tree != null) {
			return tree;
		}

		BlockState overgrowth = oasisIndustrialOvergrowthState(oasis, x, y, z);
		if (overgrowth != null) {
			return overgrowth;
		}

		int rimOuter = oasis.basinRadius() + 3;
		if (y == oasis.floorY() + 1 && isOasisBasin(oasis, basinDx, basinDz, 3)
				&& !isOasisBasin(oasis, basinDx, basinDz, 0)) {
			return BlockPalette.MOSS;
		}

		boolean giantOasis = isGiantOasisHostDistrict(oasis.district());
		int wetRadius = oasis.basinRadius() + (giantOasis ? 260 : 112);
		if (basinDist2 <= wetRadius * wetRadius
				&& (y == oasis.floorY() || y == oasis.floorY() + 1)) {
			long patchHash = MegastructureMath.hash(oasis.seed(), x, z, y);
			int distance = (int) Math.sqrt(basinDist2);
			int wetDistance = Math.max(0, distance - oasis.basinRadius());
			int coverage = giantOasis
					? (wetDistance <= 20 ? 96 : wetDistance <= 76 ? 64 : wetDistance <= 156 ? 34 : 16)
					: (wetDistance <= 12 ? 96 : wetDistance <= 36 ? 78 : wetDistance <= 72 ? 48 : 22);
			boolean connectedColony = connectedOasisMossAt(oasis, basinDx, basinDz, wetRadius, coverage);
			if (y == oasis.floorY()
					&& oasisBaseSolidAt(district, x, y, z)
					&& (isOasisBasin(oasis, basinDx, basinDz, 2)
							|| connectedColony && wetDistance <= 20)
					&& Math.floorMod(patchHash >>> 17, 11) <= 2) {
				return BlockPalette.CLAY;
			}
			if (y == oasis.floorY()
					&& oasisBaseSolidAt(district, x, y, z)
					&& connectedColony) {
				return BlockPalette.MOSS;
			}
			if (y == oasis.floorY() + 1
					&& basinDist2 > rimOuter * rimOuter
					&& wetDistance <= (giantOasis ? 118 : 58)
					&& connectedColony
					&& !oasisBaseSolidAt(district, x, y, z)
					&& oasisBaseSolidAt(district, x, y - 1, z)
					&& Math.floorMod(patchHash >>> 8, 100) < Math.max(18, coverage * 2 / 3)) {
				return BlockPalette.MOSS_CARPET;
			}
		}

		BlockState vine = oasisVineState(oasis, x, y, z);
		if (vine != null) {
			return vine;
		}
		return null;
	}

	private boolean connectedOasisMossAt(OasisDescriptor oasis, int dx, int dz, int maximumRadius, int coverage) {
		if (isOasisBasin(oasis, dx, dz, 4)) {
			return true;
		}
		boolean giant = isGiantOasisHostDistrict(oasis.district());
		int veinCount = giant ? 44 : 24;
		for (int vein = 0; vein < veinCount; vein++) {
			long veinHash = MegastructureMath.hash(oasis.seed(), vein, 0, 1891);
			int direction = Math.floorMod((int) (veinHash >>> 8) + vein * 3, TREE_DIRECTIONS_X.length);
			int directionX = TREE_DIRECTIONS_X[direction];
			int directionZ = TREE_DIRECTIONS_Z[direction];
			int projection = Math.floorDiv(dx * directionX + dz * directionZ, 100);
			int perpendicular = Math.floorDiv(dx * directionZ - dz * directionX, 100);
			int start = Math.max(0, oasis.basinRadius() - 5);
			int minimumLength = Math.min(maximumRadius, oasis.basinRadius() + (giant ? 136 : 52));
			int length = MegastructureMath.range(veinHash >>> 16, minimumLength, maximumRadius);
			if (projection < start || projection > length) {
				continue;
			}
			int progress = projection - start;
			int segment = Math.floorDiv(progress, 12);
			int segmentProgress = Math.floorMod(progress, 12);
			long firstNode = MegastructureMath.hash(veinHash, segment, 0, 1897);
			long secondNode = MegastructureMath.hash(veinHash, segment + 1, 0, 1897);
			int meanderRange = giant ? 22 : 12;
			int firstOffset = MegastructureMath.range(firstNode, -meanderRange, meanderRange);
			int secondOffset = MegastructureMath.range(secondNode, -meanderRange, meanderRange);
			int centerOffset = firstOffset + (secondOffset - firstOffset) * segmentProgress / 12;
			int widthClass = Math.floorMod((int) (veinHash >>> 40), 10);
			int width = widthClass < 4
					? 3 + Math.max(0, coverage) / 30
					: widthClass < 8
						? 6 + Math.max(0, coverage) / 20 + MegastructureMath.range(veinHash >>> 46, 0, 3)
						: 11 + Math.max(0, coverage) / 16 + MegastructureMath.range(veinHash >>> 48, 0, giant ? 8 : 5);
			if (Math.abs(perpendicular - centerOffset) <= width) {
				return true;
			}
		}
		int branchesPerVein = giant ? 5 : 3;
		for (int vein = 0; vein < veinCount; vein++) {
			long veinHash = MegastructureMath.hash(oasis.seed(), vein, 0, 1891);
			int mainDirection = Math.floorMod((int) (veinHash >>> 8) + vein * 3, TREE_DIRECTIONS_X.length);
			int mainX = TREE_DIRECTIONS_X[mainDirection];
			int mainZ = TREE_DIRECTIONS_Z[mainDirection];
			for (int branch = 0; branch < branchesPerVein; branch++) {
				long branchHash = MegastructureMath.hash(veinHash, branch, 0, 1901);
				int branchStart = oasis.basinRadius() + MegastructureMath.range(
						branchHash >>> 8,
						giant ? 20 : 8,
						giant ? 92 : 38
				);
				int startProgress = Math.max(0, branchStart - Math.max(0, oasis.basinRadius() - 5));
				int node = Math.floorDiv(startProgress, 12);
				int nodeProgress = Math.floorMod(startProgress, 12);
				int meander = giant ? 14 : 8;
				int firstOffset = MegastructureMath.range(
						MegastructureMath.hash(veinHash, node, 0, 1897), -meander, meander);
				int secondOffset = MegastructureMath.range(
						MegastructureMath.hash(veinHash, node + 1, 0, 1897), -meander, meander);
				int startOffset = firstOffset + (secondOffset - firstOffset) * nodeProgress / 12;
				int originX = mainX * branchStart / 100 + mainZ * startOffset / 100;
				int originZ = mainZ * branchStart / 100 - mainX * startOffset / 100;
				int turn = Math.floorMod(branchHash >>> 20, 2) == 0 ? 2 + branch : -2 - branch;
				int branchDirection = Math.floorMod(mainDirection + turn, TREE_DIRECTIONS_X.length);
				int directionX = TREE_DIRECTIONS_X[branchDirection];
				int directionZ = TREE_DIRECTIONS_Z[branchDirection];
				int localX = dx - originX;
				int localZ = dz - originZ;
				int projection = Math.floorDiv(localX * directionX + localZ * directionZ, 100);
				int perpendicular = Math.floorDiv(localX * directionZ - localZ * directionX, 100);
				int length = MegastructureMath.range(branchHash >>> 32, giant ? 32 : 18, giant ? 104 : 58);
				int width = 2 + MegastructureMath.range(branchHash >>> 48, 0, giant ? 4 : 2);
				if (projection >= 0 && projection <= length && Math.abs(perpendicular) <= width) {
					return true;
				}
			}
		}
		return false;
	}

	private Optional<OasisDescriptor> createOasisDescriptor(int districtX, int districtZ) {
		int centerX = districtX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
		int centerZ = districtZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
		int district = districtType(centerX, centerZ);
		if (!isOasisHostDistrict(district)) {
			return Optional.empty();
		}

		long key = packDistrict(districtX, districtZ);
		long seed = MegastructureMath.hash(worldVariantSeed, districtX, districtZ, 1701);
		long guaranteedOasisKey = findGuaranteedOasisKey(worldVariantSeed);
		boolean guaranteed = key == guaranteedOasisKey;
		if (!guaranteed && Math.floorMod(seed, 40) != 0) {
			return Optional.empty();
		}
		if (!guaranteed && hasNearbyHigherPriorityOasis(districtX, districtZ, seed)) {
			return Optional.empty();
		}

		int floorY = oasisHostFloorY(district, centerX, centerZ);
		int profile = oasisProfile(district, seed);
		int origin = oasisOrigin(district, seed);
		if (profile == 6) {
			origin = 0;
		}
		int side = Math.floorMod((int) (seed >>> 8), 4);
		if (district == DISTRICT_INDUSTRIAL_WALL) {
			boolean horizontal = Math.floorMod(districtHash(centerX, centerZ, 1301), 2) == 0;
			side = horizontal ? 3 : 1;
		}
		int basinRadius = switch (profile) {
			case 1 -> MegastructureMath.range(seed >>> 20, 22, 28);
			case 2 -> MegastructureMath.range(seed >>> 20, 14, 19);
			case 3 -> MegastructureMath.range(seed >>> 20, 17, 22);
			case 4 -> MegastructureMath.range(seed >>> 20, 13, 18);
			case 5 -> MegastructureMath.range(seed >>> 20, 18, 24);
			case 6 -> MegastructureMath.range(seed >>> 20, 12, 18);
			case 7 -> MegastructureMath.range(seed >>> 20, 24, 34);
			case 8 -> MegastructureMath.range(seed >>> 20, 16, 23);
			default -> MegastructureMath.range(seed >>> 20, 16, 22);
		};
		if (isGiantOasisHostDistrict(district)) {
			basinRadius = MegastructureMath.range(seed >>> 20, 52, 88);
		}
		int sourceHeight = switch (profile) {
			case 1 -> MegastructureMath.range(seed >>> 28, 48, 78);
			case 2 -> MegastructureMath.range(seed >>> 28, 64, 112);
			case 3 -> MegastructureMath.range(seed >>> 28, 24, 42);
			case 4 -> MegastructureMath.range(seed >>> 28, 54, 96);
			case 5 -> MegastructureMath.range(seed >>> 28, 36, 68);
			case 6 -> MegastructureMath.range(seed >>> 28, 52, 104);
			case 7 -> MegastructureMath.range(seed >>> 28, 34, 72);
			case 8 -> MegastructureMath.range(seed >>> 28, 92, 164);
			default -> MegastructureMath.range(seed >>> 28, 82, 148);
		};
		int defaultSourceY = Math.min(settings.ceilingY() - 48, floorY + sourceHeight);
		OasisAnchor anchor = oasisAnchorFor(
				district,
				districtX,
				districtZ,
				centerX,
				centerZ,
				floorY,
				side,
				defaultSourceY,
				seed
		);
		int sourceY = oasisOriginSourceY(district, floorY, anchor.sourceY(), origin, seed);
		int treeCount = isGiantOasisHostDistrict(district)
				? MegastructureMath.range(seed >>> 40, 10, 14)
				: MegastructureMath.range(seed >>> 40, 9, 11);
		boolean rootedDirt = Math.floorMod(seed >>> 52, 2) == 0;
		return Optional.of(new OasisDescriptor(
				district,
				districtX,
				districtZ,
				anchor.x(),
				anchor.z(),
				floorY,
				sourceY,
				anchor.radius(),
				anchor.pipeWallDistance(),
				anchor.basinX(),
				anchor.basinZ(),
				basinRadius,
				anchor.side(),
				profile,
				origin,
				treeCount,
				rootedDirt,
				seed
		));
	}

	private OasisAnchor oasisAnchorFor(
			int district,
			int districtX,
			int districtZ,
			int centerX,
			int centerZ,
			int floorY,
			int requestedSide,
			int defaultSourceY,
			long seed
	) {
		int side = requestedSide;
		int radius = oasisHostRadius(district, side);
		int pipeWallDistance = radius + 10;
		int anchorX = centerX;
		int anchorZ = centerZ;
		int offset = Math.max(30, radius - 52);
		int basinX = centerX + sideOffsetX(side, offset);
		int basinZ = centerZ + sideOffsetZ(side, offset);
		int sourceY = defaultSourceY;

		if (district == DISTRICT_TANK_CLUSTER) {
			int tank = Math.floorMod((int) (seed >>> 16), 5);
			int[] tankX = {-82, 72, -28, 92, 8};
			int[] tankZ = {-58, -42, 72, 82, 6};
			long tankHash = MegastructureMath.hash(districtHash(centerX, centerZ, 941), tank, 0, 967);
			radius = MegastructureMath.range(tankHash, 34, 58);
			anchorX = centerX + tankX[tank];
			anchorZ = centerZ + tankZ[tank];
			pipeWallDistance = radius;
			basinX = anchorX;
			basinZ = anchorZ;
			sourceY = Math.min(settings.ceilingY() - 48, floorY + MegastructureMath.range(seed >>> 28, 72, 142));
		} else if (district == DISTRICT_RESERVOIR_HALL) {
			int reservoirRing = Math.floorMod((int) (seed >>> 18), 3);
			int[] reservoirRadii = {78, 152, 232};
			int[] reservoirHeights = {46, 80, 114};
			radius = reservoirRadii[reservoirRing];
			pipeWallDistance = radius;
			basinX = centerX;
			basinZ = centerZ;
			sourceY = floorY + reservoirHeights[reservoirRing] - 6;
		} else if (district == DISTRICT_REACTOR_CATHEDRAL) {
			radius = 58;
			pipeWallDistance = 58;
			int spillOffset = radius + 24;
			basinX = centerX + sideOffsetX(side, spillOffset);
			basinZ = centerZ + sideOffsetZ(side, spillOffset);
			sourceY = Math.min(settings.ceilingY() - 48, floorY + MegastructureMath.range(seed >>> 28, 108, 188));
		} else if (district == DISTRICT_RING_VAULT) {
			int ring = Math.floorMod((int) (seed >>> 16), 3);
			int[] radii = {64, 124, 198};
			int[] levels = {72, 148, 224};
			radius = radii[ring];
			pipeWallDistance = radius;
			int spillOffset = radius + 20;
			basinX = centerX + sideOffsetX(side, spillOffset);
			basinZ = centerZ + sideOffsetZ(side, spillOffset);
			sourceY = Math.min(settings.ceilingY() - 48, floorY + levels[ring] + 16);
		} else if (district == DISTRICT_SCAFFOLD) {
			int originX = districtX * DISTRICT_SIZE;
			int originZ = districtZ * DISTRICT_SIZE;
			int gridStepX = MegastructureMath.range(seed >>> 16, -3, 3);
			int gridStepZ = MegastructureMath.range(seed >>> 24, -3, 3);
			anchorX = originX + 507 + gridStepX * 32;
			anchorZ = originZ + 501 + gridStepZ * 32;
			radius = 3;
			pipeWallDistance = 3;
			int scaffoldReach = MegastructureMath.range(seed >>> 32, 24, 42);
			basinX = anchorX + sideOffsetX(side, scaffoldReach);
			basinZ = anchorZ + sideOffsetZ(side, scaffoldReach);
			sourceY = Math.min(settings.ceilingY() - 48, floorY + MegastructureMath.range(seed >>> 28, 116, 164));
		} else if (district == DISTRICT_COLUMN_FOREST) {
			long forestHash = districtHash(centerX, centerZ, 823);
			int shiftX = MegastructureMath.range(forestHash >>> 8, 0, 17);
			int shiftZ = MegastructureMath.range(forestHash >>> 16, 0, 17);
			int localX = DISTRICT_SIZE / 2 - Math.floorMod(DISTRICT_SIZE / 2 + shiftX, 36);
			int localZ = DISTRICT_SIZE / 2 - Math.floorMod(DISTRICT_SIZE / 2 + shiftZ, 36);
			int columnStepX = MegastructureMath.range(seed >>> 18, -3, 3);
			int columnStepZ = MegastructureMath.range(seed >>> 26, -3, 3);
			anchorX = districtX * DISTRICT_SIZE + localX + columnStepX * 36;
			anchorZ = districtZ * DISTRICT_SIZE + localZ + columnStepZ * 36;
			radius = 2;
			pipeWallDistance = 2;
			int columnReach = MegastructureMath.range(seed >>> 34, 22, 38);
			basinX = anchorX + sideOffsetX(side, columnReach);
			basinZ = anchorZ + sideOffsetZ(side, columnReach);
			sourceY = Math.min(sourceY, floorY + 88);
		} else if (district == DISTRICT_MONOLITH_HALL) {
			radius = 14;
			pipeWallDistance = 14;
			basinX = centerX + sideOffsetX(side, 34);
			basinZ = centerZ + sideOffsetZ(side, 34);
			sourceY = Math.min(settings.ceilingY() - 48, floorY + MegastructureMath.range(seed >>> 28, 30, 52));
		} else if (district == DISTRICT_FRACTURED_HABITAT) {
			int module = Math.floorMod((int) (seed >>> 16), 6);
			long moduleHash = MegastructureMath.hash(districtHash(centerX, centerZ, 1447), module, 0, 1471);
			int moduleX = MegastructureMath.range(moduleHash, -198, 198);
			int moduleZ = MegastructureMath.range(moduleHash >>> 12, -176, 176);
			int halfX = MegastructureMath.range(moduleHash >>> 24, 24, 46);
			int halfZ = MegastructureMath.range(moduleHash >>> 32, 20, 40);
			anchorX = centerX + moduleX;
			anchorZ = centerZ + moduleZ;
			radius = side <= 1 ? halfX : halfZ;
			pipeWallDistance = radius;
			basinX = anchorX + sideOffsetX(side, radius + 22);
			basinZ = anchorZ + sideOffsetZ(side, radius + 22);
			int moduleBottom = 18 + module * 42;
			sourceY = Math.min(settings.ceilingY() - 48, floorY + moduleBottom + 24);
		} else if (district == DISTRICT_CONDUIT_BASILICA) {
			boolean positive = Math.floorMod(seed >>> 16, 2) == 0;
			anchorX = centerX + (positive ? 160 : -160);
			anchorZ = centerZ;
			side = positive ? 0 : 1;
			radius = 2;
			pipeWallDistance = 0;
			basinX = anchorX + sideOffsetX(side, 26);
			basinZ = anchorZ;
		} else if (district == DISTRICT_HANGING_ARCHIVE) {
			int archiveStepX = MegastructureMath.range(seed >>> 18, -2, 2);
			int archiveStepZ = MegastructureMath.range(seed >>> 26, -2, 2);
			anchorX = centerX + archiveStepX * 64;
			anchorZ = centerZ + archiveStepZ * 64;
			radius = 4;
			pipeWallDistance = 4;
			basinX = anchorX + sideOffsetX(side, MegastructureMath.range(seed >>> 34, 26, 44));
			basinZ = anchorZ + sideOffsetZ(side, MegastructureMath.range(seed >>> 34, 26, 44));
			sourceY = Math.min(settings.ceilingY() - 48, floorY + 286);
		} else if (district == DISTRICT_INDUSTRIAL_WALL) {
			boolean horizontal = Math.floorMod(districtHash(centerX, centerZ, 1301), 2) == 0;
			int alongShift = MegastructureMath.range(seed >>> 18, -10, 10) * 32;
			anchorX = horizontal ? centerX + alongShift : centerX;
			anchorZ = horizontal ? centerZ : centerZ + alongShift;
			radius = 108;
			pipeWallDistance = 108;
			int basinDepth = MegastructureMath.range(seed >>> 34, 38, 62);
			basinX = anchorX + sideOffsetX(side, basinDepth);
			basinZ = anchorZ + sideOffsetZ(side, basinDepth);
		} else if (district == DISTRICT_BLOCK_TOWERS) {
			radius = 50;
			pipeWallDistance = 50;
			int infectionDistance = MegastructureMath.range(seed >>> 34, 104, 178);
			basinX = centerX + sideOffsetX(side, infectionDistance);
			basinZ = centerZ + sideOffsetZ(side, infectionDistance);
			sourceY = Math.min(settings.ceilingY() - 48, floorY + MegastructureMath.range(seed >>> 28, 180, 430));
		} else if (isGiantOasisHostDistrict(district)) {
			radius = oasisHostRadius(district, side);
			pipeWallDistance = radius;
			int infectionDistance = MegastructureMath.range(seed >>> 34, 48, Math.max(64, radius - 72));
			basinX = centerX + sideOffsetX(side, infectionDistance);
			basinZ = centerZ + sideOffsetZ(side, infectionDistance);
			sourceY = Math.min(settings.ceilingY() - 36, floorY + MegastructureMath.range(seed >>> 28, 96, 238));
		}

		return new OasisAnchor(anchorX, anchorZ, radius, pipeWallDistance, side, basinX, basinZ, sourceY);
	}

	private static int sideOffsetX(int side, int distance) {
		return side == 0 ? distance : side == 1 ? -distance : 0;
	}

	private static int sideOffsetZ(int side, int distance) {
		return side == 2 ? distance : side == 3 ? -distance : 0;
	}

	private boolean hasNearbyHigherPriorityOasis(int districtX, int districtZ, long priority) {
		for (int offsetX = -2; offsetX <= 2; offsetX++) {
			for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
				if (offsetX == 0 && offsetZ == 0) {
					continue;
				}
				int neighborX = districtX + offsetX;
				int neighborZ = districtZ + offsetZ;
				long neighborKey = packDistrict(neighborX, neighborZ);
				int blockX = neighborX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				int blockZ = neighborZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				if (!isOasisHostDistrict(districtType(blockX, blockZ))) {
					continue;
				}
				long neighborPriority = MegastructureMath.hash(worldVariantSeed, neighborX, neighborZ, 1701);
				boolean neighborCandidate = neighborKey == findGuaranteedOasisKey(worldVariantSeed) || Math.floorMod(neighborPriority, 40) == 0;
				if (neighborCandidate && Long.compareUnsigned(neighborPriority, priority) < 0) {
					return true;
				}
			}
		}
		return false;
	}

	private int oasisHostFloorY(int district, int x, int z) {
		return switch (district) {
			case DISTRICT_TANK_CLUSTER -> settings.floorY() + MegastructureMath.range(districtHash(x, z, 941), 54, 612);
			case DISTRICT_RESERVOIR_HALL -> districtBaseY(x, z, 1459, 48, 420);
			case DISTRICT_REACTOR_CATHEDRAL -> districtBaseY(x, z, 1403, 48, 360);
			case DISTRICT_RING_VAULT -> districtBaseY(x, z, 1433, 52, 360);
			case DISTRICT_INDUSTRIAL_WALL -> settings.floorY() + MegastructureMath.range(districtHash(x, z, 1301) >>> 8, 40, 220) - 1;
			case DISTRICT_SCAFFOLD -> settings.floorY() + MegastructureMath.range(districtHash(x, z, 947), 48, 650) - 1;
			case DISTRICT_COLUMN_FOREST -> settings.floorY() + MegastructureMath.range(districtHash(x, z, 823), 64, 704) - 1;
			case DISTRICT_MONOLITH_HALL -> settings.floorY() + MegastructureMath.range(districtHash(x, z, 811), 96, 624) - 5;
			case DISTRICT_FRACTURED_HABITAT -> districtBaseY(x, z, 1447, 54, 380);
			case DISTRICT_CONDUIT_BASILICA -> districtBaseY(x, z, 1451, 60, 360);
			case DISTRICT_HANGING_ARCHIVE -> districtBaseY(x, z, 1409, 64, 340);
			case DISTRICT_BLOCK_TOWERS -> settings.floorY() + 18;
			case DISTRICT_MACHINE_NAVE -> districtBaseY(x, z, 1439, 70, 480);
			case DISTRICT_SUSPENDED_CITY -> districtBaseY(x, z, 1463, 52, 330);
			case DISTRICT_SILENT_FOUNDRY -> districtBaseY(x, z, 1487, 54, 360);
			case DISTRICT_COLOSSUS_LIFT -> districtBaseY(x, z, 1493, 42, 280);
			case DISTRICT_FOLDED_CITY -> districtBaseY(x, z, 1499, 56, 360);
			case DISTRICT_UPPER_RIM_CITY -> districtBaseY(x, z, 1505, 42, 300);
			case DISTRICT_ORBITAL_WEB_CORE -> districtBaseY(x, z, 1511, 60, 360);
			case DISTRICT_CROWN_SPIRE -> districtBaseY(x, z, 1521, 34, 240);
			case DISTRICT_GLOBE_MONUMENT -> districtBaseY(x, z, 1527, 60, 320);
			case DISTRICT_ATOM_STORM_ARRAY -> districtBaseY(x, z, 1537, 48, 320);
			default -> connectorNetworkY();
		};
	}

	private int oasisHostRadius(int district, int side) {
		boolean xSide = side <= 1;
		return switch (district) {
			case DISTRICT_TANK_CLUSTER -> 170;
			case DISTRICT_RESERVOIR_HALL -> 276;
			case DISTRICT_REACTOR_CATHEDRAL -> xSide ? 174 : 318;
			case DISTRICT_RING_VAULT -> 246;
			case DISTRICT_INDUSTRIAL_WALL -> 96;
			case DISTRICT_SCAFFOLD -> 152;
			case DISTRICT_COLUMN_FOREST -> 150;
			case DISTRICT_MONOLITH_HALL -> xSide ? 160 : 142;
			case DISTRICT_FRACTURED_HABITAT -> xSide ? 268 : 238;
			case DISTRICT_CONDUIT_BASILICA -> xSide ? 190 : 364;
			case DISTRICT_HANGING_ARCHIVE -> 238;
			case DISTRICT_BLOCK_TOWERS -> 500;
			case DISTRICT_MACHINE_NAVE -> xSide ? 372 : 174;
			case DISTRICT_SUSPENDED_CITY -> xSide ? 330 : 270;
			case DISTRICT_SILENT_FOUNDRY -> xSide ? 404 : 238;
			case DISTRICT_COLOSSUS_LIFT -> 276;
			case DISTRICT_FOLDED_CITY -> xSide ? 352 : 292;
			case DISTRICT_UPPER_RIM_CITY -> 452;
			case DISTRICT_ORBITAL_WEB_CORE -> 340;
			case DISTRICT_CROWN_SPIRE -> 472;
			case DISTRICT_GLOBE_MONUMENT -> 340;
			case DISTRICT_ATOM_STORM_ARRAY -> 374;
			default -> 128;
		};
	}

	private int oasisProfile(int district, long seed) {
		int[] compatible = switch (district) {
			case DISTRICT_TANK_CLUSTER -> new int[] {1, 2, 3, 5, 8};
			case DISTRICT_RESERVOIR_HALL -> new int[] {1, 4, 5, 7};
			case DISTRICT_REACTOR_CATHEDRAL -> new int[] {0, 4, 5, 8};
			case DISTRICT_RING_VAULT -> new int[] {1, 4, 6, 7, 8};
			case DISTRICT_INDUSTRIAL_WALL -> new int[] {0, 4, 6, 8};
			case DISTRICT_SCAFFOLD -> new int[] {2, 4, 6, 8};
			case DISTRICT_COLUMN_FOREST -> new int[] {2, 4, 5, 8};
			case DISTRICT_MONOLITH_HALL -> new int[] {3, 4, 5, 7};
			case DISTRICT_FRACTURED_HABITAT -> new int[] {3, 5, 6, 7, 8};
			case DISTRICT_CONDUIT_BASILICA -> new int[] {0, 4, 6, 8};
			case DISTRICT_HANGING_ARCHIVE -> new int[] {2, 5, 6, 8};
			case DISTRICT_BLOCK_TOWERS, DISTRICT_MACHINE_NAVE, DISTRICT_SUSPENDED_CITY,
					DISTRICT_SILENT_FOUNDRY, DISTRICT_COLOSSUS_LIFT, DISTRICT_FOLDED_CITY,
					DISTRICT_UPPER_RIM_CITY, DISTRICT_ORBITAL_WEB_CORE, DISTRICT_CROWN_SPIRE,
					DISTRICT_GLOBE_MONUMENT, DISTRICT_ATOM_STORM_ARRAY -> new int[] {0, 4, 5, 7, 8};
			default -> new int[] {0};
		};
		return compatible[Math.floorMod((int) (seed >>> 12), compatible.length)];
	}

	private int oasisOrigin(int district, long seed) {
		int[] compatible = switch (district) {
			case DISTRICT_SCAFFOLD, DISTRICT_COLUMN_FOREST, DISTRICT_HANGING_ARCHIVE -> new int[] {1, 3, 4};
			case DISTRICT_MONOLITH_HALL, DISTRICT_FRACTURED_HABITAT -> new int[] {2, 3, 4};
			case DISTRICT_INDUSTRIAL_WALL, DISTRICT_CONDUIT_BASILICA -> new int[] {0, 2, 4};
			case DISTRICT_TANK_CLUSTER, DISTRICT_RESERVOIR_HALL, DISTRICT_RING_VAULT -> new int[] {0, 2, 3, 4};
			case DISTRICT_REACTOR_CATHEDRAL -> new int[] {0, 1, 2, 4};
			case DISTRICT_BLOCK_TOWERS -> new int[] {0, 2, 3, 4};
			case DISTRICT_MACHINE_NAVE, DISTRICT_SUSPENDED_CITY, DISTRICT_SILENT_FOUNDRY,
					DISTRICT_COLOSSUS_LIFT, DISTRICT_FOLDED_CITY,
					DISTRICT_UPPER_RIM_CITY, DISTRICT_ORBITAL_WEB_CORE, DISTRICT_CROWN_SPIRE,
					DISTRICT_GLOBE_MONUMENT, DISTRICT_ATOM_STORM_ARRAY -> new int[] {1, 2, 3, 4};
			default -> new int[] {2, 3, 4};
		};
		return compatible[Math.floorMod((int) (seed >>> 56), compatible.length)];
	}

	private int oasisOriginSourceY(int district, int floorY, int proposedY, int origin, long seed) {
		if (origin == 1) {
			int hostHeight = switch (district) {
				case DISTRICT_TANK_CLUSTER -> 188;
				case DISTRICT_RESERVOIR_HALL -> 246;
				case DISTRICT_REACTOR_CATHEDRAL -> 286;
				case DISTRICT_RING_VAULT -> 330;
				case DISTRICT_SCAFFOLD -> 177;
				case DISTRICT_COLUMN_FOREST -> 97;
				case DISTRICT_MONOLITH_HALL -> 145;
				case DISTRICT_FRACTURED_HABITAT -> 298;
				case DISTRICT_CONDUIT_BASILICA -> 288;
				case DISTRICT_HANGING_ARCHIVE -> 320;
				case DISTRICT_CROWN_SPIRE -> 236;
				case DISTRICT_GLOBE_MONUMENT -> 280;
				case DISTRICT_ATOM_STORM_ARRAY -> 272;
				default -> 192;
			};
			return Math.min(settings.ceilingY() - 12, floorY + hostHeight - 8);
		}
		if (origin == 2) {
			return Math.min(proposedY, floorY + MegastructureMath.range(seed >>> 30, 22, 74));
		}
		if (origin == 3 || origin == 4) {
			return floorY + 2;
		}
		return proposedY;
	}

	private boolean isOasisProtectedRoute(int x, int y, int z) {
		if (isRailwayLineAt(x, z)) {
			return true;
		}
		if (isConnectorNetworkVolumeAt(x, y, z, 5)) {
			return true;
		}
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		return Math.abs(dx) <= 12 && Math.abs(dz) <= 12;
	}

	private BlockState oasisWaterState(int district, OasisDescriptor oasis, int x, int y, int z) {
		int basinDx = x - oasis.basinX();
		int basinDz = z - oasis.basinZ();
		if (oasis.profile() != 4
				&& y == oasis.floorY() + 1
				&& isOasisBasin(oasis, basinDx, basinDz, 0)
				&& (oasisFloorSiteOpen(oasis, x, z) || oasis.district() == DISTRICT_FOLDED_CITY)) {
			return BlockPalette.WATER;
		}
		BlockState sidePipeWater = oasisSidePipeWaterState(oasis, x, y, z);
		if (sidePipeWater != null) {
			return sidePipeWater;
		}
		if (hasOasisSidePipe(oasis)) {
			return null;
		}

		if (oasis.origin() == 2) {
			int wallX = oasis.centerX() + sideOffsetX(oasis.side(), oasis.pipeWallDistance());
			int wallZ = oasis.centerZ() + sideOffsetZ(oasis.side(), oasis.pipeWallDistance());
			boolean wallFall = x == wallX && z == wallZ && y >= oasis.floorY() + 2 && y <= oasis.sourceY();
			boolean floorRill = y == oasis.floorY() + 1
					&& ((oasis.side() <= 1 && z == wallZ && between(x, wallX, oasis.basinX()))
					|| (oasis.side() >= 2 && x == wallX && between(z, wallZ, oasis.basinZ())));
			return wallFall || floorRill ? BlockPalette.WATER : null;
		}
		if (oasis.origin() == 3) {
			int sourceTop = oasis.floorY() + 9;
			return x == oasis.basinX() && z == oasis.basinZ()
					&& y >= oasis.floorY() + 2 && y <= sourceTop ? BlockPalette.WATER : null;
		}
		if (oasis.origin() == 4) {
			int along = oasis.side() <= 1 ? x - oasis.basinX() : z - oasis.basinZ();
			int cross = oasis.side() <= 1 ? z - oasis.basinZ() : x - oasis.basinX();
			return y == oasis.floorY() + 1
					&& along >= oasis.basinRadius() - 2 && along <= oasis.basinRadius() + 18
					&& Math.abs(cross) <= 1 ? BlockPalette.WATER : null;
		}
		int drops = 1;
		for (int drop = 0; drop < drops; drop++) {
			long dropHash = MegastructureMath.hash(oasis.seed(), drop, 0, 1759);
			int direction = drop == 0 ? 0 : Math.floorMod((int) dropHash, 10);
			int offset = drop == 0 ? 0 : MegastructureMath.range(dropHash >>> 8, 5, 13);
			int dropX = oasis.basinX() + TREE_DIRECTIONS_X[direction] * offset / 100;
			int dropZ = oasis.basinZ() + TREE_DIRECTIONS_Z[direction] * offset / 100;
			if (!oasisFloorSiteOpen(oasis, dropX, dropZ)) {
				continue;
			}
			int dropTop = oasis.sourceY();
			if (y < oasis.floorY() + 2 || y > dropTop) {
				continue;
			}
			int dx = x - dropX;
			int dz = z - dropZ;
			int fallRadius = drop == 0 ? 1 : 0;
			if (Math.abs(dx) <= fallRadius && Math.abs(dz) <= fallRadius) {
				return BlockPalette.WATER;
			}

			boolean blockedCenter = oasisBaseSolidAt(district, dropX, y, dropZ);
			if (!blockedCenter || !oasisBaseSolidAt(district, x, y, z)) {
				continue;
			}
			long erosionHash = MegastructureMath.hash(oasis.seed(), x, z, y + drop * 31);
			int erosionRadius = 4 + Math.floorMod((int) (erosionHash >>> 12), 3);
			int distance2 = dx * dx + dz * dz;
			if (distance2 <= (erosionRadius - 2) * (erosionRadius - 2)) {
				return BlockPalette.AIR;
			}
			boolean contactSurface = !oasisBaseSolidAt(district, dropX, y + 1, dropZ)
					|| !oasisBaseSolidAt(district, dropX, y - 1, dropZ);
			if (contactSurface && distance2 <= erosionRadius * erosionRadius
					&& Math.floorMod(erosionHash, 5) != 0) {
				return Math.floorMod(erosionHash >>> 8, 4) == 0
						? BlockPalette.MOSS
						: BlockPalette.CRACKED_PANEL;
			}
		}
		return null;
	}

	private BlockState oasisHydraulicErosionState(int district, OasisDescriptor oasis, int x, int y, int z) {
		if (!oasisBaseSolidAt(district, x, y, z)) {
			return null;
		}
		boolean nearFlow = false;
		if (hasOasisSidePipe(oasis)) {
			int side = oasisSidePipeSide(oasis);
			int mouthX = oasis.basinX() + sideOffsetX(side, oasisSidePipeMouthDistance(oasis));
			int mouthZ = oasis.basinZ() + sideOffsetZ(side, oasisSidePipeMouthDistance(oasis));
			int sourceY = oasisSidePipeSourceY(oasis);
			int dx = x - mouthX;
			int dz = z - mouthZ;
			nearFlow = y >= oasis.floorY() + 2 && y <= sourceY
					&& dx * dx + dz * dz <= 5
					&& !(x == mouthX && z == mouthZ);
		} else if (oasis.origin() == 2) {
			int wallX = oasis.centerX() + sideOffsetX(oasis.side(), oasis.pipeWallDistance());
			int wallZ = oasis.centerZ() + sideOffsetZ(oasis.side(), oasis.pipeWallDistance());
			int dx = x - wallX;
			int dz = z - wallZ;
			nearFlow = y >= oasis.floorY() + 2 && y <= oasis.sourceY()
					&& dx * dx + dz * dz <= 5
					&& !(x == wallX && z == wallZ);
		} else if (oasis.origin() == 3) {
			int dx = x - oasis.basinX();
			int dz = z - oasis.basinZ();
			nearFlow = y >= oasis.floorY() + 2 && y <= oasis.floorY() + 9
					&& dx * dx + dz * dz <= 9
					&& !(x == oasis.basinX() && z == oasis.basinZ());
		} else {
			int dx = x - oasis.basinX();
			int dz = z - oasis.basinZ();
			nearFlow = y >= oasis.floorY() + 2 && y <= oasis.sourceY()
					&& dx * dx + dz * dz <= 9
					&& !(Math.abs(dx) <= 1 && Math.abs(dz) <= 1);
		}
		if (!nearFlow) {
			return null;
		}
		long hash = MegastructureMath.hash(oasis.seed(), x, y, z + 1879);
		if (Math.floorMod(hash, 4) == 0) {
			return null;
		}
		if (Math.floorMod(hash >>> 7, 7) == 0) {
			return BlockPalette.MOSS;
		}
		return Math.floorMod(hash >>> 11, 5) == 0 ? BlockPalette.CLAY : BlockPalette.CRACKED_PANEL;
	}

	private BlockState oasisSidePipeWaterState(OasisDescriptor oasis, int x, int y, int z) {
		if (!hasOasisSidePipe(oasis)) {
			return null;
		}
		int side = oasisSidePipeSide(oasis);
		int mouthDistance = oasisSidePipeMouthDistance(oasis);
		int mouthX = oasis.basinX() + sideOffsetX(side, mouthDistance);
		int mouthZ = oasis.basinZ() + sideOffsetZ(side, mouthDistance);
		int sourceY = oasisSidePipeSourceY(oasis);
		int innerRadius = MegastructureMath.range(oasis.seed() >>> 26, 2, 4);
		boolean fall = x == mouthX && z == mouthZ
				&& y >= oasis.floorY() + 2 && y <= sourceY - innerRadius;
		return fall ? BlockPalette.WATER : null;
	}

	private boolean isOasisBasin(OasisDescriptor oasis, int dx, int dz, int expansion) {
		int lobes = 1 + MegastructureMath.range(oasis.seed() >>> 42, 0, 3);
		for (int lobe = 0; lobe < lobes; lobe++) {
			long lobeHash = MegastructureMath.hash(oasis.seed(), lobe, 0, 1763);
			int direction = Math.floorMod((int) lobeHash, 10);
			int offset = lobe == 0 ? 0 : MegastructureMath.range(lobeHash >>> 8, 5, oasis.basinRadius() + 10);
			int centerX = TREE_DIRECTIONS_X[direction] * offset / 100;
			int centerZ = TREE_DIRECTIONS_Z[direction] * offset / 100;
			int baseRadius = lobe == 0
					? oasis.basinRadius()
					: Math.max(5, oasis.basinRadius() * MegastructureMath.range(lobeHash >>> 16, 46, 82) / 100);
			int radiusX = Math.max(3, baseRadius + expansion + MegastructureMath.range(lobeHash >>> 24, -3, 5));
			int radiusZ = Math.max(3, baseRadius + expansion + MegastructureMath.range(lobeHash >>> 32, -3, 5));
			if (lobe == 0 && oasisSceneFeatureEnabled(oasis, 7)) {
				if (oasis.side() <= 1) {
					radiusX *= 2;
				} else {
					radiusZ *= 2;
				}
			}
			int localX = dx - centerX;
			int localZ = dz - centerZ;
			int worldX = oasis.basinX() + dx;
			int worldZ = oasis.basinZ() + dz;
			if ((oasis.district() == DISTRICT_TANK_CLUSTER
					|| oasis.district() == DISTRICT_RESERVOIR_HALL
					|| oasis.district() == DISTRICT_RING_VAULT)
					&& (long) (worldX - oasis.centerX()) * (worldX - oasis.centerX())
							+ (long) (worldZ - oasis.centerZ()) * (worldZ - oasis.centerZ())
							> (long) Math.max(4, oasis.hostRadius() - 6) * Math.max(4, oasis.hostRadius() - 6)) {
				continue;
			}
			if ((long) localX * localX * radiusZ * radiusZ
					+ (long) localZ * localZ * radiusX * radiusX
					<= (long) radiusX * radiusX * radiusZ * radiusZ) {
				return true;
			}
		}
		return false;
	}

	private boolean oasisFloorSiteOpen(OasisDescriptor oasis, int x, int z) {
		return oasisBaseSolidAt(oasis.district(), x, oasis.floorY(), z)
				&& !oasisBaseSolidAt(oasis.district(), x, oasis.floorY() + 1, z);
	}

	private int oasisVerticalReach(OasisDescriptor oasis) {
		return isGiantOasisHostDistrict(oasis.district()) ? 360 : 180;
	}

	private BlockState oasisOriginState(OasisDescriptor oasis, int x, int y, int z) {
		if (oasis.origin() == 1) {
			int drops = 1;
			for (int drop = 0; drop < drops; drop++) {
				long dropHash = MegastructureMath.hash(oasis.seed(), drop, 0, 1759);
				int direction = drop == 0 ? 0 : Math.floorMod((int) dropHash, 10);
				int offset = drop == 0 ? 0 : MegastructureMath.range(dropHash >>> 8, 5, 13);
				int dropX = oasis.basinX() + TREE_DIRECTIONS_X[direction] * offset / 100;
				int dropZ = oasis.basinZ() + TREE_DIRECTIONS_Z[direction] * offset / 100;
				if (!oasisFloorSiteOpen(oasis, dropX, dropZ)) {
					continue;
				}
				int dropTop = oasis.sourceY();
				int dx = x - dropX;
				int dz = z - dropZ;
				if (y == dropTop + 1 && Math.abs(dx) <= 3 && Math.abs(dz) <= 3) {
					return Math.abs(dx) == 3 || Math.abs(dz) == 3 ? BlockPalette.WALL_PANEL : BlockPalette.GRATE;
				}
				if ((Math.abs(dx) == 3 && Math.abs(dz) <= 1 || Math.abs(dz) == 3 && Math.abs(dx) <= 1)
						&& y >= dropTop + 2 && y <= dropTop + 7) {
					return BlockPalette.FOUNDATION;
				}
			}
			return null;
		}

		if (oasis.origin() == 2) {
			int wallX = oasis.centerX() + sideOffsetX(oasis.side(), oasis.pipeWallDistance());
			int wallZ = oasis.centerZ() + sideOffsetZ(oasis.side(), oasis.pipeWallDistance());
			int along = oasis.side() <= 1 ? z - wallZ : x - wallX;
			int depth = oasis.side() <= 1 ? x - wallX : z - wallZ;
			if (Math.abs(depth) <= 1 && Math.abs(along) <= 7
					&& y >= oasis.floorY() + 1 && y <= oasis.sourceY() + 5
					&& oasisBaseSolidAt(oasis.district(), x, y, z)) {
				long scar = MegastructureMath.hash(oasis.seed(), along, y, 1811);
				return Math.floorMod(scar, 4) == 0 ? BlockPalette.MOSS : BlockPalette.CRACKED_PANEL;
			}
			return null;
		}

		int dx = x - oasis.basinX();
		int dz = z - oasis.basinZ();
		int dist2 = dx * dx + dz * dz;
		if (oasis.origin() == 3) {
			int sourceTop = oasis.floorY() + 9;
			boolean standpipeShell = dist2 >= 4 && dist2 <= 12
					&& y >= oasis.floorY() + 1 && y <= sourceTop;
			if (standpipeShell) {
				return Math.floorMod(y - oasis.floorY(), 4) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.PIPE;
			}
			if (y == sourceTop + 1 && dist2 <= 16 && dist2 >= 4) {
				return BlockPalette.CRACKED_PANEL;
			}
		}
		if (oasis.origin() == 3 && y == oasis.floorY() + 1) {
			if (dist2 <= 4) {
				return BlockPalette.WATER;
			}
			if (dist2 >= 16 && dist2 <= 49 && Math.floorMod(dx + dz, 4) != 0) {
				return BlockPalette.CRACKED_PANEL;
			}
		}
		if (oasis.origin() == 4 && y == oasis.floorY() + 1 && oasisFloorSiteOpen(oasis, x, z)) {
			int along = oasis.side() <= 1 ? dx : dz;
			int cross = oasis.side() <= 1 ? dz : dx;
			if (along >= oasis.basinRadius() + 4 && along <= oasis.basinRadius() + 18
					&& Math.abs(cross) <= 12 && !isOasisBasin(oasis, dx, dz, 2)) {
				return Math.floorMod(along + cross, 5) == 0 ? BlockPalette.MOSS : BlockPalette.WALL_PANEL;
			}
		}
		return null;
	}

	private boolean oasisBaseSolidAt(int district, int x, int y, int z) {
		if (y < settings.floorY() || y > settings.ceilingY()) {
			return false;
		}
		boolean air = isRailwayAir(x, y, z) || districtAir(district, x, y, z);
		BlockState structural = structuralOverlay(district, x, y, z, air);
		return structural != null ? !structural.isAir() : !air;
	}

	private BlockState oasisSceneState(OasisDescriptor oasis, int x, int y, int z) {
		for (int feature = 4; feature <= 8; feature++) {
			if (!oasisSceneFeatureEnabled(oasis, feature)) {
				continue;
			}
			BlockState state = switch (feature) {
				case 4 -> oasisTerraceState(oasis, x, y, z);
				case 5 -> oasisRootCathedralState(oasis, x, y, z);
				case 7 -> oasisDrownedGalleryState(oasis, x, y, z);
				case 8 -> oasisHangingDeltaState(oasis, x, y, z);
				default -> null;
			};
			if (state != null) {
				return state;
			}
		}
		return null;
	}

	private boolean oasisSceneFeatureEnabled(OasisDescriptor oasis, int feature) {
		if (oasis.district() == DISTRICT_TANK_CLUSTER && (feature == 4 || feature == 6 || feature == 7)) {
			return false;
		}
		if (oasis.profile() == feature) {
			return true;
		}
		int firstModifier = 4 + Math.floorMod((int) (oasis.seed() >>> 44), 5);
		int secondModifier = 4 + Math.floorMod((int) (oasis.seed() >>> 52), 5);
		return feature == firstModifier
				|| (feature == secondModifier && secondModifier != firstModifier && Math.floorMod(oasis.seed() >>> 20, 3) != 0);
	}

	private BlockState oasisTerraceState(OasisDescriptor oasis, int x, int y, int z) {
		int terraceRadius = oasis.basinRadius();
		int terraceCount = MegastructureMath.range(oasis.seed() >>> 34, 2, 5);
		int spacing = terraceRadius * 2 + MegastructureMath.range(oasis.seed() >>> 38, 5, 11);
		int direction = Math.floorMod((int) (oasis.seed() >>> 36), 2) == 0 ? 1 : -1;
		for (int terrace = 0; terrace < terraceCount; terrace++) {
			int cross = (terrace - (terraceCount - 1) / 2) * spacing * direction;
			int centerX = oasis.basinX() + (oasis.side() <= 1 ? 0 : cross);
			int centerZ = oasis.basinZ() + (oasis.side() <= 1 ? cross : 0);
			if (!oasisFloorSiteOpen(oasis, centerX, centerZ)) {
				continue;
			}
			long terraceHash = MegastructureMath.hash(oasis.seed(), terrace, 0, 1777);
			int radius = Math.max(7, terraceRadius + MegastructureMath.range(terraceHash, -5, 5));
			int level = oasis.floorY() + 1 + terrace * MegastructureMath.range(oasis.seed() >>> 30, 1, 3);
			int dx = x - centerX;
			int dz = z - centerZ;
			int distance2 = dx * dx + dz * dz;
			if (distance2 <= (radius + 3) * (radius + 3) && y >= oasis.floorY() && y < level) {
				return Math.floorMod(dx + dz + terrace, 11) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.FOUNDATION;
			}
			if (y == level && distance2 <= radius * radius) {
				return BlockPalette.WATER;
			}
			if (y == level && distance2 <= (radius + 3) * (radius + 3)) {
				return BlockPalette.MOSS;
			}
		}
		return null;
	}

	private BlockState oasisRootCathedralState(OasisDescriptor oasis, int x, int y, int z) {
		int direction = Math.floorMod((int) (oasis.seed() >>> 28), 10);
		int treeX = oasis.basinX();
		int treeZ = oasis.basinZ();
		boolean foundTreeSite = false;
		for (int attempt = 0; attempt < 10; attempt++) {
			int candidateDirection = Math.floorMod(direction + attempt * 3, 10);
			int treeOffset = oasis.basinRadius() + MegastructureMath.range(oasis.seed() >>> (32 + attempt % 4), 8, 16);
			int candidateX = oasis.basinX() + TREE_DIRECTIONS_X[candidateDirection] * treeOffset / 100;
			int candidateZ = oasis.basinZ() + TREE_DIRECTIONS_Z[candidateDirection] * treeOffset / 100;
			if (oasisTreeSiteValid(oasis, candidateX, candidateZ)) {
				treeX = candidateX;
				treeZ = candidateZ;
				foundTreeSite = true;
				break;
			}
		}
		if (!foundTreeSite) {
			return null;
		}
		int dx = x - treeX;
		int dz = z - treeZ;
		int height = MegastructureMath.range(oasis.seed() >>> 32, 17, 31);
		DynamicWorldgenPalette.TreeMaterial material = DynamicWorldgenPalette.treeMaterial(oasis.seed(), 1000);
		if (y == oasis.floorY() + 1) {
			int rootCount = MegastructureMath.range(oasis.seed() >>> 48, 4, 6);
			for (int root = 0; root < rootCount; root++) {
				long rootHash = MegastructureMath.hash(oasis.seed(), root, 0, 1801);
				int rootDirection = Math.floorMod((int) rootHash + root * 2, 10);
				int rootReach = MegastructureMath.range(rootHash >>> 12, 5, 13);
				for (int step = 0; step <= rootReach; step++) {
					int rootX = TREE_DIRECTIONS_X[rootDirection] * step / 100;
					int rootZ = TREE_DIRECTIONS_Z[rootDirection] * step / 100;
					int width = step < 3 ? 1 : 0;
					if (Math.abs(dx - rootX) <= width && Math.abs(dz - rootZ) <= width) {
						return Math.floorMod(step + root, 6) == 0 ? BlockPalette.MOSS : material.log();
					}
				}
			}
		}
		if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1
				&& y >= oasis.floorY() + 2 && y <= oasis.floorY() + height) {
			return material.log();
		}
		int crownY = oasis.floorY() + height;
		int crownDy = Math.abs(y - crownY);
		int crownRadius = MegastructureMath.range(oasis.seed() >>> 40, 6, 9) - crownDy * 2;
		if (crownRadius >= 2 && dx * dx + dz * dz <= crownRadius * crownRadius) {
			return material.leaves();
		}
		return null;
	}

	private BlockState oasisDrownedGalleryState(OasisDescriptor oasis, int x, int y, int z) {
		if (y != oasis.floorY() + 1) {
			return null;
		}
		int dx = x - oasis.basinX();
		int dz = z - oasis.basinZ();
		int along = oasis.side() <= 1 ? dx : dz;
		int cross = oasis.side() <= 1 ? dz : dx;
		int brokenBand = Math.floorMod(along + (int) oasis.seed(), 23);
		if (Math.abs(cross) <= 2 && Math.abs(along) <= oasis.basinRadius() * 2 + 4 && brokenBand > 3) {
			return BlockPalette.WALKWAY;
		}
		for (int island = -1; island <= 1; island++) {
			int islandAlong = island * oasis.basinRadius();
			int islandCross = (island == 0 ? 1 : -1) * oasis.basinRadius() / 2;
			int idx = along - islandAlong;
			int idz = cross - islandCross;
			if (idx * idx + idz * idz <= 16) {
				return Math.floorMod(idx + idz, 3) == 0 ? BlockPalette.MOSS : BlockPalette.WALL_PANEL;
			}
		}
		return null;
	}

	private BlockState oasisHangingDeltaState(OasisDescriptor oasis, int x, int y, int z) {
		int wallX = oasis.centerX() + sideOffsetX(oasis.side(), oasis.pipeWallDistance());
		int wallZ = oasis.centerZ() + sideOffsetZ(oasis.side(), oasis.pipeWallDistance());
		int along = oasis.side() <= 1 ? x - wallX : z - wallZ;
		int cross = oasis.side() <= 1 ? z - wallZ : x - wallX;
		int shelfCount = MegastructureMath.range(oasis.seed() >>> 35, 2, 5);
		int shelfStep = MegastructureMath.range(oasis.seed() >>> 39, 16, 27);
		for (int shelf = 0; shelf < shelfCount; shelf++) {
			long shelfHash = MegastructureMath.hash(oasis.seed(), shelf, 0, 1783);
			int shelfY = oasis.sourceY() - 14 - shelf * shelfStep;
			int reach = MegastructureMath.range(shelfHash, 5, 14);
			if (y == shelfY && Math.abs(cross) <= 10 + shelf * 3 && Math.abs(along) <= reach) {
				return Math.floorMod(cross + (int) shelfHash, 7) == 0 ? BlockPalette.MOSS : BlockPalette.WALKWAY;
			}
			int braceY = shelfY - Math.max(0, Math.abs(along));
			if (Math.abs(cross) == 8 + shelf * 2 && Math.abs(along) <= reach && Math.abs(y - braceY) <= 1) {
				return BlockPalette.FOUNDATION;
			}
		}
		return null;
	}

	private BlockState oasisPipeState(OasisDescriptor oasis, int x, int y, int z) {
		if (!hasOasisSidePipe(oasis)) {
			return null;
		}
		int side = oasisSidePipeSide(oasis);
		int axis;
		int mouth;
		int wall;
		int perpendicular;
		if (side <= 1) {
			axis = x;
			mouth = oasis.basinX() + sideOffsetX(side, oasisSidePipeMouthDistance(oasis));
			wall = oasis.centerX() + sideOffsetX(side, oasis.pipeWallDistance());
			perpendicular = z - oasis.basinZ();
		} else {
			axis = z;
			mouth = oasis.basinZ() + sideOffsetZ(side, oasisSidePipeMouthDistance(oasis));
			wall = oasis.centerZ() + sideOffsetZ(side, oasis.pipeWallDistance());
			perpendicular = x - oasis.basinX();
		}
		int outward = Integer.compare(mouth, wall);
		if (outward == 0) {
			outward = side == 0 || side == 2 ? 1 : -1;
		}
		int endpoint = mouth;
		int embedDepth = Math.min(8, Math.max(1, oasis.pipeWallDistance() - 1));
		int embeddedBack = wall - outward * embedDepth;
		if (!between(axis, endpoint, embeddedBack)) {
			return null;
		}
		int sourceY = oasisSidePipeSourceY(oasis);
		int dy = y - sourceY;
		int along = Math.abs(axis - endpoint);
		int innerRadius = MegastructureMath.range(oasis.seed() >>> 26, 2, 4);
		int outerRadius = innerRadius + MegastructureMath.range(oasis.seed() >>> 30, 2, 3);
		int cross2 = perpendicular * perpendicular + dy * dy;
		int collarSpacing = MegastructureMath.range(oasis.seed() >>> 38, 9, 16);
		boolean collar = Math.floorMod(along, collarSpacing) <= 1;
		if (collar && cross2 >= (outerRadius - 1) * (outerRadius - 1)
				&& cross2 <= (outerRadius + 1) * (outerRadius + 1)) {
			return BlockPalette.WALL_PANEL;
		}
		if (cross2 >= innerRadius * innerRadius && cross2 <= outerRadius * outerRadius) {
			return oasis.profile() == 0 || Math.floorMod(oasis.seed() >>> 22, 5) == 0
					? BlockPalette.RUST_PIPE
					: BlockPalette.PIPE;
		}
		if (cross2 < innerRadius * innerRadius) {
			boolean waterChannel = between(axis, mouth, embeddedBack)
					&& dy >= -(innerRadius - 1) && dy <= 0;
			return waterChannel ? BlockPalette.WATER : BlockPalette.AIR;
		}

		int braceDistance = Math.abs(axis - wall);
		int braceY = sourceY - 11 + braceDistance / 2;
		if (braceDistance <= 12 && Math.abs(perpendicular) <= 1 && Math.abs(y - braceY) <= 1) {
			return BlockPalette.FOUNDATION;
		}
		int supportSpacing = 22 + Math.floorMod((int) (oasis.seed() >>> 44), 9);
		if (Math.abs(axis - mouth) > outerRadius + 8
				&& Math.floorMod(Math.abs(axis - mouth), supportSpacing) <= 1
				&& Math.abs(perpendicular) <= 1
				&& y >= oasis.floorY() + 1 && y <= sourceY - outerRadius) {
			return Math.floorMod(y - oasis.floorY(), 11) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
		}
		return null;
	}

	private boolean hasOasisSidePipe(OasisDescriptor oasis) {
		if (oasis.pipeWallDistance() < 12) {
			return false;
		}
		int side = oasisSidePipeSide(oasis);
		int mouth = side <= 1
				? oasis.basinX() + sideOffsetX(side, oasisSidePipeMouthDistance(oasis))
				: oasis.basinZ() + sideOffsetZ(side, oasisSidePipeMouthDistance(oasis));
		int wall = side <= 1
				? oasis.centerX() + sideOffsetX(side, oasis.pipeWallDistance())
				: oasis.centerZ() + sideOffsetZ(side, oasis.pipeWallDistance());
		return Math.abs(wall - mouth) >= 18;
	}

	private int oasisSidePipeMouthDistance(OasisDescriptor oasis) {
		return Math.max(5, oasis.basinRadius() * 2 / 3);
	}

	private int oasisSidePipeSide(OasisDescriptor oasis) {
		return oasis.origin() == 0
				? oasis.side()
				: Math.floorMod((int) (oasis.seed() >>> 22), 4);
	}

	private int oasisSidePipeSourceY(OasisDescriptor oasis) {
		return oasis.origin() == 0
				? oasis.sourceY()
				: Math.min(
						settings.ceilingY() - 24,
						oasis.floorY() + MegastructureMath.range(oasis.seed() >>> 30, 48, 142)
				);
	}

	private int oasisRootedTreeIndex(OasisDescriptor oasis) {
		if (!oasis.rootedDirt()) {
			return -1;
		}
		for (int i = 0; i < oasis.treeCount(); i++) {
			long treeHash = MegastructureMath.hash(oasis.seed(), i, 0, 1733);
			int direction = Math.floorMod(
					MegastructureMath.range(treeHash >>> 4, 0, 9) + i * MegastructureMath.range(oasis.seed() >>> 40, 2, 5),
					10
			);
			boolean giantTree = isGiantOasisHostDistrict(oasis.district())
					&& i == 0 && Math.floorMod(oasis.seed() >>> 18, 3) == 0;
			int radius = oasis.basinRadius() + MegastructureMath.range(treeHash, 9, giantTree ? 58 : 25);
			int treeX = oasis.basinX() + TREE_DIRECTIONS_X[direction] * radius / 100;
			int treeZ = oasis.basinZ() + TREE_DIRECTIONS_Z[direction] * radius / 100;
			if (oasisTreeSiteValid(oasis, treeX, treeZ)) {
				return i;
			}
		}
		return -1;
	}

	private BlockState oasisTreeState(OasisDescriptor oasis, int x, int y, int z) {
		int rootedTreeIndex = oasisRootedTreeIndex(oasis);
		for (int i = 0; i < oasis.treeCount(); i++) {
			long treeHash = MegastructureMath.hash(oasis.seed(), i, 0, 1733);
			int direction = Math.floorMod(
					MegastructureMath.range(treeHash >>> 4, 0, 9) + i * MegastructureMath.range(oasis.seed() >>> 40, 2, 5),
					10
			);
			boolean giantTree = isGiantOasisHostDistrict(oasis.district())
					&& i == 0 && Math.floorMod(oasis.seed() >>> 18, 3) == 0;
			int radius = oasis.basinRadius() + MegastructureMath.range(treeHash, 9, giantTree ? 58 : 25);
			int treeX = oasis.basinX() + TREE_DIRECTIONS_X[direction] * radius / 100;
			int treeZ = oasis.basinZ() + TREE_DIRECTIONS_Z[direction] * radius / 100;
			int height = giantTree
					? MegastructureMath.range(treeHash >>> 12, 38, 76)
					: MegastructureMath.range(treeHash >>> 12, 5, i == 0 ? 16 : 12);
			if (!oasisTreeSiteValid(oasis, treeX, treeZ)) {
				continue;
			}
			int leanDirection = Math.floorMod((int) (treeHash >>> 28), 10);
			int leanAmount = MegastructureMath.range(treeHash >>> 36, 0, 3);
			int level = Math.max(0, y - (oasis.floorY() + 2));
			int currentLean = Math.min(leanAmount, level * (leanAmount + 1) / Math.max(1, height));
			int leanedX = treeX + TREE_DIRECTIONS_X[leanDirection] * currentLean / 100;
			int leanedZ = treeZ + TREE_DIRECTIONS_Z[leanDirection] * currentLean / 100;
			int dx = x - leanedX;
			int dz = z - leanedZ;
			DynamicWorldgenPalette.TreeMaterial material = DynamicWorldgenPalette.treeMaterial(oasis.seed(), i);
			if (i == rootedTreeIndex && x == treeX && z == treeZ && y == oasis.floorY()) {
				return BlockPalette.ROOTED_DIRT;
			}
			if (y == oasis.floorY() + 1) {
				int rootCount = MegastructureMath.range(treeHash >>> 6, giantTree ? 6 : 3, giantTree ? 8 : 5);
				for (int root = 0; root < rootCount; root++) {
					long rootHash = MegastructureMath.hash(treeHash, root, 0, 1739);
					int rootDirection = Math.floorMod((int) rootHash + root * 2, TREE_DIRECTIONS_X.length);
					int rootReach = MegastructureMath.range(rootHash >>> 10, giantTree ? 8 : 4, giantTree ? 20 : 11);
					int sideDirection = Math.floorMod(rootDirection + 2, TREE_DIRECTIONS_X.length);
					for (int step = 2; step <= rootReach; step++) {
						int bend = Math.floorMod((int) (rootHash >>> 24) + step / 3, 3) - 1;
						int rootX = TREE_DIRECTIONS_X[rootDirection] * step / 100
								+ TREE_DIRECTIONS_X[sideDirection] * bend / 100;
						int rootZ = TREE_DIRECTIONS_Z[rootDirection] * step / 100
								+ TREE_DIRECTIONS_Z[sideDirection] * bend / 100;
						int rootWidth = giantTree && step <= 4 ? 1 : 0;
						if (Math.abs(x - treeX - rootX) <= rootWidth && Math.abs(z - treeZ - rootZ) <= rootWidth) {
							return step <= 3 || Math.floorMod(rootHash + step, 5) != 0 ? material.log() : BlockPalette.MOSS;
						}
					}
				}
			}
			if (level <= 2 && Math.floorMod(treeHash >>> 52, 5) == 0) {
				int fallDirection = Math.floorMod((int) (treeHash >>> 56), TREE_DIRECTIONS_X.length);
				int fallLength = MegastructureMath.range(treeHash >>> 20, giantTree ? 20 : 9, giantTree ? 44 : 24);
				int fx = TREE_DIRECTIONS_X[fallDirection];
				int fz = TREE_DIRECTIONS_Z[fallDirection];
				int along = Math.floorDiv((x - treeX) * fx + (z - treeZ) * fz, 100);
				int cross = Math.floorDiv((x - treeX) * fz - (z - treeZ) * fx, 100);
				if (along >= 4 && along <= fallLength && Math.abs(cross) <= (giantTree ? 2 : 1)
						&& level == 1 + Math.min(1, along / 18)) {
					return material.log();
				}
			}
			if (y == oasis.floorY() + 1 && Math.abs(dx) + Math.abs(dz) <= 1) {
				return BlockPalette.MOSS;
			}
			if (Math.abs(dx) <= (giantTree ? 1 : 0) && Math.abs(dz) <= (giantTree ? 1 : 0)
					&& y >= oasis.floorY() + 2 && y <= oasis.floorY() + height) {
				return material.log();
			}
			int crownY = oasis.floorY() + height;
			int crownDy = Math.abs(y - crownY);
			int crownDepth = MegastructureMath.range(treeHash >>> 44, 2, 4);
			if (crownDy <= crownDepth) {
				int crownRadius = Math.max(1, MegastructureMath.range(treeHash >>> 48, giantTree ? 10 : 3, giantTree ? 16 : 5) - crownDy);
				if (dx * dx + dz * dz <= crownRadius * crownRadius) {
					long leafHash = MegastructureMath.hash(treeHash, x, y, z);
					if (Math.floorMod(leafHash, giantTree ? 17 : 11) == 0 && crownDy > 0) {
						return null;
					}
					return material.leaves();
				}
			}
			if (y < crownY && y >= crownY - MegastructureMath.range(treeHash >>> 30, 5, giantTree ? 20 : 11)) {
				int strandRadius = Math.max(2, MegastructureMath.range(treeHash >>> 48, giantTree ? 8 : 3, giantTree ? 14 : 5));
				long strandHash = MegastructureMath.hash(treeHash, Math.floorDiv(dx, 2), Math.floorDiv(dz, 2), 1747);
				if (dx * dx + dz * dz <= strandRadius * strandRadius
						&& dx * dx + dz * dz >= (strandRadius - 2) * (strandRadius - 2)
						&& Math.floorMod(strandHash, 5) == 0) {
					return dx >= 0 && Math.abs(dx) >= Math.abs(dz)
							? BlockPalette.vine(Direction.EAST)
							: dx < 0 && Math.abs(dx) >= Math.abs(dz)
									? BlockPalette.vine(Direction.WEST)
									: dz >= 0 ? BlockPalette.vine(Direction.SOUTH) : BlockPalette.vine(Direction.NORTH);
				}
			}
		}
		return null;
	}

	private BlockState oasisIndustrialOvergrowthState(OasisDescriptor oasis, int x, int y, int z) {
		int dx = x - oasis.basinX();
		int dz = z - oasis.basinZ();
		boolean giant = isGiantOasisHostDistrict(oasis.district());
		int reach = oasis.basinRadius() + (giant ? 300 : 132);
		if ((long) dx * dx + (long) dz * dz > (long) reach * reach
				|| y < oasis.floorY() + 1 || y > oasis.floorY() + oasisVerticalReach(oasis)) {
			return null;
		}
		boolean solid = oasisBaseSolidAt(oasis.district(), x, y, z);
		boolean eastSolid = oasisBaseSolidAt(oasis.district(), x + 1, y, z);
		boolean westSolid = oasisBaseSolidAt(oasis.district(), x - 1, y, z);
		boolean southSolid = oasisBaseSolidAt(oasis.district(), x, y, z + 1);
		boolean northSolid = oasisBaseSolidAt(oasis.district(), x, y, z - 1);
		int distance = (int) Math.sqrt((long) dx * dx + (long) dz * dz);
		int surfaceChance = giant
				? (distance <= oasis.basinRadius() + 72 ? 64 : distance <= oasis.basinRadius() + 188 ? 34 : 18)
				: (distance <= oasis.basinRadius() + 34 ? 58 : distance <= oasis.basinRadius() + 88 ? 28 : 12);
		long climbHash = MegastructureMath.hash(oasis.seed(), Math.floorDiv(x, 8), Math.floorDiv(z, 8), 1889);
		int climbHeight = MegastructureMath.range(climbHash, giant ? 38 : 24, oasisVerticalReach(oasis));
		boolean connectedSurface = connectedOasisMossAt(oasis, dx, dz, reach, surfaceChance);
		if (solid) {
			boolean exposed = !eastSolid || !westSolid || !southSolid || !northSolid;
			return exposed && connectedSurface && oasisWallMossInsertAt(oasis, x, y, z, climbHeight)
					? BlockPalette.MOSS
					: null;
		}

		if (!connectedSurface || y > oasis.floorY() + climbHeight) {
			return null;
		}
		boolean belowSolid = oasisBaseSolidAt(oasis.district(), x, y - 1, z);
		long strand = MegastructureMath.hash(oasis.seed(), Math.floorDiv(x, 3), Math.floorDiv(z, 3), 1877);
		int bottom = oasis.floorY() + MegastructureMath.range(strand >>> 8, 1, 18);
		int top = Math.min(
				oasis.floorY() + climbHeight,
				bottom + MegastructureMath.range(strand >>> 24, 14, 86)
		);
		if (belowSolid && y <= oasis.floorY() + (giant ? 30 : 20) && Math.floorMod(strand >>> 38, giant ? 4 : 5) != 0) {
			return BlockPalette.MOSS_CARPET;
		}
		if (y < bottom || y > top) {
			return null;
		}
		if (eastSolid) {
			return BlockPalette.vine(Direction.EAST);
		}
		if (westSolid) {
			return BlockPalette.vine(Direction.WEST);
		}
		if (southSolid) {
			return BlockPalette.vine(Direction.SOUTH);
		}
		if (northSolid) {
			return BlockPalette.vine(Direction.NORTH);
		}
		return null;
	}

	private boolean oasisWallMossInsertAt(OasisDescriptor oasis, int x, int y, int z, int climbHeight) {
		int level = y - oasis.floorY();
		if (level < 0 || level > climbHeight) {
			return false;
		}
		boolean xSurface = !oasisBaseSolidAt(oasis.district(), x + 1, y, z)
				|| !oasisBaseSolidAt(oasis.district(), x - 1, y, z);
		int along = xSurface ? z - oasis.basinZ() : x - oasis.basinX();
		int strandCount = isGiantOasisHostDistrict(oasis.district()) ? 34 : 18;
		for (int strand = 0; strand < strandCount; strand++) {
			long hash = MegastructureMath.hash(oasis.seed(), strand, xSurface ? 1 : 0, 1913);
			int start = MegastructureMath.range(hash >>> 8, -oasis.basinRadius() - 74, oasis.basinRadius() + 74);
			int height = MegastructureMath.range(hash >>> 24, 18, Math.max(24, climbHeight));
			if (level > height) {
				continue;
			}
			int slope = MegastructureMath.range(hash >>> 40, -5, 5);
			int segment = Math.floorDiv(level, 11);
			int meander = MegastructureMath.range(MegastructureMath.hash(hash, segment, 0, 1919), -5, 5);
			int center = start + level * slope / 12 + meander;
			int width = Math.floorMod(hash >>> 52, 7) == 0 ? 5 : Math.floorMod(hash >>> 48, 3) == 0 ? 3 : 2;
			if (Math.abs(along - center) <= width) {
				return true;
			}
		}
		return false;
	}

	private boolean oasisTreeSiteValid(OasisDescriptor oasis, int x, int z) {
		int dx = x - oasis.basinX();
		int dz = z - oasis.basinZ();
		return oasisFloorSiteOpen(oasis, x, z)
				&& !isOasisBasin(oasis, dx, dz, 3)
				&& !isOasisTerraceFootprint(oasis, x, z);
	}

	private boolean isOasisTerraceFootprint(OasisDescriptor oasis, int x, int z) {
		if (!oasisSceneFeatureEnabled(oasis, 4)) {
			return false;
		}
		int terraceRadius = oasis.basinRadius();
		int terraceCount = MegastructureMath.range(oasis.seed() >>> 34, 2, 5);
		int spacing = terraceRadius * 2 + MegastructureMath.range(oasis.seed() >>> 38, 5, 11);
		int direction = Math.floorMod((int) (oasis.seed() >>> 36), 2) == 0 ? 1 : -1;
		for (int terrace = 0; terrace < terraceCount; terrace++) {
			int cross = (terrace - (terraceCount - 1) / 2) * spacing * direction;
			int centerX = oasis.basinX() + (oasis.side() <= 1 ? 0 : cross);
			int centerZ = oasis.basinZ() + (oasis.side() <= 1 ? cross : 0);
			long terraceHash = MegastructureMath.hash(oasis.seed(), terrace, 0, 1777);
			int radius = Math.max(7, terraceRadius + MegastructureMath.range(terraceHash, -5, 5));
			int dx = x - centerX;
			int dz = z - centerZ;
			if (dx * dx + dz * dz <= (radius + 3) * (radius + 3)) {
				return true;
			}
		}
		return false;
	}

	private BlockState oasisVineState(OasisDescriptor oasis, int x, int y, int z) {
		if (oasis.district() == DISTRICT_RING_VAULT || oasis.district() == DISTRICT_CONDUIT_BASILICA) {
			return null;
		}
		Direction attachedTo;
		if (oasis.side() == 0) {
			attachedTo = Direction.EAST;
		} else if (oasis.side() == 1) {
			attachedTo = Direction.WEST;
		} else if (oasis.side() == 2) {
			attachedTo = Direction.SOUTH;
		} else {
			attachedTo = Direction.NORTH;
		}

		boolean circular = oasis.district() == DISTRICT_TANK_CLUSTER
				|| oasis.district() == DISTRICT_RESERVOIR_HALL
				|| oasis.district() == DISTRICT_RING_VAULT;
		int spread = Math.min(20, Math.max(1, oasis.hostRadius() - 2));
		int inner = Math.max(1, spread / 3);
		int[] offsets = {-spread, -inner, inner, spread};
		for (int i = 0; i < offsets.length; i++) {
			long vineHash = MegastructureMath.hash(oasis.seed(), i, 0, 1741);
			int wallDistance = circular
					? (int) Math.floor(Math.sqrt(oasis.hostRadius() * oasis.hostRadius() - offsets[i] * offsets[i]))
					: oasis.hostRadius();
			int vineX = oasis.centerX() + (oasis.side() == 0 ? wallDistance : oasis.side() == 1 ? -wallDistance : offsets[i]);
			int vineZ = oasis.centerZ() + (oasis.side() == 2 ? wallDistance : oasis.side() == 3 ? -wallDistance : offsets[i]);
			int top = oasis.sourceY() - MegastructureMath.range(vineHash, 6, 22);
			int bottom = Math.max(oasis.floorY() + 3, top - MegastructureMath.range(vineHash >>> 12, 12, 48));
			if (x == vineX && z == vineZ && y >= bottom && y <= top
					&& !oasisBaseSolidAt(oasis.district(), x, y, z)) {
				int supportX = x + attachedTo.getOffsetX();
				int supportZ = z + attachedTo.getOffsetZ();
				if (oasisBaseSolidAt(oasis.district(), supportX, y, supportZ)) {
					return BlockPalette.vine(attachedTo);
				}
			}
		}
		return null;
	}

	private boolean districtAir(int district, int x, int y, int z) {
		return switch (district) {
			case DISTRICT_NETWORK -> isCellCorridor(x, y, z)
					|| isApartmentCorridor(x, y, z)
					|| isApartmentRoom(x, y, z)
					|| isServiceShaft(x, y, z)
					|| isLocalAtrium(x, y, z);
			case DISTRICT_DEAD_END -> isDeadEndCorridor(x, y, z);
			case DISTRICT_MONOLITH_HALL -> isMonolithHallVoid(x, y, z);
			case DISTRICT_COLUMN_FOREST -> isColumnForestVoid(x, y, z);
			case DISTRICT_CYLINDER -> isDistrictCylinderVoid(x, y, z);
			case DISTRICT_ABYSS -> isDistrictAbyssVoid(x, y, z);
			case DISTRICT_DESCENT -> isDescentWellVoid(x, y, z);
			case DISTRICT_BLOCK_TOWERS -> isBlockTowerVoid(x, y, z);
			case DISTRICT_TANK_CLUSTER -> isTankClusterVoid(x, y, z);
			case DISTRICT_SCAFFOLD -> isScaffoldVoid(x, y, z);
			case DISTRICT_INDUSTRIAL_WALL -> isIndustrialWallVoid(x, y, z);
			case DISTRICT_TRANSIT_NEXUS -> isTransitNexusVoid(x, y, z);
			case DISTRICT_REACTOR_CATHEDRAL -> isReactorCathedralVoid(x, y, z);
			case DISTRICT_HANGING_ARCHIVE -> isHangingArchiveVoid(x, y, z);
			case DISTRICT_VENTILATION_CANYON -> isVentilationCanyonVoid(x, y, z);
			case DISTRICT_INVERTED_PYRAMID -> isInvertedPyramidVoid(x, y, z);
			case DISTRICT_RING_VAULT -> isRingVaultVoid(x, y, z);
			case DISTRICT_MACHINE_NAVE -> isMachineNaveVoid(x, y, z);
			case DISTRICT_FRACTURED_HABITAT -> isFracturedHabitatVoid(x, y, z);
			case DISTRICT_CONDUIT_BASILICA -> isConduitBasilicaVoid(x, y, z);
			case DISTRICT_RESERVOIR_HALL -> isReservoirHallVoid(x, y, z);
			case DISTRICT_SUSPENDED_CITY -> isSuspendedCityVoid(x, y, z);
			case DISTRICT_IRIS_CHASM -> isIrisChasmVoid(x, y, z);
			case DISTRICT_MACHINE_ROOT_VAULT -> isMachineRootVaultVoid(x, y, z);
			case DISTRICT_TILTED_STACKS -> isTiltedStacksVoid(x, y, z);
			case DISTRICT_SILENT_FOUNDRY -> isSilentFoundryVoid(x, y, z);
			case DISTRICT_COLOSSUS_LIFT -> isColossusLiftVoid(x, y, z);
			case DISTRICT_FOLDED_CITY -> isFoldedCityVoid(x, y, z);
			case DISTRICT_UPPER_RIM_CITY -> isUpperRimCityVoid(x, y, z);
			case DISTRICT_ORBITAL_WEB_CORE -> isOrbitalWebCoreVoid(x, y, z);
			case DISTRICT_CROWN_SPIRE -> isCrownSpireVoid(x, y, z);
			case DISTRICT_GLOBE_MONUMENT -> isGlobeMonumentVoid(x, y, z);
			case DISTRICT_VOID_ALTAR -> isVoidAltarVoid(x, y, z);
			case DISTRICT_ATOM_STORM_ARRAY -> isAtomStormArrayVoid(x, y, z);
			case DISTRICT_BLACK_HOLE_REACTOR -> isBlackHoleReactorVoid(x, y, z);
			default -> isSparseWallCorridor(x, y, z);
		};
	}

	private boolean isPrimaryRift(int x, int z) {
		return isPrimaryRiftAt(x, z, settings.motifCellSize(), settings.riftMinWidth(), settings.riftMaxWidth());
	}

	static boolean isPrimaryRiftAt(int x, int z, int motifCellSize, int riftMinWidth, int riftMaxWidth) {
		return isPrimaryRiftAt(x, z, motifCellSize, riftMinWidth, riftMaxWidth, 0);
	}

	static boolean isPrimaryRiftBiomeAt(int x, int z, int motifCellSize, int riftMinWidth, int riftMaxWidth) {
		int padding = Math.min(motifCellSize / 5, 128);
		return isPrimaryRiftAt(x, z, motifCellSize, riftMinWidth, riftMaxWidth, padding);
	}

	private static boolean isPrimaryRiftAt(
			int x,
			int z,
			int motifCellSize,
			int riftMinWidth,
			int riftMaxWidth,
			int widthPadding
	) {
		int cell = motifCellSize;
		int stripe = MegastructureMath.floorDiv(x, cell);
		long hash = riftStripeHash(stripe);
		if (!isAcceptedRiftStripe(stripe, hash)) {
			return false;
		}

		int local = MegastructureMath.floorMod(x, cell);
		int center = cell / 2;
		int maxWidth = Math.max(riftMinWidth, riftMaxWidth);
		int minWidth = Math.min(riftMinWidth, riftMaxWidth);
		int width = MegastructureMath.range(hash >>> 12, minWidth, maxWidth);
		return Math.abs(local - center) <= width / 2 + widthPadding;
	}

	private static long riftStripeHash(int stripe) {
		return MegastructureMath.hash(activeWorldVariantSeed, stripe, 0, 13);
	}

	private static boolean isAcceptedRiftStripe(int stripe, long hash) {
		return stripe == guaranteedPrimaryRiftStripe() || Math.floorMod(hash, 9) == 0;
	}

	private static int guaranteedPrimaryRiftStripe() {
		return MegastructureMath.range(MegastructureMath.hash(activeWorldVariantSeed, 0, 0, 2069), -2, 2);
	}

	private boolean isSpawnPrecinctAir(int x, int y, int z) {
		if (Math.abs(x) > 144 || Math.abs(z) > 64) {
			return false;
		}
		int baseY = settings.spawnPlatformY();
		int variant = spawnStationVariant();
		boolean stationHall = Math.abs(x) <= SPAWN_HALL_LENGTHS[variant]
				&& Math.abs(z) <= SPAWN_HALL_WIDTHS[variant]
				&& y >= baseY + 1
				&& y <= baseY + SPAWN_HALL_HEIGHTS[variant];
		boolean stationRoom = false;
		for (int[] room : SPAWN_STATION_ROOMS[variant]) {
			if (x >= room[0] && x <= room[1] && z >= room[2] && z <= room[3]
					&& y >= baseY + 1 && y <= baseY + room[4] - 1) {
				stationRoom = true;
				break;
			}
		}
		boolean stairShaft = Math.abs(x + 72) <= 8
				&& Math.abs(z - 28) <= 8
				&& y >= baseY + 1 && y <= connectorNetworkY() + 5;
		return stationHall || stationRoom || stairShaft;
	}

	private BlockState spawnPrecinctState(int x, int y, int z) {
		if (Math.abs(x) > 144 || Math.abs(z) > 64) {
			return null;
		}

		int baseY = settings.spawnPlatformY();
		int variant = spawnStationVariant();
		if (y == baseY && isPrimaryRailwayFootprintAt(x, z, 7)) {
			return BlockPalette.WALKWAY;
		}
		if (y == baseY + 1) {
			BlockState rail = primaryRailStateAt(x, z);
			if (rail != null) {
				return rail;
			}
		}
		BlockState transferredDebris = spawnTransferDebrisState(x, y, z, baseY, variant);
		if (transferredDebris != null) {
			return transferredDebris;
		}
		if (y == baseY && Math.abs(x) <= SPAWN_PLATFORM_LENGTHS[variant]
				&& Math.abs(z) >= 8 && Math.abs(z) <= SPAWN_PLATFORM_WIDTHS[variant]) {
			return BlockPalette.PLATFORM;
		}
		if (y == baseY + 1 && Math.abs(x) <= SPAWN_PLATFORM_LENGTHS[variant]
				&& Math.abs(z) == Math.min(22, SPAWN_PLATFORM_WIDTHS[variant] - 3)) {
			return BlockPalette.WALL_PANEL;
		}
		BlockState architecture = spawnStationArchitectureState(x, y, z, baseY, variant);
		if (architecture != null) {
			return architecture;
		}
		BlockState room = spawnStationRoomState(x, y, z);
		if (room != null) {
			return room;
		}
		BlockState stationStair = spawnStationAccessStairState(x, y, z);
		if (stationStair != null) {
			return stationStair;
		}
		if (y == connectorNetworkY() && isSpawnNetworkLinkAt(x, z, 3)) {
			return BlockPalette.WALKWAY;
		}

		return null;
	}

	private BlockState spawnTransferDebrisState(int x, int y, int z, int baseY, int variant) {
		if (y != baseY + 1 && y != baseY + 2) {
			return null;
		}
		if (!isSpawnTransferDebrisFloor(x, z, variant) || isPrimaryRailwayFootprintAt(x, z, 9)) {
			return null;
		}
		BlockState looseStone = spawnTransferLooseStoneState(x, y, z, baseY, variant);
		if (looseStone != null) {
			return looseStone;
		}
		BlockState epicenter = spawnTransferEpicenterState(x, y, z, baseY, variant);
		if (epicenter != null) {
			return epicenter;
		}
		BlockState wood = spawnTransferWoodState(x, y, z, baseY, variant);
		if (wood != null) {
			return wood;
		}
		BlockState wall = spawnTransferWallShardState(x, y, z, baseY, variant);
		if (wall != null) {
			return wall;
		}
		return spawnTransferEquipmentState(x, y, z, baseY, variant);
	}

	private BlockState spawnTransferLooseStoneState(int x, int y, int z, int baseY, int variant) {
		if (y != baseY + 1) {
			return null;
		}
		for (int site = 0; site < 7; site++) {
			long siteHash = MegastructureMath.hash(worldVariantSeed, site, variant, 3421);
			int centerX = spawnTransferDebrisCenterX(siteHash, variant);
			int centerZ = spawnTransferDebrisCenterZ(siteHash, variant);
			int dx = x - centerX;
			int dz = z - centerZ;
			if (Math.abs(dx) + Math.abs(dz) == 0) {
				return PrimitiveSurvivalContent.LOOSE_STONE_BLOCK.getDefaultState();
			}
			if (Math.abs(dx) + Math.abs(dz) == 1 && Math.floorMod(siteHash, 6) == 0) {
				return PrimitiveSurvivalContent.LOOSE_STONE_BLOCK.getDefaultState();
			}
		}
		return null;
	}

	private BlockState looseStoneScatterState(int district, int x, int y, int z, boolean air) {
		if (!air || y <= settings.floorY() + 2 || y >= settings.ceilingY() - 3) {
			return null;
		}
		if (isRailwayLineAt(x, z) || isPrimaryRailwayFootprintAt(x, z, 8)
				|| isConnectorNetworkVolumeAt(x, y, z, 6)) {
			return null;
		}
		if (!baseStructureSolidAt(district, x, y - 1, z) || baseStructureSolidAt(district, x, y, z)) {
			return null;
		}
		int cell = 64;
		int cellX = MegastructureMath.floorDiv(x, cell);
		int cellY = MegastructureMath.floorDiv(y - settings.floorY(), 18);
		int cellZ = MegastructureMath.floorDiv(z, cell);
		long cellHash = MegastructureMath.hash(worldVariantSeed, cellX, cellZ, 3457 + cellY * 17);
		if (Math.floorMod(cellHash, 11) != 0) {
			return null;
		}
		int siteX = cellX * cell + MegastructureMath.range(cellHash >>> 8, 9, cell - 10);
		int siteZ = cellZ * cell + MegastructureMath.range(cellHash >>> 20, 9, cell - 10);
		int dx = x - siteX;
		int dz = z - siteZ;
		int reach = Math.floorMod(cellHash >>> 34, 5) == 0 ? 1 : 0;
		return Math.abs(dx) + Math.abs(dz) <= reach
				? PrimitiveSurvivalContent.LOOSE_STONE_BLOCK.getDefaultState()
				: null;
	}

	private BlockState lavaReservoirState(int district, int x, int y, int z, boolean air) {
		if (!air || isRailwayLineAt(x, z) || isConnectorNetworkVolumeAt(x, y, z, 8)) {
			return null;
		}
		if (district != DISTRICT_SILENT_FOUNDRY
				&& district != DISTRICT_REACTOR_CATHEDRAL
				&& district != DISTRICT_MACHINE_NAVE
				&& district != DISTRICT_TANK_CLUSTER
				&& district != DISTRICT_BLACK_HOLE_REACTOR) {
			return null;
		}
		int baseY = switch (district) {
			case DISTRICT_SILENT_FOUNDRY -> districtBaseY(x, z, 1487, 54, 360);
			case DISTRICT_REACTOR_CATHEDRAL -> districtBaseY(x, z, 1403, 48, 360);
			case DISTRICT_MACHINE_NAVE -> districtBaseY(x, z, 1439, 70, 480);
			case DISTRICT_BLACK_HOLE_REACTOR -> districtBaseY(x, z, 1543, 54, 300);
			case DISTRICT_TANK_CLUSTER -> {
				long hash = districtHash(x, z, 941);
				yield settings.floorY() + MegastructureMath.range(hash, 54, 612);
			}
			default -> settings.floorY();
		};
		if (y != baseY + 1) {
			return null;
		}
		long seed = districtHash(x, z, 2209);
		if (district == DISTRICT_BLACK_HOLE_REACTOR) {
			return blackHoleReactorLavaReservoirState(x, z, seed);
		}
		if (Math.floorMod(seed, 5) == 0) {
			return null;
		}
		int localX = districtLocalX(x) - DISTRICT_SIZE / 2;
		int localZ = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int sites = district == DISTRICT_SILENT_FOUNDRY ? 3 : 2;
		for (int site = 0; site < sites; site++) {
			long siteHash = MegastructureMath.hash(seed, site, 0, 2213);
			int cx = district == DISTRICT_SILENT_FOUNDRY && site == 0
					? 0
					: MegastructureMath.range(siteHash, -168, 168);
			int cz = district == DISTRICT_SILENT_FOUNDRY && site == 0
					? 0
					: MegastructureMath.range(siteHash >>> 12, -116, 116);
			int radiusX = district == DISTRICT_SILENT_FOUNDRY && site == 0
					? 48
					: MegastructureMath.range(siteHash >>> 24, 14, 34);
			int radiusZ = district == DISTRICT_SILENT_FOUNDRY && site == 0
					? 34
					: MegastructureMath.range(siteHash >>> 32, 12, 30);
			int dx = localX - cx;
			int dz = localZ - cz;
			long metric = (long) dx * dx * radiusZ * radiusZ + (long) dz * dz * radiusX * radiusX;
			long inner = (long) radiusX * radiusX * radiusZ * radiusZ;
			long outer = (long) (radiusX + 4) * (radiusX + 4) * (radiusZ + 4) * (radiusZ + 4);
			if (metric <= inner) {
				return BlockPalette.LAVA;
			}
			if (metric <= outer && Math.floorMod(dx + dz + site, 4) != 0) {
				return Math.floorMod(siteHash >>> 40, 3) == 0 ? BlockPalette.RUST_PIPE : BlockPalette.CRACKED_PANEL;
			}
		}
		return null;
	}

	private BlockState blackHoleReactorLavaReservoirState(int x, int z, long seed) {
		if (Math.floorMod(seed, 4) != 0) {
			return null;
		}
		int localX = districtLocalX(x) - DISTRICT_SIZE / 2;
		int localZ = districtLocalZ(z) - DISTRICT_SIZE / 2;
		for (int site = 0; site < 4; site++) {
			double angle = site * Math.PI * 0.5 + (Math.floorMod((int) (seed >>> 10), 19) - 9) * 0.018;
			int cx = (int) Math.round(Math.cos(angle) * 276);
			int cz = (int) Math.round(Math.sin(angle) * 276);
			int dx = localX - cx;
			int dz = localZ - cz;
			int radiusX = 30 + Math.floorMod((int) (seed >>> (18 + site * 3)), 13);
			int radiusZ = 14 + Math.floorMod((int) (seed >>> (30 + site * 3)), 9);
			long metric = (long) dx * dx * radiusZ * radiusZ + (long) dz * dz * radiusX * radiusX;
			long inner = (long) radiusX * radiusX * radiusZ * radiusZ;
			long rim = (long) (radiusX + 5) * (radiusX + 5) * (radiusZ + 5) * (radiusZ + 5);
			if (metric <= inner) {
				return BlockPalette.LAVA;
			}
			if (metric <= rim && Math.floorMod(dx - dz + site, 5) != 0) {
				return Math.floorMod(seed >>> (42 + site), 3) == 0 ? BlockPalette.RUST_PIPE : BlockPalette.DARK_STONE;
			}
		}
		return null;
	}

	private boolean isSpawnTransferDebrisFloor(int x, int z, int variant) {
		if (Math.abs(z) == Math.min(22, SPAWN_PLATFORM_WIDTHS[variant] - 3)) {
			return false;
		}
		if (Math.abs(x) <= SPAWN_PLATFORM_LENGTHS[variant] - 6
				&& Math.abs(z) >= 10 && Math.abs(z) <= SPAWN_PLATFORM_WIDTHS[variant] - 5) {
			return true;
		}
		if (Math.abs(x) <= SPAWN_HALL_LENGTHS[variant] - 8
				&& Math.abs(z) >= 11 && Math.abs(z) <= SPAWN_HALL_WIDTHS[variant] - 5) {
			return true;
		}
		for (int[] room : SPAWN_STATION_ROOMS[variant]) {
			if (x >= room[0] + 4 && x <= room[1] - 4 && z >= room[2] + 4 && z <= room[3] - 4) {
				return true;
			}
		}
		return false;
	}

	private int spawnTransferDebrisCenterX(long hash, int variant) {
		int stationSpan = SPAWN_PLATFORM_LENGTHS[variant] - 16;
		int band = Math.floorMod(hash, 8);
		if (band >= 6) {
			int[][] rooms = SPAWN_STATION_ROOMS[variant];
			int[] room = rooms[Math.floorMod((int) (hash >>> 8), rooms.length)];
			return MegastructureMath.range(hash >>> 16, room[0] + 7, room[1] - 7);
		}
		return MegastructureMath.range(hash >>> 16, -stationSpan, stationSpan);
	}

	private int spawnTransferDebrisCenterZ(long hash, int variant) {
		int band = Math.floorMod(hash, 8);
		if (band >= 6) {
			int[][] rooms = SPAWN_STATION_ROOMS[variant];
			int[] room = rooms[Math.floorMod((int) (hash >>> 8), rooms.length)];
			return MegastructureMath.range(hash >>> 28, room[2] + 7, room[3] - 7);
		}
		int side = Math.floorMod(hash >>> 40, 2) == 0 ? -1 : 1;
		int min = 12;
		int max = Math.max(min, SPAWN_PLATFORM_WIDTHS[variant] - 7);
		return side * MegastructureMath.range(hash >>> 28, min, max);
	}

	private BlockState spawnTransferEpicenterState(int x, int y, int z, int baseY, int variant) {
		for (int site = 0; site < 3; site++) {
			long hash = MegastructureMath.hash(worldVariantSeed, variant, site, 3301);
			int centerX = spawnTransferDebrisCenterX(hash, variant);
			int centerZ = spawnTransferDebrisCenterZ(hash, variant);
			int dx = x - centerX;
			int dz = z - centerZ;
			int distance2 = dx * dx + dz * dz;
			int radius = 6 + Math.floorMod((int) (hash >>> 12), 4);
			if (distance2 > radius * radius) {
				continue;
			}
			if (y == baseY + 2) {
				if (distance2 <= 2) {
					return Math.floorMod(hash, 2) == 0 ? BlockPalette.QUARTZ_BLOCK : BlockPalette.OAK_PLANKS;
				}
				if (distance2 <= 6 && Math.floorMod(MegastructureMath.hash(hash, dx, dz, 3307), 4) == 0) {
					return BlockPalette.QUARTZ_SLAB;
				}
				continue;
			}
			if (distance2 <= 4) {
				return Math.floorMod(hash + dx - dz, 4) == 0 ? BlockPalette.LIGHT_SLAB : BlockPalette.CRACKED_PANEL;
			}
			if (distance2 <= radius * radius && Math.floorMod(MegastructureMath.hash(hash, dx, dz, 3313), 3) == 0) {
				return Math.floorMod(hash + dx, 5) == 0 ? BlockPalette.OAK_SLAB : BlockPalette.QUARTZ_SLAB;
			}
		}
		return null;
	}

	private BlockState spawnTransferWoodState(int x, int y, int z, int baseY, int variant) {
		for (int site = 0; site < 34; site++) {
			long siteHash = MegastructureMath.hash(worldVariantSeed, site, variant, 3329);
			int centerX = spawnTransferDebrisCenterX(siteHash, variant);
			int centerZ = spawnTransferDebrisCenterZ(siteHash, variant);
			int dx = x - centerX;
			int dz = z - centerZ;
			int length = MegastructureMath.range(siteHash >>> 12, 1, 4);
			int width = MegastructureMath.range(siteHash >>> 19, 0, 1);
			boolean xAxis = Math.floorMod(siteHash >>> 24, 2) == 0;
			int along = xAxis ? dx : dz;
			int cross = xAxis ? dz : dx;
			int jitter = Math.floorMod(MegastructureMath.hash(siteHash, x, z, y), 7);
			if (y == baseY + 2) {
				if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && Math.floorMod(siteHash, 7) == 0) {
					return BlockPalette.OAK_PLANKS;
				}
				continue;
			}
			boolean plank = Math.abs(along) <= length && Math.abs(cross) <= width
					&& jitter != 0 && !(Math.abs(along) == length && Math.abs(cross) == width);
			if (plank) {
				return Math.floorMod(siteHash, 5) == 0 ? BlockPalette.OAK_PLANKS : BlockPalette.OAK_SLAB;
			}
		}
		return null;
	}

	private BlockState spawnTransferWallShardState(int x, int y, int z, int baseY, int variant) {
		for (int site = 0; site < 18; site++) {
			long siteHash = MegastructureMath.hash(worldVariantSeed, site, variant, 3361);
			int centerX = spawnTransferDebrisCenterX(siteHash, variant);
			int centerZ = spawnTransferDebrisCenterZ(siteHash, variant);
			int dx = x - centerX;
			int dz = z - centerZ;
			int length = MegastructureMath.range(siteHash >>> 10, 1, 4);
			int width = MegastructureMath.range(siteHash >>> 16, 0, 2);
			boolean xAxis = Math.floorMod(siteHash >>> 21, 2) == 0;
			int along = xAxis ? dx : dz;
			int cross = xAxis ? dz : dx;
			if (y == baseY + 2) {
				if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && Math.floorMod(siteHash, 6) == 0) {
					return Math.floorMod(siteHash, 2) == 0 ? BlockPalette.QUARTZ_BLOCK : BlockPalette.QUARTZ_PILLAR;
				}
				continue;
			}
			boolean shard = Math.abs(along) <= length && Math.abs(cross) <= width
					&& Math.floorMod(MegastructureMath.hash(siteHash, dx, dz, 3373), 5) <= 2;
			if (shard) {
				int material = Math.floorMod((int) (siteHash >>> 28), 5);
				return switch (material) {
					case 0 -> BlockPalette.QUARTZ_BLOCK;
					case 1, 2 -> BlockPalette.QUARTZ_SLAB;
					default -> BlockPalette.CRACKED_PANEL;
				};
			}
		}
		return null;
	}

	private BlockState spawnTransferEquipmentState(int x, int y, int z, int baseY, int variant) {
		for (int site = 0; site < 14; site++) {
			long siteHash = MegastructureMath.hash(worldVariantSeed, site, variant, 3391);
			int centerX = spawnTransferDebrisCenterX(siteHash, variant);
			int centerZ = spawnTransferDebrisCenterZ(siteHash, variant);
			int dx = x - centerX;
			int dz = z - centerZ;
			int distance2 = dx * dx + dz * dz;
			int radius = 2 + Math.floorMod((int) (siteHash >>> 14), 3);
			if (distance2 > radius * radius) {
				continue;
			}
			if (y == baseY + 2) {
				if (distance2 == 0 && Math.floorMod(siteHash, 4) == 0) {
					return Math.floorMod(siteHash, 2) == 0 ? BlockPalette.GRATE : BlockPalette.QUARTZ_PILLAR;
				}
				continue;
			}
			if (distance2 <= 1) {
				return switch (Math.floorMod((int) (siteHash >>> 20), 4)) {
					case 0 -> BlockPalette.GRATE;
					case 1 -> BlockPalette.RUST_PIPE;
					case 2 -> BlockPalette.LIGHT_SLAB;
					default -> BlockPalette.LAMP;
				};
			}
			if (Math.floorMod(MegastructureMath.hash(siteHash, dx, dz, 3407), 3) == 0) {
				return switch (Math.floorMod((int) (siteHash >>> 27), 4)) {
					case 0 -> BlockPalette.QUARTZ_SLAB;
					case 1 -> BlockPalette.LIGHT_SLAB;
					case 2 -> BlockPalette.OAK_SLAB;
					default -> BlockPalette.CRACKED_PANEL;
				};
			}
		}
		return null;
	}

	private BlockState spawnStationRoomState(int x, int y, int z) {
		int baseY = settings.spawnPlatformY();
		int variant = spawnStationVariant();
		for (int roomIndex = 0; roomIndex < SPAWN_STATION_ROOMS[variant].length; roomIndex++) {
			int[] room = SPAWN_STATION_ROOMS[variant][roomIndex];
			int minX = room[0];
			int maxX = room[1];
			int minZ = room[2];
			int maxZ = room[3];
			if (x < minX || x > maxX || z < minZ || z > maxZ) {
				continue;
			}
			int top = baseY + room[4];
			int program = room[5];
			boolean positiveSide = minZ > 0;
			boolean stairOpening = positiveSide && Math.abs(x + 72) <= 8 && Math.abs(z - 28) <= 8;
			if (stairOpening && y > baseY) {
				return null;
			}
			int doorCenter = (minX + maxX) / 2;
			boolean doorway = (positiveSide ? z == minZ : z == maxZ)
					&& Math.abs(x - doorCenter) <= 4 && y >= baseY + 1 && y <= baseY + 6;
			if (y == baseY) {
				return Math.floorMod(x + z + variant * 7, 17) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.WALKWAY;
			}
			if (y == top) {
				return Math.floorMod(x + roomIndex * 5, 12) <= 1 ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
			}
			boolean wall = x == minX || x == maxX || z == minZ || z == maxZ;
			if (wall && y > baseY && y < top && !doorway) {
				return Math.floorMod(y - baseY, 4) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
			}
			int level = y - baseY;
			int centerZ = (minZ + maxZ) / 2;
			if (program == 0 && level == 1 && Math.abs(z - centerZ) >= 5
					&& Math.floorMod(x - minX, 12) <= 6) {
				return BlockPalette.stairs(positiveSide ? Direction.NORTH : Direction.SOUTH);
			}
			if (program == 1 && level >= 1 && level <= Math.min(7, room[4] - 1)
					&& (Math.abs(x - minX - 8) <= 1 || Math.abs(x - maxX + 8) <= 1)
					&& Math.abs(z - centerZ) <= 1) {
				return level == Math.min(7, room[4] - 1) ? BlockPalette.RUST_PIPE : BlockPalette.FOUNDATION;
			}
			if (program == 2 && level == 1 && Math.abs(z - centerZ) <= 2
					&& Math.abs(x - doorCenter) >= 7) {
				return Math.floorMod(x, 5) == 0 ? BlockPalette.LAMP : BlockPalette.GRATE;
			}
			if (program == 3 && level <= 4 && x == doorCenter
					&& Math.abs(z - centerZ) >= 4) {
				return BlockPalette.WALL_PANEL;
			}
			return null;
		}
		return null;
	}

	private int spawnStationVariant() {
		return Math.floorMod((int) MegastructureMath.hash(worldVariantSeed, 0, 0, 1901), SPAWN_STATION_ROOMS.length);
	}

	private BlockState spawnStationArchitectureState(int x, int y, int z, int baseY, int variant) {
		int level = y - baseY;
		return switch (variant) {
			case 0 -> {
				boolean support = Math.abs(x) <= 124 && Math.floorMod(x, 16) <= 1
						&& Math.abs(Math.abs(z) - 27) <= 1 && level >= 1 && level <= 11;
				if (support) yield level % 4 == 0 ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
				yield level == 12 && Math.abs(x) <= 124 && Math.abs(z) <= 28 && Math.floorMod(x, 16) <= 1
						? BlockPalette.FOUNDATION : null;
			}
			case 1 -> {
				boolean rib = Math.floorMod(x + 120, 24) <= 1 && Math.abs(z) <= 34
						&& ((level >= 1 && level <= 14 && Math.abs(z) >= 31) || level == 14);
				yield rib ? (level == 14 && Math.abs(z) <= 1 ? BlockPalette.LAMP : BlockPalette.FOUNDATION) : null;
			}
			case 2 -> {
				boolean craneRail = level == 13 && Math.abs(x) <= 108 && Math.abs(Math.abs(z) - 19) <= 1;
				boolean cranePost = Math.floorMod(x + 104, 32) <= 1 && Math.abs(Math.abs(z) - 25) <= 1
						&& level >= 1 && level <= 13;
				yield craneRail ? BlockPalette.RUST_PIPE : cranePost ? BlockPalette.FOUNDATION : null;
			}
			case 3 -> {
				boolean mezzanine = level == 7 && Math.abs(x) <= 104 && Math.abs(z) >= 18 && Math.abs(z) <= 36;
				boolean support = Math.floorMod(x + 96, 32) <= 1 && Math.abs(Math.abs(z) - 34) <= 1
						&& level >= 1 && level <= 15;
				yield mezzanine ? BlockPalette.WALKWAY : support ? BlockPalette.FOUNDATION : null;
			}
			case 4 -> {
				boolean portal = (Math.abs(x + 72) <= 1 || Math.abs(x + 24) <= 1
						|| Math.abs(x - 24) <= 1 || Math.abs(x - 72) <= 1)
						&& Math.abs(z) <= 27 && level >= 1 && level <= 11
						&& (Math.abs(z) >= 23 || level >= 9);
				yield portal ? (level == 10 && Math.abs(z) <= 1 ? BlockPalette.LAMP : BlockPalette.DARK_STONE) : null;
			}
			default -> {
				boolean brokenRoof = level == 13 && Math.abs(x) <= 120 && Math.abs(z) <= 30
						&& Math.floorMod(x + 120, 20) <= 8 && Math.floorMod(x + z, 7) != 0;
				boolean sparsePost = Math.floorMod(x + 108, 36) <= 1 && Math.abs(Math.abs(z) - 29) <= 1
						&& level >= 1 && level <= 12 && Math.floorMod(x, 5) != 0;
				yield brokenRoof ? BlockPalette.CRACKED_PANEL : sparsePost ? BlockPalette.FOUNDATION : null;
			}
		};
	}

	private BlockState spawnStationAccessStairState(int x, int y, int z) {
		int bottom = settings.spawnPlatformY() + 1;
		int top = connectorNetworkY();
		int dx = x + 72;
		int dz = z - 28;
		if (y < bottom || y > top || Math.abs(dx) > 8 || Math.abs(dz) > 8) {
			return null;
		}
		int rise = y - bottom;
		int flight = Math.floorDiv(rise, 12);
		int step = Math.floorMod(rise, 12);
		boolean eastbound = Math.floorMod(flight, 2) == 0;
		int stairX = eastbound ? -6 + step : 6 - step;
		int stairZ = eastbound ? -6 : 6;
		if (Math.abs(dx - stairX) <= 1 && Math.abs(dz - stairZ) <= 1) {
			return BlockPalette.stairs(eastbound ? Direction.EAST : Direction.WEST);
		}
		int landingX = eastbound ? -6 : 6;
		if (step == 0 && Math.abs(dx - landingX) <= 1 && dz >= -6 && dz <= 6) {
			return BlockPalette.WALKWAY;
		}
		return null;
	}

	private boolean isSpawnNetworkLinkAt(int x, int z, int width) {
		boolean firstLeg = x >= -72 && x <= DISTRICT_SIZE / 2 && Math.abs(z - 28) <= width;
		boolean secondLeg = Math.abs(x - DISTRICT_SIZE / 2) <= width && z >= 28 && z <= DISTRICT_SIZE / 2;
		return firstLeg || secondLeg;
	}

	private BlockState riftBridgeAccessState(int x, int y, int z) {
		int cell = settings.motifCellSize();
		int stripe = MegastructureMath.floorDiv(x, cell);
		long riftHash = riftStripeHash(stripe);
		if (!isAcceptedRiftStripe(stripe, riftHash)) {
			return null;
		}

		int localX = MegastructureMath.floorMod(x, cell);
		int width = MegastructureMath.range(
				riftHash >>> 12,
				Math.min(settings.riftMinWidth(), settings.riftMaxWidth()),
				Math.max(settings.riftMinWidth(), settings.riftMaxWidth())
		);
		int edgeDistance = Math.abs(localX - cell / 2) - width / 2;
		if (edgeDistance < 0 || edgeDistance > 92) {
			return null;
		}

		int bridgeBand = MegastructureMath.floorDiv(z, 160);
		long bridgeHash = MegastructureMath.hash(activeWorldVariantSeed, stripe, bridgeBand, 211);
		if (Math.floorMod(bridgeHash, 4) == 0) {
			return null;
		}
		int bridgeZ = bridgeBand * 160 + MegastructureMath.range(bridgeHash >>> 8, 32, 128);
		int bridgeY = settings.floorY() + MegastructureMath.range(bridgeHash >>> 16, 112, Math.max(176, getWorldHeight() - 160));
		int dz = z - bridgeZ;
		boolean accessTunnel = edgeDistance <= 72 && Math.abs(dz) <= 8;
		boolean junction = edgeDistance >= 58 && edgeDistance <= 92 && Math.abs(dz) <= 10;
		if (!accessTunnel && !junction) {
			return null;
		}
		return bridgeConnectorCorridorState(x, z, true, x, dz, y, bridgeY, bridgeHash, junction && !accessTunnel);
	}

	private BlockState riftBridgeRoomLinkState(int x, int y, int z) {
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		int districtMinX = districtX * DISTRICT_SIZE;
		int districtMinZ = districtZ * DISTRICT_SIZE;
		int centerX = districtMinX + DISTRICT_SIZE / 2;
		int centerZ = districtMinZ + DISTRICT_SIZE / 2;
		if (Math.abs(x - centerX) <= 10 && Math.abs(z - centerZ) <= 10) {
			return null;
		}

		RiftBridgeRoomLink link = riftBridgeRoomLinkForDistrict(districtX, districtZ);
		if (link == null) {
			return null;
		}
		boolean legZ = Math.abs(x - link.entryX()) <= 10 && between(z, link.bridgeZ(), centerZ);
		boolean legX = Math.abs(z - centerZ) <= 10 && between(x, link.entryX(), centerX);
		if (!legZ && !legX) {
			return null;
		}
		boolean corner = Math.abs(x - link.entryX()) <= 6 && Math.abs(z - centerZ) <= 6;
		if (legZ) {
			BlockState state = bridgeConnectorCorridorState(
					x,
					z,
					false,
					z,
					x - link.entryX(),
					y,
					link.bridgeY(),
					link.seed(),
					corner
			);
			if (state != null) {
				return state;
			}
		}
		if (legX) {
			BlockState state = bridgeConnectorCorridorState(
					x,
					z,
					true,
					x,
					z - centerZ,
					y,
					link.bridgeY(),
					link.seed() ^ 0x5F5E100L,
					corner
			);
			if (state != null) {
				return state;
			}
		}
		return null;
	}

	private RiftBridgeRoomLink riftBridgeRoomLinkForDistrict(int districtX, int districtZ) {
		int districtMinX = districtX * DISTRICT_SIZE;
		int districtMinZ = districtZ * DISTRICT_SIZE;
		int cell = settings.motifCellSize();
		int firstStripe = MegastructureMath.floorDiv(districtMinX - 128, cell);
		int lastStripe = MegastructureMath.floorDiv(districtMinX + DISTRICT_SIZE + 128, cell);
		int firstBand = MegastructureMath.floorDiv(districtMinZ, 160);
		int lastBand = MegastructureMath.floorDiv(districtMinZ + DISTRICT_SIZE - 1, 160);
		RiftBridgeRoomLink selected = null;
		long selectedPriority = 0L;
		for (int stripe = firstStripe; stripe <= lastStripe; stripe++) {
			long riftHash = riftStripeHash(stripe);
			if (!isAcceptedRiftStripe(stripe, riftHash)) {
				continue;
			}
			int width = MegastructureMath.range(
					riftHash >>> 12,
					Math.min(settings.riftMinWidth(), settings.riftMaxWidth()),
					Math.max(settings.riftMinWidth(), settings.riftMaxWidth())
			);
			int riftCenterX = stripe * cell + cell / 2;
			for (int side = -1; side <= 1; side += 2) {
				int entryX = riftCenterX + side * (width / 2 + 72);
					if (MegastructureMath.floorDiv(entryX, DISTRICT_SIZE) != districtX) {
						continue;
					}
					for (int band = firstBand; band <= lastBand; band++) {
						long bridgeHash = MegastructureMath.hash(activeWorldVariantSeed, stripe, band, 211);
					if (Math.floorMod(bridgeHash, 4) == 0) {
						continue;
					}
					int bridgeZ = band * 160 + MegastructureMath.range(bridgeHash >>> 8, 32, 128);
					if (MegastructureMath.floorDiv(bridgeZ, DISTRICT_SIZE) != districtZ) {
						continue;
					}
					int bridgeY = settings.floorY() + MegastructureMath.range(
							bridgeHash >>> 16, 112, Math.max(176, getWorldHeight() - 160)
					);
					long priority = MegastructureMath.hash(activeWorldVariantSeed, entryX, bridgeZ, 2237);
					if (selected == null || Long.compareUnsigned(priority, selectedPriority) < 0) {
						selected = new RiftBridgeRoomLink(entryX, bridgeZ, bridgeY, bridgeHash);
						selectedPriority = priority;
					}
				}
			}
		}
		return selected;
	}

	private BlockState connectorNetworkState(int x, int y, int z) {
		boolean spawnOuter = isSpawnNetworkLinkAt(x, z, 5);
		if (spawnOuter) {
			BlockState spawnLink = connectorLayerState(x, y, z, connectorNetworkY());
			if (spawnLink != null) {
				return spawnLink;
			}
		}
		boolean horizontalOuter = isHorizontalDistrictConnectorAt(x, z, 5);
		if (horizontalOuter) {
			BlockState horizontal = connectorLayerState(x, y, z, horizontalConnectorYAt(x, z));
			if (horizontal != null) {
				return horizontal;
			}
		}
		if (isVerticalDistrictConnectorAt(x, z, 5)) {
			return connectorLayerState(x, y, z, verticalConnectorYAt(x, z));
		}
		return null;
	}

	private BlockState connectorLayerState(int x, int y, int z, int level) {
		int relY = y - level;
		boolean outer = isSpawnNetworkLinkAt(x, z, 5)
				|| isHorizontalDistrictConnectorAt(x, z, 5)
				|| isVerticalDistrictConnectorAt(x, z, 5);
		if (!outer) {
			return null;
		}
		boolean mid = isSpawnNetworkLinkAt(x, z, 3)
				|| isHorizontalDistrictConnectorAt(x, z, 3)
				|| isVerticalDistrictConnectorAt(x, z, 3);
		boolean core = isSpawnNetworkLinkAt(x, z, 1)
				|| isHorizontalDistrictConnectorAt(x, z, 1)
				|| isVerticalDistrictConnectorAt(x, z, 1);
		if (relY < 0 || relY > 7) {
			return (mid || core) && isConnectorTransitionAirAt(x, y, z, level) ? BlockPalette.AIR : null;
		}
		boolean horizontal = isHorizontalDistrictConnectorAt(x, z, 5);
		boolean vertical = isVerticalDistrictConnectorAt(x, z, 5);
		boolean junction = (horizontal && vertical) || (isSpawnNetworkLinkAt(x, z, 5) && (horizontal || vertical));
		int along = horizontal && !vertical ? x : vertical && !horizontal ? z : x + z;
		boolean rib = Math.floorMod(along, 12) == 0;
		boolean lamp = Math.floorMod(along, 18) == 0;
		boolean bulkhead = Math.floorMod(along, 36) == 0;
		boolean sideShell = outer && !mid;
		boolean window = sideShell
				&& (relY == 3 || relY == 4)
				&& !rib
				&& !bulkhead
				&& connectorFacesOpenVoid(x, y, z)
				&& Math.floorMod(along, 14) >= 4
				&& Math.floorMod(along, 14) <= 9;

		if (relY == 0) {
			if (core) {
				return BlockPalette.GRATE;
			}
			if (mid) {
				return BlockPalette.WALKWAY;
			}
			return BlockPalette.FOUNDATION;
		}
		if (relY == 1 && sideShell) {
			return rib || bulkhead ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
		}
		if (relY >= 2 && relY <= 5 && sideShell) {
			if (window) {
				return BlockPalette.AIR;
			}
			return rib || bulkhead ? BlockPalette.FOUNDATION : relY == 3 && Math.floorMod(along, 18) > 5 ? BlockPalette.DARK_STONE : BlockPalette.WALL_PANEL;
		}
		if (relY == 6) {
			if (core && lamp) {
				return BlockPalette.LAMP;
			}
			if (mid) {
				return rib || bulkhead || junction ? BlockPalette.FOUNDATION : BlockPalette.LIGHT_STONE;
			}
			if (outer) {
				return BlockPalette.FOUNDATION;
			}
		}
		if (relY == 7 && mid) {
			return BlockPalette.FOUNDATION;
		}
		return BlockPalette.AIR;
	}

	private boolean connectorFacesOpenVoid(int x, int y, int z) {
		int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] direction : directions) {
			for (int step = 1; step <= 4; step++) {
				int sampleX = x + direction[0] * step;
				int sampleZ = z + direction[1] * step;
				if (isConnectorOuterAt(sampleX, sampleZ, 5)) {
					continue;
				}
				if (isOpenVoidForConnectorWindow(sampleX, y, sampleZ)) {
					return true;
				}
				break;
			}
		}
		return false;
	}

	private boolean isConnectorOuterAt(int x, int z, int width) {
		return isSpawnNetworkLinkAt(x, z, width)
				|| isHorizontalDistrictConnectorAt(x, z, width)
				|| isVerticalDistrictConnectorAt(x, z, width);
	}

	private boolean isOpenVoidForConnectorWindow(int x, int y, int z) {
		if (y < settings.floorY() || y > settings.ceilingY()) {
			return true;
		}
		if (isPrimaryRift(x, z) || isBlackHoleCoreExclusion(x, y, z)) {
			return true;
		}
		int district = districtType(x, z);
		return isRailwayAir(x, y, z) || districtAir(district, x, y, z);
	}

	private boolean isConnectorTransitionAirAt(int x, int y, int z, int fallbackLevel) {
		int minLevel = fallbackLevel;
		int maxLevel = fallbackLevel;
		boolean found = false;
		if (isSpawnNetworkLinkAt(x, z, 5)) {
			found = true;
			minLevel = Math.min(minLevel, connectorNetworkY());
			maxLevel = Math.max(maxLevel, connectorNetworkY());
		}
		for (int offset = -6; offset <= 6; offset += 3) {
			if (isHorizontalDistrictConnectorAt(x + offset, z, 5)) {
				int level = horizontalConnectorYAt(x + offset, z);
				found = true;
				minLevel = Math.min(minLevel, level);
				maxLevel = Math.max(maxLevel, level);
			}
			if (isVerticalDistrictConnectorAt(x, z + offset, 5)) {
				int level = verticalConnectorYAt(x, z + offset);
				found = true;
				minLevel = Math.min(minLevel, level);
				maxLevel = Math.max(maxLevel, level);
			}
		}
		return found && maxLevel > minLevel && y >= minLevel + 1 && y <= maxLevel + 7;
	}

	private BlockState bridgeConnectorCorridorState(
			int x,
			int z,
			boolean xAxis,
			int along,
			int cross,
			int y,
			int baseY,
			long styleSeed,
			boolean junction
	) {
		int relY = y - baseY;
		int absCross = Math.abs(cross);
		int outerWall = junction ? 12 : 10;
		int shellStart = outerWall - 2;
		if (relY < 0 || relY > 8 || absCross > outerWall) {
			return null;
		}

		boolean rib = Math.floorMod(along + (int) (styleSeed >>> 8), 18) <= 1;
		boolean bulkhead = Math.floorMod(along + (int) (styleSeed >>> 28), 72) <= 2;
		boolean lampBay = Math.floorMod(along + (int) (styleSeed >>> 20) + 9, 36) <= 1;
		boolean servicePipe = !rib && !bulkhead
				&& Math.floorMod(along + (int) styleSeed, 20) >= 5
				&& Math.floorMod(along + (int) styleSeed, 20) <= 13;
		boolean window = !junction
				&& (relY == 3 || relY == 4)
				&& absCross >= shellStart
				&& !rib
				&& !bulkhead
				&& bridgeConnectorWallFacesOpenVoid(x, y, z, xAxis, cross)
				&& Math.floorMod(along, 14) >= 4
				&& Math.floorMod(along, 14) <= 9;

		if (relY == 0) {
			if (absCross <= 1) {
				return BlockPalette.GRATE;
			}
			if (absCross <= 7) {
				return Math.floorMod(along + cross, 11) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.WALKWAY;
			}
			return absCross <= 9 ? BlockPalette.FOUNDATION : BlockPalette.DARK_STONE;
		}
		if (relY == 1) {
			if (absCross == 6) {
				return BlockPalette.FOUNDATION;
			}
			if (absCross >= shellStart && servicePipe) {
				return BlockPalette.PIPE;
			}
		}
		if (relY >= 1 && relY <= 6 && absCross >= shellStart) {
			if (window) {
				return BlockPalette.AIR;
			}
			return rib || bulkhead ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
		}
		if (relY == 2 && absCross == shellStart - 1) {
			return rib ? BlockPalette.FOUNDATION : BlockPalette.DARK_STONE;
		}
		if (relY >= 3 && relY <= 4 && absCross == shellStart - 1 && servicePipe) {
			return Math.floorMod(along + relY, 6) <= 1 ? BlockPalette.PIPE : BlockPalette.DARK_STONE;
		}
		if (relY == 6 && absCross <= shellStart && (rib || bulkhead)) {
			return BlockPalette.FOUNDATION;
		}
		if (relY == 7) {
			if (absCross == 0 && lampBay) {
				return BlockPalette.LAMP;
			}
			if (absCross <= shellStart) {
				return rib || bulkhead || junction ? BlockPalette.FOUNDATION : BlockPalette.LIGHT_STONE;
			}
			return BlockPalette.FOUNDATION;
		}
		if (relY == 8 && absCross <= shellStart + 1 && (rib || bulkhead || junction)) {
			return BlockPalette.FOUNDATION;
		}
		return BlockPalette.AIR;
	}

	private boolean bridgeConnectorWallFacesOpenVoid(int x, int y, int z, boolean xAxis, int cross) {
		int side = cross < 0 ? -1 : 1;
		int stepX = xAxis ? 0 : side;
		int stepZ = xAxis ? side : 0;
		for (int step = 1; step <= 6; step++) {
			int sampleX = x + stepX * step;
			int sampleZ = z + stepZ * step;
			if (isOpenVoidForConnectorWindow(sampleX, y, sampleZ)) {
				return true;
			}
		}
		return false;
	}

	private int connectorNetworkY() {
		return settings.floorY() + Math.min(540, getWorldHeight() - 96);
	}

	private boolean isDistrictConnectorAt(int x, int z, int width) {
		return isHorizontalDistrictConnectorAt(x, z, width) || isVerticalDistrictConnectorAt(x, z, width);
	}

	private boolean isHorizontalDistrictConnectorAt(int x, int z, int width) {
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 1601);
		int westZ = connectorEdgeZ(districtX, districtZ);
		int eastZ = connectorEdgeZ(districtX + 1, districtZ);
		int bendWest = MegastructureMath.range(hash, 224, 320);
		int bendEast = MegastructureMath.range(hash >>> 12, 704, 800);
		boolean horizontal = (localX <= bendWest && Math.abs(localZ - westZ) <= width)
				|| (Math.abs(localX - bendWest) <= width && between(localZ, westZ, DISTRICT_SIZE / 2))
				|| (localX >= bendWest && localX <= bendEast && Math.abs(localZ - DISTRICT_SIZE / 2) <= width)
				|| (Math.abs(localX - bendEast) <= width && between(localZ, DISTRICT_SIZE / 2, eastZ))
				|| (localX >= bendEast && Math.abs(localZ - eastZ) <= width);

		return horizontal;
	}

	private boolean isVerticalDistrictConnectorAt(int x, int z, int width) {
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		if (Math.floorMod(districtX, 4) != 0) {
			return false;
		}
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 1601);
		int northX = connectorEdgeX(districtX, districtZ);
		int southX = connectorEdgeX(districtX, districtZ + 1);
		int bendNorth = MegastructureMath.range(hash >>> 24, 224, 320);
		int bendSouth = MegastructureMath.range(hash >>> 36, 704, 800);
		boolean vertical = (localZ <= bendNorth && Math.abs(localX - northX) <= width)
				|| (Math.abs(localZ - bendNorth) <= width && between(localX, northX, DISTRICT_SIZE / 2))
				|| (localZ >= bendNorth && localZ <= bendSouth && Math.abs(localX - DISTRICT_SIZE / 2) <= width)
				|| (Math.abs(localZ - bendSouth) <= width && between(localX, DISTRICT_SIZE / 2, southX))
				|| (localZ >= bendSouth && Math.abs(localX - southX) <= width);
		return vertical;
	}

	private int horizontalConnectorYAt(int x, int z) {
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int westY = horizontalConnectorEdgeY(districtX, districtZ);
		int centerY = connectorCenterY(districtX, districtZ);
		int eastY = horizontalConnectorEdgeY(districtX + 1, districtZ);
		return localX <= DISTRICT_SIZE / 2
				? interpolateConnectorY(westY, centerY, localX, DISTRICT_SIZE / 2)
				: interpolateConnectorY(centerY, eastY, localX - DISTRICT_SIZE / 2, DISTRICT_SIZE / 2);
	}

	private int verticalConnectorYAt(int x, int z) {
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int northY = verticalConnectorEdgeY(districtX, districtZ);
		int centerY = connectorCenterY(districtX, districtZ);
		int southY = verticalConnectorEdgeY(districtX, districtZ + 1);
		return localZ <= DISTRICT_SIZE / 2
				? interpolateConnectorY(northY, centerY, localZ, DISTRICT_SIZE / 2)
				: interpolateConnectorY(centerY, southY, localZ - DISTRICT_SIZE / 2, DISTRICT_SIZE / 2);
	}

	private int interpolateConnectorY(int start, int end, int progress, int length) {
		return start + Math.floorDiv((end - start) * progress + length / 2, length);
	}

	private int connectorCenterY(int districtX, int districtZ) {
		if (districtX == 0 && districtZ == 0) {
			return connectorNetworkY();
		}
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 1619);
		return connectorNetworkY() + MegastructureMath.range(hash, -72, 72);
	}

	private int horizontalConnectorEdgeY(int edgeX, int districtZ) {
		long hash = MegastructureMath.hash(SHAPE_SEED, edgeX, districtZ, 1621);
		return connectorNetworkY() + MegastructureMath.range(hash, -72, 72);
	}

	private int verticalConnectorEdgeY(int districtX, int edgeZ) {
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, edgeZ, 1627);
		return connectorNetworkY() + MegastructureMath.range(hash, -72, 72);
	}

	private boolean isConnectorNetworkVolumeAt(int x, int y, int z, int width) {
		if (isSpawnNetworkLinkAt(x, z, width) && y >= connectorNetworkY() - 1 && y <= connectorNetworkY() + 11) {
			return true;
		}
		if (isHorizontalDistrictConnectorAt(x, z, width)) {
			int level = horizontalConnectorYAt(x, z);
			if (y >= level - 1 && y <= level + 11) {
				return true;
			}
		}
		if (isVerticalDistrictConnectorAt(x, z, width)) {
			int level = verticalConnectorYAt(x, z);
			return y >= level - 1 && y <= level + 11;
		}
		return false;
	}

	private int connectorEdgeZ(int edgeX, int districtZ) {
		long hash = MegastructureMath.hash(SHAPE_SEED, edgeX, districtZ, 1607);
		return MegastructureMath.range(hash, 128, DISTRICT_SIZE - 128);
	}

	private int connectorEdgeX(int districtX, int edgeZ) {
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, edgeZ, 1613);
		return MegastructureMath.range(hash, 128, DISTRICT_SIZE - 128);
	}

	private BlockState districtAccessState(int x, int y, int z) {
		int localX = districtLocalX(x);
		int localZ = districtLocalZ(z);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		if (Math.abs(dx) > 9 || Math.abs(dz) > 9) {
			return null;
		}
		int bottom = settings.floorY() + 12;
		int top = settings.ceilingY() - 12;
		if (y < bottom || y > top) {
			return null;
		}
		if (y == bottom) {
			return BlockPalette.PLATFORM;
		}
		int rise = y - bottom;
		int flight = Math.floorDiv(rise, 12);
		int step = Math.floorMod(rise, 12);
		boolean eastbound = Math.floorMod(flight, 2) == 0;
		int stairX = eastbound ? -6 + step : 6 - step;
		int stairZ = eastbound ? -6 : 6;
		if (Math.abs(dx - stairX) <= 1 && Math.abs(dz - stairZ) <= 1) {
			return BlockPalette.stairs(eastbound ? Direction.EAST : Direction.WEST);
		}
		int landingX = eastbound ? -6 : 6;
		if (step == 0 && Math.abs(dx - landingX) <= 1 && dz >= -6 && dz <= 6) {
			return BlockPalette.WALKWAY;
		}
		return BlockPalette.AIR;
	}

	static boolean isRailwayLineAt(int x, int z) {
		return isPrimaryRailwayFootprintAt(x, z, 10) || isRailwayXLineAt(z) || isRailwayZLineAt(x);
	}

	private static boolean isPrimaryRailwayFootprintAt(int x, int z, int radius) {
		return Math.abs(z) <= radius;
	}

	private static boolean isRailwayXLineAt(int z) {
		int lane = MegastructureMath.floorDiv(z, 768);
		long hash = MegastructureMath.hash(activeWorldVariantSeed, lane, 0, 1201);
		if (Math.floorMod(hash, 5) > 1) {
			return false;
		}
		int centerZ = lane * 768 + MegastructureMath.range(hash >>> 8, 160, 608);
		return Math.abs(z - centerZ) <= 10;
	}

	private static boolean isRailwayZLineAt(int x) {
		int lane = MegastructureMath.floorDiv(x, 768);
		long hash = MegastructureMath.hash(activeWorldVariantSeed, lane, 0, 1207);
		if (Math.floorMod(hash, 6) != 0) {
			return false;
		}
		int centerX = lane * 768 + MegastructureMath.range(hash >>> 8, 160, 608);
		return Math.abs(x - centerX) <= 10;
	}

	private BlockState railwayState(int x, int y, int z) {
		BlockState pilgrimOasis = primaryRailOasisState(x, y, z);
		if (pilgrimOasis != null) {
			return pilgrimOasis;
		}
		BlockState railwayRuin = primaryRailwayRuinState(x, y, z);
		if (railwayRuin != null) {
			return railwayRuin;
		}
		BlockState nexusRailway = transitNexusRailwayState(x, y, z);
		if (nexusRailway != null) {
			return nexusRailway;
		}
		BlockState primaryRailway = primaryRailwayState(x, y, z);
		if (primaryRailway != null) {
			return primaryRailway;
		}
		BlockState railwayTurn = railwayTurnState(x, y, z);
		if (railwayTurn != null) {
			return railwayTurn;
		}
		BlockState xRailway = railwayXState(x, y, z);
		if (xRailway != null) {
			return xRailway;
		}
		return railwayZState(x, y, z);
	}

	private boolean isRailwayAir(int x, int y, int z) {
		return isPrimaryRailOasisAir(x, y, z)
				|| isTransitNexusRailwayAir(x, y, z)
				|| isPrimaryRailwayAir(x, y, z)
				|| isRailwayTurnAir(x, y, z)
				|| isRailwayXAir(x, y, z)
				|| isRailwayZAir(x, y, z);
	}

	private BlockState railwayTurnState(int x, int y, int z) {
		RailwayTurn turn = railwayTurnAt(x, z);
		if (turn == null) {
			return null;
		}
		int dx = x - turn.centerX();
		int dz = z - turn.centerZ();
		int baseY = turn.baseY();
		if (Math.abs(dx) > 12 || Math.abs(dz) > 12 || y < baseY || y > baseY + 8) {
			return null;
		}
		if (y == baseY) {
			if (Math.abs(dx) <= 10 && Math.abs(dz) <= 10) {
				return Math.floorMod(dx + dz, 9) == 0 ? BlockPalette.GRATE : BlockPalette.WALKWAY;
			}
			return null;
		}
		if (y == baseY + 1) {
			if (dx == -3 && dz == -3) {
				return BlockPalette.RAIL_SOUTH_EAST;
			}
			if (dx == 3 && dz == -3) {
				return BlockPalette.RAIL_SOUTH_WEST;
			}
			if (dx == -3 && dz == 3) {
				return BlockPalette.RAIL_NORTH_EAST;
			}
			if (dx == 3 && dz == 3) {
				return BlockPalette.RAIL_NORTH_WEST;
			}
			if ((dz == -3 || dz == -2 || dz == 2 || dz == 3) && Math.abs(dx) <= 3) {
				return BlockPalette.RAIL_X;
			}
			if ((dx == -3 || dx == -2 || dx == 2 || dx == 3) && Math.abs(dz) <= 3) {
				return BlockPalette.RAIL_Z;
			}
			if (Math.abs(dx) == 8 || Math.abs(dz) == 8) {
				return BlockPalette.FOUNDATION;
			}
		}
		boolean shell = Math.abs(dx) == 12 || Math.abs(dz) == 12;
		boolean rib = Math.floorMod(dx + dz + turn.salt(), 8) == 0;
		if (y >= baseY + 1 && y <= baseY + 6 && shell) {
			return rib ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
		}
		if (y == baseY + 7 && Math.abs(dx) <= 11 && Math.abs(dz) <= 11) {
			return Math.abs(dx) <= 1 && Math.abs(dz) <= 1 ? BlockPalette.LAMP : BlockPalette.LIGHT_STONE;
		}
		if (y == baseY + 8 && (rib || Math.abs(dx) == 12 || Math.abs(dz) == 12)) {
			return BlockPalette.FOUNDATION;
		}
		return null;
	}

	private boolean isRailwayTurnAir(int x, int y, int z) {
		RailwayTurn turn = railwayTurnAt(x, z);
		if (turn == null) {
			return false;
		}
		return Math.abs(x - turn.centerX()) <= 11
				&& Math.abs(z - turn.centerZ()) <= 11
				&& y >= turn.baseY() + 1
				&& y <= turn.baseY() + 7;
	}

	private RailwayTurn railwayTurnAt(int x, int z) {
		int laneZ = MegastructureMath.floorDiv(z, 768);
		long xHash = MegastructureMath.hash(worldVariantSeed, laneZ, 0, 1201);
		if (Math.floorMod(xHash, 5) > 1) {
			return null;
		}
		int centerZ = laneZ * 768 + MegastructureMath.range(xHash >>> 8, 160, 608);
		if (Math.abs(z - centerZ) > 12) {
			return null;
		}
		int laneX = MegastructureMath.floorDiv(x, 768);
		long zHash = MegastructureMath.hash(worldVariantSeed, laneX, 0, 1207);
		if (Math.floorMod(zHash, 6) != 0) {
			return null;
		}
		int centerX = laneX * 768 + MegastructureMath.range(zHash >>> 8, 160, 608);
		if (Math.abs(x - centerX) > 12) {
			return null;
		}
		int xBase = railwayBaseY(xHash);
		int zBase = railwayBaseY(zHash);
		if (Math.abs(xBase - zBase) > 2) {
			return null;
		}
		return new RailwayTurn(centerX, centerZ, (xBase + zBase) / 2, (int) (xHash ^ zHash));
	}

	private BlockState transitNexusRailwayState(int x, int y, int z) {
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		int[] offsets = {0, -1, 1, -2, 2};
		if (Math.abs(MegastructureMath.floorMod(z, DISTRICT_SIZE) - DISTRICT_SIZE / 2) <= 10) {
			for (int offset : offsets) {
				int nexusDistrictX = districtX + offset;
				int centerX = nexusDistrictX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				int centerZ = districtZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				if (districtType(centerX, centerZ) != DISTRICT_TRANSIT_NEXUS || Math.abs(x - centerX) > 1792) {
					continue;
				}
				int baseY = districtBaseY(centerX, centerZ, 1401, 72, 420);
				BlockState state = transitNexusApproachState(x - centerX, z - centerZ, y - baseY, true);
				if (state != null) {
					return state;
				}
			}
		}
		if (Math.abs(MegastructureMath.floorMod(x, DISTRICT_SIZE) - DISTRICT_SIZE / 2) <= 10) {
			for (int offset : offsets) {
				int nexusDistrictZ = districtZ + offset;
				int centerX = districtX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				int centerZ = nexusDistrictZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				if (districtType(centerX, centerZ) != DISTRICT_TRANSIT_NEXUS || Math.abs(z - centerZ) > 1792) {
					continue;
				}
				int baseY = districtBaseY(centerX, centerZ, 1401, 72, 420);
				BlockState state = transitNexusApproachState(z - centerZ, x - centerX, y - baseY, false);
				if (state != null) {
					return state;
				}
			}
		}
		return null;
	}

	private BlockState transitNexusApproachState(int along, int cross, int level, boolean xAxis) {
		int railFloor = xAxis ? 38 : 92;
		int stationExtent = xAxis ? 252 : 188;
		if (Math.abs(along) <= stationExtent || Math.abs(along) > 1792 || Math.abs(cross) > 10) {
			return null;
		}
		if (level == railFloor) {
			return BlockPalette.WALKWAY;
		}
		if (level == railFloor + 1 && (cross == -3 || cross == -2 || cross == 2 || cross == 3)) {
			return xAxis ? BlockPalette.RAIL_X : BlockPalette.RAIL_Z;
		}
		if (level == railFloor + 9) {
			return Math.floorMod(along, 24) <= 1 ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
		}
		if (Math.abs(cross) == 10 && level >= railFloor + 1 && level <= railFloor + 8) {
			return Math.floorMod(level - railFloor, 4) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
		}
		if (level == railFloor + 8 && Math.floorMod(along, 32) <= 1) {
			return Math.abs(cross) <= 1 ? BlockPalette.LAMP : BlockPalette.FOUNDATION;
		}
		return null;
	}

	private boolean isTransitNexusRailwayAir(int x, int y, int z) {
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		int[] offsets = {0, -1, 1, -2, 2};
		if (Math.abs(MegastructureMath.floorMod(z, DISTRICT_SIZE) - DISTRICT_SIZE / 2) <= 9) {
			for (int offset : offsets) {
				int centerX = (districtX + offset) * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				int centerZ = districtZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				if (districtType(centerX, centerZ) == DISTRICT_TRANSIT_NEXUS && Math.abs(x - centerX) <= 1792) {
					int baseY = districtBaseY(centerX, centerZ, 1401, 72, 420);
					if (y >= baseY + 39 && y <= baseY + 46) {
						return true;
					}
				}
			}
		}
		if (Math.abs(MegastructureMath.floorMod(x, DISTRICT_SIZE) - DISTRICT_SIZE / 2) <= 9) {
			for (int offset : offsets) {
				int centerX = districtX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				int centerZ = (districtZ + offset) * DISTRICT_SIZE + DISTRICT_SIZE / 2;
				if (districtType(centerX, centerZ) == DISTRICT_TRANSIT_NEXUS && Math.abs(z - centerZ) <= 1792) {
					int baseY = districtBaseY(centerX, centerZ, 1401, 72, 420);
					if (y >= baseY + 93 && y <= baseY + 100) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private int primaryRailOasisCenter(boolean positive) {
		long hash = MegastructureMath.hash(worldVariantSeed, positive ? 1 : -1, 0, 2047);
		int distance = MegastructureMath.range(hash, 5000, 15000);
		distance = Math.floorDiv(distance + 8, 16) * 16;
		return positive ? distance : -distance;
	}

	private boolean isPrimaryRailOasisAir(int x, int y, int z) {
		int center = x >= 0 ? primaryRailOasisCenter(true) : primaryRailOasisCenter(false);
		int baseY = primaryRailYAt(center);
		return Math.abs(x - center) < 132 && Math.abs(z) < 88
				&& y >= baseY + 1 && y < baseY + 64;
	}

	private BlockState primaryRailOasisState(int x, int y, int z) {
		int center = x >= 0 ? primaryRailOasisCenter(true) : primaryRailOasisCenter(false);
		int dx = x - center;
		int baseY = primaryRailYAt(center);
		int level = y - baseY;
		if (Math.abs(dx) > 132 || Math.abs(z) > 88 || level < 0 || level > 64) {
			return null;
		}
		long seed = MegastructureMath.hash(worldVariantSeed, center, 0, 2053);
		int side = Math.floorMod(seed, 2) == 0 ? 1 : -1;
		int basinX = center + MegastructureMath.range(seed >>> 8, -34, 34);
		int basinZ = side * MegastructureMath.range(seed >>> 18, 34, 46);
		int basinRadius = MegastructureMath.range(seed >>> 26, 22, 31);
		int localBasinX = x - basinX;
		int localBasinZ = z - basinZ;
		int basinDistance2 = localBasinX * localBasinX + localBasinZ * localBasinZ;

		BlockState pipe = primaryRailOasisPipeState(
				x, y, z, baseY, basinX, basinZ, basinRadius, side, seed);
		if (pipe != null) {
			return pipe;
		}

		if (level == 1) {
			BlockState rail = primaryRailStateAt(x, z);
			if (rail != null) {
				return rail;
			}
			BlockState tree = primaryRailOasisTreeState(
					x, y, z, baseY, basinX, basinZ, basinRadius, seed);
			if (tree != null) {
				return tree;
			}
			if (basinDistance2 <= basinRadius * basinRadius) {
				return BlockPalette.WATER;
			}
			if (primaryRailOasisMossAt(localBasinX, localBasinZ, basinRadius, seed)
					&& Math.floorMod(MegastructureMath.hash(seed, x, z, 2069), 5) == 0) {
				return BlockPalette.MOSS_CARPET;
			}
		}
		if (level >= 2 && level <= 50) {
			BlockState tree = primaryRailOasisTreeState(
					x, y, z, baseY, basinX, basinZ, basinRadius, seed);
			if (tree != null) {
				return tree;
			}
		}

		boolean entry = Math.abs(dx) >= 130 && Math.abs(z) <= 8 && level >= 1 && level <= 9;
		if (level == 0) {
			if (basinDistance2 <= (basinRadius + 2) * (basinRadius + 2)) {
				return Math.floorMod(MegastructureMath.hash(seed, x, z, 2059), 7) <= 2
						? BlockPalette.CLAY
						: BlockPalette.MOSS;
			}
			return primaryRailOasisMossAt(localBasinX, localBasinZ, basinRadius, seed)
					? BlockPalette.MOSS
					: Math.floorMod(MegastructureMath.hash(seed, x, z, 2063), 19) == 0
						? BlockPalette.CRACKED_PANEL
						: BlockPalette.WALKWAY;
		}
		if (level == 64) {
			return Math.floorMod(dx + z, 17) <= 2 ? BlockPalette.CRACKED_PANEL : BlockPalette.WALL_PANEL;
		}
		if ((Math.abs(dx) == 132 || Math.abs(z) == 88) && level >= 1 && level < 64 && !entry) {
			int panel = Math.floorMod((Math.abs(dx) == 132 ? z : dx) + level * 3, 29);
			return panel <= 2 ? BlockPalette.FOUNDATION : panel == 7 ? BlockPalette.CRACKED_PANEL : BlockPalette.WALL_PANEL;
		}

		int architecture = Math.floorMod(seed >>> 40, 4);
		if (architecture == 0 && level >= 1 && level <= 52 && Math.floorMod(dx + 132, 32) <= 1
				&& Math.abs(z) >= 76) {
			return BlockPalette.FOUNDATION;
		}
		if (architecture == 1 && (level == 18 || level == 36) && Math.abs(z) >= 62 && Math.abs(dx) <= 112) {
			return Math.floorMod(dx, 11) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.WALKWAY;
		}
		if (architecture == 2 && level >= 44 && Math.floorMod(dx + z, 27) <= 1 && Math.abs(z) >= 54) {
			return BlockPalette.FOUNDATION;
		}
		if (architecture == 3 && level <= 14 && Math.floorMod(dx + 96, 48) <= 2 && Math.abs(z) >= 66) {
			return BlockPalette.DARK_STONE;
		}
		return null;
	}

	private BlockState primaryRailOasisPipeState(
			int x,
			int y,
			int z,
			int baseY,
			int basinX,
			int basinZ,
			int basinRadius,
			int side,
			long seed
	) {
		int sourceY = baseY + MegastructureMath.range(seed >>> 32, 34, 47);
		int mouthZ = basinZ + side * Math.max(6, basinRadius / 2);
		int wallZ = side * 88;
		if (!between(z, mouthZ, wallZ)) {
			return null;
		}
		int cross = x - basinX;
		int vertical = y - sourceY;
		int innerRadius = 3;
		int outerRadius = 5;
		int radius2 = cross * cross + vertical * vertical;
		int fromMouth = Math.abs(z - mouthZ);
		boolean collar = fromMouth > 3 && Math.floorMod(fromMouth, 13) <= 1;
		if (radius2 >= innerRadius * innerRadius && radius2 <= outerRadius * outerRadius) {
			return collar ? BlockPalette.WALL_PANEL : BlockPalette.PIPE;
		}
		if (radius2 < innerRadius * innerRadius) {
			return vertical <= 0 ? BlockPalette.WATER : BlockPalette.AIR;
		}
		if (x == basinX && z == mouthZ && y >= baseY + 2 && y <= sourceY - innerRadius) {
			return BlockPalette.WATER;
		}
		boolean ceilingBrace = fromMouth > 10 && Math.floorMod(fromMouth, 28) <= 1
				&& Math.abs(cross) == outerRadius && y >= sourceY + outerRadius && y <= baseY + 63;
		return ceilingBrace ? BlockPalette.FOUNDATION : null;
	}

	private BlockState primaryRailOasisTreeState(
			int x,
			int y,
			int z,
			int baseY,
			int basinX,
			int basinZ,
			int basinRadius,
			long seed
	) {
		int level = y - baseY;
		for (int tree = 0; tree < 10; tree++) {
			long treeHash = MegastructureMath.hash(seed, tree, 0, 2091);
			int direction = Math.floorMod((int) (treeHash >>> 4) + tree * 3, TREE_DIRECTIONS_X.length);
			int radius = MegastructureMath.range(treeHash >>> 12, basinRadius + 4, basinRadius + 30);
			int treeX = basinX + TREE_DIRECTIONS_X[direction] * radius / 100;
			int treeZ = basinZ + TREE_DIRECTIONS_Z[direction] * radius / 100;
			int dx = x - treeX;
			int dz = z - treeZ;
			int height = MegastructureMath.range(treeHash >>> 28, 12, tree == 0 ? 36 : 24);
			DynamicWorldgenPalette.TreeMaterial material = DynamicWorldgenPalette.treeMaterial(seed, tree + 2000);
			if (level == 1) {
				for (int root = 0; root < 6; root++) {
					long rootHash = MegastructureMath.hash(treeHash, root, 0, 2081);
					int rootDirection = Math.floorMod((int) rootHash + root * 2, TREE_DIRECTIONS_X.length);
					int reach = MegastructureMath.range(rootHash >>> 12, 4, 12);
					int sideDirection = Math.floorMod(rootDirection + 2, TREE_DIRECTIONS_X.length);
					for (int step = 2; step <= reach; step++) {
						int bend = Math.floorMod((int) (rootHash >>> 24) + step / 3, 3) - 1;
						int rootX = TREE_DIRECTIONS_X[rootDirection] * step / 100
								+ TREE_DIRECTIONS_X[sideDirection] * bend / 100;
						int rootZ = TREE_DIRECTIONS_Z[rootDirection] * step / 100
								+ TREE_DIRECTIONS_Z[sideDirection] * bend / 100;
						int width = tree == 0 && step <= 3 ? 1 : 0;
						if (Math.abs(dx - rootX) <= width && Math.abs(dz - rootZ) <= width) {
							return Math.floorMod(rootHash + step, 5) == 0 ? BlockPalette.MOSS : material.log();
						}
					}
				}
			}
			if (level <= 2 && Math.floorMod(treeHash >>> 52, 4) == 0) {
				int fallDirection = Math.floorMod((int) (treeHash >>> 56), TREE_DIRECTIONS_X.length);
				int fallLength = MegastructureMath.range(treeHash >>> 20, 12, 32);
				int fx = TREE_DIRECTIONS_X[fallDirection];
				int fz = TREE_DIRECTIONS_Z[fallDirection];
				int along = Math.floorDiv(dx * fx + dz * fz, 100);
				int cross = Math.floorDiv(dx * fz - dz * fx, 100);
				if (along >= 4 && along <= fallLength && Math.abs(cross) <= 1 && level == 1 + Math.min(1, along / 16)) {
					return material.log();
				}
			}
			if (dx * dx + dz * dz <= (tree == 0 ? 4 : 1) && level >= 2 && level <= height) {
				return material.log();
			}
			int crownDy = Math.abs(level - height);
			int crownRadius = (tree == 0 ? 12 : 7) - crownDy;
			if (crownRadius >= 2 && dx * dx + dz * dz <= crownRadius * crownRadius) {
				if (Math.floorMod(MegastructureMath.hash(treeHash, x, y, z), 13) == 0 && crownDy > 0) {
					return null;
				}
				return material.leaves();
			}
		}
		return null;
	}

	private boolean primaryRailOasisMossAt(int dx, int dz, int radius, long seed) {
		if (dx * dx + dz * dz <= radius * radius) {
			return true;
		}
		for (int vein = 0; vein < 14; vein++) {
			long hash = MegastructureMath.hash(seed, vein, 0, 2087);
			int direction = Math.floorMod((int) hash + vein * 3, TREE_DIRECTIONS_X.length);
			int directionX = TREE_DIRECTIONS_X[direction];
			int directionZ = TREE_DIRECTIONS_Z[direction];
			int projection = Math.floorDiv(dx * directionX + dz * directionZ, 100);
			int perpendicular = Math.floorDiv(dx * directionZ - dz * directionX, 100);
			int length = MegastructureMath.range(hash >>> 16, radius + 18, radius + 68);
			int width = 1 + MegastructureMath.range(hash >>> 40, 0, 4);
			int wave = MegastructureMath.range(MegastructureMath.hash(hash, Math.floorDiv(projection, 9), 0, 2099), -5, 5);
			if (projection >= radius - 3 && projection <= length && Math.abs(perpendicular - wave) <= width) {
				return true;
			}
		}
		return false;
	}

	private BlockState primaryRailwayRuinState(int x, int y, int z) {
		if (Math.abs(x) < 1200 || Math.abs(z) > 7) {
			return null;
		}
		int segment = Math.floorDiv(x, 1024);
		long hash = MegastructureMath.hash(worldVariantSeed, segment, 0, 2101);
		if (Math.floorMod(hash, 7) != 0) {
			return null;
		}
		int center = segment * 1024 + MegastructureMath.range(hash >>> 8, 320, 704);
		int along = x - center;
		if (Math.abs(along) > 24) {
			return null;
		}
		int baseY = primaryRailYAt(x);
		long fragment = MegastructureMath.hash(hash, along, z, y);
		if (y == baseY && (Math.abs(z) <= 1 || Math.abs(z) >= 5)
				&& Math.floorMod(fragment, 9) <= 1) {
			return BlockPalette.CRACKED_PANEL;
		}
		if (y == baseY + 1 && (Math.abs(z) <= 1 || Math.abs(z) >= 5)
				&& Math.floorMod(fragment >>> 11, 13) == 0) {
			return ruinMaterial(hash, along, z, 1);
		}
		return null;
	}

	private BlockState primaryRailwayState(int x, int y, int z) {
		int stationCenter = primaryRailwayStationCenter(x);
		boolean station = stationCenter != Integer.MIN_VALUE && Math.abs(x - stationCenter) <= 80;
		int baseY = station ? primaryRailYAt(stationCenter) : primaryRailYAt(x);
		long stationSeed = station ? primaryRailwayStationSeed(stationCenter) : 0L;
		if (station) {
			BlockState room = railwayStationRoomState(x - stationCenter, z, y, baseY, stationSeed);
			if (room != null) {
				return room;
			}
			BlockState architecture = railwayStationArchitectureState(x - stationCenter, z, y, baseY, stationSeed);
			if (architecture != null) {
				return architecture;
			}
		}
		int outerWidth = station ? 14 : 10;
		if (Math.abs(z) > outerWidth) {
			return null;
		}
		if (!station) {
			return primaryRailwayTunnelState(x, y, z, baseY);
		}
		if (y == baseY && Math.abs(z) <= 13) {
			return BlockPalette.WALKWAY;
		}
		if (y == baseY + 1) {
			BlockState rail = primaryRailStateAt(x, z);
			if (rail != null) {
				return rail;
			}
		}
		if (station && y == baseY + 1 && Math.abs(z) >= 6 && Math.abs(z) <= 12) {
			return BlockPalette.PLATFORM;
		}
		if (station && Math.abs(z) == 14 && y >= baseY + 1 && y <= baseY + 8) {
			if (isRailwayStationRoomDoor(x - stationCenter, z, y, baseY, stationSeed)) {
				return BlockPalette.AIR;
			}
			boolean centralEntrance = Math.abs(x - stationCenter) <= 7;
			if (!centralEntrance) {
				return Math.floorMod(y - baseY, 4) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
			}
		}
		if (station && y == baseY + 9 && Math.abs(z) <= 14) {
			return Math.floorMod(x - stationCenter, 12) <= 1 ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
		}
		if (station && y == baseY + 8 && Math.abs(z) == 10 && Math.floorMod(x - stationCenter, 24) == 0) {
			return BlockPalette.LAMP;
		}
		if (isPrimaryRift(x, z) && y == baseY + 1 && isPrimaryRailwayFootprintAt(x, z, 8) && primaryRailStateAt(x, z) == null) {
			return BlockPalette.GRATE;
		}
		return null;
	}

	private BlockState primaryRailStateAt(int x, int z) {
		if (z != -3 && z != -2 && z != 2 && z != 3) {
			return null;
		}
		int here = primaryRailYAt(x);
		int east = primaryRailYAt(x + 1);
		int west = primaryRailYAt(x - 1);
		if (east > here) {
			return BlockPalette.RAIL_ASCENDING_EAST;
		}
		if (west > here) {
			return BlockPalette.RAIL_ASCENDING_WEST;
		}
		return BlockPalette.RAIL_X;
	}

	private boolean isPrimaryRailwayAir(int x, int y, int z) {
		int baseY = primaryRailYAt(x);
		boolean tunnel = isPrimaryRailwayFootprintAt(x, z, 7) && isPrimaryRailwayTunnelAirAt(x, y, baseY);
		int stationCenter = primaryRailwayStationCenter(x);
		if (stationCenter == Integer.MIN_VALUE) {
			return tunnel;
		}
		int stationBaseY = primaryRailYAt(stationCenter);
		long stationSeed = primaryRailwayStationSeed(stationCenter);
		boolean station = Math.abs(x - stationCenter) <= 80
				&& Math.abs(z) <= 13
				&& y >= stationBaseY + 1 && y <= stationBaseY + 8;
		boolean room = isRailwayStationRoomAir(x - stationCenter, z, y, stationBaseY, stationSeed);
		boolean exit = Math.abs(x - stationCenter) <= 7 && Math.abs(z) <= 34
				&& y >= stationBaseY + 1 && y <= stationBaseY + 6;
		return tunnel || station || room || exit;
	}

	private int primaryRailYAt(int x) {
		int baseY = settings.spawnPlatformY();
		int distance = Math.abs(x);
		int protectedSpawnRun = 1024;
		if (distance <= protectedSpawnRun) {
			return baseY;
		}
		int sign = x < 0 ? -1 : 1;
		int segmentLength = 512;
		int pathDistance = distance - protectedSpawnRun;
		int segment = Math.floorDiv(pathDistance, segmentLength);
		int local = Math.floorMod(pathDistance, segmentLength);
		int from = primaryRailYOffset(sign, segment);
		int to = primaryRailYOffset(sign, segment + 1);
		double t = local / (double) segmentLength;
		double smooth = t * t * (3.0D - 2.0D * t);
		int grade = (int) Math.round(from + (to - from) * smooth);
		return Math.max(settings.floorY() + 32, Math.min(settings.ceilingY() - 96, baseY + grade));
	}

	private int primaryRailYOffset(int sign, int segment) {
		if (segment <= 0) {
			return 0;
		}
		long hash = MegastructureMath.hash(worldVariantSeed, sign * segment, segment, 2111);
		int coarse = MegastructureMath.range(hash, -46, 46);
		int drift = MegastructureMath.range(hash >>> 18, -18, 18);
		int longWave = MegastructureMath.range(
				MegastructureMath.hash(worldVariantSeed, sign, Math.floorDiv(segment, 4), 2117),
				-24,
				24
		);
		return Math.max(-58, Math.min(58, (coarse * 2 + drift + longWave) / 3));
	}

	private BlockState primaryRailwayTunnelState(int x, int y, int z, int baseY) {
		int relY = y - baseY;
		int absZ = Math.abs(z);
		if (!isPrimaryRailwayTunnelAirAt(x, y, baseY) && (relY < 0 || relY > 8 || absZ > 10)) {
			return null;
		}
		boolean rib = Math.floorMod(x, 18) <= 1;
		boolean bulkhead = Math.floorMod(x, 72) <= 2;
		boolean lampBay = Math.floorMod(x + 9, 36) <= 1;
		if (isPrimaryRailwayTransitionOnly(x, y, baseY) && absZ <= 7) {
			return BlockPalette.AIR;
		}
		if (relY == 0 && absZ <= 9) {
			if (absZ <= 1) {
				return BlockPalette.GRATE;
			}
			return Math.floorMod(x + z, 11) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.WALKWAY;
		}
		if (relY == 1) {
			BlockState rail = primaryRailStateAt(x, z);
			if (rail != null) {
				return rail;
			}
			if (absZ == 6) {
				return BlockPalette.FOUNDATION;
			}
		}
		if (relY >= 1 && relY <= 6 && absZ >= 8 && absZ <= 10) {
			if ((relY == 3 || relY == 4) && !rib && !bulkhead
					&& railwayWallFacesOpenVoid(x, y, z, 0, z > 0 ? 1 : -1)
					&& Math.floorMod(x, 14) >= 4 && Math.floorMod(x, 14) <= 9) {
				return BlockPalette.AIR;
			}
			return rib || bulkhead ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
		}
		if (relY == 2 && absZ == 7) {
			return rib ? BlockPalette.FOUNDATION : BlockPalette.DARK_STONE;
		}
		if (relY == 6 && absZ <= 8 && (rib || bulkhead)) {
			return BlockPalette.FOUNDATION;
		}
		if (relY == 7 && absZ <= 8) {
			if (absZ == 0 && lampBay) {
				return BlockPalette.LAMP;
			}
			return rib || bulkhead ? BlockPalette.FOUNDATION : BlockPalette.LIGHT_STONE;
		}
		if (relY == 8 && absZ <= 9 && (rib || bulkhead)) {
			return BlockPalette.FOUNDATION;
		}
		return null;
	}

	private boolean railwayWallFacesOpenVoid(int x, int y, int z, int stepX, int stepZ) {
		for (int step = 1; step <= 5; step++) {
			int sampleX = x + stepX * step;
			int sampleZ = z + stepZ * step;
			if (isRailwayLineAt(sampleX, sampleZ)) {
				continue;
			}
			return isOpenVoidForConnectorWindow(sampleX, y, sampleZ);
		}
		return false;
	}

	private boolean isPrimaryRailwayTunnelAirAt(int x, int y, int baseY) {
		int minY = baseY;
		int maxY = baseY;
		for (int offset = -3; offset <= 3; offset++) {
			int nearby = primaryRailYAt(x + offset);
			minY = Math.min(minY, nearby);
			maxY = Math.max(maxY, nearby);
		}
		return y >= minY + 1 && y <= maxY + 7;
	}

	private boolean isPrimaryRailwayTransitionOnly(int x, int y, int baseY) {
		int minY = baseY;
		int maxY = baseY;
		for (int offset = -2; offset <= 2; offset++) {
			int nearby = primaryRailYAt(x + offset);
			minY = Math.min(minY, nearby);
			maxY = Math.max(maxY, nearby);
		}
		return maxY > minY && y >= minY + 1 && y <= maxY + 7 && (y < baseY || y > baseY + 8);
	}

	private int primaryRailwayStationCenter(int x) {
		int segment = MegastructureMath.floorDiv(x, 1536);
		if (segment == 0) {
			return Integer.MIN_VALUE;
		}
		long hash = MegastructureMath.hash(worldVariantSeed, segment, 0, 1181);
		if (Math.floorMod(hash, 2) != 0) {
			return Integer.MIN_VALUE;
		}
		int center = segment * 1536 + MegastructureMath.range(hash >>> 8, 288, 1248);
		return riftIntersectsXRange(center - 90, center + 90) ? Integer.MIN_VALUE : center;
	}

	private long primaryRailwayStationSeed(int stationCenter) {
		return MegastructureMath.hash(worldVariantSeed, MegastructureMath.floorDiv(stationCenter, 1536), 0, 1181);
	}

	private BlockState railwayXState(int x, int y, int z) {
		int lane = MegastructureMath.floorDiv(z, 768);
		long hash = MegastructureMath.hash(worldVariantSeed, lane, 0, 1201);
		if (Math.floorMod(hash, 5) > 1) {
			return null;
		}
		int centerZ = lane * 768 + MegastructureMath.range(hash >>> 8, 160, 608);
		int dz = z - centerZ;
		int stationCenter = railwayStationCenter(x, centerZ, lane, 1221, true);
		boolean station = stationCenter != Integer.MIN_VALUE && Math.abs(x - stationCenter) <= 80;
		long stationSeed = station ? railwayStationSeed(stationCenter, lane, 1221) : 0L;
		if (station) {
			BlockState room = railwayStationRoomState(x - stationCenter, dz, y, railwayBaseY(hash), stationSeed);
			if (room != null) {
				return room;
			}
			BlockState architecture = railwayStationArchitectureState(x - stationCenter, dz, y, railwayBaseY(hash), stationSeed);
			if (architecture != null) {
				return architecture;
			}
		}
		int outerWidth = station ? 14 : 10;
		if (Math.abs(dz) > outerWidth) {
			return null;
		}
		int baseY = railwayBaseY(hash);
		if (y == baseY && Math.abs(dz) <= (station ? 13 : 7)) {
			return BlockPalette.WALKWAY;
		}
		if (y == baseY + 1 && (dz == -3 || dz == -2 || dz == 2 || dz == 3)) {
			return BlockPalette.RAIL_X;
		}
		if (station && y == baseY + 1 && Math.abs(dz) >= 6 && Math.abs(dz) <= 12) {
			return BlockPalette.PLATFORM;
		}
		if (station && isRailwayStationRoomDoor(x - stationCenter, dz, y, baseY, stationSeed)) {
			return BlockPalette.AIR;
		}
		boolean entrance = station && Math.abs(x - stationCenter) <= 7;
		if (station && Math.abs(dz) == 14 && y >= baseY + 1 && y <= baseY + 8 && !entrance) {
			return Math.floorMod(y - baseY, 4) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
		}
		if (station && y == baseY + 9 && Math.abs(dz) <= 14) {
			return Math.floorMod(x - stationCenter, 12) <= 1 ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
		}
		if (station && y == baseY + 8 && Math.abs(dz) == 10 && Math.floorMod(x - stationCenter, 24) == 0) {
			return BlockPalette.LAMP;
		}
		if (isPrimaryRift(x, z) && y == baseY + 1 && Math.abs(dz) == 8) {
			return BlockPalette.GRATE;
		}
		return null;
	}

	private BlockState railwayZState(int x, int y, int z) {
		int lane = MegastructureMath.floorDiv(x, 768);
		long hash = MegastructureMath.hash(worldVariantSeed, lane, 0, 1207);
		if (Math.floorMod(hash, 6) != 0) {
			return null;
		}
		int centerX = lane * 768 + MegastructureMath.range(hash >>> 8, 160, 608);
		int dx = x - centerX;
		int stationCenter = railwayStationCenter(z, centerX, lane, 1227, false);
		boolean station = stationCenter != Integer.MIN_VALUE && Math.abs(z - stationCenter) <= 80;
		long stationSeed = station ? railwayStationSeed(stationCenter, lane, 1227) : 0L;
		if (station) {
			BlockState room = railwayStationRoomState(z - stationCenter, dx, y, railwayBaseY(hash), stationSeed);
			if (room != null) {
				return room;
			}
			BlockState architecture = railwayStationArchitectureState(z - stationCenter, dx, y, railwayBaseY(hash), stationSeed);
			if (architecture != null) {
				return architecture;
			}
		}
		int outerWidth = station ? 14 : 10;
		if (Math.abs(dx) > outerWidth) {
			return null;
		}
		int baseY = railwayBaseY(hash);
		if (y == baseY && Math.abs(dx) <= (station ? 13 : 7)) {
			return BlockPalette.WALKWAY;
		}
		if (y == baseY + 1 && (dx == -3 || dx == -2 || dx == 2 || dx == 3)) {
			return BlockPalette.RAIL_Z;
		}
		if (station && y == baseY + 1 && Math.abs(dx) >= 6 && Math.abs(dx) <= 12) {
			return BlockPalette.PLATFORM;
		}
		if (station && isRailwayStationRoomDoor(z - stationCenter, dx, y, baseY, stationSeed)) {
			return BlockPalette.AIR;
		}
		boolean entrance = station && Math.abs(z - stationCenter) <= 7;
		if (station && Math.abs(dx) == 14 && y >= baseY + 1 && y <= baseY + 8 && !entrance) {
			return Math.floorMod(y - baseY, 4) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
		}
		if (station && y == baseY + 9 && Math.abs(dx) <= 14) {
			return Math.floorMod(z - stationCenter, 12) <= 1 ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
		}
		if (station && y == baseY + 8 && Math.abs(dx) == 10 && Math.floorMod(z - stationCenter, 24) == 0) {
			return BlockPalette.LAMP;
		}
		if (isPrimaryRift(x, z) && y == baseY + 1 && Math.abs(dx) == 8) {
			return BlockPalette.GRATE;
		}
		return null;
	}

	private boolean isRailwayXAir(int x, int y, int z) {
		int lane = MegastructureMath.floorDiv(z, 768);
		long hash = MegastructureMath.hash(worldVariantSeed, lane, 0, 1201);
		if (Math.floorMod(hash, 5) > 1) {
			return false;
		}
		int centerZ = lane * 768 + MegastructureMath.range(hash >>> 8, 160, 608);
		int baseY = railwayBaseY(hash);
		boolean tunnel = Math.abs(z - centerZ) <= 7 && y >= baseY + 1 && y <= baseY + 7;
		int stationCenter = railwayStationCenter(x, centerZ, lane, 1221, true);
		boolean station = stationCenter != Integer.MIN_VALUE
				&& Math.abs(x - stationCenter) <= 80
				&& Math.abs(z - centerZ) <= 13
				&& y >= baseY + 1
				&& y <= baseY + 8;
		boolean exit = stationCenter != Integer.MIN_VALUE
				&& Math.abs(x - stationCenter) <= 7
				&& Math.abs(z - centerZ) <= 34
				&& y >= baseY + 1
				&& y <= baseY + 6;
		boolean room = stationCenter != Integer.MIN_VALUE
				&& isRailwayStationRoomAir(
					x - stationCenter,
					z - centerZ,
					y,
					baseY,
					railwayStationSeed(stationCenter, lane, 1221)
				);
		return tunnel || station || exit || room;
	}

	private boolean isRailwayZAir(int x, int y, int z) {
		int lane = MegastructureMath.floorDiv(x, 768);
		long hash = MegastructureMath.hash(worldVariantSeed, lane, 0, 1207);
		if (Math.floorMod(hash, 6) != 0) {
			return false;
		}
		int centerX = lane * 768 + MegastructureMath.range(hash >>> 8, 160, 608);
		int baseY = railwayBaseY(hash);
		boolean tunnel = Math.abs(x - centerX) <= 7 && y >= baseY + 1 && y <= baseY + 7;
		int stationCenter = railwayStationCenter(z, centerX, lane, 1227, false);
		boolean station = stationCenter != Integer.MIN_VALUE
				&& Math.abs(z - stationCenter) <= 80
				&& Math.abs(x - centerX) <= 13
				&& y >= baseY + 1
				&& y <= baseY + 8;
		boolean exit = stationCenter != Integer.MIN_VALUE
				&& Math.abs(z - stationCenter) <= 7
				&& Math.abs(x - centerX) <= 34
				&& y >= baseY + 1
				&& y <= baseY + 6;
		boolean room = stationCenter != Integer.MIN_VALUE
				&& isRailwayStationRoomAir(
					z - stationCenter,
					x - centerX,
					y,
					baseY,
					railwayStationSeed(stationCenter, lane, 1227)
				);
		return tunnel || station || exit || room;
	}

	private int railwayBaseY(long hash) {
		int maximumOffset = Math.max(176, getWorldHeight() - 160);
		return settings.floorY() + MegastructureMath.range(hash >>> 20, 112, maximumOffset);
	}

	private int railwayStationCenter(int along, int fixed, int lane, int salt, boolean xAxis) {
		int segment = MegastructureMath.floorDiv(along, 1536);
		long stationHash = MegastructureMath.hash(worldVariantSeed, lane, segment, salt);
		if (Math.floorMod(stationHash, 4) != 0) {
			return Integer.MIN_VALUE;
		}
		int center = segment * 1536 + MegastructureMath.range(stationHash >>> 8, 384, 1152);
		if ((xAxis && riftIntersectsXRange(center - 90, center + 90)) || (!xAxis && isPrimaryRift(fixed, center))) {
			return Integer.MIN_VALUE;
		}
		int district = xAxis ? districtType(center, fixed) : districtType(fixed, center);
		return supportsRailwayStation(district) ? center : Integer.MIN_VALUE;
	}

	private long railwayStationSeed(int stationCenter, int lane, int salt) {
		return MegastructureMath.hash(
				SHAPE_SEED,
				lane,
				MegastructureMath.floorDiv(stationCenter, 1536),
				salt
		);
	}

	private BlockState railwayStationArchitectureState(int along, int cross, int y, int baseY, long stationSeed) {
		int absoluteCross = Math.abs(cross);
		int architecture = Math.floorMod((int) (stationSeed >>> 12), 6);
		if (Math.abs(along) <= 8 && absoluteCross >= 14 && absoluteCross <= 34) {
			if (y == baseY) {
				return BlockPalette.WALKWAY;
			}
			if (y == baseY + 7) {
				return Math.floorMod(cross, 10) <= 1 ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
			}
			if (Math.abs(along) == 8 && y >= baseY + 1 && y <= baseY + 6) {
				return Math.floorMod(y - baseY, 3) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
			}
			if (absoluteCross == 34 && y >= baseY + 1 && y <= baseY + 6 && Math.abs(along) > 3) {
				return BlockPalette.WALL_PANEL;
			}
		}
		boolean sideColumn = absoluteCross >= 12 && absoluteCross <= 13
				&& Math.floorMod(along + 72, 24) <= 1
				&& y >= baseY + 1 && y <= baseY + 8;
		if (sideColumn) {
			return Math.floorMod(y - baseY, 4) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
		}
		if (architecture == 0
				&& y == baseY + 8
				&& absoluteCross <= 13
				&& Math.floorMod(along + 80, 16) <= 1) {
			return BlockPalette.FOUNDATION;
		}
		if (architecture == 1
				&& y == baseY + 7
				&& absoluteCross <= 11
				&& (Math.abs(along) == 32 || Math.abs(along) == 1)) {
			return absoluteCross <= 1 ? BlockPalette.LAMP : BlockPalette.WALL_PANEL;
		}
		if (architecture == 2
				&& y == baseY + 6
				&& absoluteCross >= 10 && absoluteCross <= 13
				&& Math.floorMod(along + 80, 20) <= 12) {
			return Math.floorMod(along, 7) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.WALL_PANEL;
		}
		if (architecture == 3 && Math.abs(along) <= 3 && absoluteCross <= 13) {
			if (y == baseY + 6) {
				return BlockPalette.WALKWAY;
			}
			if (absoluteCross >= 12 && y >= baseY + 1 && y <= baseY + 5) {
				return BlockPalette.FOUNDATION;
			}
		}
		if (architecture == 4
				&& Math.abs(Math.abs(along) - 56) <= 1
				&& absoluteCross <= 13
				&& y >= baseY + 1 && y <= baseY + 8
				&& (absoluteCross >= 11 || y >= baseY + 7)) {
			return BlockPalette.DARK_STONE;
		}
		if (architecture == 5
				&& y == baseY + 5
				&& absoluteCross == 11
				&& (Math.abs(along - 24) <= 4 || Math.abs(along + 24) <= 4)) {
			return Math.abs(along) == 24 ? BlockPalette.LAMP : BlockPalette.RUST_PIPE;
		}
		boolean endPortal = Math.abs(Math.abs(along) - 72) <= 1
				&& absoluteCross <= 14
				&& y >= baseY + 1 && y <= baseY + 8
				&& (absoluteCross >= 10 || y >= baseY + 7);
		return endPortal ? BlockPalette.FOUNDATION : null;
	}

	private BlockState railwayStationRoomState(int along, int cross, int y, int baseY, long stationSeed) {
		int absoluteCross = Math.abs(cross);
		if (absoluteCross < 14 || absoluteCross > 36 || y < baseY || y > baseY + 9) {
			return railwayStationTandemConnectorState(along, cross, y, baseY, stationSeed);
		}
		int side = cross >= 0 ? 1 : 0;
		for (int slot = 0; slot < 4; slot++) {
			int roomCenter = -48 + slot * 32;
			int localAlong = along - roomCenter;
			if (Math.abs(localAlong) > 11 || !railwayStationRoomPresent(stationSeed, slot, side)) {
				continue;
			}
			long roomHash = MegastructureMath.hash(stationSeed, slot, side, 1259);
			int roomType = railwayStationRoomType(stationSeed, slot, side);
			int level = y - baseY;
			int depth = absoluteCross - 14;
			if ((absoluteCross == 14 && Math.abs(localAlong) <= 3 && level >= 1 && level <= 7)
					|| isRailwayStationTandemPortal(stationSeed, slot, side, localAlong, depth, level)) {
				return BlockPalette.AIR;
			}
			if (y == baseY) {
				if (roomType == 10 && depth >= 5 && depth <= 18 && Math.floorMod(localAlong, 4) == 0) {
					return BlockPalette.GRATE;
				}
				if (roomType == 11 && Math.floorMod(localAlong * 3 + depth * 5 + (int) roomHash, 17) <= 2) {
					return BlockPalette.CRACKED_PANEL;
				}
				return Math.floorMod(localAlong + cross, 13) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.WALKWAY;
			}
			if (y == baseY + 9) {
				return Math.floorMod(localAlong, 8) <= 1 ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
			}
			boolean wall = Math.abs(localAlong) == 11 || absoluteCross == 14 || absoluteCross == 36;
			if (wall) {
				return Math.floorMod(y - baseY, 4) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
			}

			BlockState program = railwayStationRoomProgramState(roomType, localAlong, depth, level, cross > 0);
			if (program != null) {
				return program;
			}
			BlockState accent = railwayStationRoomAccentState(roomHash, localAlong, depth, level);
			if (accent != null) {
				return accent;
			}
			return null;
		}
		return railwayStationTandemConnectorState(along, cross, y, baseY, stationSeed);
	}

	private BlockState railwayStationRoomProgramState(
			int roomType,
			int localAlong,
			int depth,
			int level,
			boolean positiveSide
	) {
		return switch (roomType) {
			case 0 -> {
				boolean console = level == 1 && depth >= 16 && depth <= 18 && Math.abs(localAlong) <= 7;
				boolean mapWall = depth == 20 && level >= 2 && level <= 5 && Math.abs(localAlong) <= 6;
				yield console ? (Math.floorMod(localAlong, 4) == 0 ? BlockPalette.LAMP : BlockPalette.GRATE)
						: mapWall ? BlockPalette.WALL_PANEL : null;
			}
			case 1 -> {
				boolean pit = level == 1 && depth >= 7 && depth <= 15 && Math.abs(localAlong) <= 4;
				boolean hoist = Math.abs(localAlong) == 7 && depth == 12 && level >= 1 && level <= 6;
				yield pit ? BlockPalette.GRATE : hoist ? BlockPalette.FOUNDATION : null;
			}
			case 2 -> {
				boolean bench = level == 1 && (depth == 7 || depth == 15)
						&& Math.abs(localAlong) >= 2 && Math.abs(localAlong) <= 8;
				yield bench ? BlockPalette.stairs(positiveSide ? Direction.NORTH : Direction.SOUTH) : null;
			}
			case 3 -> {
				boolean archive = Math.abs(localAlong) >= 8 && depth >= 5 && depth <= 19
						&& level >= 1 && level <= 6 && Math.floorMod(depth, 4) <= 1;
				yield archive ? (level % 3 == 0 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE) : null;
			}
			case 4 -> {
				boolean riser = (localAlong == -5 || localAlong == 5) && depth == 14 && level >= 1 && level <= 6;
				boolean header = level == 6 && depth == 14 && Math.abs(localAlong) <= 5;
				yield header ? BlockPalette.RUST_PIPE : riser ? BlockPalette.PIPE : null;
			}
			case 5 -> {
				boolean bank = (Math.abs(localAlong) == 6) && depth >= 7 && depth <= 18 && level <= 3;
				boolean bus = level == 4 && depth >= 7 && depth <= 18 && Math.abs(localAlong) == 6;
				yield bus ? BlockPalette.RUST_PIPE : bank ? BlockPalette.DARK_STONE : null;
			}
			case 6 -> {
				boolean baffle = (localAlong == -3 && depth >= 3 && depth <= 9
						|| localAlong == 3 && depth >= 8 && depth <= 14) && level >= 1 && level <= 4;
				boolean booth = depth >= 17 && depth <= 20 && localAlong >= 4 && localAlong <= 8 && level <= 4;
				yield baffle ? BlockPalette.WALL_PANEL : booth ? BlockPalette.GRATE : null;
			}
			case 7 -> {
				boolean lockers = depth == 20 && Math.abs(localAlong) <= 9 && level >= 1 && level <= 5
						&& Math.floorMod(localAlong, 3) != 0;
				boolean divider = localAlong == 0 && depth >= 7 && depth <= 16 && level <= 4;
				yield lockers ? BlockPalette.DARK_STONE : divider ? BlockPalette.WALL_PANEL : null;
			}
			case 8 -> {
				boolean mast = localAlong == 0 && depth == 12 && level >= 1 && level <= 7;
				boolean relay = level == 2 && depth >= 9 && depth <= 15 && Math.abs(localAlong) == 4;
				yield mast ? (level == 7 ? BlockPalette.LAMP : BlockPalette.RUST_PIPE)
						: relay ? BlockPalette.GRATE : null;
			}
			case 9 -> {
				boolean cubicle = (Math.abs(localAlong) == 4 && depth >= 6 && depth <= 18
						|| (depth == 8 || depth == 16) && Math.abs(localAlong) <= 8) && level <= 3;
				yield cubicle ? (level == 1 ? BlockPalette.DARK_STONE : BlockPalette.WALL_PANEL) : null;
			}
			case 10 -> {
				boolean cableRiser = (localAlong == -7 || localAlong == 7) && depth == 18 && level <= 6;
				boolean tray = level == 5 && depth >= 5 && depth <= 18 && Math.abs(localAlong) == 7;
				yield tray ? BlockPalette.RUST_PIPE : cableRiser ? BlockPalette.PIPE : null;
			}
			case 11 -> {
				boolean brokenPartition = localAlong == 3 && depth >= 8 && depth <= 20 && level >= 1 && level <= 6
						&& !(depth <= 11 && level <= 3) && Math.floorMod(depth + level, 5) != 0;
				yield brokenPartition ? BlockPalette.CRACKED_PANEL : null;
			}
			default -> null;
		};
	}

	private BlockState railwayStationRoomAccentState(long roomHash, int localAlong, int depth, int level) {
		int accent = Math.floorMod((int) (roomHash >>> 17), 8);
		return switch (accent) {
			case 0 -> level == 8 && depth == 11 && (localAlong == -4 || localAlong == 4) ? BlockPalette.LAMP : null;
			case 1 -> level == 7 && Math.floorMod(localAlong + 10, 5) == 0 && depth >= 5 && depth <= 18
					? BlockPalette.FOUNDATION : null;
			case 2 -> level == 5 && depth == 20 && Math.abs(localAlong) <= 8 ? BlockPalette.RUST_PIPE : null;
			case 3 -> level == 1 && Math.floorMod(localAlong * 7 + depth * 3, 19) == 0 ? BlockPalette.STAIN : null;
			case 4 -> localAlong == 0 && depth == 18 && level >= 1 && level <= 7 ? BlockPalette.FOUNDATION : null;
			case 5 -> depth == 20 && level == 4 && Math.abs(localAlong) <= 8 ? BlockPalette.WALL_PANEL : null;
			case 6 -> level == 1 && depth >= 8 && depth <= 16 && Math.abs(localAlong) == 8 ? BlockPalette.GRATE : null;
			default -> null;
		};
	}

	private BlockState railwayStationTandemConnectorState(int along, int cross, int y, int baseY, long stationSeed) {
		int absoluteCross = Math.abs(cross);
		if (absoluteCross < 24 || absoluteCross > 28 || y < baseY || y > baseY + 6) {
			return null;
		}
		int side = cross >= 0 ? 1 : 0;
		for (int slot = 0; slot < 3; slot++) {
			if (!railwayStationRoomPresent(stationSeed, slot, side)
					|| !railwayStationRoomPresent(stationSeed, slot + 1, side)) {
				continue;
			}
			int midpoint = -32 + slot * 32;
			if (Math.abs(along - midpoint) > 5) {
				continue;
			}
			if (y == baseY || y == baseY + 6) {
				return y == baseY ? BlockPalette.WALKWAY : BlockPalette.WALL_PANEL;
			}
			if (absoluteCross == 24 || absoluteCross == 28) {
				return BlockPalette.DARK_STONE;
			}
			return BlockPalette.AIR;
		}
		return null;
	}

	private boolean isRailwayStationTandemPortal(
			long stationSeed,
			int slot,
			int side,
			int localAlong,
			int depth,
			int level
	) {
		if (depth < 10 || depth > 14 || level < 1 || level > 4) {
			return false;
		}
		return (localAlong == 11 && slot < 3 && railwayStationRoomPresent(stationSeed, slot + 1, side))
				|| (localAlong == -11 && slot > 0 && railwayStationRoomPresent(stationSeed, slot - 1, side));
	}

	private boolean isRailwayStationRoomAir(int along, int cross, int y, int baseY, long stationSeed) {
		int absoluteCross = Math.abs(cross);
		if (y >= baseY + 1 && y <= baseY + 5 && absoluteCross >= 25 && absoluteCross <= 27) {
			int side = cross >= 0 ? 1 : 0;
			for (int slot = 0; slot < 3; slot++) {
				int midpoint = -32 + slot * 32;
				if (Math.abs(along - midpoint) <= 5
						&& railwayStationRoomPresent(stationSeed, slot, side)
						&& railwayStationRoomPresent(stationSeed, slot + 1, side)) {
					return true;
				}
			}
		}
		if (absoluteCross < 14 || absoluteCross > 36 || y < baseY + 1 || y > baseY + 8) {
			return false;
		}
		int side = cross >= 0 ? 1 : 0;
		for (int slot = 0; slot < 4; slot++) {
			int roomCenter = -48 + slot * 32;
			if (Math.abs(along - roomCenter) <= 10 && railwayStationRoomPresent(stationSeed, slot, side)) {
				return true;
			}
		}
		return false;
	}

	private boolean isRailwayStationRoomDoor(int along, int cross, int y, int baseY, long stationSeed) {
		if (Math.abs(cross) != 14 || y < baseY + 1 || y > baseY + 7) {
			return false;
		}
		int side = cross >= 0 ? 1 : 0;
		for (int slot = 0; slot < 4; slot++) {
			int roomCenter = -48 + slot * 32;
			if (Math.abs(along - roomCenter) <= 3 && railwayStationRoomPresent(stationSeed, slot, side)) {
				return true;
			}
		}
		return false;
	}

	private boolean railwayStationRoomPresent(long stationSeed, int slot, int side) {
		long roomHash = MegastructureMath.hash(stationSeed, slot, side, 1249);
		int program = Math.floorMod((int) (stationSeed >>> 24), 8);
		int selected = Math.floorMod((int) (stationSeed >>> 32), 8);
		int selectedSide = selected / 4;
		int selectedSlot = selected % 4;
		return switch (program) {
			case 0 -> side * 4 + slot == selected;
			case 1 -> side == selectedSide && slot / 2 == selectedSlot / 2;
			case 2 -> slot == selectedSlot;
			case 3 -> slot / 2 == selectedSlot / 2;
			case 4 -> side == selectedSide ? slot != (selectedSlot + 2) % 4 : slot == selectedSlot;
			case 5 -> slot == 1 || slot == 2 || (side == selectedSide && slot == selectedSlot);
			case 6 -> Math.floorMod(roomHash, 100) < 58;
			default -> Math.floorMod(roomHash, 100) < 82;
		};
	}

	private int railwayStationRoomType(long stationSeed, int slot, int side) {
		int pair = slot / 2;
		long familyHash = MegastructureMath.hash(stationSeed, pair, 0, 1277);
		int family = Math.floorMod((int) familyHash, 6);
		int complement = Math.floorMod(slot + side + (int) (stationSeed >>> 20), 2);
		return family * 2 + complement;
	}

	private boolean riftIntersectsXRange(int minX, int maxX) {
		int cell = settings.motifCellSize();
		int firstStripe = MegastructureMath.floorDiv(minX, cell);
		int lastStripe = MegastructureMath.floorDiv(maxX, cell);
		for (int stripe = firstStripe; stripe <= lastStripe; stripe++) {
			long hash = riftStripeHash(stripe);
			if (!isAcceptedRiftStripe(stripe, hash)) {
				continue;
			}
			int width = MegastructureMath.range(hash >>> 12, Math.min(settings.riftMinWidth(), settings.riftMaxWidth()), Math.max(settings.riftMinWidth(), settings.riftMaxWidth()));
			int center = stripe * cell + cell / 2;
			if (center + width / 2 >= minX && center - width / 2 <= maxX) {
				return true;
			}
		}
		return false;
	}

	private boolean supportsRailwayStation(int district) {
		return district == DISTRICT_NETWORK
				|| district == DISTRICT_DEAD_END
				|| district == DISTRICT_DENSE_WALL
				|| district == DISTRICT_INDUSTRIAL_WALL
				|| district == DISTRICT_MACHINE_NAVE
				|| district == DISTRICT_CONDUIT_BASILICA;
	}

	private BlockState riftSuspensionBridgeState(int x, int y, int z) {
		int cell = settings.motifCellSize();
		int stripe = MegastructureMath.floorDiv(x, cell);
		long riftHash = riftStripeHash(stripe);
		if (!isAcceptedRiftStripe(stripe, riftHash)) {
			return null;
		}
		int bridgeBand = MegastructureMath.floorDiv(z, 160);
		long hash = MegastructureMath.hash(activeWorldVariantSeed, stripe, bridgeBand, 211);
		if (Math.floorMod(hash, 4) == 0) {
			return null;
		}

		int localZ = MegastructureMath.floorMod(z, 160);
		int bridgeZ = MegastructureMath.range(hash >>> 8, 32, 128);
		int bridgeY = settings.floorY() + MegastructureMath.range(hash >>> 16, 112, Math.max(176, getWorldHeight() - 160));
		int dz = localZ - bridgeZ;
		int centerX = stripe * cell + cell / 2;
		int width = MegastructureMath.range(
				riftHash >>> 12,
				Math.min(settings.riftMinWidth(), settings.riftMaxWidth()),
				Math.max(settings.riftMinWidth(), settings.riftMaxWidth())
		);
		int spanHalf = Math.max(18, width / 2);
		int distFromCenter = Math.abs(x - centerX);
		if (distFromCenter > spanHalf + 3) {
			return null;
		}

		if (y == bridgeY) {
			if (Math.abs(dz) == 3) {
				return BlockPalette.FOUNDATION;
			}
			if (Math.abs(dz) <= 2) {
				return Math.floorMod(x + z + (int) hash, 9) == 0 ? BlockPalette.GRATE : BlockPalette.WALKWAY;
			}
		}

		boolean lampPost = Math.floorMod(x + (int) (hash >>> 24), 16) == 0;
		boolean towerPier = Math.floorMod(x + (int) (hash >>> 32), 32) == 0;
		if (lampPost && Math.abs(dz) == 4 && y >= bridgeY + 1 && y <= bridgeY + 4) {
			return y == bridgeY + 4 ? BlockPalette.LAMP : BlockPalette.PIPE;
		}
		if (lampPost && Math.abs(dz) >= 4 && Math.abs(dz) <= 6 && y == bridgeY + 3) {
			return Math.abs(dz) == 6 ? BlockPalette.LAMP : BlockPalette.RUST_PIPE;
		}
		if (y == bridgeY + 1 && Math.abs(dz) == 4) {
			return lampPost ? BlockPalette.LAMP : BlockPalette.FOUNDATION;
		}
		if (y == bridgeY + 2 && Math.abs(dz) == 4) {
			return BlockPalette.PIPE;
		}
		if (y == bridgeY - 1 && Math.abs(dz) <= 3 && Math.floorMod(x + (int) hash, 8) == 0) {
			return BlockPalette.FOUNDATION;
		}
		if (towerPier && Math.abs(dz) == 3 && y < bridgeY && y >= bridgeY - 30) {
			return Math.floorMod(bridgeY - y, 7) <= 1 ? BlockPalette.RUST_PIPE : BlockPalette.FOUNDATION;
		}
		if (towerPier && y == bridgeY + 6 && Math.abs(dz) <= 7) {
			return Math.abs(dz) <= 1 ? BlockPalette.LAMP : BlockPalette.FOUNDATION;
		}

		int cableBaseY = bridgeY + 16;
		int cableRise = Math.floorDiv(distFromCenter * 22, spanHalf);
		int cableY = cableBaseY + cableRise;
		if ((Math.abs(dz) == 6 || Math.abs(dz) == 9) && Math.abs(y - cableY) <= 1) {
			return BlockPalette.PIPE;
		}
		boolean hanger = Math.floorMod(x + (int) (hash >>> 20), 12) == 0;
		if (hanger && (Math.abs(dz) == 4 || Math.abs(dz) == 6) && y >= bridgeY + 2 && y < cableY) {
			return BlockPalette.PIPE;
		}
		boolean diagonalBrace = Math.floorMod(x + (int) (hash >>> 36), 24) <= 2;
		if (diagonalBrace && y > bridgeY + 2 && y < cableY
				&& Math.abs(Math.abs(dz) - (4 + Math.floorDiv(y - bridgeY, 8))) <= 0) {
			return BlockPalette.PIPE;
		}
		return null;
	}

	private boolean isCellCorridor(int x, int y, int z) {
		int cell = settings.cellSize();
		int localX = MegastructureMath.floorMod(x, cell);
		int localZ = MegastructureMath.floorMod(z, cell);
		int districtLocalX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int districtLocalZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		if (districtLocalX < 8 || districtLocalX > DISTRICT_SIZE - 9 || districtLocalZ < 8 || districtLocalZ > DISTRICT_SIZE - 9) {
			return false;
		}
		int floor = MegastructureMath.floorDiv(y - settings.floorY(), 36);
		int bandY = settings.floorY() + 10 + floor * 36;
		boolean yBand = y >= bandY && y <= bandY + 4;
		boolean corridorX = Math.abs(localZ - cell / 2) <= 3;
		boolean corridorZ = Math.abs(localX - cell / 2) <= 3;
		boolean sideHallX = (Math.abs(localZ - 22) <= 2 || Math.abs(localZ - 74) <= 2);
		boolean sideHallZ = (Math.abs(localX - 22) <= 2 || Math.abs(localX - 74) <= 2);
		boolean deadEndGate = Math.floorMod(MegastructureMath.hash(SHAPE_SEED, MegastructureMath.floorDiv(x, cell), MegastructureMath.floorDiv(z, cell), floor), 17) != 0;
		boolean trunk = corridorX || corridorZ;
		boolean sideHall = deadEndGate && (sideHallX || sideHallZ);
		return yBand && (trunk || sideHall);
	}

	private boolean isApartmentCorridor(int x, int y, int z) {
		int floor = MegastructureMath.floorDiv(y - settings.floorY(), 18);
		int baseY = settings.floorY() + 6 + floor * 18;
		if (y < baseY || y > baseY + 4) {
			return false;
		}

		int districtLocalX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int districtLocalZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		if (districtLocalX < 8 || districtLocalX > DISTRICT_SIZE - 9 || districtLocalZ < 8 || districtLocalZ > DISTRICT_SIZE - 9) {
			return false;
		}

		int cell = 72;
		int localX = MegastructureMath.floorMod(x, cell);
		int localZ = MegastructureMath.floorMod(z, cell);
		boolean trunkX = Math.abs(localZ - 36) <= 2;
		boolean trunkZ = Math.abs(localX - 36) <= 2;
		boolean sideX = (Math.abs(localZ - 18) <= 1 || Math.abs(localZ - 54) <= 1) && Math.floorMod(floor, 4) != 2;
		boolean sideZ = (Math.abs(localX - 18) <= 1 || Math.abs(localX - 54) <= 1) && Math.floorMod(floor, 4) == 2;
		return trunkX || trunkZ || sideX || sideZ;
	}

	private boolean isApartmentRoom(int x, int y, int z) {
		int floor = MegastructureMath.floorDiv(y - settings.floorY(), 18);
		int baseY = settings.floorY() + 6 + floor * 18;
		if (y < baseY || y > baseY + 4) {
			return false;
		}

		int module = 72;
		int districtLocalX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int districtLocalZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		if (districtLocalX < 64 || districtLocalX > DISTRICT_SIZE - 65 || districtLocalZ < 64 || districtLocalZ > DISTRICT_SIZE - 65) {
			return false;
		}

		int moduleX = MegastructureMath.floorDiv(x, module);
		int moduleZ = MegastructureMath.floorDiv(z, module);
		int localX = MegastructureMath.floorMod(x, module);
		int localZ = MegastructureMath.floorMod(z, module);
		if (Math.abs(localX - 36) <= 5 && Math.abs(localZ - 36) <= 5) {
			return false;
		}

		boolean northRoom = localZ >= 22 && localZ <= 31 && isRoomSlot(localX);
		boolean northDoorway = localZ >= 32 && localZ <= 33 && isDoorSlot(localX);
		boolean southRoom = localZ >= 41 && localZ <= 50 && isRoomSlot(localX);
		boolean southDoorway = localZ >= 39 && localZ <= 40 && isDoorSlot(localX);
		boolean westRoom = localX >= 22 && localX <= 31 && isRoomSlot(localZ);
		boolean westDoorway = localX >= 32 && localX <= 33 && isDoorSlot(localZ);
		boolean eastRoom = localX >= 41 && localX <= 50 && isRoomSlot(localZ);
		boolean eastDoorway = localX >= 39 && localX <= 40 && isDoorSlot(localZ);
		if (!(northRoom || northDoorway || southRoom || southDoorway || westRoom || westDoorway || eastRoom || eastDoorway)) {
			return false;
		}

		int roomGridX = MegastructureMath.floorDiv(x, 18);
		int roomGridZ = MegastructureMath.floorDiv(z, 18);
		long hash = MegastructureMath.hash(SHAPE_SEED, roomGridX + moduleX * 7, roomGridZ + moduleZ * 11, floor);
		return Math.floorMod(hash, 9) != 0;
	}

	private boolean isRoomSlot(int local) {
		return (local >= 6 && local <= 16)
				|| (local >= 20 && local <= 30)
				|| (local >= 42 && local <= 52)
				|| (local >= 56 && local <= 66);
	}

	private boolean isDoorSlot(int local) {
		return (local >= 10 && local <= 12)
				|| (local >= 24 && local <= 26)
				|| (local >= 46 && local <= 48)
				|| (local >= 60 && local <= 62);
	}

	private boolean isServiceShaft(int x, int y, int z) {
		int cell = 96;
		int localX = MegastructureMath.floorMod(x, cell);
		int localZ = MegastructureMath.floorMod(z, cell);
		boolean shaft = localX >= 8 && localX <= 23 && localZ >= 8 && localZ <= 23;
		boolean elevatorLike = localX >= 72 && localX <= 87 && localZ >= 10 && localZ <= 25;
		return shaft || elevatorLike;
	}

	private boolean isLocalAtrium(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int floor = MegastructureMath.floorDiv(y - settings.floorY(), 144);
		int baseY = settings.floorY() + 42 + floor * 144;
		boolean verticalRange = y >= baseY && y <= baseY + 48;
		return verticalRange && dx * dx + dz * dz <= 42 * 42;
	}

	private boolean isDeadEndCorridor(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int floor = MegastructureMath.floorDiv(y - settings.floorY(), 54);
		int baseY = settings.floorY() + 24 + floor * 54;
		if (y < baseY || y > baseY + 5) {
			return false;
		}

		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, floor + 701);
		boolean horizontal = Math.floorMod(hash, 2) == 0;
		int side = MegastructureMath.range(hash >>> 8, 46, DISTRICT_SIZE - 46);
		if (horizontal) {
			boolean main = Math.abs(localZ - side) <= 3;
			boolean centerTie = Math.abs(localX - DISTRICT_SIZE / 2) <= 3
					&& between(localZ, side, DISTRICT_SIZE / 2);
			boolean edgeMouth = (localX <= 12 || localX >= DISTRICT_SIZE - 13)
					&& Math.abs(localZ - side) <= 5;
			return main || centerTie || edgeMouth;
		}
		boolean main = Math.abs(localX - side) <= 3;
		boolean centerTie = Math.abs(localZ - DISTRICT_SIZE / 2) <= 3
				&& between(localX, side, DISTRICT_SIZE / 2);
		boolean edgeMouth = (localZ <= 12 || localZ >= DISTRICT_SIZE - 13)
				&& Math.abs(localX - side) <= 5;
		return main || centerTie || edgeMouth;
	}

	private boolean isMonolithHallVoid(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 811);
		int baseY = settings.floorY() + MegastructureMath.range(hash, 96, 624);
		boolean hall = y >= baseY - 4 && y <= baseY + 140
				&& Math.abs(dx) <= 160
				&& Math.abs(dz) <= 142;
		if (!hall) {
			return false;
		}
		boolean centralColumn = Math.abs(dx) <= 13 && Math.abs(dz) <= 13 && y <= baseY + 116;
		return !centralColumn;
	}

	private boolean isColumnForestVoid(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 823);
		int baseY = settings.floorY() + MegastructureMath.range(hash, 64, 704);
		boolean chamber = y >= baseY && y <= baseY + 96 && Math.abs(dx) <= 150 && Math.abs(dz) <= 150;
		if (!chamber) {
			return false;
		}
		int gridX = Math.floorMod(localX + MegastructureMath.range(hash >>> 8, 0, 17), 36);
		int gridZ = Math.floorMod(localZ + MegastructureMath.range(hash >>> 16, 0, 17), 36);
		boolean thinColumn = (gridX <= 2 || gridX >= 34) && (gridZ <= 2 || gridZ >= 34);
		boolean diagonalBeam = Math.floorMod(y - baseY, 23) <= 1 && Math.abs(gridX - gridZ) <= 1;
		return !(thinColumn || diagonalBeam);
	}

	private boolean isDistrictCylinderVoid(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int radius = 118;
		return dx * dx + dz * dz <= radius * radius;
	}

	private boolean isDistrictAbyssVoid(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int radius = 132;
		return dx * dx + dz * dz <= radius * radius;
	}

	private boolean isDescentWellVoid(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int radius = 34;
		boolean well = dx * dx + dz * dz <= radius * radius;
		boolean accessSlot = Math.abs(dx) <= 4 && dz >= -150 && dz <= -radius;
		return well || accessSlot;
	}

	private boolean isBlockTowerVoid(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int radius = 500;
		return y >= settings.floorY() + 12
				&& y <= settings.ceilingY() - 28
				&& dx * dx + dz * dz <= radius * radius;
	}

	private boolean isTankClusterVoid(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 941);
		int baseY = settings.floorY() + MegastructureMath.range(hash, 54, 612);
		if (y < baseY || y > baseY + 188 || Math.abs(dx) > 168 || Math.abs(dz) > 168) {
			return false;
		}
		return dx * dx + dz * dz <= 170 * 170;
	}

	private boolean isScaffoldVoid(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 947);
		int baseY = settings.floorY() + MegastructureMath.range(hash, 48, 650);
		return y >= baseY && y <= baseY + 176 && Math.abs(dx) <= 152 && Math.abs(dz) <= 152;
	}

	private boolean isIndustrialWallVoid(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 1301);
		boolean horizontal = Math.floorMod(hash, 2) == 0;
		int baseY = settings.floorY() + MegastructureMath.range(hash >>> 8, 40, 220);
		int topY = Math.min(settings.ceilingY() - 32, baseY + MegastructureMath.range(hash >>> 16, 420, 780));
		if (y < baseY || y > topY) {
			return false;
		}
		boolean mainCut = horizontal
				? Math.abs(dz) <= 96 && Math.abs(dx) <= 430
				: Math.abs(dx) <= 96 && Math.abs(dz) <= 430;
		boolean deepServiceSlice = horizontal
				? Math.abs(dz + 148) <= 18 && Math.abs(dx) <= 360
				: Math.abs(dx + 148) <= 18 && Math.abs(dz) <= 360;
		return mainCut || deepServiceSlice;
	}

	private boolean isTransitNexusVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1401, 72, 420);
		boolean hall = y >= baseY - 4 && y <= Math.min(settings.ceilingY() - 24, baseY + 198)
				&& Math.abs(dx) <= 260 && Math.abs(dz) <= 196;
		boolean approachX = y >= baseY + 38 && y <= baseY + 47 && Math.abs(dz) <= 9 && Math.abs(dx) <= 440;
		boolean approachZ = y >= baseY + 92 && y <= baseY + 101 && Math.abs(dx) <= 9 && Math.abs(dz) <= 420;
		return hall || approachX || approachZ;
	}

	private boolean isReactorCathedralVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1403, 48, 360);
		int topY = Math.min(settings.ceilingY() - 24, baseY + 286);
		boolean nave = y >= baseY && y <= topY && Math.abs(dx) <= 174 && Math.abs(dz) <= 318;
		boolean transept = y >= baseY + 52 && y <= topY - 18 && Math.abs(dx) <= 278 && Math.abs(dz) <= 112;
		return nave || transept;
	}

	private boolean isHangingArchiveVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1409, 64, 340);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 20, baseY + 320)
				&& Math.abs(dx) <= 238 && Math.abs(dz) <= 238;
	}

	private boolean isVentilationCanyonVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		long hash = districtHash(x, z, 1423);
		boolean horizontal = Math.floorMod(hash, 2) == 0;
		int along = horizontal ? dx : dz;
		int depth = horizontal ? dz : dx;
		return y >= settings.floorY() + 28 && y <= settings.ceilingY() - 30
				&& Math.abs(along) <= 470 && Math.abs(depth) <= 72;
	}

	private boolean isInvertedPyramidVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1427, 72, 330);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 22, baseY + 310)
				&& Math.abs(dx) <= 276 && Math.abs(dz) <= 276;
	}

	private boolean isRingVaultVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1433, 52, 360);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 22, baseY + 330)
				&& dx * dx + dz * dz <= 246 * 246;
	}

	private boolean isMachineNaveVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1439, 70, 480);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 26, baseY + 178)
				&& Math.abs(dx) <= 372 && Math.abs(dz) <= 174;
	}

	private boolean isFracturedHabitatVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1447, 54, 380);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 24, baseY + 298)
				&& Math.abs(dx) <= 268 && Math.abs(dz) <= 238;
	}

	private boolean isConduitBasilicaVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1451, 60, 360);
		boolean nave = y >= baseY && y <= Math.min(settings.ceilingY() - 22, baseY + 288)
				&& Math.abs(dx) <= 190 && Math.abs(dz) <= 364;
		boolean sideGallery = y >= baseY + 34 && y <= baseY + 94 && Math.abs(dx) <= 292 && Math.abs(dz) <= 238;
		return nave || sideGallery;
	}

	private boolean isReservoirHallVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1459, 48, 420);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 26, baseY + 246)
				&& dx * dx + dz * dz <= 276 * 276;
	}

	private boolean isSuspendedCityVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1463, 52, 330);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 24, baseY + 360)
				&& Math.abs(dx) <= 330 && Math.abs(dz) <= 270;
	}

	private boolean isIrisChasmVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		long hash = districtHash(x, z, 1469);
		boolean horizontal = Math.floorMod(hash, 2) == 0;
		int along = horizontal ? dx : dz;
		int depth = horizontal ? dz : dx;
		int baseY = districtBaseY(x, z, 1469, 36, 260);
		return y >= baseY && y <= settings.ceilingY() - 28
				&& Math.abs(along) <= 442 && Math.abs(depth) <= 148;
	}

	private boolean isMachineRootVaultVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1475, 46, 320);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 24, baseY + 390)
				&& dx * dx + dz * dz <= 280 * 280;
	}

	private boolean isTiltedStacksVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1481, 58, 350);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 24, baseY + 304)
				&& Math.abs(dx) <= 312 && Math.abs(dz) <= 274;
	}

	private boolean isSilentFoundryVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1487, 54, 360);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 26, baseY + 232)
				&& Math.abs(dx) <= 404 && Math.abs(dz) <= 238;
	}

	private boolean isColossusLiftVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1493, 42, 280);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 22, baseY + 430)
				&& Math.abs(dx) <= 276 && Math.abs(dz) <= 276;
	}

	private boolean isFoldedCityVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1499, 56, 360);
		return y >= baseY && y <= Math.min(settings.ceilingY() - 24, baseY + 320)
				&& Math.abs(dx) <= 352 && Math.abs(dz) <= 292;
	}

	private boolean isUpperRimCityVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1505, 42, 300);
		int topY = Math.min(settings.ceilingY() - 18, baseY + 500);
		long dist2 = (long) dx * dx + (long) dz * dz;
		return y >= baseY && y <= topY && dist2 <= 470L * 470L;
	}

	private boolean isOrbitalWebCoreVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1511, 60, 360);
		int topY = Math.min(settings.ceilingY() - 20, baseY + 380);
		long dist2 = (long) dx * dx + (long) dz * dz;
		return y >= baseY && y <= topY && dist2 <= 360L * 360L;
	}

	private boolean isCrownSpireVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1521, 34, 240);
		long dist2 = (long) dx * dx + (long) dz * dz;
		return y >= baseY && y <= settings.ceilingY() - 18 && dist2 <= 486L * 486L;
	}

	private boolean isGlobeMonumentVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1527, 60, 320);
		long dist2 = (long) dx * dx + (long) dz * dz;
		return y >= baseY && y <= Math.min(settings.ceilingY() - 20, baseY + 430) && dist2 <= 360L * 360L;
	}

	private boolean isVoidAltarVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1531, 52, 340);
		long dist2 = (long) dx * dx + (long) dz * dz;
		return y >= baseY && y <= Math.min(settings.ceilingY() - 18, baseY + 360) && dist2 <= 340L * 340L;
	}

	private boolean isAtomStormArrayVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1537, 48, 320);
		long dist2 = (long) dx * dx + (long) dz * dz;
		return y >= baseY && y <= Math.min(settings.ceilingY() - 20, baseY + 330) && dist2 <= 390L * 390L;
	}

	private boolean isBlackHoleReactorVoid(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1543, 54, 300);
		long dist2 = (long) dx * dx + (long) dz * dz;
		return y >= baseY && y <= Math.min(settings.ceilingY() - 18, baseY + 420) && dist2 <= 430L * 430L;
	}

	private int districtLocalX(int x) {
		return MegastructureMath.floorMod(x, DISTRICT_SIZE);
	}

	private int districtLocalZ(int z) {
		return MegastructureMath.floorMod(z, DISTRICT_SIZE);
	}

	private long districtHash(int x, int z, int salt) {
		return MegastructureMath.hash(activeWorldVariantSeed, MegastructureMath.floorDiv(x, DISTRICT_SIZE), MegastructureMath.floorDiv(z, DISTRICT_SIZE), salt);
	}

	private int districtBaseY(int x, int z, int salt, int minOffset, int maxOffset) {
		int safeMaximum = Math.min(maxOffset, Math.max(minOffset, getWorldHeight() - 360));
		return settings.floorY() + MegastructureMath.range(districtHash(x, z, salt), minOffset, safeMaximum);
	}

	private boolean isSparseWallCorridor(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		int level = MegastructureMath.floorDiv(y - settings.floorY(), 96);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 901 + level);
		if (Math.floorMod(hash, 3) != 0) {
			return false;
		}
		int baseY = settings.floorY() + 38 + level * 96;
		if (y < baseY || y > baseY + 4) {
			return false;
		}
		int lane = MegastructureMath.range(hash >>> 9, 64, DISTRICT_SIZE - 64);
		int crossLane = MegastructureMath.range(hash >>> 21, 64, DISTRICT_SIZE - 64);
		return Math.abs(localX - lane) <= 3 || Math.abs(localZ - crossLane) <= 3;
	}

	private BlockState structuralOverlay(int district, int x, int y, int z, boolean air) {
		BlockState ruin = ruinOverlayState(district, x, y, z, air);
		if (ruin != null) {
			return ruin;
		}
		BlockState districtStructure = districtStructureState(district, x, y, z);
		if (districtStructure != null) {
			return districtStructure;
		}

		BlockState stair = stairState(district, x, y, z);
		if (stair != null) {
			return stair;
		}

		BlockState corridor = corridorDetailState(district, x, y, z, air);
		if (corridor != null) {
			return corridor;
		}

		BlockState facade = facadeState(x, y, z, air);
		if (facade != null) {
			return facade;
		}

		if (air && district == DISTRICT_COLUMN_FOREST) {
			BlockState web = webColumnState(x, y, z);
			if (web != null) {
				return web;
			}
		}

		return null;
	}

	private BlockState ruinOverlayState(int district, int x, int y, int z, boolean air) {
		if (district == DISTRICT_NETWORK || district == DISTRICT_DEAD_END || district == DISTRICT_DENSE_WALL) {
			return null;
		}
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		if (districtX == 0 && districtZ == 0) {
			return null;
		}
		int centerX = districtX * DISTRICT_SIZE + DISTRICT_SIZE / 2;
		int centerZ = districtZ * DISTRICT_SIZE + DISTRICT_SIZE / 2;
		long districtSeed = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 2003);
		int baseY = ruinBaseY(district, centerX, centerZ);
		if (y < baseY - 224 || y > baseY + 224
				|| isRailwayLineAt(x, z) || isConnectorNetworkVolumeAt(x, y, z, 7)) {
			return null;
		}
		for (int site = 0; site < 5; site++) {
			long siteHash = MegastructureMath.hash(districtSeed, site, 0, 2011);
			if (site > 1 && Math.floorMod(siteHash, 5) == 0) {
				continue;
			}
			int ruinX = centerX + MegastructureMath.range(siteHash >>> 8, -214, 214);
			int ruinZ = centerZ + MegastructureMath.range(siteHash >>> 20, -190, 190);
			int proposedY = baseY + (site == 0 ? 0 : MegastructureMath.range(siteHash >>> 32, -2, 4) * 16);
			long groundKey = MegastructureMath.hash(districtSeed, site, 0, 2029);
			int ruinY = ruinGroundCache.computeIfAbsent(
					groundKey,
					ignored -> findRuinGroundY(district, ruinX, ruinZ, proposedY)
			);
			if (ruinY == Integer.MIN_VALUE) {
				continue;
			}
			int dx = x - ruinX;
			int dz = z - ruinZ;
			int dy = y - ruinY;
			boolean xAxis = Math.floorMod(siteHash >>> 44, 2) == 0;
			int type = Math.floorMod((int) (siteHash >>> 48), 30);
			int horizontalReach = ruinHorizontalReach(type);
			int verticalReach = ruinVerticalReach(type);
			if (Math.abs(dx) > horizontalReach || Math.abs(dz) > horizontalReach || dy < -8 || dy > verticalReach) {
				continue;
			}
			int along = xAxis ? dx : dz;
			int cross = xAxis ? dz : dx;
			BlockState impact = ruinImpactState(district, x, y, z, along, cross, dy, siteHash);
			if (impact != null) {
				return impact;
			}
			BlockState state = ruinMotifState(type, along, cross, dy, siteHash, xAxis, air);
			if (state != null) {
				return state;
			}
		}
		return null;
	}

	private int ruinHorizontalReach(int type) {
		return switch (type) {
			case 20, 21, 26 -> 128;
			case 22, 23, 24, 27, 29 -> 104;
			case 25, 28 -> 92;
			default -> 72;
		};
	}

	private int ruinVerticalReach(int type) {
		return switch (type) {
			case 20, 21, 22, 26 -> 156;
			case 23, 24, 27, 29 -> 132;
			case 25, 28 -> 118;
			default -> 112;
		};
	}

	private int findRuinGroundY(int district, int x, int z, int proposedY) {
		int top = Math.min(settings.ceilingY() - 12, proposedY + 56);
		int bottom = Math.max(settings.floorY() + 1, proposedY - 224);
		for (int y = top; y >= bottom; y--) {
			int support = 0;
			int[][] samples = {{0, 0}, {16, 0}, {-16, 0}, {0, 16}, {0, -16}};
			for (int[] sample : samples) {
				int sampleX = x + sample[0];
				int sampleZ = z + sample[1];
				if (baseStructureSolidAt(district, sampleX, y, sampleZ)
						&& !baseStructureSolidAt(district, sampleX, y + 1, sampleZ)) {
					support++;
				}
			}
			if (support == samples.length) {
				return y;
			}
		}
		return Integer.MIN_VALUE;
	}

	private boolean baseStructureSolidAt(int district, int x, int y, int z) {
		if (y < settings.floorY() || y > settings.ceilingY()) {
			return false;
		}
		boolean air = isRailwayAir(x, y, z) || districtAir(district, x, y, z);
		BlockState districtStructure = districtStructureState(district, x, y, z);
		if (districtStructure != null) {
			return !districtStructure.isAir();
		}
		BlockState stair = stairState(district, x, y, z);
		if (stair != null) {
			return !stair.isAir();
		}
		BlockState corridor = corridorDetailState(district, x, y, z, air);
		if (corridor != null) {
			return !corridor.isAir();
		}
		BlockState facade = facadeState(x, y, z, air);
		if (facade != null) {
			return !facade.isAir();
		}
		if (air && district == DISTRICT_COLUMN_FOREST) {
			BlockState web = webColumnState(x, y, z);
			if (web != null) {
				return !web.isAir();
			}
		}
		return !air;
	}

	private BlockState ruinImpactState(
			int district,
			int x,
			int y,
			int z,
			int along,
			int cross,
			int dy,
			long hash
	) {
		int radius = MegastructureMath.range(hash >>> 22, 8, 17) + 10;
		int distance2 = along * along + cross * cross;
		if (distance2 > radius * radius) {
			return null;
		}
		long fracture = MegastructureMath.hash(hash, along, cross, 2039);
		boolean radialCrack = Math.abs(cross * 3 - along) <= 1
				|| Math.abs(cross * 2 + along) <= 1
				|| Math.abs(along * 3 + cross) <= 1;
		if (dy == 0 && baseStructureSolidAt(district, x, y, z)
				&& (radialCrack || Math.floorMod(fracture, 13) <= 1)) {
			return BlockPalette.CRACKED_PANEL;
		}
		if (dy == 1 && baseStructureSolidAt(district, x, y - 1, z)
				&& distance2 >= 16 && Math.floorMod(fracture >>> 12, 19) == 0) {
			return ruinMaterial(hash, along, cross, dy);
		}
		return null;
	}

	private int ruinBaseY(int district, int x, int z) {
		return switch (district) {
			case DISTRICT_MONOLITH_HALL -> oasisHostFloorY(district, x, z);
			case DISTRICT_COLUMN_FOREST -> oasisHostFloorY(district, x, z);
			case DISTRICT_CYLINDER, DISTRICT_ABYSS, DISTRICT_DESCENT -> connectorCenterY(
					MegastructureMath.floorDiv(x, DISTRICT_SIZE), MegastructureMath.floorDiv(z, DISTRICT_SIZE));
			case DISTRICT_BLOCK_TOWERS -> settings.floorY() + 18;
			case DISTRICT_TANK_CLUSTER -> oasisHostFloorY(district, x, z);
			case DISTRICT_SCAFFOLD -> oasisHostFloorY(district, x, z);
			case DISTRICT_INDUSTRIAL_WALL -> oasisHostFloorY(district, x, z);
			case DISTRICT_TRANSIT_NEXUS -> districtBaseY(x, z, 1401, 72, 420) + 38;
			case DISTRICT_REACTOR_CATHEDRAL -> districtBaseY(x, z, 1403, 48, 360);
			case DISTRICT_HANGING_ARCHIVE -> districtBaseY(x, z, 1409, 64, 340);
			case DISTRICT_VENTILATION_CANYON -> districtBaseY(x, z, 1423, 36, 260);
			case DISTRICT_INVERTED_PYRAMID -> districtBaseY(x, z, 1427, 72, 330);
			case DISTRICT_RING_VAULT -> districtBaseY(x, z, 1433, 52, 360);
			case DISTRICT_MACHINE_NAVE -> districtBaseY(x, z, 1439, 70, 480);
			case DISTRICT_FRACTURED_HABITAT -> districtBaseY(x, z, 1447, 54, 380);
			case DISTRICT_CONDUIT_BASILICA -> districtBaseY(x, z, 1451, 60, 360);
			case DISTRICT_RESERVOIR_HALL -> districtBaseY(x, z, 1459, 48, 420);
			case DISTRICT_SUSPENDED_CITY -> districtBaseY(x, z, 1463, 52, 330);
			case DISTRICT_IRIS_CHASM -> districtBaseY(x, z, 1469, 36, 260);
			case DISTRICT_MACHINE_ROOT_VAULT -> districtBaseY(x, z, 1475, 46, 320);
			case DISTRICT_TILTED_STACKS -> districtBaseY(x, z, 1481, 58, 350);
			case DISTRICT_SILENT_FOUNDRY -> districtBaseY(x, z, 1487, 54, 360);
			case DISTRICT_COLOSSUS_LIFT -> districtBaseY(x, z, 1493, 42, 280);
			case DISTRICT_FOLDED_CITY -> districtBaseY(x, z, 1499, 56, 360);
			case DISTRICT_UPPER_RIM_CITY -> districtBaseY(x, z, 1505, 42, 300);
			case DISTRICT_ORBITAL_WEB_CORE -> districtBaseY(x, z, 1511, 60, 360);
			default -> connectorNetworkY();
		};
	}

	private BlockState ruinMotifState(
			int type,
			int along,
			int cross,
			int dy,
			long hash,
			boolean xAxis,
			boolean air
	) {
		int width = MegastructureMath.range(hash >>> 6, 18, 34);
		int height = MegastructureMath.range(hash >>> 14, 22, 52);
		int radius = MegastructureMath.range(hash >>> 22, 8, 17);
		BlockState debris = ruinMaterial(hash, along, cross, dy);
		int distance2 = along * along + cross * cross;
		int pileHeight = Math.max(0, (radius * radius - distance2) / Math.max(12, radius * 5));
		boolean rubblePile = dy >= 1 && dy <= pileHeight
				&& Math.floorMod(MegastructureMath.hash(hash, along, cross, dy), 7) != 0;
		return switch (type) {
			case 0 -> {
				boolean fallenSlab = dy == 2 + Math.floorDiv(along + width, 7)
						&& Math.abs(along) <= width && Math.abs(cross) <= width / 2;
				yield fallenSlab || rubblePile ? debris : null;
			}
			case 1 -> {
				boolean column = Math.abs(along) <= width && cross * cross + (dy - 4) * (dy - 4) <= radius * radius;
				yield column && Math.floorMod(along + width, 17) > 2 ? debris : rubblePile ? debris : null;
			}
			case 2 -> {
				int columnCenter = Math.floorDiv(dy * 3, 2) - width;
				boolean leaning = dy >= 1 && dy <= height && Math.abs(along - columnCenter) <= 3 && Math.abs(cross) <= 3;
				yield leaning || rubblePile ? debris : null;
			}
			case 3 -> {
				boolean beam = Math.abs(along) <= width && Math.abs(cross) <= 2
						&& Math.abs(dy - 4 - Math.abs(along) / 8) <= 1;
				boolean crossBeam = Math.abs(cross) <= width / 2 && Math.abs(along) <= 2 && dy >= 3 && dy <= 5;
				yield beam || crossBeam ? BlockPalette.FOUNDATION : rubblePile ? debris : null;
			}
			case 4 -> {
				boolean brokenSpan = Math.abs(along) <= width && Math.abs(cross) <= 4
						&& dy == 8 - Math.abs(along) / 5 && Math.abs(along) > 5;
				yield brokenSpan ? BlockPalette.WALKWAY : rubblePile ? debris : null;
			}
			case 5 -> {
				boolean wallPlane = Math.abs(cross) <= 1 && Math.abs(along) <= width && dy >= 1 && dy <= height;
				boolean breach = along * along + (dy - height / 2) * (dy - height / 2) <= radius * radius;
				yield wallPlane ? (breach ? BlockPalette.AIR : debris) : rubblePile ? debris : null;
			}
			case 6 -> {
				boolean plate = Math.abs(along) <= width && Math.abs(cross) <= width
						&& (dy == 2 || dy == 7 || dy == 12) && Math.floorMod(along - cross + dy, 13) > 2;
				yield plate ? BlockPalette.CRACKED_PANEL : rubblePile ? debris : null;
			}
			case 7 -> {
				int step = Math.floorDiv(along + width, 3);
				boolean stair = Math.abs(along) <= width && Math.abs(cross) <= 3 && dy == 1 + step
						&& Math.floorMod(step, 7) != 3;
				yield stair ? BlockPalette.stairs(xAxis ? Direction.EAST : Direction.SOUTH) : rubblePile ? debris : null;
			}
			case 8 -> {
				boolean pipe = Math.abs(along) <= width && Math.abs(cross) <= 2 && Math.abs(dy - 12) <= 2;
				boolean riser = Math.abs(along + width / 2) <= 2 && Math.abs(cross) <= 2 && dy >= 1 && dy <= 12;
				yield (pipe && Math.floorMod(along, 11) > 2) || riser ? BlockPalette.RUST_PIPE : rubblePile ? debris : null;
			}
			case 9 -> {
				int archMetric = cross * cross + (dy - radius) * (dy - radius);
				boolean arch = Math.abs(along) <= 3 && archMetric >= (radius - 3) * (radius - 3)
						&& archMetric <= (radius + 2) * (radius + 2) && dy >= 1;
				yield arch && !(cross > 0 && dy > radius) ? debris : rubblePile ? debris : null;
			}
			case 10 -> {
				boolean fallenTooth = Math.abs(along) <= width && Math.abs(cross) <= width / 2
						&& dy >= 1 && dy <= 3 + Math.floorMod(Math.abs(along) + cross, 5)
						&& Math.floorMod(along * 3 + cross, 11) > 2;
				yield fallenTooth || rubblePile ? BlockPalette.CRACKED_PANEL : null;
			}
			case 11 -> {
				boolean stump = Math.abs(along) <= 4 && Math.abs(cross) <= 4 && dy >= 1 && dy <= height / 2;
				int fallenCenter = dy + 4;
				boolean fallen = along >= 4 && along <= width && Math.abs(along - fallenCenter) <= 3 && Math.abs(cross) <= 4;
				yield stump || fallen ? BlockPalette.FOUNDATION : rubblePile ? debris : null;
			}
			case 12 -> {
				boolean shell = Math.abs(along) <= width / 2 && Math.abs(cross) <= radius && dy >= 1 && dy <= radius * 2
						&& (Math.abs(along) >= width / 2 - 2 || Math.abs(cross) >= radius - 2 || dy <= 3 || dy >= radius * 2 - 2);
				yield shell && Math.floorMod(along + cross + dy, 11) != 0 ? BlockPalette.WALL_PANEL : null;
			}
			case 13 -> {
				boolean backWall = Math.abs(along + width / 2) <= 2 && Math.abs(cross) <= width / 2 && dy <= height;
				boolean balcony = along >= -width / 2 && along <= width / 2 && Math.abs(cross) <= width / 2
						&& dy == Math.max(1, 9 - Math.max(0, along) / 4);
				yield backWall ? BlockPalette.WALL_PANEL : balcony ? BlockPalette.WALKWAY : rubblePile ? debris : null;
			}
			case 14 -> {
				int ring2 = along * along + cross * cross;
				boolean ring = dy >= 1 && dy <= 3 && ring2 >= (width - 3) * (width - 3) && ring2 <= (width + 3) * (width + 3);
				yield ring && !(along > 0 && cross > 0) ? BlockPalette.FOUNDATION : rubblePile ? debris : null;
			}
			case 15 -> rubblePile || (dy == 1 && distance2 <= (radius + 8) * (radius + 8)
					&& Math.floorMod(MegastructureMath.hash(hash, along, cross, 15), 5) == 0) ? debris : null;
			case 16 -> {
				boolean panelStack = Math.abs(along) <= width && Math.abs(cross) <= width / 2
						&& (dy == 2 + Math.floorDiv(along + width, 9)
						|| dy == 5 + Math.floorDiv(cross + width / 2, 7));
				yield panelStack ? BlockPalette.WALL_PANEL : rubblePile ? debris : null;
			}
			case 17 -> {
				boolean supports = Math.floorMod(along + width, 12) <= 2 && Math.abs(cross) <= width / 2
						&& dy >= 1 && dy <= height - Math.floorMod(along * 5, 17);
				yield supports && Math.floorMod(along + cross, 9) != 0 ? BlockPalette.FOUNDATION : rubblePile ? debris : null;
			}
			case 18 -> {
				int craterRadius = radius + 5;
				boolean crater = dy >= -3 && dy <= 4 && distance2 <= craterRadius * craterRadius;
				int rim2 = (craterRadius + 5) * (craterRadius + 5);
				yield crater ? BlockPalette.AIR
						: dy == 1 && distance2 <= rim2 && distance2 >= craterRadius * craterRadius ? debris : null;
			}
			case 19 -> {
				boolean catwalk = Math.abs(along) <= width && Math.abs(cross) <= 2 && dy == 14
						&& !(Math.abs(along) <= 7 || Math.floorMod(along + width, 19) <= 3);
				boolean dropped = Math.abs(along) <= 7 && Math.abs(cross) <= 2 && dy == 3 + Math.abs(along) / 3;
				yield catwalk || dropped ? BlockPalette.WALKWAY : rubblePile ? debris : null;
			}
			case 20 -> {
				int length = 82 + Math.floorMod((int) (hash >>> 9), 36);
				int columnRadius = 7 + Math.floorMod((int) (hash >>> 15), 4);
				int sag = Math.max(0, Math.abs(along) / 18);
				boolean core = along >= -length / 3 && along <= length
						&& cross * cross + (dy - 6 - sag) * (dy - 6 - sag) <= columnRadius * columnRadius
						&& Math.floorMod(along + length, 23) > 2;
				boolean crushedBase = dy >= 1 && dy <= 4 && Math.abs(cross) <= columnRadius + 5
						&& along >= -length / 3 - 8 && along <= length + 12
						&& Math.floorMod(along * 5 + cross * 3, 11) != 0;
				yield core ? BlockPalette.FOUNDATION : crushedBase ? debris : rubblePile ? debris : null;
			}
			case 21 -> {
				int slabLength = 96 + Math.floorMod((int) (hash >>> 17), 28);
				int slabWidth = 18 + Math.floorMod((int) (hash >>> 25), 12);
				int tilt = Math.floorDiv(along + slabLength / 2, 13);
				boolean slab = along >= -slabLength / 2 && along <= slabLength / 2
						&& Math.abs(cross) <= slabWidth
						&& dy >= 2 + tilt && dy <= 4 + tilt
						&& Math.floorMod(along - cross + dy, 17) > 1;
				boolean undersideRubble = dy >= 1 && dy <= 3 && Math.abs(cross) <= slabWidth + 8
						&& Math.abs(along) <= slabLength / 2 + 10
						&& Math.floorMod(MegastructureMath.hash(hash, along, cross, 2101), 6) != 0;
				yield slab ? BlockPalette.CRACKED_PANEL : undersideRubble ? debris : null;
			}
			case 22 -> {
				int towerWidth = 20 + Math.floorMod((int) (hash >>> 12), 16);
				int towerHeight = 48 + Math.floorMod((int) (hash >>> 28), 46);
				int leanCenter = Math.floorDiv(dy * 5, 3) - towerHeight / 2;
				boolean facadeChunk = dy >= 1 && dy <= towerHeight
						&& Math.abs(along - leanCenter) <= 3
						&& Math.abs(cross) <= towerWidth
						&& Math.floorMod(cross + dy, 9) != 0;
				boolean footRubble = dy >= 1 && dy <= 6
						&& Math.abs(along + towerHeight / 2) <= towerWidth + 14
						&& Math.abs(cross) <= towerWidth + 10
						&& Math.floorMod(MegastructureMath.hash(hash, along, cross, dy), 5) != 0;
				yield facadeChunk ? BlockPalette.WALL_PANEL : footRubble ? debris : null;
			}
			case 23 -> {
				int pipeRadius = 10 + Math.floorMod((int) (hash >>> 14), 7);
				int arcCenter = width + 28;
				int arc = (along - arcCenter) * (along - arcCenter) + (dy - 16) * (dy - 16);
				boolean pipeCrescent = Math.abs(cross) <= 3
						&& arc >= (pipeRadius + 24) * (pipeRadius + 24)
						&& arc <= (pipeRadius + 31) * (pipeRadius + 31)
						&& dy >= 1 && along <= arcCenter;
				boolean brokenSegments = Math.abs(cross) <= 4 && dy >= 1 && dy <= 7
						&& Math.abs(along) <= arcCenter + pipeRadius
						&& Math.floorMod(along, 14) <= 5;
				yield pipeCrescent || brokenSegments ? BlockPalette.RUST_PIPE : rubblePile ? debris : null;
			}
			case 24 -> {
				int spineLength = 74 + Math.floorMod((int) (hash >>> 11), 28);
				int step = Math.floorDiv(along + spineLength / 2, 4);
				boolean stairSpine = along >= -spineLength / 2 && along <= spineLength / 2
						&& Math.abs(cross) <= 5
						&& dy == 1 + Math.max(0, step / 2)
						&& Math.floorMod(step, 9) != 4;
				boolean landingChunks = Math.abs(cross) <= 9 && Math.abs(along) <= spineLength / 2 + 8
						&& dy >= 1 && dy <= 3
						&& Math.floorMod(MegastructureMath.hash(hash, along, cross, 2401), 8) <= 1;
				yield stairSpine ? BlockPalette.stairs(xAxis ? Direction.EAST : Direction.SOUTH)
						: landingChunks ? BlockPalette.WALKWAY : rubblePile ? debris : null;
			}
			case 25 -> {
				int ribRadius = 24 + Math.floorMod((int) (hash >>> 18), 12);
				boolean rib = Math.abs(along) <= 5
						&& cross * cross + (dy - ribRadius) * (dy - ribRadius) >= (ribRadius - 3) * (ribRadius - 3)
						&& cross * cross + (dy - ribRadius) * (dy - ribRadius) <= (ribRadius + 3) * (ribRadius + 3)
						&& dy >= 1
						&& Math.floorMod(cross + dy, 11) > 1;
				boolean fallenRib = Math.abs(cross) <= 4 && Math.abs(along) <= ribRadius + 12
						&& dy >= 1 && dy <= 4 + Math.abs(along) / 12;
				yield rib ? BlockPalette.FOUNDATION : fallenRib ? debris : null;
			}
			case 26 -> {
				int machineLength = 92 + Math.floorMod((int) (hash >>> 19), 38);
				int machineWidth = 12 + Math.floorMod((int) (hash >>> 27), 9);
				boolean hull = Math.abs(along) <= machineLength / 2 && Math.abs(cross) <= machineWidth
						&& dy >= 1 && dy <= 14
						&& (dy <= 3 || Math.abs(cross) >= machineWidth - 2 || Math.floorMod(along, 17) <= 2);
				boolean scattered = dy >= 1 && dy <= 5
						&& Math.abs(along) <= machineLength / 2 + 20
						&& Math.abs(cross) <= machineWidth + 18
						&& Math.floorMod(MegastructureMath.hash(hash, along, cross, 2601), 7) <= 2;
				yield hull ? BlockPalette.DARK_STONE : scattered ? debris : null;
			}
			case 27 -> {
				int block = 26 + Math.floorMod((int) (hash >>> 16), 18);
				int slide = Math.max(0, Math.floorDiv(along + block, 9));
				boolean pancaked = Math.abs(along) <= block * 2 && Math.abs(cross) <= block
						&& dy >= 1 + slide && dy <= 3 + slide
						&& Math.floorMod(along + cross, 13) != 0;
				boolean sideBreak = Math.abs(along) <= block * 2 + 8 && Math.abs(cross) <= block + 8
						&& dy >= 1 && dy <= 6
						&& Math.floorMod(MegastructureMath.hash(hash, along, cross, 2701), 9) <= 1;
				yield pancaked ? BlockPalette.CRACKED_PANEL : sideBreak ? debris : null;
			}
			case 28 -> {
				int conduitHeight = 36 + Math.floorMod((int) (hash >>> 21), 52);
				boolean snappedBase = Math.abs(along) <= 5 && Math.abs(cross) <= 5 && dy >= 1 && dy <= conduitHeight / 3;
				boolean fallenConduit = along >= 2 && along <= conduitHeight
						&& Math.abs(cross) <= 3
						&& Math.abs(dy - 4 - along / 9) <= 2
						&& Math.floorMod(along, 16) > 2;
				yield snappedBase || fallenConduit ? BlockPalette.RUST_PIPE : rubblePile ? debris : null;
			}
			default -> {
				int run = 70 + Math.floorMod((int) (hash >>> 10), 34);
				int localPile = Math.max(0, 9 - Math.abs(cross) / 2 - Math.max(0, along) / 18);
				boolean cascade = along >= -16 && along <= run && dy >= 1 && dy <= localPile
						&& Math.floorMod(MegastructureMath.hash(hash, along, cross, dy), 6) != 0;
				boolean heavyShard = dy >= 2 && dy <= 8 && Math.abs(cross) <= 4
						&& along >= run / 3 && along <= run
						&& Math.floorMod(along + dy, 17) <= 5;
				yield heavyShard ? BlockPalette.FOUNDATION : cascade ? debris : null;
			}
		};
	}

	private BlockState ruinMaterial(long hash, int along, int cross, int dy) {
		int material = Math.floorMod((int) MegastructureMath.hash(hash, along, cross, dy), 7);
		return switch (material) {
			case 0 -> BlockPalette.CRACKED_PANEL;
			case 1 -> BlockPalette.DARK_STONE;
			case 2 -> BlockPalette.WALL_PANEL;
			default -> BlockPalette.FOUNDATION;
		};
	}

	private BlockState districtStructureState(int district, int x, int y, int z) {
		return switch (district) {
			case DISTRICT_MONOLITH_HALL -> monolithHallStructureState(x, y, z);
			case DISTRICT_CYLINDER -> districtCylinderStructureState(x, y, z);
			case DISTRICT_ABYSS -> districtAbyssStructureState(x, y, z);
			case DISTRICT_DESCENT -> descentWellStructureState(x, y, z);
			case DISTRICT_BLOCK_TOWERS -> blockTowerStructureState(x, y, z);
			case DISTRICT_TANK_CLUSTER -> tankClusterStructureState(x, y, z);
			case DISTRICT_SCAFFOLD -> scaffoldStructureState(x, y, z);
			case DISTRICT_INDUSTRIAL_WALL -> industrialWallStructureState(x, y, z);
			case DISTRICT_TRANSIT_NEXUS -> transitNexusStructureState(x, y, z);
			case DISTRICT_REACTOR_CATHEDRAL -> reactorCathedralStructureState(x, y, z);
			case DISTRICT_HANGING_ARCHIVE -> hangingArchiveStructureState(x, y, z);
			case DISTRICT_VENTILATION_CANYON -> ventilationCanyonStructureState(x, y, z);
			case DISTRICT_INVERTED_PYRAMID -> invertedPyramidStructureState(x, y, z);
			case DISTRICT_RING_VAULT -> ringVaultStructureState(x, y, z);
			case DISTRICT_MACHINE_NAVE -> machineNaveStructureState(x, y, z);
			case DISTRICT_FRACTURED_HABITAT -> fracturedHabitatStructureState(x, y, z);
			case DISTRICT_CONDUIT_BASILICA -> conduitBasilicaStructureState(x, y, z);
			case DISTRICT_RESERVOIR_HALL -> reservoirHallStructureState(x, y, z);
			case DISTRICT_SUSPENDED_CITY -> suspendedCityStructureState(x, y, z);
			case DISTRICT_IRIS_CHASM -> irisChasmStructureState(x, y, z);
			case DISTRICT_MACHINE_ROOT_VAULT -> machineRootVaultStructureState(x, y, z);
			case DISTRICT_TILTED_STACKS -> tiltedStacksStructureState(x, y, z);
			case DISTRICT_SILENT_FOUNDRY -> silentFoundryStructureState(x, y, z);
			case DISTRICT_COLOSSUS_LIFT -> colossusLiftStructureState(x, y, z);
			case DISTRICT_FOLDED_CITY -> foldedCityStructureState(x, y, z);
			case DISTRICT_UPPER_RIM_CITY -> upperRimCityStructureState(x, y, z);
			case DISTRICT_ORBITAL_WEB_CORE -> orbitalWebCoreStructureState(x, y, z);
			case DISTRICT_CROWN_SPIRE -> crownSpireStructureState(x, y, z);
			case DISTRICT_GLOBE_MONUMENT -> globeMonumentStructureState(x, y, z);
			case DISTRICT_VOID_ALTAR -> voidAltarStructureState(x, y, z);
			case DISTRICT_ATOM_STORM_ARRAY -> atomStormArrayStructureState(x, y, z);
			case DISTRICT_BLACK_HOLE_REACTOR -> blackHoleReactorStructureState(x, y, z);
			default -> null;
		};
	}

	private BlockState monolithHallStructureState(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 811);
		int baseY = settings.floorY() + MegastructureMath.range(hash, 96, 624);
		if (y < baseY || y > baseY + 132 || Math.abs(dx) > 150 || Math.abs(dz) > 132) {
			return null;
		}
		if (Math.abs(dx) <= 14 && Math.abs(dz) <= 14 && y <= baseY + 116) {
			return y > baseY + 104 && (Math.abs(dx) > 9 || Math.abs(dz) > 9) ? BlockPalette.CRACKED_PANEL : BlockPalette.FOUNDATION;
		}
		boolean brokenTop = y == baseY + 116 && Math.abs(dx) <= 28 && Math.abs(dz) <= 28 && Math.floorMod(dx + dz, 5) != 0;
		if (brokenTop) {
			return BlockPalette.WALKWAY;
		}
		boolean bridge = y == baseY + 38 && Math.abs(dz) <= 2 && dx >= -126 && dx <= -16;
		if (bridge) {
			return BlockPalette.WALKWAY;
		}
		boolean stairRibbon = Math.abs(Math.abs(dx) - 20) <= 1 && Math.abs(dz - Math.floorMod(y - baseY, 74) + 37) <= 1;
		return stairRibbon ? BlockPalette.stairs(Direction.SOUTH) : null;
	}

	private BlockState districtCylinderStructureState(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int dist2 = dx * dx + dz * dz;
		int radius = 118;
		if (Math.abs(dist2 - radius * radius) <= radius * 2) {
			return BlockPalette.DARK_STONE;
		}
		if (Math.floorMod(y - settings.floorY(), 42) == 0 && Math.abs(dist2 - (radius - 10) * (radius - 10)) <= radius * 2) {
			return BlockPalette.WALKWAY;
		}
		boolean balcony = Math.floorMod(y - settings.floorY(), 42) == 1
				&& Math.abs(dist2 - (radius - 18) * (radius - 18)) <= radius
				&& Math.floorMod((int) Math.round(Math.atan2(dz, dx) * 16), 4) == 0;
		return balcony ? BlockPalette.WALL_PANEL : null;
	}

	private BlockState districtAbyssStructureState(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int level = settings.spawnPlatformY() + 40;
		if (y == level && Math.abs(dz) <= 2 && dx >= -132 && dx <= -28) {
			return BlockPalette.WALKWAY;
		}
		int houseY = level + 12;
		int sphere = (dx + 8) * (dx + 8) + (y - houseY) * (y - houseY) + dz * dz;
		boolean entrance = dx >= -30 && dx <= -23 && Math.abs(dz) <= 3 && y >= houseY - 3 && y <= houseY + 4;
		if (entrance) {
			return BlockPalette.AIR;
		}
		if (sphere >= 18 * 18 && sphere <= 22 * 22) {
			return BlockPalette.LIGHT_STONE;
		}
		boolean growth = y >= houseY - 2 && y <= houseY + 4 && dx >= 14 && dx <= 40 && Math.abs(dz) <= 6;
		return growth ? BlockPalette.WALL_PANEL : null;
	}

	private BlockState descentWellStructureState(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		if (Math.abs(dx) > 32 || Math.abs(dz) > 32) {
			return null;
		}
		BlockState stair = squareStairState(dx + 32, dz + 32, y - settings.floorY(), 12, 12, 52, 52);
		if (stair != null) {
			return stair;
		}
		boolean landing = Math.floorMod(y - settings.floorY(), 64) == 0 && Math.abs(dx) <= 28 && Math.abs(dz) <= 28 && (Math.abs(dx) >= 20 || Math.abs(dz) >= 20);
		return landing ? BlockPalette.WALKWAY : null;
	}

	private BlockState blockTowerStructureState(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 937);
		int baseY = settings.floorY() + 18;
		int topY = settings.ceilingY() - 38;
		int roomRadius = 500;
		if (y < baseY || y > topY || dx * dx + dz * dz > roomRadius * roomRadius) {
			return null;
		}

		if (y == baseY && dx * dx + dz * dz <= (roomRadius - 8) * (roomRadius - 8)) {
			long floorHash = MegastructureMath.hash(hash, Math.floorDiv(dx, 24), Math.floorDiv(dz, 24), 991);
			return Math.floorMod(floorHash, 17) == 0 ? BlockPalette.MASS_STONE_VARIANT : BlockPalette.PLATFORM;
		}

		int towerRadius = 50;
		int innerRadius = 39;
		int dist2 = dx * dx + dz * dz;
		int towerTop = topY - 12;
		boolean centralTower = dist2 <= towerRadius * towerRadius && y <= towerTop;
		if (centralTower) {
			int inner2 = innerRadius * innerRadius;
			int level = y - baseY;
			boolean shell = dist2 >= inner2;
			boolean serviceFloor = level > 0 && Math.floorMod(level, 32) == 0 && dist2 < inner2;
			boolean centralAtrium = Math.abs(dx) <= 10 && Math.abs(dz) <= 10;
			if (serviceFloor && !centralAtrium) {
				return Math.floorMod(dx + dz + level, 29) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.WALKWAY;
			}
			int stationY = connectorCenterY(districtX, districtZ);
			int stationLevel = y - stationY;
			boolean lowerPlatform = stationLevel == 0 && Math.abs(dz) <= 18 && Math.abs(dx) <= innerRadius - 2;
			boolean upperPlatform = stationLevel == 32 && Math.abs(dx) <= 18 && Math.abs(dz) <= innerRadius - 2;
			if ((lowerPlatform || upperPlatform) && !centralAtrium) {
				return BlockPalette.PLATFORM;
			}
			BlockState floorProgram = titanTowerFloorProgramState(dx, dz, y, baseY, stationY, hash);
			if (floorProgram != null) {
				return floorProgram;
			}
			BlockState office = titanDistributionRoomState(dx, dz, y, stationY, hash);
			if (office != null) {
				return office;
			}
			if (!shell) {
				return BlockPalette.AIR;
			}
			boolean portalX = Math.abs(dz) <= 7
					&& (Math.abs(y - stationY) <= 6 || Math.abs(y - (stationY + 32)) <= 6);
			boolean portalZ = Math.abs(dx) <= 7 && Math.abs(y - (stationY + 32)) <= 6;
			if (portalX || portalZ) {
				return BlockPalette.AIR;
			}
			boolean seam = Math.floorMod(level, 31) == 0
					|| Math.floorMod(dx, 23) == 0
					|| Math.floorMod(dz, 23) == 0;
			return seam ? BlockPalette.WALL_PANEL : BlockPalette.LIGHT_STONE;
		}

		boolean blockOutgrowth = false;
		for (int i = 0; i < 9; i++) {
			long blockHash = MegastructureMath.hash(hash, i, 0, 953);
			int side = Math.floorMod((int) blockHash, 4);
			int cx = switch (side) {
				case 0 -> MegastructureMath.range(blockHash >>> 8, -38, 38);
				case 1 -> MegastructureMath.range(blockHash >>> 8, -38, 38);
				case 2 -> -towerRadius - MegastructureMath.range(blockHash >>> 8, 8, 34);
				default -> towerRadius + MegastructureMath.range(blockHash >>> 8, 8, 34);
			};
			int cz = switch (side) {
				case 0 -> -towerRadius - MegastructureMath.range(blockHash >>> 16, 8, 34);
				case 1 -> towerRadius + MegastructureMath.range(blockHash >>> 16, 8, 34);
				case 2 -> MegastructureMath.range(blockHash >>> 16, -38, 38);
				default -> MegastructureMath.range(blockHash >>> 16, -38, 38);
			};
			int half = MegastructureMath.range(blockHash >>> 24, 10, 24);
			int bottom = baseY + MegastructureMath.range(blockHash >>> 32, 70, 760);
			int top = Math.min(topY - 20, bottom + MegastructureMath.range(blockHash >>> 40, 52, 180));
			if (Math.abs(dx - cx) <= half && Math.abs(dz - cz) <= half && y >= bottom && y <= top) {
				blockOutgrowth = true;
				break;
			}
		}

		if (blockOutgrowth) {
			boolean seam = Math.floorMod(y - baseY, 31) == 0
					|| Math.floorMod(dx, 23) == 0
					|| Math.floorMod(dz, 23) == 0;
			if (seam) {
				return BlockPalette.WALL_PANEL;
			}
			return BlockPalette.LIGHT_STONE;
		}

		boolean horizontalConnector = isHorizontalDistrictConnectorAt(x, z, 3);
		boolean verticalConnector = isVerticalDistrictConnectorAt(x, z, 3);
		int localConnectorY = horizontalConnector ? horizontalConnectorYAt(x, z) : verticalConnectorYAt(x, z);
		if ((horizontalConnector || verticalConnector) && y < localConnectorY && y >= baseY) {
			boolean runsX = isHorizontalDistrictConnectorAt(x - 4, z, 1)
					&& isHorizontalDistrictConnectorAt(x + 4, z, 1);
			int along = runsX ? x : z;
			boolean supportNode = Math.floorMod(along + (int) hash, 64) <= 2;
			if (supportNode && isDistrictConnectorAt(x, z, 1)) {
				return Math.floorMod(y - baseY, 24) <= 2 ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
			}
		}

		boolean approachDeck = y == localConnectorY
				&& isDistrictConnectorAt(x, z, 9)
				&& !isDistrictConnectorAt(x, z, 3);
		if (approachDeck) {
			return BlockPalette.PLATFORM;
		}

		return null;
	}

	private BlockState titanDistributionRoomState(int dx, int dz, int y, int stationY, long hash) {
		int[] levels = {stationY, stationY + 32};
		for (int index = 0; index < levels.length; index++) {
			int base = levels[index];
			int localY = y - base;
			if (localY < 0 || localY > 11) {
				continue;
			}
			boolean xStation = index == 0;
			int along = xStation ? dx : dz;
			int cross = xStation ? dz : dx;
			if (Math.abs(along) > 32 || Math.abs(cross) < 20 || Math.abs(cross) > 36) {
				continue;
			}
			boolean doorway = Math.abs(along) <= 4 && Math.abs(cross) == 20 && localY >= 1 && localY <= 6;
			if (doorway) {
				return BlockPalette.AIR;
			}
			if (localY == 0 || localY == 11) {
				return localY == 0 ? BlockPalette.WALKWAY : BlockPalette.WALL_PANEL;
			}
			boolean wall = Math.abs(along) == 32 || Math.abs(cross) == 20 || Math.abs(cross) == 36;
			if (wall) {
				return Math.floorMod(localY + along + (int) hash, 7) == 0
						? BlockPalette.CRACKED_PANEL : BlockPalette.DARK_STONE;
			}
			boolean deskBank = localY == 1 && Math.floorMod(along + 28, 12) <= 5 && Math.abs(cross) >= 26;
			if (deskBank) {
				return BlockPalette.WALL_PANEL;
			}
			return BlockPalette.AIR;
		}
		return null;
	}

	private BlockState titanTowerFloorProgramState(int dx, int dz, int y, int baseY, int stationY, long hash) {
		int level = y - baseY;
		if (level <= 0) {
			return null;
		}
		int floorIndex = Math.floorDiv(level, 16);
		int floorBase = baseY + floorIndex * 16;
		int localY = y - floorBase;
		if (localY < 0 || localY > 8) {
			return null;
		}
		if (Math.abs(y - stationY) <= 8 || Math.abs(y - (stationY + 32)) <= 8) {
			return null;
		}
		int dist2 = dx * dx + dz * dz;
		if (dist2 >= 38 * 38 || Math.abs(dx) <= 10 && Math.abs(dz) <= 10) {
			return null;
		}
		long floorHash = MegastructureMath.hash(hash, floorIndex, 0, 987);
		int program = Math.floorMod((int) floorHash, 6);
		boolean ringWalk = dist2 >= 25 * 25 || Math.abs(dx) <= 15 || Math.abs(dz) <= 15;
		if (localY == 0 && ringWalk) {
			return Math.floorMod(dx * 3 + dz * 5 + floorIndex, 37) == 0
					? BlockPalette.GRATE
					: BlockPalette.WALKWAY;
		}
		if (localY == 8 && ringWalk && Math.floorMod(dx + dz + floorIndex, 5) != 0) {
			return BlockPalette.WALL_PANEL;
		}
		boolean outerPartition = (Math.abs(dx) == 31 || Math.abs(dz) == 31)
				&& localY >= 1 && localY <= 5
				&& Math.floorMod(dx + dz + floorIndex, 11) != 0;
		if (outerPartition) {
			return program % 2 == 0 ? BlockPalette.DARK_STONE : BlockPalette.WALL_PANEL;
		}
		boolean radialPartition = localY >= 1 && localY <= 4
				&& (Math.abs(dx) <= 2 || Math.abs(dz) <= 2)
				&& Math.max(Math.abs(dx), Math.abs(dz)) >= 18
				&& Math.floorMod(floorIndex + program, 3) != 0;
		if (radialPartition) {
			return BlockPalette.WALL_PANEL;
		}
		boolean equipmentBank = localY == 1
				&& Math.abs(dx) >= 18 && Math.abs(dz) >= 18
				&& Math.floorMod(Math.abs(dx) + floorIndex * 3, 12) <= 4
				&& Math.floorMod(Math.abs(dz) + program * 5, 14) <= 5;
		if (equipmentBank) {
			return Math.floorMod(dx + dz + program, 7) == 0 ? BlockPalette.LAMP : BlockPalette.GRATE;
		}
		boolean pipeRiser = localY >= 1 && localY <= 7
				&& Math.floorMod(Math.abs(dx) + 17, 19) <= 1
				&& Math.floorMod(Math.abs(dz) + floorIndex, 23) <= 1;
		if (pipeRiser) {
			return Math.floorMod(localY, 4) == 0 ? BlockPalette.RUST_PIPE : BlockPalette.PIPE;
		}
		return null;
	}

	private BlockState tankClusterStructureState(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 941);
		int baseY = settings.floorY() + MegastructureMath.range(hash, 54, 612);
		if (y < baseY || y > baseY + 188 || Math.abs(dx) > 170 || Math.abs(dz) > 170) {
			return null;
		}

		if (y == baseY && dx * dx + dz * dz <= 168 * 168) {
			return Math.floorMod(dx + dz, 31) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.PLATFORM;
		}

		for (int i = 0; i < 5; i++) {
			int cx = switch (i) {
				case 0 -> -82;
				case 1 -> 72;
				case 2 -> -28;
				case 3 -> 92;
				default -> 8;
			};
			int cz = switch (i) {
				case 0 -> -58;
				case 1 -> -42;
				case 2 -> 72;
				case 3 -> 82;
				default -> 6;
			};
			long tankHash = MegastructureMath.hash(hash, i, 0, 967);
			int radius = MegastructureMath.range(tankHash, 34, 58);
			int tx = dx - cx;
			int tz = dz - cz;
			int dist2 = tx * tx + tz * tz;
			boolean wall = Math.abs(dist2 - radius * radius) <= radius * 2 && y >= baseY + 1 && y <= baseY + 164;
			if (wall) {
				return Math.floorMod(y - baseY, 19) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
			}
			boolean rim = Math.floorMod(y - baseY, 48) == 20
					&& dist2 >= (radius - 5) * (radius - 5)
					&& dist2 <= (radius + 5) * (radius + 5);
			if (rim) {
				return BlockPalette.WALKWAY;
			}
		}

		boolean lowServiceBridge = y == baseY + 24 && Math.abs(dz + 48) <= 2 && dx >= -118 && dx <= 116;
		boolean upperServiceBridge = y == baseY + 72 && Math.abs(dx - 18) <= 2 && dz >= -104 && dz <= 118;
		boolean diagonalBrace = y == baseY + 116 && Math.abs(dx - dz) <= 1 && dx >= -96 && dx <= 112;
		if (lowServiceBridge || upperServiceBridge || diagonalBrace) {
			return BlockPalette.WALKWAY;
		}

		return null;
	}

	private BlockState scaffoldStructureState(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 947);
		int baseY = settings.floorY() + MegastructureMath.range(hash, 48, 650);
		if (y < baseY || y > baseY + 176 || Math.abs(dx) > 152 || Math.abs(dz) > 152) {
			return null;
		}

		int stairX = dx + 122;
		int stairZ = dz + 118;
		if (stairX >= 0 && stairX <= 23 && stairZ >= 0 && stairZ <= 23) {
			BlockState stair = squareStairState(stairX, stairZ, y - baseY, 3, 3, 19, 19);
			if (stair != null) {
				return stair;
			}
		}

		int gridX = Math.floorMod(localX + 5, 32);
		int gridZ = Math.floorMod(localZ + 11, 32);
		boolean column = (gridX <= 1 || gridX >= 31) && (gridZ <= 1 || gridZ >= 31);
		if (column) {
			return BlockPalette.FOUNDATION;
		}

		int level = y - baseY;
		boolean beamLevel = Math.floorMod(level, 24) <= 1;
		boolean beamX = beamLevel && (gridZ <= 1 || gridZ >= 31) && Math.abs(dx) <= 144;
		boolean beamZ = beamLevel && (gridX <= 1 || gridX >= 31) && Math.abs(dz) <= 144;
		if (beamX || beamZ) {
			return BlockPalette.FOUNDATION;
		}

		boolean platformX = (level == 28 || level == 84 || level == 140) && Math.abs(dz) <= 3 && dx >= -132 && dx <= 132;
		boolean platformZ = (level == 56 || level == 112) && Math.abs(dx) <= 3 && dz >= -132 && dz <= 132;
		if (platformX || platformZ) {
			return BlockPalette.WALKWAY;
		}

		return null;
	}

	private BlockState industrialWallStructureState(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		long hash = MegastructureMath.hash(SHAPE_SEED, districtX, districtZ, 1301);
		boolean horizontal = Math.floorMod(hash, 2) == 0;
		int baseY = settings.floorY() + MegastructureMath.range(hash >>> 8, 40, 220);
		int topY = Math.min(settings.ceilingY() - 32, baseY + MegastructureMath.range(hash >>> 16, 420, 780));
		if (y < baseY || y > topY) {
			return null;
		}

		int along = horizontal ? dx : dz;
		int depth = horizontal ? dz : dx;
		boolean inCut = Math.abs(depth) <= 110 && Math.abs(along) <= 450;
		if (!inCut) {
			return null;
		}

		boolean rearWall = Math.abs(depth + 108) <= 1 && Math.abs(along) <= 430;
		if (rearWall) {
			boolean panel = Math.floorMod(y - baseY, 24) == 0 || Math.floorMod(along, 32) <= 1;
			return panel ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
		}

		boolean verticalShaft = Math.abs(depth + 68) <= 5
				&& (Math.floorMod(along + 320, 160) <= 9)
				&& y <= topY - 24;
		if (verticalShaft) {
			return BlockPalette.FOUNDATION;
		}

		boolean railDeck = Math.floorMod(y - baseY, 96) == 36
				&& Math.abs(depth - 12) <= 3
				&& Math.abs(along) <= 390;
		if (railDeck) {
			return BlockPalette.WALKWAY;
		}
		boolean rail = Math.floorMod(y - baseY, 96) == 37
				&& (depth == 10 || depth == 14)
				&& Math.abs(along) <= 388;
		if (rail) {
			return horizontal ? BlockPalette.RAIL_X : BlockPalette.RAIL_Z;
		}

		boolean servicePlatform = Math.floorMod(y - baseY, 64) == 18
				&& Math.abs(depth + 42) <= 2
				&& Math.abs(along) <= 420
				&& Math.floorMod(along + (int) hash, 53) != 0;
		if (servicePlatform) {
			return BlockPalette.WALKWAY;
		}

		boolean pipeStack = Math.floorMod(y - baseY, 31) <= 1
				&& Math.abs(depth + 92) <= 2
				&& Math.abs(along) <= 410
				&& Math.floorMod(along, 11) != 0;
		if (pipeStack) {
			return BlockPalette.PIPE;
		}

		boolean hangingModule = Math.floorMod(y - baseY, 128) >= 44
				&& Math.floorMod(y - baseY, 128) <= 70
				&& Math.abs(depth - 54) <= 16
				&& Math.floorMod(along + 70, 180) <= 34;
		if (hangingModule) {
			boolean skin = Math.abs(depth - 54) >= 14 || Math.floorMod(y - baseY, 128) == 44 || Math.floorMod(y - baseY, 128) == 70;
			return skin ? BlockPalette.WALL_PANEL : BlockPalette.AIR;
		}

		return null;
	}

	private BlockState transitNexusStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1401, 72, 420);
		int level = y - baseY;
		if (level < 0 || level > 190 || Math.abs(dx) > 252 || Math.abs(dz) > 188) {
			return null;
		}
		if (level == 0) {
			return Math.floorMod(dx + dz, 37) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.PLATFORM;
		}
		long hash = districtHash(x, z, 1401);
		BlockState station = transitNexusStationState(dx, dz, level, hash);
		if (station != null) {
			return station;
		}
		boolean lowerPassage = level >= 39 && level <= 47 && Math.abs(dz) <= 10;
		boolean upperPassage = level >= 93 && level <= 101 && Math.abs(dx) <= 10;
		if (Math.abs(dx) <= 28 && Math.abs(dz) <= 28 && level <= 176) {
			if (lowerPassage || upperPassage) {
				return BlockPalette.AIR;
			}
			return Math.floorMod(level, 32) <= 1 ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
		}
		boolean pylon = (Math.abs(dx) == 92 || Math.abs(dx) == 188) && Math.abs(dz) <= 3 && level <= 38;
		boolean upperPylon = (Math.abs(dz) == 74 || Math.abs(dz) == 146) && Math.abs(dx) <= 3 && level <= 92;
		if (pylon || upperPylon) {
			return BlockPalette.DARK_STONE;
		}
		int dist2 = dx * dx + dz * dz;
		boolean concourseRing = (level == 52 || level == 108 || level == 156)
				&& dist2 >= 64 * 64 && dist2 <= 70 * 70;
		boolean concourseSpoke = (level == 52 || level == 108 || level == 156)
				&& ((Math.abs(dx) <= 2 && Math.abs(dz) <= 70) || (Math.abs(dz) <= 2 && Math.abs(dx) <= 70));
		return concourseRing || concourseSpoke ? BlockPalette.WALKWAY : null;
	}

	private BlockState transitNexusStationState(int dx, int dz, int level, long hash) {
		boolean lowerDeck = level == 38 && Math.abs(dx) <= 252 && Math.abs(dz) <= 10;
		boolean upperDeck = level == 92 && Math.abs(dz) <= 188 && Math.abs(dx) <= 10;
		if (lowerDeck || upperDeck) {
			return BlockPalette.WALKWAY;
		}
		if (level == 39 && Math.abs(dx) <= 252 && (dz == -3 || dz == -2 || dz == 2 || dz == 3)) {
			return BlockPalette.RAIL_X;
		}
		if (level == 93 && Math.abs(dz) <= 188 && (dx == -3 || dx == -2 || dx == 2 || dx == 3)) {
			return BlockPalette.RAIL_Z;
		}
		boolean lowerPlatform = level == 39 && Math.abs(dx) <= 116 && Math.abs(dz) >= 7 && Math.abs(dz) <= 18;
		boolean upperPlatform = level == 93 && Math.abs(dz) <= 116 && Math.abs(dx) >= 7 && Math.abs(dx) <= 18;
		if (lowerPlatform || upperPlatform) {
			return Math.floorMod((lowerPlatform ? dx : dz) + (int) hash, 31) == 0
					? BlockPalette.CRACKED_PANEL : BlockPalette.PLATFORM;
		}
		boolean lowerCanopy = level == 48 && Math.abs(dx) <= 112 && Math.abs(dz) >= 8 && Math.abs(dz) <= 18;
		boolean upperCanopy = level == 102 && Math.abs(dz) <= 112 && Math.abs(dx) >= 8 && Math.abs(dx) <= 18;
		if (lowerCanopy || upperCanopy) {
			int along = lowerCanopy ? dx : dz;
			return Math.floorMod(along + 120, 24) <= 2 ? BlockPalette.FOUNDATION : BlockPalette.WALL_PANEL;
		}
		boolean lowerColumn = Math.floorMod(dx + 108, 36) <= 1 && Math.abs(dz) == 17 && level >= 40 && level <= 47;
		boolean upperColumn = Math.floorMod(dz + 108, 36) <= 1 && Math.abs(dx) == 17 && level >= 94 && level <= 101;
		if (lowerColumn || upperColumn) {
			return BlockPalette.FOUNDATION;
		}

		BlockState room = transitNexusDistributionRoomState(dx, dz, level, hash);
		if (room != null) {
			return room;
		}
		return transitNexusTransferStairState(dx, dz, level);
	}

	private BlockState transitNexusDistributionRoomState(int dx, int dz, int level, long hash) {
		for (int floor = 0; floor < 2; floor++) {
			boolean lower = floor == 0;
			int floorLevel = lower ? 38 : 92;
			int along = lower ? dx : dz;
			int cross = lower ? dz : dx;
			for (int sideIndex = 0; sideIndex < 2; sideIndex++) {
				int side = sideIndex == 0 ? -1 : 1;
				int roomCross = cross * side;
				if (roomCross < 23 || roomCross > 45) {
					continue;
				}
				for (int slot = 0; slot < 4; slot++) {
					int center = -72 + slot * 48;
					int localAlong = along - center;
					long roomHash = MegastructureMath.hash(hash, floor * 8 + (side > 0 ? 4 : 0) + slot, 0, 1913);
					if (Math.abs(localAlong) > 17 || Math.floorMod(roomHash, 5) == 0) {
						continue;
					}
					int roomLevel = level - floorLevel;
					boolean doorway = roomCross == 23 && Math.abs(localAlong) <= 3 && roomLevel >= 1 && roomLevel <= 5;
					if (doorway) {
						return BlockPalette.AIR;
					}
					if (roomLevel == 0 || roomLevel == 11) {
						return roomLevel == 0 ? BlockPalette.WALKWAY : BlockPalette.WALL_PANEL;
					}
					boolean wall = Math.abs(localAlong) == 17 || roomCross == 23 || roomCross == 45;
					if (wall && roomLevel >= 1 && roomLevel <= 10) {
						return Math.floorMod(roomLevel, 4) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
					}
					int program = Math.floorMod((int) (roomHash >>> 12), 4);
					if (program == 0 && roomLevel == 1 && roomCross >= 34 && Math.abs(localAlong) <= 12) {
						return Math.floorMod(localAlong, 5) == 0 ? BlockPalette.LAMP : BlockPalette.GRATE;
					}
					if (program == 1 && roomLevel <= 6 && roomCross == 40 && Math.abs(localAlong) >= 9) {
						return roomLevel % 3 == 0 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
					}
					if (program == 2 && roomLevel >= 1 && roomLevel <= 8 && Math.abs(localAlong) == 11 && roomCross == 36) {
						return roomLevel == 8 ? BlockPalette.LAMP : BlockPalette.RUST_PIPE;
					}
					if (program == 3 && roomLevel <= 4 && roomCross >= 31 && roomCross <= 39 && localAlong == 0) {
						return BlockPalette.WALL_PANEL;
					}
				}
			}
		}
		return null;
	}

	private BlockState transitNexusTransferStairState(int dx, int dz, int level) {
		if (level == 39 && dx >= 32 && dx <= 34 && dz >= -26 && dz <= -18) {
			return BlockPalette.WALKWAY;
		}
		for (int rise = 0; rise <= 26; rise++) {
			int stairZ = -18 + rise;
			if (dx >= 32 && dx <= 34 && dz == stairZ) {
				if (level == 39 + rise) {
					return BlockPalette.stairs(Direction.SOUTH);
				}
				if (level == 38 + rise) {
					return BlockPalette.FOUNDATION;
				}
			}
		}
		if (level == 65 && dx >= 18 && dx <= 34 && dz >= 8 && dz <= 10) {
			return BlockPalette.WALKWAY;
		}
		for (int rise = 0; rise <= 27; rise++) {
			int stairZ = 8 - rise;
			if (dx >= 18 && dx <= 20 && dz == stairZ) {
				if (level == 65 + rise) {
					return BlockPalette.stairs(Direction.NORTH);
				}
				if (level == 64 + rise) {
					return BlockPalette.FOUNDATION;
				}
			}
		}
		return null;
	}

	private BlockState reactorCathedralStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1403, 48, 360);
		int topY = Math.min(settings.ceilingY() - 24, baseY + 286);
		if (y < baseY || y > topY) {
			return null;
		}
		int level = y - baseY;
		int dist2 = dx * dx + dz * dz;
		if (level == 0 && Math.abs(dx) <= 276 && Math.abs(dz) <= 316) {
			return BlockPalette.PLATFORM;
		}
		boolean reactorShell = dist2 >= 48 * 48 && dist2 <= 58 * 58 && level <= 238;
		boolean reactorCore = dist2 <= 14 * 14 && level <= 260;
		boolean reactorRing = Math.floorMod(level, 42) <= 3 && dist2 <= 78 * 78;
		if (reactorShell || reactorCore || reactorRing) {
			return Math.floorMod(level, 21) == 0 ? BlockPalette.RUST_PIPE : BlockPalette.DARK_STONE;
		}
		boolean buttress = (Math.abs(dx) >= 122 && Math.abs(dx) <= 132 && Math.abs(dz) <= 10)
				|| (Math.abs(dz) >= 210 && Math.abs(dz) <= 220 && Math.abs(dx) <= 10);
		if (buttress && level <= 210) {
			return BlockPalette.FOUNDATION;
		}
		boolean processionalBridge = level == 54 && Math.abs(dx) <= 270 && Math.abs(dz) <= 3;
		boolean upperBridge = level == 146 && Math.abs(dz) <= 250 && Math.abs(dx) <= 3;
		return processionalBridge || upperBridge ? BlockPalette.WALKWAY : null;
	}

	private BlockState hangingArchiveStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1409, 64, 340);
		int topY = Math.min(settings.ceilingY() - 20, baseY + 320);
		if (y < baseY || y > topY || Math.abs(dx) > 238 || Math.abs(dz) > 238) {
			return null;
		}
		if (y == baseY) {
			return BlockPalette.PLATFORM;
		}
		int ceilingDepth = topY - y;
		int gridX = Math.floorMod(dx + 192, 64);
		int gridZ = Math.floorMod(dz + 192, 64);
		boolean anchor = (gridX <= 4 || gridX >= 60) && (gridZ <= 4 || gridZ >= 60) && ceilingDepth <= 286;
		boolean archiveSlab = ceilingDepth >= 34 && ceilingDepth <= 272
				&& Math.floorMod(ceilingDepth, 46) <= 6
				&& gridX >= 3 && gridX <= 61 && gridZ >= 3 && gridZ <= 61;
		if (anchor || archiveSlab) {
			return archiveSlab ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
		}
		boolean galleryX = (y == baseY + 72 || y == baseY + 168 || y == baseY + 264) && Math.abs(dz) <= 3 && Math.abs(dx) <= 224;
		boolean galleryZ = (y == baseY + 120 || y == baseY + 216) && Math.abs(dx) <= 3 && Math.abs(dz) <= 224;
		return galleryX || galleryZ ? BlockPalette.WALKWAY : null;
	}

	private BlockState ventilationCanyonStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		long hash = districtHash(x, z, 1423);
		boolean horizontal = Math.floorMod(hash, 2) == 0;
		int along = horizontal ? dx : dz;
		int depth = horizontal ? dz : dx;
		int bottom = settings.floorY() + 28;
		int top = settings.ceilingY() - 30;
		if (y < bottom || y > top || Math.abs(along) > 470 || Math.abs(depth) > 74) {
			return null;
		}
		if (y == bottom) {
			return BlockPalette.PLATFORM;
		}
		boolean ductTrunk = Math.floorMod(along + 448, 128) <= 11 && Math.abs(depth) >= 46 && Math.abs(depth) <= 62;
		if (ductTrunk) {
			return Math.floorMod(y - bottom, 28) <= 1 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
		}
		boolean catwalk = Math.floorMod(y - bottom, 128) == 48 && Math.abs(depth) >= 54 && Math.abs(depth) <= 60;
		boolean crossBridge = Math.floorMod(along + 384, 192) <= 4 && Math.floorMod(y - bottom, 192) == 96 && Math.abs(depth) <= 68;
		return catwalk || crossBridge ? BlockPalette.WALKWAY : null;
	}

	private BlockState invertedPyramidStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1427, 72, 330);
		int topY = Math.min(settings.ceilingY() - 22, baseY + 310);
		if (y < baseY || y > topY || Math.abs(dx) > 276 || Math.abs(dz) > 276) {
			return null;
		}
		if (y == baseY) {
			return BlockPalette.PLATFORM;
		}
		int depth = topY - y;
		int half = Math.max(7, 158 - depth / 2);
		boolean pyramid = depth <= 302 && Math.abs(dx) <= half && Math.abs(dz) <= half;
		if (pyramid) {
			boolean shell = Math.abs(dx) >= half - 3 || Math.abs(dz) >= half - 3 || Math.floorMod(depth, 28) <= 2;
			return shell ? BlockPalette.LIGHT_STONE : BlockPalette.DARK_STONE;
		}
		boolean upperBridgeX = y == topY - 52 && Math.abs(dz) <= 3 && Math.abs(dx) <= 264;
		boolean upperBridgeZ = y == topY - 116 && Math.abs(dx) <= 3 && Math.abs(dz) <= 264;
		return upperBridgeX || upperBridgeZ ? BlockPalette.WALKWAY : null;
	}

	private BlockState ringVaultStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1433, 52, 360);
		int topY = Math.min(settings.ceilingY() - 22, baseY + 330);
		int level = y - baseY;
		int dist2 = dx * dx + dz * dz;
		if (level < 0 || y > topY || dist2 > 246 * 246) {
			return null;
		}
		if (level == 0) {
			return BlockPalette.PLATFORM;
		}

		boolean spindleShell = dist2 >= 18 * 18 && dist2 <= 30 * 30 && level <= 304;
		boolean spindleBand = Math.floorMod(level, 48) <= 3 && dist2 <= 38 * 38;
		if (spindleShell || spindleBand) {
			return spindleBand ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
		}

		int[] radii = {64, 124, 198};
		int[] ringLevels = {72, 148, 224};
		for (int i = 0; i < radii.length; i++) {
			int radius = radii[i];
			int ringLevel = ringLevels[i];
			boolean ringDeck = level == ringLevel
					&& dist2 >= (radius - 7) * (radius - 7)
					&& dist2 <= (radius + 7) * (radius + 7);
			boolean radialSpoke = level == ringLevel
					&& ((Math.abs(dx) <= 3 && Math.abs(dz) >= 28 && Math.abs(dz) <= radius + 4)
					|| (Math.abs(dz) <= 3 && Math.abs(dx) >= 28 && Math.abs(dx) <= radius + 4));
			if (ringDeck || radialSpoke) {
				return BlockPalette.WALKWAY;
			}

			boolean conduitBus = level == ringLevel - 1
					&& Math.abs(dist2 - radius * radius) <= radius * 2;
			if (conduitBus) {
				return i == 1 ? BlockPalette.RUST_PIPE : BlockPalette.PIPE;
			}

			boolean ringSupport = level < ringLevel
					&& ((Math.abs(Math.abs(dx) - radius) <= 3 && Math.abs(dz) <= 3)
					|| (Math.abs(Math.abs(dz) - radius) <= 3 && Math.abs(dx) <= 3));
			if (ringSupport) {
				return BlockPalette.FOUNDATION;
			}

			boolean driveOnX = Math.abs(Math.abs(dx) - radius) <= 10 && Math.abs(dz) <= 12;
			boolean driveOnZ = Math.abs(Math.abs(dz) - radius) <= 10 && Math.abs(dx) <= 12;
			boolean driveVolume = level >= ringLevel + 1 && level <= ringLevel + 16 && (driveOnX || driveOnZ);
			if (driveVolume) {
				boolean shell = level == ringLevel + 1
						|| level == ringLevel + 16
						|| (driveOnX && (Math.abs(Math.abs(dx) - radius) >= 8 || Math.abs(dz) >= 10))
						|| (driveOnZ && (Math.abs(Math.abs(dz) - radius) >= 8 || Math.abs(dx) >= 10));
				return shell ? BlockPalette.WALL_PANEL : BlockPalette.AIR;
			}
		}

		boolean outerAccessX = level == ringLevels[2]
				&& Math.abs(dz) <= 3
				&& Math.abs(dx) >= radii[2] - 4
				&& Math.abs(dx) <= 242;
		boolean outerAccessZ = level == ringLevels[2]
				&& Math.abs(dx) <= 3
				&& Math.abs(dz) >= radii[2] - 4
				&& Math.abs(dz) <= 242;
		return outerAccessX || outerAccessZ ? BlockPalette.WALKWAY : null;
	}

	private BlockState machineNaveStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1439, 70, 480);
		int level = y - baseY;
		if (level < 0 || level > 178 || Math.abs(dx) > 372 || Math.abs(dz) > 174) {
			return null;
		}
		if (level == 0) {
			return BlockPalette.PLATFORM;
		}
		int machineX = Math.floorMod(dx + 344, 86);
		boolean machineBank = Math.abs(dz) >= 44 && Math.abs(dz) <= 146 && machineX <= 46 && level <= 72 + Math.floorMod(dx, 38);
		if (machineBank) {
			boolean casing = machineX <= 3 || machineX >= 43 || level <= 3 || Math.floorMod(level, 24) <= 1;
			return casing ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
		}
		boolean column = Math.floorMod(dx + 344, 86) <= 5 && Math.abs(dz) >= 150 && level <= 158;
		boolean gantry = (level == 82 || level == 142) && Math.abs(dx) <= 354 && Math.abs(dz) <= 154 && (Math.abs(dz) >= 132 || Math.floorMod(dx + 344, 86) <= 5);
		if (column) {
			return BlockPalette.FOUNDATION;
		}
		return gantry ? BlockPalette.WALKWAY : null;
	}

	private BlockState fracturedHabitatStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1447, 54, 380);
		int level = y - baseY;
		if (level < 0 || level > 298 || Math.abs(dx) > 268 || Math.abs(dz) > 238) {
			return null;
		}
		if (level == 0) {
			return BlockPalette.PLATFORM;
		}
		if (Math.abs(dx) <= 18 && Math.abs(dz) <= 18 && level <= 284) {
			return BlockPalette.FOUNDATION;
		}
		for (int i = 0; i < 6; i++) {
			long moduleHash = MegastructureMath.hash(districtHash(x, z, 1447), i, 0, 1471);
			int cx = MegastructureMath.range(moduleHash, -198, 198);
			int cz = MegastructureMath.range(moduleHash >>> 12, -176, 176);
			int bottom = 18 + i * 42;
			int halfX = MegastructureMath.range(moduleHash >>> 24, 24, 46);
			int halfZ = MegastructureMath.range(moduleHash >>> 32, 20, 40);
			boolean module = Math.abs(dx - cx) <= halfX && Math.abs(dz - cz) <= halfZ && level >= bottom && level <= bottom + 34;
			if (module) {
				boolean skin = Math.abs(dx - cx) >= halfX - 2 || Math.abs(dz - cz) >= halfZ - 2 || level == bottom || level == bottom + 34;
				return skin ? BlockPalette.WALL_PANEL : BlockPalette.AIR;
			}
			boolean connectorX = level == bottom + 8 && Math.abs(dz - cz) <= 2 && between(dx, cx, 0);
			boolean connectorZ = level == bottom + 8 && Math.abs(dx) <= 2 && between(dz, cz, 0);
			if (connectorX || connectorZ) {
				return BlockPalette.WALKWAY;
			}
		}
		return null;
	}

	private BlockState conduitBasilicaStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1451, 60, 360);
		int level = y - baseY;
		if (level < 0 || level > 288 || Math.abs(dx) > 292 || Math.abs(dz) > 364) {
			return null;
		}
		if (level == 0 && Math.abs(dx) <= 188 && Math.abs(dz) <= 360) {
			return BlockPalette.PLATFORM;
		}
		int arch = Math.floorMod(dz + 336, 84);
		boolean archPillar = arch <= 5 && Math.abs(dx) >= 132 && Math.abs(dx) <= 146 && level <= 248;
		int archRise = Math.max(0, 146 - Math.abs(dx));
		boolean archCrown = arch <= 5 && level >= 236 + archRise / 3 && level <= 244 + archRise / 3 && Math.abs(dx) <= 146;
		if (archPillar || archCrown) {
			return BlockPalette.FOUNDATION;
		}
		boolean mainConduit = (Math.abs(dx) == 160 || Math.abs(dx) == 174) && Math.abs(dz) <= 344 && level >= 24 && level <= 266;
		if (mainConduit) {
			return Math.floorMod(level, 31) <= 2 ? BlockPalette.RUST_PIPE : BlockPalette.PIPE;
		}
		boolean conduitBracket = arch <= 5
				&& Math.floorMod(level - 32, 64) <= 2
				&& Math.abs(dx) >= 142
				&& Math.abs(dx) <= 176;
		if (conduitBracket) {
			return BlockPalette.FOUNDATION;
		}
		boolean gallery = (level == 74 || level == 166) && Math.abs(dx) >= 142 && Math.abs(dx) <= 188 && Math.abs(dz) <= 342;
		return gallery ? BlockPalette.WALKWAY : null;
	}

	private BlockState reservoirHallStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1459, 48, 420);
		int level = y - baseY;
		int dist2 = dx * dx + dz * dz;
		if (level < 0 || level > 246 || dist2 > 276 * 276) {
			return null;
		}
		if (level == 0) {
			return dist2 <= 44 * 44 ? BlockPalette.DARK_STONE : BlockPalette.PLATFORM;
		}
		int[] basinRadii = {78, 152, 232};
		for (int i = 0; i < basinRadii.length; i++) {
			int radius = basinRadii[i];
			boolean basinWall = Math.abs(dist2 - radius * radius) <= radius * 3 && level <= 46 + i * 34;
			boolean rim = level == 46 + i * 34 && dist2 >= (radius - 7) * (radius - 7) && dist2 <= (radius + 7) * (radius + 7);
			if (basinWall) {
				return BlockPalette.DARK_STONE;
			}
			if (rim) {
				return BlockPalette.WALKWAY;
			}
		}
		boolean centralGauge = dist2 <= 18 * 18 && level <= 214;
		boolean accessBridge = level == 52 && Math.abs(dz) <= 3 && dx >= -262 && dx <= -24;
		if (centralGauge) {
			return Math.floorMod(level, 24) <= 2 ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
		}
		return accessBridge ? BlockPalette.WALKWAY : null;
	}

	private BlockState suspendedCityStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1463, 52, 330);
		int topY = Math.min(settings.ceilingY() - 24, baseY + 360);
		int level = y - baseY;
		if (level < 0 || y > topY || Math.abs(dx) > 330 || Math.abs(dz) > 270) {
			return null;
		}
		if (level == 0) {
			return Math.floorMod(dx + dz, 43) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.PLATFORM;
		}

		int cellX = Math.floorDiv(dx + 288, 96);
		int cellZ = Math.floorDiv(dz + 240, 96);
		int centerX = -288 + cellX * 96 + 48;
		int centerZ = -240 + cellZ * 96 + 48;
		long cellHash = MegastructureMath.hash(districtHash(x, z, 1463), cellX, cellZ, 1493);
		int moduleBottom = 42 + MegastructureMath.range(cellHash, 0, 3) * 68;
		int halfX = MegastructureMath.range(cellHash >>> 12, 22, 38);
		int halfZ = MegastructureMath.range(cellHash >>> 20, 18, 32);
		int moduleHeight = MegastructureMath.range(cellHash >>> 28, 18, 34);
		int mx = dx - centerX;
		int mz = dz - centerZ;
		boolean module = Math.abs(mx) <= halfX && Math.abs(mz) <= halfZ
				&& level >= moduleBottom && level <= moduleBottom + moduleHeight;
		if (module) {
			boolean skin = Math.abs(mx) >= halfX - 2 || Math.abs(mz) >= halfZ - 2
					|| level == moduleBottom || level == moduleBottom + moduleHeight;
			return skin ? BlockPalette.WALL_PANEL : BlockPalette.AIR;
		}

		boolean ceilingTether = Math.abs(Math.abs(mx) - halfX + 2) <= 1
				&& Math.abs(Math.abs(mz) - halfZ + 2) <= 1
				&& level >= moduleBottom + moduleHeight && y <= topY;
		boolean floorPylon = Math.floorMod(cellX + cellZ, 3) == 0
				&& Math.abs(mx) <= 2 && Math.abs(mz) <= 2 && level <= moduleBottom;
		if (ceilingTether || floorPylon) {
			return Math.floorMod(level, 24) <= 1 ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
		}

		boolean skywayX = Math.floorMod(level - 108, 96) == 0
				&& Math.abs(Math.floorMod(dz + 240, 96) - 48) <= 2 && Math.abs(dx) <= 316;
		boolean skywayZ = Math.floorMod(level - 156, 96) == 0
				&& Math.abs(Math.floorMod(dx + 288, 96) - 48) <= 2 && Math.abs(dz) <= 256;
		return skywayX || skywayZ ? BlockPalette.WALKWAY : null;
	}

	private BlockState irisChasmStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		long hash = districtHash(x, z, 1469);
		boolean horizontal = Math.floorMod(hash, 2) == 0;
		int along = horizontal ? dx : dz;
		int depth = horizontal ? dz : dx;
		int baseY = districtBaseY(x, z, 1469, 36, 260);
		int level = y - baseY;
		int topY = settings.ceilingY() - 28;
		if (level < 0 || y > topY || Math.abs(along) > 442 || Math.abs(depth) > 148) {
			return null;
		}
		if (level == 0) {
			return BlockPalette.PLATFORM;
		}
		int segment = Math.floorDiv(along + 400, 160);
		int segmentCenter = -400 + segment * 160 + 80;
		long irisHash = MegastructureMath.hash(hash, segment, 0, 1499);
		int irisY = baseY + MegastructureMath.range(irisHash, 76, Math.max(92, topY - baseY - 76));
		int irisRadius = MegastructureMath.range(irisHash >>> 16, 28, 52);
		int apertureAlong = along - segmentCenter;
		int apertureY = y - irisY;
		boolean aperture = apertureAlong * apertureAlong + apertureY * apertureY <= irisRadius * irisRadius;
		boolean bulkhead = Math.abs(Math.abs(depth) - 100) <= 3;
		boolean irisRim = bulkhead
				&& !aperture
				&& apertureAlong * apertureAlong + apertureY * apertureY <= (irisRadius + 7) * (irisRadius + 7);
		if (irisRim) {
			return BlockPalette.RUST_PIPE;
		}
		if (bulkhead && !aperture) {
			boolean frame = Math.floorMod(level, 32) <= 2 || Math.floorMod(along + 400, 40) <= 2;
			return frame ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
		}
		boolean crossBridge = Math.floorMod(segment, 3) == 0 && y == irisY
				&& Math.abs(apertureAlong) <= irisRadius && Math.abs(depth) <= 136;
		return crossBridge ? BlockPalette.WALKWAY : null;
	}

	private BlockState machineRootVaultStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1475, 46, 320);
		int topY = Math.min(settings.ceilingY() - 24, baseY + 390);
		int level = y - baseY;
		int height = topY - baseY;
		int dist2 = dx * dx + dz * dz;
		if (level < 0 || y > topY || dist2 > 280 * 280) {
			return null;
		}
		if (level == 0) {
			return Math.floorMod(dx - dz, 37) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.PLATFORM;
		}
		if (dist2 <= 20 * 20) {
			return Math.floorMod(level, 36) <= 2 ? BlockPalette.RUST_PIPE : BlockPalette.FOUNDATION;
		}
		for (int root = 0; root < 10; root++) {
			long rootHash = MegastructureMath.hash(districtHash(x, z, 1475), root, 0, 1511);
			int direction = Math.floorMod(root * 10 / 10 + Math.floorMod((int) rootHash, 10), 10);
			int reach = MegastructureMath.range(rootHash >>> 12, 138, 252);
			int radial = reach * (height - level) / Math.max(1, height);
			int targetX = TREE_DIRECTIONS_X[direction] * radial / 100;
			int targetZ = TREE_DIRECTIONS_Z[direction] * radial / 100;
			int thickness = 3 + (height - level) * 5 / Math.max(1, height);
			if (Math.abs(dx - targetX) <= thickness && Math.abs(dz - targetZ) <= thickness) {
				return Math.floorMod(level + root, 29) <= 2 ? BlockPalette.WALL_PANEL : BlockPalette.PIPE;
			}
		}
		boolean ring = (level == 88 || level == 196 || level == 304)
				&& dist2 >= 210 * 210 && dist2 <= 220 * 220;
		return ring ? BlockPalette.WALKWAY : null;
	}

	private BlockState tiltedStacksStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1481, 58, 350);
		int level = y - baseY;
		if (level < 0 || level > 304 || Math.abs(dx) > 312 || Math.abs(dz) > 274) {
			return null;
		}
		if (level == 0) {
			return BlockPalette.PLATFORM;
		}
		long districtSeed = districtHash(x, z, 1481);
		for (int stack = 0; stack < 7; stack++) {
			long stackHash = MegastructureMath.hash(districtSeed, stack, 0, 1517);
			int originX = MegastructureMath.range(stackHash, -238, 238);
			int originZ = MegastructureMath.range(stackHash >>> 12, -196, 196);
			int stackHeight = MegastructureMath.range(stackHash >>> 24, 112, 280);
			if (level > stackHeight) {
				continue;
			}
			int tiltX = MegastructureMath.range(stackHash >>> 36, -1, 1);
			int tiltZ = MegastructureMath.range(stackHash >>> 40, -1, 1);
			int shiftedX = originX + tiltX * level / 8;
			int shiftedZ = originZ + tiltZ * level / 8;
			int halfX = MegastructureMath.range(stackHash >>> 44, 24, 42);
			int halfZ = MegastructureMath.range(stackHash >>> 50, 22, 38);
			int sx = dx - shiftedX;
			int sz = dz - shiftedZ;
			if (Math.abs(sx) <= halfX && Math.abs(sz) <= halfZ) {
				boolean shell = Math.abs(sx) >= halfX - 2 || Math.abs(sz) >= halfZ - 2
						|| Math.floorMod(level, 42) <= 2;
				return shell ? BlockPalette.WALL_PANEL : BlockPalette.AIR;
			}
		}
		boolean transferBridge = (level == 74 || level == 158 || level == 242)
				&& ((Math.abs(dx) <= 270 && Math.abs(dz) <= 2)
				|| (Math.abs(dz) <= 230 && Math.abs(dx) <= 2));
		return transferBridge ? BlockPalette.WALKWAY : null;
	}

	private BlockState silentFoundryStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1487, 54, 360);
		int level = y - baseY;
		if (level < 0 || level > 232 || Math.abs(dx) > 404 || Math.abs(dz) > 238) {
			return null;
		}
		if (level == 0) {
			int pit = dx * dx / 4 + dz * dz;
			return pit <= 92 * 92 ? BlockPalette.DARK_STONE : BlockPalette.PLATFORM;
		}
		boolean craneColumn = Math.floorMod(dx + 360, 120) <= 5
				&& Math.abs(dz) >= 182 && Math.abs(dz) <= 194 && level <= 206;
		boolean craneRail = level >= 198 && level <= 204
				&& Math.abs(dz) >= 180 && Math.abs(dz) <= 196;
		if (craneColumn || craneRail) {
			return Math.floorMod(level, 24) <= 2 ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
		}
		int hullMetric = dx * dx / 9 + dz * dz;
		boolean hull = level >= 18 && level <= 118
				&& hullMetric >= 76 * 76 && hullMetric <= 88 * 88
				&& !(dx > 40 && dz > 0 && level > 62);
		if (hull) {
			return Math.floorMod(level, 18) <= 2 ? BlockPalette.RUST_PIPE : BlockPalette.DARK_STONE;
		}
		boolean gantry = (level == 46 || level == 126 || level == 182)
				&& Math.abs(dx) <= 382 && Math.abs(dz) <= 216
				&& (Math.abs(dz) >= 168 || Math.floorMod(dx + 360, 120) <= 5);
		boolean inspectionSpine = level == 88 && Math.abs(dz) <= 3 && Math.abs(dx) <= 360;
		return gantry || inspectionSpine ? BlockPalette.WALKWAY : null;
	}

	private BlockState colossusLiftStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1493, 42, 280);
		int topY = Math.min(settings.ceilingY() - 22, baseY + 430);
		int level = y - baseY;
		if (level < 0 || y > topY || Math.abs(dx) > 276 || Math.abs(dz) > 276) {
			return null;
		}
		if (level == 0) {
			return Math.floorMod(dx + dz, 41) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.PLATFORM;
		}
		int[][] guides = {{-104, -104}, {104, -104}, {-104, 104}, {104, 104}};
		for (int i = 0; i < guides.length; i++) {
			int gx = dx - guides[i][0];
			int gz = dz - guides[i][1];
			boolean guide = (Math.abs(gx) >= 18 && Math.abs(gx) <= 23 && Math.abs(gz) <= 23)
					|| (Math.abs(gz) >= 18 && Math.abs(gz) <= 23 && Math.abs(gx) <= 23);
			if (guide) {
				return Math.floorMod(level, 28) <= 2 ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
			}
			long liftHash = MegastructureMath.hash(districtHash(x, z, 1493), i, 0, 1511);
			int carriageBase = 42 + MegastructureMath.range(liftHash, 0, 5) * 58;
			int carriageTop = carriageBase + MegastructureMath.range(liftHash >>> 12, 34, 62);
			if (level >= carriageBase && level <= carriageTop && Math.abs(gx) <= 16 && Math.abs(gz) <= 16) {
				boolean shell = Math.abs(gx) >= 13 || Math.abs(gz) >= 13 || level == carriageBase || level == carriageTop;
				return shell ? BlockPalette.DARK_STONE : BlockPalette.AIR;
			}
		}
		boolean transferDeck = (level == 64 || level == 176 || level == 288 || level == 400)
				&& ((Math.abs(dx) <= 232 && Math.abs(dz) <= 4)
				|| (Math.abs(dz) <= 232 && Math.abs(dx) <= 4));
		if (transferDeck) {
			return BlockPalette.WALKWAY;
		}
		boolean wallStation = (level == 64 || level == 176 || level == 288 || level == 400)
				&& (Math.abs(Math.abs(dx) - 246) <= 3 || Math.abs(Math.abs(dz) - 246) <= 3)
				&& Math.min(Math.abs(dx), Math.abs(dz)) <= 32;
		return wallStation ? BlockPalette.PLATFORM : null;
	}

	private BlockState foldedCityStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1499, 56, 360);
		int level = y - baseY;
		if (level < 0 || level > 320 || Math.abs(dx) > 352 || Math.abs(dz) > 292) {
			return null;
		}
		if (level == 0) {
			return Math.floorMod(dx - dz, 43) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.PLATFORM;
		}
		int[] shellsX = {72, 142, 224, 332};
		int[] shellsZ = {58, 116, 188, 272};
		for (int shellIndex = 0; shellIndex < shellsX.length; shellIndex++) {
			int shiftX = shellIndex % 2 == 0 ? -18 * shellIndex : 14 * shellIndex;
			int shiftZ = shellIndex % 2 == 0 ? 12 * shellIndex : -16 * shellIndex;
			int sx = dx - shiftX;
			int sz = dz - shiftZ;
			int halfX = shellsX[shellIndex];
			int halfZ = shellsZ[shellIndex];
			int shellTop = 116 + shellIndex * 56;
			if (level > shellTop || Math.abs(sx) > halfX || Math.abs(sz) > halfZ) {
				continue;
			}
			boolean boundary = Math.abs(sx) >= halfX - 3 || Math.abs(sz) >= halfZ - 3;
			boolean floor = Math.floorMod(level, 28 + shellIndex * 2) <= 1;
			boolean portal = (Math.abs(sx) <= 7 && Math.abs(sz) >= halfZ - 3)
					|| (Math.abs(sz) <= 7 && Math.abs(sx) >= halfX - 3);
			if (boundary && portal && Math.floorMod(level, 56) >= 2 && Math.floorMod(level, 56) <= 9) {
				return BlockPalette.AIR;
			}
			if (boundary || floor) {
				return floor ? BlockPalette.WALKWAY
						: Math.floorMod(level + sx + sz, 31) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.WALL_PANEL;
			}
		}
		boolean transfer = (level == 58 || level == 126 || level == 202 || level == 278)
				&& ((Math.abs(dx) <= 338 && Math.abs(dz) <= 3)
				|| (Math.abs(dz) <= 278 && Math.abs(dx) <= 3));
		return transfer ? BlockPalette.WALKWAY : null;
	}

	private BlockState upperRimCityStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1505, 42, 300);
		int topY = Math.min(settings.ceilingY() - 18, baseY + 500);
		int level = y - baseY;
		int height = topY - baseY;
		long dist2 = (long) dx * dx + (long) dz * dz;
		if (level < 0 || y > topY || dist2 > 470L * 470L) {
			return null;
		}

		if (dist2 >= 444L * 444L && dist2 <= 456L * 456L) {
			return Math.floorMod(level, 28) <= 2 ? BlockPalette.WALL_PANEL : BlockPalette.DARK_STONE;
		}

		int upperDeck = Math.max(72, height - 54);
		if (Math.abs(level - upperDeck) <= 1 && dist2 <= 438L * 438L) {
			return Math.floorMod(dx + dz, 47) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.LIGHT_STONE;
		}

		if (dist2 <= 11L * 11L) {
			return Math.floorMod(level, 42) <= 2 ? BlockPalette.RUST_PIPE : BlockPalette.PIPE;
		}
		if ((level == 36 || level == 84 || level == 136 || level == upperDeck)
				&& dist2 >= 18L * 18L && dist2 <= 430L * 430L
				&& (Math.abs(dx) <= 4 || Math.abs(dz) <= 4 || Math.abs(dx - dz) <= 4 || Math.abs(dx + dz) <= 4)) {
			return BlockPalette.WALKWAY;
		}
		if ((level == 58 || level == 112 || level == upperDeck + 2)
				&& ((dist2 >= 300L * 300L && dist2 <= 306L * 306L)
				|| (dist2 >= 382L * 382L && dist2 <= 388L * 388L))) {
			return BlockPalette.GRATE;
		}

		long districtSeed = districtHash(x, z, 1505);
		if (level >= 0 && level <= 172 && dist2 >= 210L * 210L && dist2 <= 438L * 438L) {
			int cellX = Math.floorDiv(dx + 512, 24);
			int cellZ = Math.floorDiv(dz + 512, 24);
			long cellHash = MegastructureMath.hash(districtSeed, cellX, cellZ, 1601);
			int localX = Math.floorMod(dx + 512, 24);
			int localZ = Math.floorMod(dz + 512, 24);
			int towerHeight = MegastructureMath.range(cellHash >>> 10, 18, 158);
			boolean cityCell = Math.floorMod(cellHash, 5) != 0;
			if (cityCell && level <= towerHeight && (localX <= 2 || localZ <= 2
					|| localX >= 21 || localZ >= 21 || Math.floorMod(level, 18) <= 1)) {
				return Math.floorMod(cellHash + level, 31) == 0 ? BlockPalette.LAMP : BlockPalette.WALL_PANEL;
			}
			boolean narrowStreet = level <= 3 && (localX <= 1 || localZ <= 1);
			if (narrowStreet) {
				return BlockPalette.WALKWAY;
			}
		}

		int[][] antennae = {
				{-338, -104, 92}, {-284, 246, 142}, {-172, -326, 116}, {-72, 362, 128},
				{64, -294, 98}, {152, 214, 154}, {248, -188, 132}, {336, 74, 106},
				{-386, 118, 118}, {394, -42, 148}
		};
		for (int i = 0; i < antennae.length; i++) {
			int ax = dx - antennae[i][0];
			int az = dz - antennae[i][1];
			int mastHeight = antennae[i][2];
			if (level >= upperDeck && level <= upperDeck + mastHeight && ax * ax + az * az <= 10) {
				return Math.floorMod(level + i, 24) <= 2 ? BlockPalette.FOUNDATION : BlockPalette.PIPE;
			}
			if (Math.abs(level - (upperDeck + 8 + i * 3 % 36)) <= 1 && ax * ax + az * az <= 22 * 22) {
				return BlockPalette.GRATE;
			}
		}

		if (level > upperDeck && level <= upperDeck + 24 && dist2 <= 432L * 432L) {
			int cellX = Math.floorDiv(dx + 512, 42);
			int cellZ = Math.floorDiv(dz + 512, 42);
			long hash = MegastructureMath.hash(districtSeed, cellX, cellZ, 1607);
			int localX = Math.floorMod(dx + 512, 42);
			int localZ = Math.floorMod(dz + 512, 42);
			if (Math.floorMod(hash, 7) <= 2 && (localX <= 1 || localZ <= 1 || localX >= 40 || localZ >= 40)) {
				return BlockPalette.WALL_PANEL;
			}
		}
		return null;
	}

	private BlockState orbitalWebCoreStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1511, 60, 360);
		int level = y - baseY;
		int centerY = 188;
		long dist2 = (long) dx * dx + (long) dz * dz;
		if (level < 0 || level > 380 || dist2 > 360L * 360L) {
			return null;
		}

		long core2 = (long) dx * dx + (long) dz * dz + (long) (level - centerY) * (level - centerY);
		if (core2 <= 58L * 58L) {
			boolean shell = core2 >= 48L * 48L || Math.floorMod(level, 19) <= 1;
			return shell ? BlockPalette.DARK_STONE : BlockPalette.AIR;
		}
		if ((Math.abs(level - centerY) <= 1 || Math.abs(level - centerY - 34) <= 1 || Math.abs(level - centerY + 34) <= 1)
				&& ((dist2 >= 84L * 84L && dist2 <= 90L * 90L)
				|| (dist2 >= 140L * 140L && dist2 <= 148L * 148L))) {
			return BlockPalette.GRATE;
		}

		int[][] nodes = {
				{-228, 124, 118, 34}, {210, -146, 244, 44}, {-118, -252, 286, 28},
				{264, 188, 108, 36}, {-306, -38, 230, 30}, {86, 286, 306, 26},
				{-42, 196, 84, 22}, {308, -12, 318, 24}, {-260, 244, 168, 28}
		};
		for (int i = 0; i < nodes.length; i++) {
			int nx = nodes[i][0];
			int nz = nodes[i][1];
			int ny = nodes[i][2];
			int radius = nodes[i][3];
			long node2 = (long) (dx - nx) * (dx - nx)
					+ (long) (dz - nz) * (dz - nz)
					+ (long) (level - ny) * (level - ny);
			if (node2 <= (long) radius * radius && node2 >= (long) (radius - 7) * (radius - 7)) {
				return Math.floorMod(level + i, 27) <= 2 ? BlockPalette.LAMP : BlockPalette.WALL_PANEL;
			}
			if (nearSegment(dx, level, dz, 0, centerY, 0, nx, ny, nz, i % 3 == 0 ? 3 : 2)) {
				return Math.floorMod(level + i, 13) == 0 ? BlockPalette.RUST_PIPE : BlockPalette.PIPE;
			}
			int next = (i + 2) % nodes.length;
			if (i % 2 == 0 && nearSegment(dx, level, dz, nx, ny, nz, nodes[next][0], nodes[next][2], nodes[next][1], 2)) {
				return BlockPalette.GRATE;
			}
		}

		for (int spoke = 0; spoke < 10; spoke++) {
			long spokeHash = MegastructureMath.hash(districtHash(x, z, 1511), spoke, 0, 1661);
			int ex = TREE_DIRECTIONS_X[spoke] * MegastructureMath.range(spokeHash, 230, 342) / 100;
			int ez = TREE_DIRECTIONS_Z[spoke] * MegastructureMath.range(spokeHash >>> 10, 230, 342) / 100;
			int ey = MegastructureMath.range(spokeHash >>> 20, 64, 344);
			if (nearSegment(dx, level, dz, 0, centerY, 0, ex, ey, ez, 1)) {
				return BlockPalette.PIPE;
			}
		}

		boolean outerDisk = (level == 46 || level == 334)
				&& dist2 >= 248L * 248L && dist2 <= 318L * 318L
				&& Math.floorMod(dx * 3 + dz * 5, 29) <= 2;
		return outerDisk ? BlockPalette.WALKWAY : null;
	}

	private BlockState crownSpireStructureState(int x, int y, int z) {
		int localX = districtLocalX(x);
		int localZ = districtLocalZ(z);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1521, 34, 240);
		int level = y - baseY;
		if (level < 0 || y > settings.ceilingY() - 18) {
			return null;
		}
		long dist2 = (long) dx * dx + (long) dz * dz;
		if (level == 0 && dist2 <= 462L * 462L) {
			return Math.floorMod(dx * 7 + dz * 11, 41) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.PLATFORM;
		}
		int shaftRadius = Math.max(30, 156 - level / 5);
		if (level <= 660 && dist2 <= (long) shaftRadius * shaftRadius && dist2 >= (long) (shaftRadius - 7) * (shaftRadius - 7)) {
			return Math.floorMod(level, 31) <= 1 ? BlockPalette.WALL_PANEL : BlockPalette.LIGHT_STONE;
		}
		if (level <= 660 && Math.floorMod(level, 26) == 0 && dist2 >= (long) (shaftRadius + 8) * (shaftRadius + 8)
				&& dist2 <= (long) (shaftRadius + 22) * (shaftRadius + 22)) {
			return BlockPalette.WALKWAY;
		}
		int crownBase = 520;
		if (level >= crownBase - 18 && level <= crownBase + 4 && dist2 <= 334L * 334L) {
			boolean outerLip = dist2 >= 306L * 306L;
			boolean serviceGroove = Math.floorMod(dx * 5 + dz * 7, 37) <= 1;
			if (outerLip || level == crownBase - 2 || level == crownBase + 2) {
				return serviceGroove ? BlockPalette.CRACKED_PANEL : BlockPalette.PLATFORM;
			}
			return Math.floorMod(dx * 11 + dz * 13 + level, 53) == 0 ? BlockPalette.GRATE : BlockPalette.DARK_STONE;
		}
		if (level >= crownBase - 104 && level < crownBase - 18 && dist2 <= 344L * 344L) {
			boolean radialRib = (Math.abs(dx) <= 6 || Math.abs(dz) <= 6
					|| Math.abs(Math.abs(dx) - Math.abs(dz)) <= 5)
					&& dist2 >= 102L * 102L;
			int ring = (int) Math.round(Math.sqrt(dist2));
			boolean annularRib = Math.floorMod(ring - 122, 46) <= 4 && dist2 >= 118L * 118L;
			boolean hangingColumn = Math.floorMod(dx + 512, 64) <= 5
					&& Math.floorMod(dz + 512, 64) <= 5
					&& level >= crownBase - 96;
			if (radialRib || annularRib || hangingColumn) {
				return Math.floorMod(level, 29) == 0 ? BlockPalette.WALL_PANEL : BlockPalette.FOUNDATION;
			}
		}
		if (level >= crownBase && level <= 760 && dist2 <= 310L * 310L) {
			int cellX = Math.floorDiv(dx + 320, 28);
			int cellZ = Math.floorDiv(dz + 320, 28);
			long hash = MegastructureMath.hash(districtHash(x, z, 1521), cellX, cellZ, 1539);
			int towerX = cellX * 28 - 306 + MegastructureMath.range(hash, -4, 4);
			int towerZ = cellZ * 28 - 306 + MegastructureMath.range(hash >>> 9, -4, 4);
			int half = MegastructureMath.range(hash >>> 18, 5, 14);
			int height = MegastructureMath.range(hash >>> 27, 28, 154);
			int bottom = crownBase + MegastructureMath.range(hash >>> 36, 0, 42);
			boolean cityBlock = Math.abs(dx - towerX) <= half && Math.abs(dz - towerZ) <= half && level >= bottom && level <= bottom + height
					&& Math.floorMod(hash, 5) != 0;
			if (cityBlock) {
				boolean shell = Math.abs(dx - towerX) >= half - 1 || Math.abs(dz - towerZ) >= half - 1 || Math.floorMod(level - bottom, 18) == 0;
				return shell ? BlockPalette.WALL_PANEL : BlockPalette.AIR;
			}
			if (Math.floorMod(level - crownBase, 34) == 0 && dist2 >= 170L * 170L && dist2 <= 300L * 300L) {
				return BlockPalette.WALKWAY;
			}
		}
		for (int mast = 0; mast < 10; mast++) {
			long hash = MegastructureMath.hash(districtHash(x, z, 1521), mast, 0, 1547);
			int mx = MegastructureMath.range(hash, -260, 260);
			int mz = MegastructureMath.range(hash >>> 12, -260, 260);
			int bottom = 635 + MegastructureMath.range(hash >>> 24, 0, 92);
			int height = MegastructureMath.range(hash >>> 36, 80, 230);
			if (level >= bottom && level <= bottom + height && (long) (dx - mx) * (dx - mx) + (long) (dz - mz) * (dz - mz) <= 3L * 3L) {
				return Math.floorMod(level, 19) == 0 ? BlockPalette.LAMP : BlockPalette.PIPE;
			}
		}
		return null;
	}

	private BlockState globeMonumentStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1527, 60, 320);
		int level = y - baseY;
		if (level < 0 || level > 430) {
			return null;
		}
		long dist2 = (long) dx * dx + (long) dz * dz;
		if (level == 0 && dist2 <= 190L * 190L) {
			return BlockPalette.GLOBE_SUPPORT;
		}
		if (level <= 172) {
			for (int support = 0; support < 12; support++) {
				double angle = support * Math.PI * 2.0 / 12.0;
				int baseX = (int) Math.round(Math.cos(angle) * 166.0);
				int baseZ = (int) Math.round(Math.sin(angle) * 166.0);
				int contactX = (int) Math.round(Math.cos(angle) * 72.0);
				int contactZ = (int) Math.round(Math.sin(angle) * 72.0);
				if (nearSegment(dx, level, dz, baseX, 0, baseZ, contactX, 158, contactZ, 6)) {
					return Math.floorMod(level, 23) == 0 ? BlockPalette.GLOBE_RIB : BlockPalette.GLOBE_SUPPORT;
				}
				int braceX = (int) Math.round(Math.cos(angle + Math.PI / 12.0) * 128.0);
				int braceZ = (int) Math.round(Math.sin(angle + Math.PI / 12.0) * 128.0);
				if (nearSegment(dx, level, dz, baseX, 28, baseZ, braceX, 112, braceZ, 3)) {
					return BlockPalette.GLOBE_SUPPORT;
				}
			}
		}
		int cy = 250;
		int radius = 118;
		long sphere = dist2 + (long) (level - cy) * (level - cy);
		if (sphere <= (long) radius * radius && sphere >= (long) (radius - 7) * (radius - 7)) {
			boolean rib = Math.floorMod((int) Math.round(Math.atan2(dz, dx) * 96.0), 12) == 0
					|| Math.floorMod(level - cy + 160, 28) <= 2;
			if (rib) {
				return BlockPalette.GLOBE_RIB;
			}
			return Math.floorMod(dx * 5 + dz * 7 + level, 37) == 0 ? BlockPalette.GLOBE_PANEL : BlockPalette.GLOBE_SHELL;
		}
		if (Math.abs(level - cy) <= 2 && dist2 >= (long) (radius + 8) * (radius + 8) && dist2 <= (long) (radius + 20) * (radius + 20)) {
			return BlockPalette.GLOBE_SUPPORT;
		}
		if (Math.abs(dx) <= 4 && Math.abs(dz) <= 4 && level >= 36 && level <= cy - radius + 24) {
			return BlockPalette.GLOBE_SUPPORT;
		}
		if (level >= cy - radius - 10 && level <= cy - radius + 12 && dist2 <= 44L * 44L) {
			return BlockPalette.GLOBE_RIB;
		}
		return null;
	}

	private BlockState voidAltarStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1531, 52, 340);
		int level = y - baseY;
		if (level < 0 || level > 360) {
			return null;
		}
		long dist2 = (long) dx * dx + (long) dz * dz;
		int pyramidHalf = Math.max(0, 96 - level * 2);
		if (level <= 48 && Math.abs(dx) <= pyramidHalf && Math.abs(dz) <= pyramidHalf) {
			boolean shell = Math.abs(dx) >= pyramidHalf - 2 || Math.abs(dz) >= pyramidHalf - 2 || Math.floorMod(level, 7) == 0;
			return shell ? BlockPalette.LIGHT_STONE : null;
		}
		int apertureY = 292;
		if (Math.abs(level - apertureY) <= 5 && dist2 >= 118L * 118L && dist2 <= 178L * 178L) {
			return Math.floorMod(dx + dz + level, 13) == 0 ? BlockPalette.LAMP : BlockPalette.DARK_STONE;
		}
		for (int tendril = 0; tendril < 16; tendril++) {
			double angle = tendril * Math.PI * 2.0 / 16.0;
			int ax = (int) Math.round(Math.cos(angle) * 148);
			int az = (int) Math.round(Math.sin(angle) * 148);
			if (nearSegment(dx, level, dz, ax, apertureY, az, 0, 58, 0, tendril % 3 == 0 ? 3 : 2)) {
				return tendril % 4 == 0 ? BlockPalette.RUST_PIPE : BlockPalette.PIPE;
			}
		}
		if ((Math.abs(dx) <= 3 || Math.abs(dz) <= 3) && level == 6 && dist2 <= 250L * 250L) {
			return BlockPalette.WALKWAY;
		}
		return null;
	}

	private BlockState atomStormArrayStructureState(int x, int y, int z) {
		int dx = districtLocalX(x) - DISTRICT_SIZE / 2;
		int dz = districtLocalZ(z) - DISTRICT_SIZE / 2;
		int baseY = districtBaseY(x, z, 1537, 48, 320);
		int level = y - baseY;
		if (level < 0 || level > 330) {
			return null;
		}
		long dist2 = (long) dx * dx + (long) dz * dz;
		if (level == 0 && dist2 <= 340L * 340L) {
			return Math.floorMod(dx * 13 + dz * 17, 61) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.PLATFORM;
		}
		int cy = 162;
		long core = dist2 + (long) (level - cy) * (level - cy);
		long noisy = Math.floorMod(MegastructureMath.hash(districtHash(x, z, 1537), dx >> 2, dz >> 2, level >> 2), 38);
		if (core <= (62 + noisy) * (62 + noisy) && core >= 34L * 34L) {
			return Math.floorMod(dx + dz + level, 11) == 0 ? BlockPalette.LAMP : BlockPalette.DARK_STONE;
		}
		if (Math.abs(dist2 - 206L * 206L) <= 520 && Math.abs(level - cy) <= 2) {
			return BlockPalette.GRATE;
		}
		if (Math.abs((long) dx * dx + (long) (level - cy) * (level - cy) - 174L * 174L) <= 430 && Math.abs(dz) <= 2) {
			return BlockPalette.PIPE;
		}
		if (Math.abs((long) dz * dz + (long) (level - cy) * (level - cy) - 150L * 150L) <= 380 && Math.abs(dx) <= 2) {
			return BlockPalette.RUST_PIPE;
		}
		for (int pylon = 0; pylon < 8; pylon++) {
			double angle = pylon * Math.PI * 2.0 / 8.0;
			int px = (int) Math.round(Math.cos(angle) * 286);
			int pz = (int) Math.round(Math.sin(angle) * 286);
			if ((long) (dx - px) * (dx - px) + (long) (dz - pz) * (dz - pz) <= 8L * 8L && level <= 250) {
				return Math.floorMod(level, 31) == 0 ? BlockPalette.LAMP : BlockPalette.FOUNDATION;
			}
		}
		return null;
	}

	private BlockState blackHoleReactorStructureState(int x, int y, int z) {
		int districtX = MegastructureMath.floorDiv(x, DISTRICT_SIZE);
		int districtZ = MegastructureMath.floorDiv(z, DISTRICT_SIZE);
		BlackHoleCoreHint core = blackHoleCoreHintForDistrict(districtX, districtZ);
		int dx = x - core.x();
		int dz = z - core.z();
		int dy = y - core.y();
		int baseY = districtBaseY(x, z, 1543, 54, 300);
		int level = y - baseY;
		if (level < 0 || level > 420) {
			return null;
		}
		long dist2 = (long) dx * dx + (long) dz * dz;
		long coreVoid = dist2 + (long) dy * dy;
		if (coreVoid <= 132L * 132L || (Math.abs(dy) <= 24 && dist2 <= 196L * 196L)) {
			return null;
		}
		if (Math.abs(dy) <= 118 && dist2 >= 142L * 142L && dist2 <= 154L * 154L) {
			boolean meridian = Math.floorMod((int) (Math.atan2(dz, dx) * 128.0), 18) == 0;
			boolean latitude = Math.floorMod(dy + 180, 28) <= 2;
			if (meridian || latitude) {
				return Math.floorMod(dx * 13 + dz * 17 + dy, 19) <= 2 ? BlockPalette.LAMP : BlockPalette.GRATE;
			}
		}
		if (Math.abs(dy) <= 172 && dist2 >= 268L * 268L && dist2 <= 286L * 286L) {
			boolean containmentRib = Math.floorMod(y - baseY, 22) <= 2;
			boolean collectorGap = Math.floorMod((int) Math.round(Math.atan2(dz, dx) * 64.0), 8) == 0;
			if (containmentRib || collectorGap) {
				return Math.floorMod(dx * 5 - dz * 7 + dy, 31) <= 3 ? BlockPalette.LAMP : BlockPalette.DARK_STONE;
			}
		}
		for (int collector = 0; collector < 8; collector++) {
			double angle = collector * Math.PI * 2.0 / 8.0;
			int ax = (int) Math.round(Math.cos(angle) * 182);
			int az = (int) Math.round(Math.sin(angle) * 182);
			int bx = (int) Math.round(Math.cos(angle) * 382);
			int bz = (int) Math.round(Math.sin(angle) * 382);
			if (nearSegment(dx, dy, dz, ax, -126, az, bx, -186, bz, 4)
					|| nearSegment(dx, dy, dz, ax, 126, az, bx, 186, bz, 4)) {
				return Math.floorMod(collector + dy, 13) == 0 ? BlockPalette.LAMP : BlockPalette.PIPE;
			}
		}
		for (int band : new int[] {-118, -82, -46, 46, 82, 118}) {
			if (Math.abs(dy - band) <= 2 && dist2 >= 166L * 166L && dist2 <= 232L * 232L) {
				return Math.floorMod(dx * 17 + dz * 19 + band, 29) <= 2 ? BlockPalette.LAMP : BlockPalette.GRATE;
			}
		}
		for (int spoke = 0; spoke < 24; spoke++) {
			double angle = spoke * Math.PI * 2.0 / 24.0;
			int innerX = (int) Math.round(Math.cos(angle) * 204);
			int innerZ = (int) Math.round(Math.sin(angle) * 204);
			int outerX = (int) Math.round(Math.cos(angle) * 354);
			int outerZ = (int) Math.round(Math.sin(angle) * 354);
			if (Math.abs(dy) <= 4 && nearSegment(dx, dy, dz, innerX, 0, innerZ, outerX, 0, outerZ, 3)) {
				return Math.floorMod(spoke, 3) == 0 ? BlockPalette.RUST_PIPE : BlockPalette.PIPE;
			}
			if (Math.abs(dy - 72) <= 3 && Math.floorMod(spoke, 2) == 0
					&& nearSegment(dx, dy, dz, innerX, 72, innerZ, outerX, 104, outerZ, 2)) {
				return BlockPalette.PIPE;
			}
			if (Math.abs(dy + 72) <= 3 && Math.floorMod(spoke, 2) == 1
					&& nearSegment(dx, dy, dz, innerX, -72, innerZ, outerX, -104, outerZ, 2)) {
				return BlockPalette.PIPE;
			}
		}
		for (int mast = 0; mast < 16; mast++) {
			double angle = mast * Math.PI * 2.0 / 16.0;
			int mx = (int) Math.round(Math.cos(angle) * 184);
			int mz = (int) Math.round(Math.sin(angle) * 184);
			long mastDistance2 = (long) (dx - mx) * (dx - mx) + (long) (dz - mz) * (dz - mz);
			if (mastDistance2 <= 3L * 3L && Math.abs(dy) <= 138) {
				return Math.floorMod(dy + mast * 7, 31) == 0 ? BlockPalette.LAMP : BlockPalette.DARK_STONE;
			}
		}
		if (Math.abs(dy) >= 138 && Math.abs(dy) <= 144 && dist2 >= 132L * 132L && dist2 <= 308L * 308L) {
			return Math.floorMod(dx + dz + dy, 23) == 0 ? BlockPalette.LAMP : BlockPalette.WALL_PANEL;
		}
		if (level == 0 && dist2 <= 392L * 392L) {
			return Math.floorMod(dx * 7 + dz * 5, 67) == 0 ? BlockPalette.CRACKED_PANEL : BlockPalette.DARK_STONE;
		}
		if (Math.abs(dy) <= 3 && dist2 >= 58L * 58L && dist2 <= 132L * 132L) {
			return Math.floorMod(dx + dz + level, 17) == 0 ? BlockPalette.LAMP : BlockPalette.GRATE;
		}
		if (Math.abs((long) dx * dx + (long) dy * dy - 150L * 150L) <= 540 && Math.abs(dz) <= 2) {
			return BlockPalette.PIPE;
		}
		if (Math.abs((long) dz * dz + (long) dy * dy - 150L * 150L) <= 540 && Math.abs(dx) <= 2) {
			return BlockPalette.PIPE;
		}
		for (int tower = 0; tower < 12; tower++) {
			double angle = tower * Math.PI * 2.0 / 12.0;
			int tx = (int) Math.round(Math.cos(angle) * 210);
			int tz = (int) Math.round(Math.sin(angle) * 210);
			if ((long) (dx - tx) * (dx - tx) + (long) (dz - tz) * (dz - tz) <= 9L * 9L && level <= 330) {
				return Math.floorMod(level, 28) == 0 ? BlockPalette.LAMP : BlockPalette.FOUNDATION;
			}
			if (nearSegment(dx, dy, dz, tx, baseY + 48 - core.y(), tz, 0, 0, 0, 2)) {
				return BlockPalette.RUST_PIPE;
			}
		}
		if ((Math.abs(dx) <= 4 || Math.abs(dz) <= 4) && Math.abs(dy) <= 2 && dist2 <= 330L * 330L && dist2 >= 244L * 244L) {
			return BlockPalette.WALKWAY;
		}
		return null;
	}

	private static boolean nearSegment(
			int x,
			int y,
			int z,
			int ax,
			int ay,
			int az,
			int bx,
			int by,
			int bz,
			int thickness
	) {
		double vx = bx - ax;
		double vy = by - ay;
		double vz = bz - az;
		double wx = x - ax;
		double wy = y - ay;
		double wz = z - az;
		double length2 = vx * vx + vy * vy + vz * vz;
		if (length2 <= 0.0001) {
			return false;
		}
		double t = Math.max(0.0, Math.min(1.0, (wx * vx + wy * vy + wz * vz) / length2));
		double px = ax + vx * t;
		double py = ay + vy * t;
		double pz = az + vz * t;
		double dx = x - px;
		double dy = y - py;
		double dz = z - pz;
		return dx * dx + dy * dy + dz * dz <= thickness * thickness;
	}

	private boolean between(int value, int first, int second) {
		return value >= Math.min(first, second) && value <= Math.max(first, second);
	}

	private BlockState facadeState(int x, int y, int z, boolean air) {
		if (air) {
			return null;
		}

		int cell = settings.cellSize();
		int localX = MegastructureMath.floorMod(x, cell);
		int localZ = MegastructureMath.floorMod(z, cell);
		boolean nearWallFace = localX <= 2 || localZ <= 2 || localX >= cell - 3 || localZ >= cell - 3;
		long hash = MegastructureMath.hash(SHAPE_SEED, x / 8, z / 8, y / 12);
		boolean cracked = nearWallFace && Math.floorMod(hash, 97) == 0;
		if (cracked) {
			return BlockPalette.CRACKED_PANEL;
		}
		boolean stain = nearWallFace && Math.floorMod(hash, 131) == 0 && Math.floorMod(y - settings.floorY(), 18) < 8;
		if (stain) {
			return BlockPalette.STAIN;
		}

		boolean panelSeam = Math.floorMod(y - settings.floorY(), 16) == 0 || Math.floorMod(localX, 24) == 0 || Math.floorMod(localZ, 24) == 0;
		if (nearWallFace && panelSeam) {
			return BlockPalette.WALL_PANEL;
		}

		boolean verticalRib = (Math.floorMod(localX, 32) <= 1 || Math.floorMod(localZ, 32) <= 1) && Math.floorMod(y - settings.floorY(), 48) > 4;
		if (nearWallFace && verticalRib) {
			return BlockPalette.FOUNDATION;
		}

		boolean pipeRun = nearWallFace && Math.floorMod(y - settings.floorY(), 37) == 0 && (Math.floorMod(localX + localZ, 19) <= 1);
		if (pipeRun) {
			return BlockPalette.PIPE;
		}

		boolean falseWindow = nearWallFace && Math.floorMod(y - settings.floorY(), 28) >= 7 && Math.floorMod(y - settings.floorY(), 28) <= 9
				&& Math.floorMod(localX + localZ * 3, 41) <= 2;
		if (falseWindow) {
			return BlockPalette.GRATE;
		}

		return null;
	}

	private BlockState stairState(int district, int x, int y, int z) {
		if (district == DISTRICT_NETWORK) {
			BlockState serviceStair = serviceShaftStairState(x, y, z);
			if (serviceStair != null) {
				return serviceStair;
			}
		}

		if (district == DISTRICT_CYLINDER) {
			return cylindricalShaftStairState(x, y, z);
		}

		return null;
	}

	private BlockState squareStairState(int localX, int localZ, int levelY, int minX, int minZ, int maxX, int maxZ) {
		int width = maxX - minX;
		int depth = maxZ - minZ;
		int period = Math.max(4, width * 2 + depth * 2);
		int phase = Math.floorMod(levelY, period);
		int targetX;
		int targetZ;
		Direction facing;
		if (phase < width) {
			targetX = minX + phase;
			targetZ = minZ;
			facing = Direction.EAST;
		} else if (phase < width + depth) {
			targetX = maxX;
			targetZ = minZ + (phase - width);
			facing = Direction.SOUTH;
		} else if (phase < width * 2 + depth) {
			targetX = maxX - (phase - width - depth);
			targetZ = maxZ;
			facing = Direction.WEST;
		} else {
			targetX = minX;
			targetZ = maxZ - (phase - width * 2 - depth);
			facing = Direction.NORTH;
		}

		if (Math.abs(localX - targetX) + Math.abs(localZ - targetZ) <= 1) {
			return BlockPalette.stairs(facing);
		}

		boolean corner = phase == 0 || phase == width || phase == width + depth || phase == width * 2 + depth;
		if (corner && Math.abs(localX - targetX) <= 2 && Math.abs(localZ - targetZ) <= 2) {
			return BlockPalette.WALKWAY;
		}

		return null;
	}

	private BlockState serviceShaftStairState(int x, int y, int z) {
		int cell = 96;
		int localX = MegastructureMath.floorMod(x, cell);
		int localZ = MegastructureMath.floorMod(z, cell);
		if (localX < 8 || localX > 23 || localZ < 8 || localZ > 23) {
			return null;
		}

		return squareStairState(localX, localZ, y - settings.floorY(), 10, 10, 21, 21);
	}

	private BlockState cylindricalShaftStairState(int x, int y, int z) {
		int localX = MegastructureMath.floorMod(x, DISTRICT_SIZE);
		int localZ = MegastructureMath.floorMod(z, DISTRICT_SIZE);
		int dx = localX - DISTRICT_SIZE / 2;
		int dz = localZ - DISTRICT_SIZE / 2;
		int stairX = dx - 58;
		int stairZ = dz + 18;
		if (stairX < 0 || stairX > 36 || stairZ < 0 || stairZ > 36) {
			return null;
		}
		return squareStairState(stairX, stairZ, y - settings.floorY(), 3, 3, 33, 33);
	}

	private BlockState corridorDetailState(int district, int x, int y, int z, boolean air) {
		if (!air) {
			return null;
		}
		if (district != DISTRICT_NETWORK && district != DISTRICT_DEAD_END && district != DISTRICT_DENSE_WALL) {
			return null;
		}

		int floor = MegastructureMath.floorDiv(y - settings.floorY(), 18);
		int baseY = settings.floorY() + 6 + floor * 18;
		if (y != baseY + 4) {
			return null;
		}

		int localX = MegastructureMath.floorMod(x, 18);
		int localZ = MegastructureMath.floorMod(z, 18);
		boolean isolatedLight = Math.floorMod(x, 48) == 24
				&& Math.floorMod(z, 48) == 24
				&& Math.floorMod(floor, 5) == 0;
		if (isolatedLight) {
			return BlockPalette.LAMP;
		}
		return null;
	}

	private BlockState webColumnState(int x, int y, int z) {
		int cell = settings.cellSize();
		int localX = MegastructureMath.floorMod(x, cell);
		int localZ = MegastructureMath.floorMod(z, cell);
		int cellX = MegastructureMath.floorDiv(x, cell);
		int cellZ = MegastructureMath.floorDiv(z, cell);
		long hash = MegastructureMath.hash(SHAPE_SEED, cellX, cellZ, 141);
		if (Math.floorMod(hash, 5) != 0) {
			return null;
		}

		boolean vertical = (Math.abs(localX - 18) <= 1 || Math.abs(localX - 48) <= 1 || Math.abs(localX - 76) <= 1)
				&& (Math.abs(localZ - 18) <= 1 || Math.abs(localZ - 48) <= 1 || Math.abs(localZ - 76) <= 1);
		boolean beamX = Math.floorMod(y - settings.floorY(), 31) <= 2 && Math.abs(localZ - localX) <= 1;
		boolean beamZ = Math.floorMod(y - settings.floorY() + 11, 37) <= 2 && Math.abs(localZ - (cell - 1 - localX)) <= 1;
		if (vertical || beamX || beamZ) {
			return BlockPalette.FOUNDATION;
		}
		return null;
	}

	private BlockState corridorLootChestState(int district, int x, int y, int z, boolean air) {
		if (!air || isPrimaryRift(x, z) || y <= settings.floorY() + 1 || y >= settings.ceilingY() - 2
				|| isSpawnPrecinctAir(x, y, z) || isOasisProtectedRoute(x, y, z)) {
			return null;
		}
		if (Math.abs(x) < 192 && Math.abs(z) < 192) {
			return null;
		}
		if (isStructureAir(district, x, y - 1, z)
				|| !isStructureAir(district, x, y + 1, z)
				|| !isStructureAir(district, x, y + 2, z)) {
			return null;
		}
		int cell = 48;
		int cellX = MegastructureMath.floorDiv(x, cell);
		int cellZ = MegastructureMath.floorDiv(z, cell);
		long hash = MegastructureMath.hash(worldVariantSeed, cellX, cellZ, 2917);
		if (Math.floorMod(hash, 11) != 0) {
			return null;
		}
		int anchorX = cellX * cell + MegastructureMath.range(hash >>> 8, 8, cell - 9);
		int anchorZ = cellZ * cell + MegastructureMath.range(hash >>> 16, 8, cell - 9);
		if (x != anchorX || z != anchorZ) {
			return null;
		}
		Direction facing = switch (Math.floorMod(hash >>> 24, 4)) {
			case 0 -> Direction.NORTH;
			case 1 -> Direction.SOUTH;
			case 2 -> Direction.WEST;
			default -> Direction.EAST;
		};
		return Blocks.CHEST.getDefaultState()
				.with(ChestBlock.FACING, facing)
				.with(ChestBlock.WATERLOGGED, false);
	}

	private boolean isStructureAir(int district, int x, int y, int z) {
		if (y < settings.floorY() || y > settings.ceilingY()) {
			return false;
		}
		return isRailwayAir(x, y, z) || districtAir(district, x, y, z);
	}

	private void populateCorridorLootChests(StructureWorldAccess world, Chunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int minY = Math.max(chunk.getBottomY(), settings.floorY() + 1);
		int maxY = Math.min(chunk.getTopY(), settings.ceilingY());
		for (int localX = 0; localX < 16; localX++) {
			int x = chunkPos.getStartX() + localX;
			for (int localZ = 0; localZ < 16; localZ++) {
				int z = chunkPos.getStartZ() + localZ;
				int district = districtType(x, z);
				for (int y = minY; y < maxY; y++) {
					mutable.set(x, y, z);
					BlockState state = chunk.getBlockState(mutable);
					if (!state.isOf(Blocks.CHEST) || corridorLootChestState(district, x, y, z, true) == null) {
						continue;
					}
					world.setBlockState(mutable, state, 2);
					BlockEntity blockEntity = world.getBlockEntity(mutable);
					if (blockEntity instanceof ChestBlockEntity chest) {
						fillCorridorLootChest(chest, x, y, z);
					}
				}
			}
		}
	}

	private void fillCorridorLootChest(ChestBlockEntity chest, int x, int y, int z) {
		long hash = MegastructureMath.hash(worldVariantSeed, x, z, y ^ 2939);
		int stacks = MegastructureMath.range(hash, 3, 7);
		for (int stack = 0; stack < stacks; stack++) {
			long stackHash = MegastructureMath.hash(hash, stack, 0, 2953);
			Item item = DynamicWorldgenPalette.corridorLootItem(stackHash);
			int maxCount = Math.max(1, Math.min(item.getMaxCount(), 12));
			int count = MegastructureMath.range(stackHash >>> 12, 1, maxCount);
			int slot = Math.floorMod((int) (stackHash >>> 24), chest.size());
			if (chest.getStack(slot).isEmpty()) {
				chest.setStack(slot, new ItemStack(item, count));
			}
		}
		chest.markDirty();
	}

	private BlockState wallDetail(int x, int y, int z) {
		int cell = settings.cellSize();
		int localX = MegastructureMath.floorMod(x, cell);
		int localZ = MegastructureMath.floorMod(z, cell);
		int edge = MegastructureMath.manhattanToCellEdge(localX, localZ, cell);
		if (edge > 2) {
			return null;
		}

		long hash = MegastructureMath.hash(SHAPE_SEED, x / 4, z / 4, y / 8);
		if (Math.floorMod(y, 23) == 0 && Math.floorMod(hash, 4) == 0) {
			return BlockPalette.PIPE;
		}
		if (Math.floorMod(y, 31) == 0 && Math.floorMod(hash, 7) == 0) {
			return BlockPalette.RUST_PIPE;
		}
		return null;
	}


	private BlockState wallOreState(int district, int x, int y, int z) {
		if (settings.oreRate() <= 0 || y <= settings.floorY() + 8 || y >= settings.ceilingY() - 8) {
			return null;
		}
		int cellX = MegastructureMath.floorDiv(x, 18);
		int cellY = MegastructureMath.floorDiv(y - settings.floorY(), 12);
		int cellZ = MegastructureMath.floorDiv(z, 18);
		long veinHash = MegastructureMath.hash(worldVariantSeed, cellX, cellZ, cellY ^ 3011);
		int chance = Math.max(16, 92 - settings.oreRate());
		if (Math.floorMod(veinHash, chance) != 0) {
			return null;
		}
		int centerX = cellX * 18 + MegastructureMath.range(veinHash >>> 8, 3, 15);
		int centerY = settings.floorY() + cellY * 12 + MegastructureMath.range(veinHash >>> 16, 2, 10);
		int centerZ = cellZ * 18 + MegastructureMath.range(veinHash >>> 24, 3, 15);
		int radiusX = MegastructureMath.range(veinHash >>> 32, 3, 7);
		int radiusY = MegastructureMath.range(veinHash >>> 40, 2, 5);
		int radiusZ = MegastructureMath.range(veinHash >>> 48, 3, 7);
		double dx = (x - centerX) / (double) radiusX;
		double dy = (y - centerY) / (double) radiusY;
		double dz = (z - centerZ) / (double) radiusZ;
		if (dx * dx + dy * dy + dz * dz > 1.0D) {
			return null;
		}
		long chipHash = MegastructureMath.hash(veinHash, x, y, z);
		if (Math.floorMod(chipHash, 9) == 0) {
			return null;
		}
		return DynamicWorldgenPalette.ore(veinHash ^ (long) district * 31L);
	}

	private BlockState massState(int x, int y, int z) {
		long hash = MegastructureMath.hash(SHAPE_SEED, Math.floorDiv(x, 5), Math.floorDiv(y, 7), Math.floorDiv(z, 5));
		if (Math.floorMod(hash, 43) == 0) {
			return BlockPalette.MASS_STONE_VARIANT;
		}
		if (Math.floorMod(hash, 18) == 0) {
			return BlockPalette.MASS_ANDESITE_VARIANT;
		}
		if (Math.floorMod(hash >>> 11, 37) == 0) {
			return BlockPalette.MASS_GRANITE_VARIANT;
		}
		return BlockPalette.MASS;
	}

	private String motifName(int x, int z) {
		if (isPrimaryRift(x, z)) {
			return "primary rift";
		}
		return districtName(districtType(x, z));
	}

	private String districtName(int district) {
		return switch (district) {
			case DISTRICT_NETWORK -> "interior network";
			case DISTRICT_DEAD_END -> "dead-end corridor district";
			case DISTRICT_MONOLITH_HALL -> "monolith hall";
			case DISTRICT_COLUMN_FOREST -> "column forest";
			case DISTRICT_CYLINDER -> "cylindrical atrium";
			case DISTRICT_ABYSS -> "abyss dwelling";
			case DISTRICT_DESCENT -> "descent well";
			case DISTRICT_BLOCK_TOWERS -> "block tower field";
			case DISTRICT_TANK_CLUSTER -> "tank cluster";
			case DISTRICT_SCAFFOLD -> "scaffold chamber";
			case DISTRICT_INDUSTRIAL_WALL -> "industrial wall";
			case DISTRICT_TRANSIT_NEXUS -> "transit nexus";
			case DISTRICT_REACTOR_CATHEDRAL -> "reactor cathedral";
			case DISTRICT_HANGING_ARCHIVE -> "hanging archive";
			case DISTRICT_VENTILATION_CANYON -> "ventilation canyon";
			case DISTRICT_INVERTED_PYRAMID -> "inverted pyramid";
			case DISTRICT_RING_VAULT -> "ring vault";
			case DISTRICT_MACHINE_NAVE -> "machine nave";
			case DISTRICT_FRACTURED_HABITAT -> "fractured habitat";
			case DISTRICT_CONDUIT_BASILICA -> "conduit basilica";
			case DISTRICT_RESERVOIR_HALL -> "reservoir hall";
			case DISTRICT_SUSPENDED_CITY -> "suspended city";
			case DISTRICT_IRIS_CHASM -> "iris chasm";
			case DISTRICT_MACHINE_ROOT_VAULT -> "machine-root vault";
			case DISTRICT_TILTED_STACKS -> "tilted habitat stacks";
			case DISTRICT_SILENT_FOUNDRY -> "silent foundry";
			case DISTRICT_COLOSSUS_LIFT -> "colossus lift";
			case DISTRICT_FOLDED_CITY -> "folded city";
			case DISTRICT_UPPER_RIM_CITY -> "upper rim city";
			case DISTRICT_ORBITAL_WEB_CORE -> "orbital web core";
			case DISTRICT_CROWN_SPIRE -> "crown spire";
			case DISTRICT_GLOBE_MONUMENT -> "globe monument";
			case DISTRICT_VOID_ALTAR -> "void altar";
			case DISTRICT_ATOM_STORM_ARRAY -> "atom storm array";
			case DISTRICT_BLACK_HOLE_REACTOR -> "black hole reactor";
			default -> "dense wall";
		};
	}
}
