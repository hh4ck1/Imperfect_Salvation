package ru.nikit.megastructure.survival;

import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public enum CrudeStoneToolMaterial implements ToolMaterial {
	INSTANCE;

	@Override
	public int getDurability() {
		return 30;
	}

	@Override
	public float getMiningSpeedMultiplier() {
		return 2.0F;
	}

	@Override
	public float getAttackDamage() {
		return 0.0F;
	}

	@Override
	public int getMiningLevel() {
		return 0;
	}

	@Override
	public int getEnchantability() {
		return 4;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.empty();
	}
}
