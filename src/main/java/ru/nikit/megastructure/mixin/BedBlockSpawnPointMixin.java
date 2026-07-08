package ru.nikit.megastructure.mixin;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.nikit.megastructure.world.MegastructureChunkGenerator;

@Mixin(BedBlock.class)
public abstract class BedBlockSpawnPointMixin {
	@Inject(method = "onUse", at = @At("HEAD"))
	private void megastructure$setSpawnPointOnBedUse(
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			BlockHitResult hit,
			CallbackInfoReturnable<ActionResult> callbackInfo
	) {
		if (!(world instanceof ServerWorld serverWorld)
				|| !(player instanceof ServerPlayerEntity serverPlayer)
				|| !(serverWorld.getChunkManager().getChunkGenerator() instanceof MegastructureChunkGenerator)) {
			return;
		}

		BlockPos spawnPos = state.get(BedBlock.PART) == BedPart.FOOT
				? pos.offset(state.get(BedBlock.FACING))
				: pos;
		serverPlayer.setSpawnPoint(serverWorld.getRegistryKey(), spawnPos, player.getYaw(), false, true);
	}
}
