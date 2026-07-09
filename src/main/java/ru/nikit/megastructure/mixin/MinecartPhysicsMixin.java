package ru.nikit.megastructure.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.nikit.megastructure.traversal.LinkedMinecart;

@Mixin(AbstractMinecartEntity.class)
public abstract class MinecartPhysicsMixin implements LinkedMinecart {
	@Unique
	private static final double MEGASTRUCTURE_CRUISE_SPEED = 1.08;
	@Unique
	private static final double MEGASTRUCTURE_MAX_SPEED = 1.35;
	@Unique
	private static final double MEGASTRUCTURE_EMPTY_BRAKE = 0.74;
	@Unique
	private static final TrackedData<Optional<UUID>> MEGASTRUCTURE_LINK_A = DataTracker.registerData(
			AbstractMinecartEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
	@Unique
	private static final TrackedData<Optional<UUID>> MEGASTRUCTURE_LINK_B = DataTracker.registerData(
			AbstractMinecartEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
	@Unique
	private boolean megastructure$hadPassenger;
	@Unique
	private int megastructure$exitBrakeTicks;

	@Inject(method = "initDataTracker", at = @At("TAIL"))
	private void megastructure$trackLinks(CallbackInfo callbackInfo) {
		AbstractMinecartEntity cart = (AbstractMinecartEntity) (Object) this;
		cart.getDataTracker().startTracking(MEGASTRUCTURE_LINK_A, Optional.empty());
		cart.getDataTracker().startTracking(MEGASTRUCTURE_LINK_B, Optional.empty());
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void megastructure$writeLinks(NbtCompound nbt, CallbackInfo callbackInfo) {
		List<UUID> links = megastructure$getLinks();
		if (!links.isEmpty()) {
			nbt.putUuid("MegastructureLinkA", links.get(0));
		}
		if (links.size() > 1) {
			nbt.putUuid("MegastructureLinkB", links.get(1));
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void megastructure$readLinks(NbtCompound nbt, CallbackInfo callbackInfo) {
		AbstractMinecartEntity cart = (AbstractMinecartEntity) (Object) this;
		cart.getDataTracker().set(
				MEGASTRUCTURE_LINK_A,
				nbt.containsUuid("MegastructureLinkA") ? Optional.of(nbt.getUuid("MegastructureLinkA")) : Optional.empty()
		);
		cart.getDataTracker().set(
				MEGASTRUCTURE_LINK_B,
				nbt.containsUuid("MegastructureLinkB") ? Optional.of(nbt.getUuid("MegastructureLinkB")) : Optional.empty()
		);
	}

	@Override
	public List<UUID> megastructure$getLinks() {
		AbstractMinecartEntity cart = (AbstractMinecartEntity) (Object) this;
		ArrayList<UUID> links = new ArrayList<>(2);
		cart.getDataTracker().get(MEGASTRUCTURE_LINK_A).ifPresent(links::add);
		cart.getDataTracker().get(MEGASTRUCTURE_LINK_B).ifPresent(links::add);
		return List.copyOf(links);
	}

	@Override
	public boolean megastructure$addLink(UUID uuid) {
		AbstractMinecartEntity cart = (AbstractMinecartEntity) (Object) this;
		if (megastructure$getLinks().contains(uuid)) {
			return true;
		}
		if (cart.getDataTracker().get(MEGASTRUCTURE_LINK_A).isEmpty()) {
			cart.getDataTracker().set(MEGASTRUCTURE_LINK_A, Optional.of(uuid));
			return true;
		}
		if (cart.getDataTracker().get(MEGASTRUCTURE_LINK_B).isEmpty()) {
			cart.getDataTracker().set(MEGASTRUCTURE_LINK_B, Optional.of(uuid));
			return true;
		}
		return false;
	}

	@Override
	public boolean megastructure$removeLink(UUID uuid) {
		AbstractMinecartEntity cart = (AbstractMinecartEntity) (Object) this;
		if (cart.getDataTracker().get(MEGASTRUCTURE_LINK_A).filter(uuid::equals).isPresent()) {
			cart.getDataTracker().set(MEGASTRUCTURE_LINK_A, Optional.empty());
			return true;
		}
		if (cart.getDataTracker().get(MEGASTRUCTURE_LINK_B).filter(uuid::equals).isPresent()) {
			cart.getDataTracker().set(MEGASTRUCTURE_LINK_B, Optional.empty());
			return true;
		}
		return false;
	}

	@Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
	private void megastructure$raiseRailSpeed(CallbackInfoReturnable<Double> callbackInfo) {
		AbstractMinecartEntity cart = (AbstractMinecartEntity) (Object) this;
		if (megastructure$isInsideMegastructure(cart)) {
			callbackInfo.setReturnValue(MEGASTRUCTURE_MAX_SPEED);
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void megastructure$maintainPassengerCruise(CallbackInfo callbackInfo) {
		AbstractMinecartEntity cart = (AbstractMinecartEntity) (Object) this;
		boolean insideMegastructure = megastructure$isInsideMegastructure(cart);
		megastructure$applyLinkForces(cart);
		megastructure$lockCourseToRail(cart, insideMegastructure);
		megastructure$brakeAfterDismount(cart, insideMegastructure);
		if (!cart.isOnRail() || !cart.hasPassengers() || !insideMegastructure) {
			megastructure$hadPassenger = cart.hasPassengers();
			return;
		}
		Entity passenger = cart.getFirstPassenger();
		Vec3d velocity = cart.getVelocity();
		if (passenger != null && passenger.isSneaking()) {
			cart.setVelocity(velocity.x * 0.72, velocity.y, velocity.z * 0.72);
			return;
		}

		double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		Vec3d direction;
		if (horizontalSpeed >= 0.035) {
			direction = new Vec3d(velocity.x / horizontalSpeed, 0.0, velocity.z / horizontalSpeed);
		} else {
			direction = megastructure$startingDirection(cart, passenger);
			if (direction == null) {
				return;
			}
		}
		double accelerated = Math.min(MEGASTRUCTURE_CRUISE_SPEED, Math.max(0.27, horizontalSpeed + 0.042));
		cart.setVelocity(direction.x * accelerated, velocity.y, direction.z * accelerated);
		megastructure$hadPassenger = true;
	}

	@Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
	private void megastructure$ignoreMinecartCoursePush(Entity entity, CallbackInfo callbackInfo) {
		AbstractMinecartEntity cart = (AbstractMinecartEntity) (Object) this;
		if (entity instanceof AbstractMinecartEntity && cart.isOnRail() && megastructure$isInsideMegastructure(cart)) {
			callbackInfo.cancel();
		}
	}

	@Unique
	private void megastructure$applyLinkForces(AbstractMinecartEntity cart) {
		if (!(cart.getWorld() instanceof ServerWorld serverWorld) || !cart.isOnRail()) {
			return;
		}
		for (UUID link : megastructure$getLinks()) {
			if (cart.getUuid().compareTo(link) >= 0) {
				continue;
			}
			Entity entity = serverWorld.getEntity(link);
			if (!(entity instanceof AbstractMinecartEntity partner) || !partner.isOnRail()) {
				continue;
			}
			Vec3d delta = partner.getPos().subtract(cart.getPos());
			double distance = delta.length();
			if (distance < 0.05 || distance > 24.0) {
				continue;
			}
			Vec3d direction = delta.multiply(1.0 / distance);
			double relative = partner.getVelocity().subtract(cart.getVelocity()).dotProduct(direction);
			double correction = Math.max(-0.045, Math.min(0.060, (distance - 1.45) * 0.040 + relative * 0.012));
			Vec3d force = direction.multiply(correction);
			cart.addVelocity(force.x, force.y * 0.08, force.z);
			partner.addVelocity(-force.x, -force.y * 0.08, -force.z);

			boolean cartDriven = cart.hasPassengers();
			boolean partnerDriven = partner.hasPassengers();
			if (cartDriven != partnerDriven) {
				AbstractMinecartEntity driven = cartDriven ? cart : partner;
				AbstractMinecartEntity trailing = cartDriven ? partner : cart;
				Vec3d drivenRail = megastructure$railDirection(driven);
				Vec3d trailingRail = megastructure$railDirection(trailing);
				if (drivenRail != null && trailingRail != null) {
					double drivenSpeed = Math.abs(driven.getVelocity().dotProduct(drivenRail));
					double trailingSpeed = trailing.getVelocity().dotProduct(trailingRail);
					double sign = trailingSpeed < 0.0 ? -1.0 : 1.0;
					if (Math.abs(trailingSpeed) < drivenSpeed * 0.78) {
						Vec3d trailingVelocity = trailing.getVelocity();
						double target = sign * drivenSpeed * 0.78;
						double blended = trailingSpeed + (target - trailingSpeed) * 0.18;
						trailing.setVelocity(trailingRail.x * blended, trailingVelocity.y, trailingRail.z * blended);
					}
				}
			}
		}
	}

	@Unique
	private void megastructure$brakeAfterDismount(AbstractMinecartEntity cart, boolean insideMegastructure) {
		if (!insideMegastructure || !cart.isOnRail()) {
			megastructure$hadPassenger = cart.hasPassengers();
			megastructure$exitBrakeTicks = 0;
			return;
		}
		if (cart.hasPassengers()) {
			megastructure$hadPassenger = true;
			megastructure$exitBrakeTicks = 0;
			return;
		}
		if (megastructure$hadPassenger) {
			megastructure$exitBrakeTicks = 36;
		}
		megastructure$hadPassenger = false;
		if (megastructure$exitBrakeTicks > 0) {
			Vec3d velocity = cart.getVelocity();
			cart.setVelocity(velocity.x * MEGASTRUCTURE_EMPTY_BRAKE, velocity.y, velocity.z * MEGASTRUCTURE_EMPTY_BRAKE);
			megastructure$exitBrakeTicks--;
		}
	}

	@Unique
	private void megastructure$lockCourseToRail(AbstractMinecartEntity cart, boolean insideMegastructure) {
		if (!insideMegastructure || !cart.isOnRail()) {
			return;
		}
		Vec3d railDirection = megastructure$railDirection(cart);
		if (railDirection == null) {
			return;
		}
		Vec3d velocity = cart.getVelocity();
		double speed = velocity.dotProduct(railDirection);
		if (Math.abs(speed) < 0.001) {
			cart.setVelocity(0.0, velocity.y, 0.0);
			return;
		}
		cart.setVelocity(railDirection.x * speed, velocity.y, railDirection.z * speed);
	}

	@Unique
	private static Vec3d megastructure$startingDirection(AbstractMinecartEntity cart, Entity passenger) {
		RailShape shape = megastructure$railShape(cart);
		if (shape == null) {
			return null;
		}
		Vec3d look = passenger == null ? cart.getRotationVec(1.0F) : passenger.getRotationVec(1.0F);
		return switch (shape) {
			case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> new Vec3d(look.x >= 0.0 ? 1.0 : -1.0, 0.0, 0.0);
			case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> new Vec3d(0.0, 0.0, look.z >= 0.0 ? 1.0 : -1.0);
			case SOUTH_EAST -> megastructure$closestDirection(look, new Vec3d(0.0, 0.0, 1.0), new Vec3d(1.0, 0.0, 0.0));
			case SOUTH_WEST -> megastructure$closestDirection(look, new Vec3d(0.0, 0.0, 1.0), new Vec3d(-1.0, 0.0, 0.0));
			case NORTH_EAST -> megastructure$closestDirection(look, new Vec3d(0.0, 0.0, -1.0), new Vec3d(1.0, 0.0, 0.0));
			case NORTH_WEST -> megastructure$closestDirection(look, new Vec3d(0.0, 0.0, -1.0), new Vec3d(-1.0, 0.0, 0.0));
		};
	}

	@Unique
	private static Vec3d megastructure$railDirection(AbstractMinecartEntity cart) {
		RailShape shape = megastructure$railShape(cart);
		if (shape == null) {
			return null;
		}
		return switch (shape) {
			case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> new Vec3d(1.0, 0.0, 0.0);
			case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> new Vec3d(0.0, 0.0, 1.0);
			case SOUTH_EAST, NORTH_WEST -> new Vec3d(1.0, 0.0, 1.0).normalize();
			case SOUTH_WEST, NORTH_EAST -> new Vec3d(-1.0, 0.0, 1.0).normalize();
		};
	}

	@Unique
	private static RailShape megastructure$railShape(AbstractMinecartEntity cart) {
		BlockPos railPos = cart.getBlockPos();
		BlockState rail = cart.getWorld().getBlockState(railPos);
		if (!rail.contains(Properties.RAIL_SHAPE) && !rail.contains(Properties.STRAIGHT_RAIL_SHAPE)) {
			railPos = railPos.down();
			rail = cart.getWorld().getBlockState(railPos);
		}
		if (rail.contains(Properties.RAIL_SHAPE)) {
			return rail.get(Properties.RAIL_SHAPE);
		} else if (rail.contains(Properties.STRAIGHT_RAIL_SHAPE)) {
			return rail.get(Properties.STRAIGHT_RAIL_SHAPE);
		}
		return null;
	}

	@Unique
	private static Vec3d megastructure$closestDirection(Vec3d look, Vec3d first, Vec3d second) {
		return Math.abs(look.dotProduct(first)) >= Math.abs(look.dotProduct(second))
				? first.multiply(look.dotProduct(first) >= 0.0 ? 1.0 : -1.0)
				: second.multiply(look.dotProduct(second) >= 0.0 ? 1.0 : -1.0);
	}

	@Unique
	private static boolean megastructure$isInsideMegastructure(AbstractMinecartEntity cart) {
		return cart.getWorld().getBiome(cart.getBlockPos()).getKey()
				.map(key -> key.getValue().getNamespace().equals("megastructure"))
				.orElse(false);
	}
}
