package ru.nikit.megastructure.survival;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class LooseStoneKnappingScreenHandler extends ScreenHandler {
	private static final int BUTTON_HIT_OFFSET = 1000;
	private static final int TARGET_SCALE = 1000;

	private final BlockPos pos;
	private final boolean hammerConsumed;
	private final World world;
	private final LooseStoneBlockEntity blockEntity;
	private final PropertyDelegate properties;
	private boolean resolved;

	public LooseStoneKnappingScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
		this(syncId, inventory, inventory.player.getWorld(), buf.readBlockPos(), buf.readBoolean(), createClientProperties(), null);
	}

	private LooseStoneKnappingScreenHandler(
			int syncId,
			PlayerInventory inventory,
			World world,
			BlockPos pos,
			boolean hammerConsumed,
			PropertyDelegate properties,
			LooseStoneBlockEntity blockEntity
	) {
		super(PrimitiveSurvivalContent.LOOSE_STONE_KNAPPING, syncId);
		this.pos = pos;
		this.hammerConsumed = hammerConsumed;
		this.world = world;
		this.blockEntity = blockEntity instanceof LooseStoneBlockEntity ? blockEntity : resolveBlockEntity(world, pos);
		this.properties = properties;
		addProperties(properties);
	}

	public static LooseStoneKnappingScreenHandler createServer(
			int syncId,
			PlayerInventory inventory,
			World world,
			BlockPos pos,
			boolean hammerConsumed
	) {
		LooseStoneBlockEntity blockEntity = resolveBlockEntity(world, pos);
		return new LooseStoneKnappingScreenHandler(
				syncId,
				inventory,
				world,
				pos,
				hammerConsumed,
				new KnappingProperties(blockEntity),
				blockEntity
		);
	}

	private static LooseStoneBlockEntity resolveBlockEntity(World world, BlockPos pos) {
		return world.getBlockEntity(pos) instanceof LooseStoneBlockEntity looseStone ? looseStone : null;
	}

	private static PropertyDelegate createClientProperties() {
		return new PropertyDelegate() {
			private final int[] values = {0, 220, 220, 0};

			@Override
			public int get(int index) {
				return values[index];
			}

			@Override
			public void set(int index, int value) {
				values[index] = value;
			}

			@Override
			public int size() {
				return values.length;
			}
		};
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return world.getBlockState(pos).isOf(PrimitiveSurvivalContent.LOOSE_STONE_BLOCK)
				&& player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		if (id < BUTTON_HIT_OFFSET) {
			return false;
		}
		if (world.isClient) {
			return true;
		}
		if (resolved || blockEntity == null) {
			return false;
		}
		float cursor = Math.max(0.0F, Math.min(1.0F, (id - BUTTON_HIT_OFFSET) / (float) TARGET_SCALE));
		float start = properties.get(1) / (float) TARGET_SCALE;
		float end = (properties.get(1) + properties.get(2)) / (float) TARGET_SCALE;
		if (cursor >= start && cursor <= end) {
			world.playSound(null, pos, SoundEvents.BLOCK_STONE_HIT, SoundCategory.BLOCKS, 0.9F, 0.85F + world.random.nextFloat() * 0.2F);
			int nextPhase = blockEntity.getPhase() + 1;
			if (nextPhase >= 5) {
				resolved = true;
				world.removeBlock(pos, false);
				giveReward(player);
				world.playSound(null, pos, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 0.95F, 0.72F);
				closePlayerScreen(player);
				return true;
			}
			blockEntity.setPhase(nextPhase);
			properties.set(0, nextPhase);
			properties.set(3, 0);
			retarget(nextPhase, properties);
			return true;
		}

		int misses = properties.get(3) + 1;
		properties.set(3, misses);
		world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 0.65F, 0.55F + world.random.nextFloat() * 0.12F);
		if (misses >= 3) {
			resolved = true;
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.8F, 0.86F);
			closePlayerScreen(player);
			return true;
		}
		retarget(blockEntity.getPhase(), properties);
		return true;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (!world.isClient && hammerConsumed && !resolved && !player.isCreative()) {
			if (!player.getInventory().insertStack(new ItemStack(PrimitiveSurvivalContent.LOOSE_STONE))) {
				player.dropItem(new ItemStack(PrimitiveSurvivalContent.LOOSE_STONE), false);
			}
		}
	}

	public int phase() {
		return properties.get(0);
	}

	public int targetStart() {
		return properties.get(1);
	}

	public int targetWidth() {
		return properties.get(2);
	}

	public int misses() {
		return properties.get(3);
	}

	public Text progressText() {
		return Text.translatable("screen.megastructure.loose_stone_knapping.progress", phase(), 5);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}

	static void retarget(int phase, PropertyDelegate properties) {
		int width = Math.max(90, 230 - phase * 24);
		long salt = System.nanoTime() + phase * 911L + properties.get(3) * 73L;
		int start = (int) Math.floorMod(salt, TARGET_SCALE - width - 40) + 20;
		properties.set(1, start);
		properties.set(2, width);
	}

	private static void giveReward(PlayerEntity player) {
		ItemStack reward = new ItemStack(PrimitiveSurvivalContent.KNAPPED_DIGGING_STONE);
		if (!player.getInventory().insertStack(reward)) {
			player.dropItem(reward, false);
		}
	}

	private static void closePlayerScreen(PlayerEntity player) {
		if (player instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.closeHandledScreen();
		}
	}

	public static final class OpeningDataFactory implements ExtendedScreenHandlerFactory {
		private final BlockPos pos;
		private final boolean hammerConsumed;

		public OpeningDataFactory(BlockPos pos, boolean hammerConsumed) {
			this.pos = pos;
			this.hammerConsumed = hammerConsumed;
		}

		@Override
		public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
			buf.writeBlockPos(pos);
			buf.writeBoolean(hammerConsumed);
		}

		@Override
		public Text getDisplayName() {
			return Text.translatable("screen.megastructure.loose_stone_knapping");
		}

		@Override
		public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
			return LooseStoneKnappingScreenHandler.createServer(
					syncId,
					playerInventory,
					player.getWorld(),
					pos,
					hammerConsumed
			);
		}
	}

	private static final class KnappingProperties implements PropertyDelegate {
		private final LooseStoneBlockEntity blockEntity;
		private int targetStart;
		private int targetWidth;
		private int misses;

		private KnappingProperties(LooseStoneBlockEntity blockEntity) {
			this.blockEntity = blockEntity;
			retarget(blockEntity == null ? 0 : blockEntity.getPhase(), this);
		}

		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> blockEntity == null ? 0 : blockEntity.getPhase();
				case 1 -> targetStart;
				case 2 -> targetWidth;
				case 3 -> misses;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			switch (index) {
				case 1 -> targetStart = value;
				case 2 -> targetWidth = value;
				case 3 -> misses = value;
				default -> {
				}
			}
		}

		@Override
		public int size() {
			return 4;
		}
	}
}
