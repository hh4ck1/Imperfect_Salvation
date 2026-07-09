package ru.nikit.megastructure.survival;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public final class LooseStoneBlockEntity extends BlockEntity {
	private int phase;

	public LooseStoneBlockEntity(BlockPos pos, BlockState state) {
		super(PrimitiveSurvivalContent.LOOSE_STONE_BLOCK_ENTITY, pos, state);
	}

	public int getPhase() {
		return phase;
	}

	public void setPhase(int phase) {
		int clamped = Math.max(0, Math.min(4, phase));
		if (this.phase == clamped) {
			return;
		}
		this.phase = clamped;
		if (world != null) {
			world.setBlockState(pos, getCachedState().with(LooseStoneBlock.PHASE, clamped), 3);
			markDirty();
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		nbt.putInt("Phase", phase);
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		phase = Math.max(0, Math.min(4, nbt.getInt("Phase")));
	}
}
