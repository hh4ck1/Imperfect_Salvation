package ru.nikit.megastructure.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

public final class DistrictBiomeSource extends BiomeSource {
	public static final Codec<DistrictBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Biome.REGISTRY_CODEC.fieldOf("primary_rift").forGetter(source -> source.primaryRift),
			Biome.REGISTRY_CODEC.fieldOf("railway_tunnel").forGetter(source -> source.railwayTunnel),
			Biome.REGISTRY_CODEC.fieldOf("interior_network").forGetter(source -> source.interiorNetwork),
			Biome.REGISTRY_CODEC.fieldOf("dead_end_corridors").forGetter(source -> source.deadEndCorridors),
			Biome.REGISTRY_CODEC.fieldOf("monolith_hall").forGetter(source -> source.monolithHall),
			Biome.REGISTRY_CODEC.fieldOf("column_forest").forGetter(source -> source.columnForest),
			Biome.REGISTRY_CODEC.fieldOf("cylindrical_atrium").forGetter(source -> source.cylindricalAtrium),
			Biome.REGISTRY_CODEC.fieldOf("abyss_dwelling").forGetter(source -> source.abyssDwelling),
			Biome.REGISTRY_CODEC.fieldOf("descent_well").forGetter(source -> source.descentWell),
			Biome.REGISTRY_CODEC.fieldOf("titan_tower_hall").forGetter(source -> source.titanTowerHall),
			Biome.REGISTRY_CODEC.fieldOf("tank_cluster").forGetter(source -> source.tankCluster),
			Biome.REGISTRY_CODEC.fieldOf("scaffold_chamber").forGetter(source -> source.scaffoldChamber),
			Biome.REGISTRY_CODEC.fieldOf("industrial_wall").forGetter(source -> source.industrialWall),
			Biome.REGISTRY_CODEC.fieldOf("dense_wall").forGetter(source -> source.denseWall),
			AdditionalDistricts.CODEC.fieldOf("additional_districts").forGetter(DistrictBiomeSource::additionalDistricts)
	).apply(instance, DistrictBiomeSource::new));

	private record AdditionalDistricts(
			RegistryEntry<Biome> transitNexus,
			RegistryEntry<Biome> reactorCathedral,
			RegistryEntry<Biome> hangingArchive,
			RegistryEntry<Biome> ventilationCanyon,
			RegistryEntry<Biome> invertedPyramid,
			RegistryEntry<Biome> ringVault,
			RegistryEntry<Biome> machineNave,
			RegistryEntry<Biome> fracturedHabitat,
			RegistryEntry<Biome> conduitBasilica,
			RegistryEntry<Biome> reservoirHall,
			RegistryEntry<Biome> suspendedCity,
			RegistryEntry<Biome> irisChasm,
			RegistryEntry<Biome> machineRootVault,
			RegistryEntry<Biome> tiltedStacks,
			RegistryEntry<Biome> silentFoundry,
			NewestDistricts newestDistricts
	) {
		private static final Codec<AdditionalDistricts> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Biome.REGISTRY_CODEC.fieldOf("transit_nexus").forGetter(AdditionalDistricts::transitNexus),
				Biome.REGISTRY_CODEC.fieldOf("reactor_cathedral").forGetter(AdditionalDistricts::reactorCathedral),
				Biome.REGISTRY_CODEC.fieldOf("hanging_archive").forGetter(AdditionalDistricts::hangingArchive),
				Biome.REGISTRY_CODEC.fieldOf("ventilation_canyon").forGetter(AdditionalDistricts::ventilationCanyon),
				Biome.REGISTRY_CODEC.fieldOf("inverted_pyramid").forGetter(AdditionalDistricts::invertedPyramid),
				Biome.REGISTRY_CODEC.fieldOf("ring_vault").forGetter(AdditionalDistricts::ringVault),
				Biome.REGISTRY_CODEC.fieldOf("machine_nave").forGetter(AdditionalDistricts::machineNave),
				Biome.REGISTRY_CODEC.fieldOf("fractured_habitat").forGetter(AdditionalDistricts::fracturedHabitat),
				Biome.REGISTRY_CODEC.fieldOf("conduit_basilica").forGetter(AdditionalDistricts::conduitBasilica),
				Biome.REGISTRY_CODEC.fieldOf("reservoir_hall").forGetter(AdditionalDistricts::reservoirHall),
				Biome.REGISTRY_CODEC.fieldOf("suspended_city").forGetter(AdditionalDistricts::suspendedCity),
				Biome.REGISTRY_CODEC.fieldOf("iris_chasm").forGetter(AdditionalDistricts::irisChasm),
				Biome.REGISTRY_CODEC.fieldOf("machine_root_vault").forGetter(AdditionalDistricts::machineRootVault),
				Biome.REGISTRY_CODEC.fieldOf("tilted_stacks").forGetter(AdditionalDistricts::tiltedStacks),
				Biome.REGISTRY_CODEC.fieldOf("silent_foundry").forGetter(AdditionalDistricts::silentFoundry),
				NewestDistricts.CODEC.fieldOf("newest_districts").forGetter(AdditionalDistricts::newestDistricts)
		).apply(instance, AdditionalDistricts::new));
	}

	private record NewestDistricts(
			RegistryEntry<Biome> colossusLift,
			RegistryEntry<Biome> foldedCity,
			RegistryEntry<Biome> upperRimCity,
			RegistryEntry<Biome> orbitalWebCore,
			RegistryEntry<Biome> crownSpire,
			RegistryEntry<Biome> globeMonument,
			RegistryEntry<Biome> voidAltar,
			RegistryEntry<Biome> atomStormArray,
			RegistryEntry<Biome> blackHoleReactor
	) {
		private static final Codec<NewestDistricts> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Biome.REGISTRY_CODEC.fieldOf("colossus_lift").forGetter(NewestDistricts::colossusLift),
				Biome.REGISTRY_CODEC.fieldOf("folded_city").forGetter(NewestDistricts::foldedCity),
				Biome.REGISTRY_CODEC.fieldOf("upper_rim_city").forGetter(NewestDistricts::upperRimCity),
				Biome.REGISTRY_CODEC.fieldOf("orbital_web_core").forGetter(NewestDistricts::orbitalWebCore),
				Biome.REGISTRY_CODEC.fieldOf("crown_spire").forGetter(NewestDistricts::crownSpire),
				Biome.REGISTRY_CODEC.fieldOf("globe_monument").forGetter(NewestDistricts::globeMonument),
				Biome.REGISTRY_CODEC.fieldOf("void_altar").forGetter(NewestDistricts::voidAltar),
				Biome.REGISTRY_CODEC.fieldOf("atom_storm_array").forGetter(NewestDistricts::atomStormArray),
				Biome.REGISTRY_CODEC.fieldOf("black_hole_reactor").forGetter(NewestDistricts::blackHoleReactor)
		).apply(instance, NewestDistricts::new));
	}

	private final RegistryEntry<Biome> primaryRift;
	private final RegistryEntry<Biome> railwayTunnel;
	private final RegistryEntry<Biome> interiorNetwork;
	private final RegistryEntry<Biome> deadEndCorridors;
	private final RegistryEntry<Biome> monolithHall;
	private final RegistryEntry<Biome> columnForest;
	private final RegistryEntry<Biome> cylindricalAtrium;
	private final RegistryEntry<Biome> abyssDwelling;
	private final RegistryEntry<Biome> descentWell;
	private final RegistryEntry<Biome> titanTowerHall;
	private final RegistryEntry<Biome> tankCluster;
	private final RegistryEntry<Biome> scaffoldChamber;
	private final RegistryEntry<Biome> industrialWall;
	private final RegistryEntry<Biome> denseWall;
	private final RegistryEntry<Biome> transitNexus;
	private final RegistryEntry<Biome> reactorCathedral;
	private final RegistryEntry<Biome> hangingArchive;
	private final RegistryEntry<Biome> ventilationCanyon;
	private final RegistryEntry<Biome> invertedPyramid;
	private final RegistryEntry<Biome> ringVault;
	private final RegistryEntry<Biome> machineNave;
	private final RegistryEntry<Biome> fracturedHabitat;
	private final RegistryEntry<Biome> conduitBasilica;
	private final RegistryEntry<Biome> reservoirHall;
	private final RegistryEntry<Biome> suspendedCity;
	private final RegistryEntry<Biome> irisChasm;
	private final RegistryEntry<Biome> machineRootVault;
	private final RegistryEntry<Biome> tiltedStacks;
	private final RegistryEntry<Biome> silentFoundry;
	private final RegistryEntry<Biome> colossusLift;
	private final RegistryEntry<Biome> foldedCity;
	private final RegistryEntry<Biome> upperRimCity;
	private final RegistryEntry<Biome> orbitalWebCore;
	private final RegistryEntry<Biome> crownSpire;
	private final RegistryEntry<Biome> globeMonument;
	private final RegistryEntry<Biome> voidAltar;
	private final RegistryEntry<Biome> atomStormArray;
	private final RegistryEntry<Biome> blackHoleReactor;
	private volatile long worldVariantSeed = MegastructureChunkGenerator.DISTRICT_SEED;

	public DistrictBiomeSource(
			RegistryEntry<Biome> primaryRift,
			RegistryEntry<Biome> railwayTunnel,
			RegistryEntry<Biome> interiorNetwork,
			RegistryEntry<Biome> deadEndCorridors,
			RegistryEntry<Biome> monolithHall,
			RegistryEntry<Biome> columnForest,
			RegistryEntry<Biome> cylindricalAtrium,
			RegistryEntry<Biome> abyssDwelling,
			RegistryEntry<Biome> descentWell,
			RegistryEntry<Biome> titanTowerHall,
			RegistryEntry<Biome> tankCluster,
			RegistryEntry<Biome> scaffoldChamber,
			RegistryEntry<Biome> industrialWall,
			RegistryEntry<Biome> denseWall,
			AdditionalDistricts additionalDistricts
	) {
		this.primaryRift = primaryRift;
		this.railwayTunnel = railwayTunnel;
		this.interiorNetwork = interiorNetwork;
		this.deadEndCorridors = deadEndCorridors;
		this.monolithHall = monolithHall;
		this.columnForest = columnForest;
		this.cylindricalAtrium = cylindricalAtrium;
		this.abyssDwelling = abyssDwelling;
		this.descentWell = descentWell;
		this.titanTowerHall = titanTowerHall;
		this.tankCluster = tankCluster;
		this.scaffoldChamber = scaffoldChamber;
		this.industrialWall = industrialWall;
		this.denseWall = denseWall;
		this.transitNexus = additionalDistricts.transitNexus();
		this.reactorCathedral = additionalDistricts.reactorCathedral();
		this.hangingArchive = additionalDistricts.hangingArchive();
		this.ventilationCanyon = additionalDistricts.ventilationCanyon();
		this.invertedPyramid = additionalDistricts.invertedPyramid();
		this.ringVault = additionalDistricts.ringVault();
		this.machineNave = additionalDistricts.machineNave();
		this.fracturedHabitat = additionalDistricts.fracturedHabitat();
		this.conduitBasilica = additionalDistricts.conduitBasilica();
		this.reservoirHall = additionalDistricts.reservoirHall();
		this.suspendedCity = additionalDistricts.suspendedCity();
		this.irisChasm = additionalDistricts.irisChasm();
		this.machineRootVault = additionalDistricts.machineRootVault();
		this.tiltedStacks = additionalDistricts.tiltedStacks();
		this.silentFoundry = additionalDistricts.silentFoundry();
		this.colossusLift = additionalDistricts.newestDistricts().colossusLift();
		this.foldedCity = additionalDistricts.newestDistricts().foldedCity();
		this.upperRimCity = additionalDistricts.newestDistricts().upperRimCity();
		this.orbitalWebCore = additionalDistricts.newestDistricts().orbitalWebCore();
		this.crownSpire = additionalDistricts.newestDistricts().crownSpire();
		this.globeMonument = additionalDistricts.newestDistricts().globeMonument();
		this.voidAltar = additionalDistricts.newestDistricts().voidAltar();
		this.atomStormArray = additionalDistricts.newestDistricts().atomStormArray();
		this.blackHoleReactor = additionalDistricts.newestDistricts().blackHoleReactor();
	}

	private AdditionalDistricts additionalDistricts() {
		return new AdditionalDistricts(
				transitNexus,
				reactorCathedral,
				hangingArchive,
				ventilationCanyon,
				invertedPyramid,
				ringVault,
				machineNave,
				fracturedHabitat,
				conduitBasilica,
				reservoirHall,
				suspendedCity,
				irisChasm,
				machineRootVault,
				tiltedStacks,
				silentFoundry,
				new NewestDistricts(
						colossusLift,
						foldedCity,
						upperRimCity,
						orbitalWebCore,
						crownSpire,
						globeMonument,
						voidAltar,
						atomStormArray,
						blackHoleReactor
				)
		);
	}

	@Override
	protected Codec<? extends BiomeSource> getCodec() {
		return CODEC;
	}

	void setWorldVariantSeed(long worldVariantSeed) {
		this.worldVariantSeed = worldVariantSeed;
	}

	@Override
	protected Stream<RegistryEntry<Biome>> biomeStream() {
		return Stream.of(
				primaryRift,
				railwayTunnel,
				interiorNetwork,
				deadEndCorridors,
				monolithHall,
				columnForest,
				cylindricalAtrium,
				abyssDwelling,
				descentWell,
				titanTowerHall,
				tankCluster,
				scaffoldChamber,
				industrialWall,
				denseWall,
				transitNexus,
				reactorCathedral,
				hangingArchive,
				ventilationCanyon,
				invertedPyramid,
				ringVault,
				machineNave,
				fracturedHabitat,
				conduitBasilica,
				reservoirHall,
				suspendedCity,
				irisChasm,
				machineRootVault,
				tiltedStacks,
				silentFoundry,
				colossusLift,
				foldedCity,
				upperRimCity,
				orbitalWebCore,
				crownSpire,
				globeMonument,
				voidAltar,
				atomStormArray,
				blackHoleReactor
		);
	}

	@Override
	public RegistryEntry<Biome> getBiome(int biomeX, int biomeY, int biomeZ, MultiNoiseUtil.MultiNoiseSampler noise) {
		int x = biomeX << 2;
		int z = biomeZ << 2;
		if (MegastructureChunkGenerator.isRailwayLineAt(x, z)) {
			return railwayTunnel;
		}
		if (MegastructureChunkGenerator.isPrimaryRiftBiomeAt(x, z, MegastructureSettings.DEFAULT.motifCellSize(), MegastructureSettings.DEFAULT.riftMinWidth(), MegastructureSettings.DEFAULT.riftMaxWidth())) {
			return primaryRift;
		}

		int district = MegastructureChunkGenerator.districtTypeAt(x, z, worldVariantSeed);
		if (!MegastructureChunkGenerator.isDistrictBiomeFootprintAt(district, x, z)) {
			return interiorNetwork;
		}

		return switch (district) {
			case MegastructureChunkGenerator.DISTRICT_NETWORK -> interiorNetwork;
			case MegastructureChunkGenerator.DISTRICT_DEAD_END -> deadEndCorridors;
			case MegastructureChunkGenerator.DISTRICT_MONOLITH_HALL -> monolithHall;
			case MegastructureChunkGenerator.DISTRICT_COLUMN_FOREST -> columnForest;
			case MegastructureChunkGenerator.DISTRICT_CYLINDER -> cylindricalAtrium;
			case MegastructureChunkGenerator.DISTRICT_ABYSS -> abyssDwelling;
			case MegastructureChunkGenerator.DISTRICT_DESCENT -> descentWell;
			case MegastructureChunkGenerator.DISTRICT_BLOCK_TOWERS -> titanTowerHall;
			case MegastructureChunkGenerator.DISTRICT_TANK_CLUSTER -> tankCluster;
			case MegastructureChunkGenerator.DISTRICT_SCAFFOLD -> scaffoldChamber;
			case MegastructureChunkGenerator.DISTRICT_INDUSTRIAL_WALL -> industrialWall;
			case MegastructureChunkGenerator.DISTRICT_TRANSIT_NEXUS -> transitNexus;
			case MegastructureChunkGenerator.DISTRICT_REACTOR_CATHEDRAL -> reactorCathedral;
			case MegastructureChunkGenerator.DISTRICT_HANGING_ARCHIVE -> hangingArchive;
			case MegastructureChunkGenerator.DISTRICT_VENTILATION_CANYON -> ventilationCanyon;
			case MegastructureChunkGenerator.DISTRICT_INVERTED_PYRAMID -> invertedPyramid;
			case MegastructureChunkGenerator.DISTRICT_RING_VAULT -> ringVault;
			case MegastructureChunkGenerator.DISTRICT_MACHINE_NAVE -> machineNave;
			case MegastructureChunkGenerator.DISTRICT_FRACTURED_HABITAT -> fracturedHabitat;
			case MegastructureChunkGenerator.DISTRICT_CONDUIT_BASILICA -> conduitBasilica;
			case MegastructureChunkGenerator.DISTRICT_RESERVOIR_HALL -> reservoirHall;
			case MegastructureChunkGenerator.DISTRICT_SUSPENDED_CITY -> suspendedCity;
			case MegastructureChunkGenerator.DISTRICT_IRIS_CHASM -> irisChasm;
			case MegastructureChunkGenerator.DISTRICT_MACHINE_ROOT_VAULT -> machineRootVault;
			case MegastructureChunkGenerator.DISTRICT_TILTED_STACKS -> tiltedStacks;
			case MegastructureChunkGenerator.DISTRICT_SILENT_FOUNDRY -> silentFoundry;
			case MegastructureChunkGenerator.DISTRICT_COLOSSUS_LIFT -> colossusLift;
			case MegastructureChunkGenerator.DISTRICT_FOLDED_CITY -> foldedCity;
			case MegastructureChunkGenerator.DISTRICT_UPPER_RIM_CITY -> upperRimCity;
			case MegastructureChunkGenerator.DISTRICT_ORBITAL_WEB_CORE -> orbitalWebCore;
			case MegastructureChunkGenerator.DISTRICT_CROWN_SPIRE -> crownSpire;
			case MegastructureChunkGenerator.DISTRICT_GLOBE_MONUMENT -> globeMonument;
			case MegastructureChunkGenerator.DISTRICT_VOID_ALTAR -> voidAltar;
			case MegastructureChunkGenerator.DISTRICT_ATOM_STORM_ARRAY -> atomStormArray;
			case MegastructureChunkGenerator.DISTRICT_BLACK_HOLE_REACTOR -> blackHoleReactor;
			default -> denseWall;
		};
	}
}
