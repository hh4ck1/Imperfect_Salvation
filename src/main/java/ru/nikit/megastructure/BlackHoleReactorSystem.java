package ru.nikit.megastructure;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.nikit.megastructure.world.MegastructureChunkGenerator;

public final class BlackHoleReactorSystem {
	private static final RegistryKey<DamageType> CREATOR_FANTASY_DAMAGE =
			RegistryKey.of(RegistryKeys.DAMAGE_TYPE, MegastructureMod.id("creator_fantasy"));
	private static final Set<UUID> PROTECTED_PLAYERS = new HashSet<>();

	private BlackHoleReactorSystem() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(BlackHoleReactorSystem::tickWorld);
	}

	private static void tickWorld(ServerWorld world) {
		if (!(world.getChunkManager().getChunkGenerator() instanceof MegastructureChunkGenerator)) {
			return;
		}
		for (PlayerEntity player : world.getPlayers()) {
			if (player.isSpectator()) {
				continue;
			}
			Optional<MegastructureChunkGenerator.BlackHoleCoreHint> nearest =
					MegastructureChunkGenerator.findNearestBlackHoleCore(
							player.getX(),
							player.getY(),
							player.getZ(),
							2
					);
			if (nearest.isPresent()) {
				applyCore(world, nearest.get());
			}
		}
	}

	private static void applyCore(ServerWorld world, MegastructureChunkGenerator.BlackHoleCoreHint core) {
		double radius = core.influenceRadius();
		Vec3d center = new Vec3d(core.x() + 0.5, core.y() + 0.5, core.z() + 0.5);
		Box box = new Box(
				center.x - radius,
				center.y - radius,
				center.z - radius,
				center.x + radius,
				center.y + radius,
				center.z + radius
		);
		for (Entity entity : world.getOtherEntities(null, box, entity -> !entity.isRemoved())) {
			if (entity instanceof PlayerEntity player && player.isSpectator()) {
				continue;
			}
			Vec3d offset = center.subtract(entity.getPos());
			double distance = Math.max(0.001, offset.length());
			if (distance > radius) {
				continue;
			}
			double falloff = 1.0 - distance / radius;
			Vec3d radial = offset.multiply(1.0 / distance);
			Vec3d tangent = new Vec3d(-radial.z, radial.y * 0.18, radial.x);
			double capture = Math.max(0.0, 1.0 - distance / 96.0);
			double pull = 0.018 + falloff * falloff * 0.115 + capture * capture * 0.22;
			double swirl = 0.010 + falloff * 0.075 + capture * 0.18;
			Vec3d velocity = entity.getVelocity()
					.add(radial.multiply(pull))
					.add(tangent.multiply(swirl));
			if (capture > 0.0) {
				velocity = velocity.add(radial.multiply(capture * 0.08));
			}
			double speedCap = entity instanceof PlayerEntity ? 2.35 : 3.45;
			if (velocity.lengthSquared() > speedCap * speedCap) {
				velocity = velocity.normalize().multiply(speedCap);
			}
			entity.setVelocity(velocity);
			entity.velocityModified = true;

			if (distance <= core.eventHorizonRadius()) {
				consume(world, entity, radial, falloff);
			}
		}
	}

	private static void consume(ServerWorld world, Entity entity, Vec3d radial, double falloff) {
		if (entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity) {
			entity.discard();
			return;
		}
		if (entity instanceof PlayerEntity player) {
			if (PROTECTED_PLAYERS.add(player.getUuid())) {
				player.damage(world.getDamageSources().create(CREATOR_FANTASY_DAMAGE), 8.0F);
			} else {
				player.damage(world.getDamageSources().create(CREATOR_FANTASY_DAMAGE), 3.0F + (float) (falloff * 5.0));
			}
			player.setVelocity(player.getVelocity().add(radial.multiply(-0.45)));
			player.velocityModified = true;
			return;
		}
		entity.damage(world.getDamageSources().create(CREATOR_FANTASY_DAMAGE), 12.0F);
		if (!entity.isAlive()) {
			entity.discard();
		}
	}
}
