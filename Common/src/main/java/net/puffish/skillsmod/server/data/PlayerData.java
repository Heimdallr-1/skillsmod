package net.puffish.skillsmod.server.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.config.CategoryConfig;

import java.util.HashMap;
import java.util.Map;

public class PlayerData {
	private final Map<Identifier, CategoryData> categories;

	private PlayerData(Map<Identifier, CategoryData> categories) {
		this.categories = categories;
	}

	public static PlayerData empty() {
		return new PlayerData(new HashMap<>());
	}

	public static PlayerData read(NbtCompound nbt) {
		var categories = new HashMap<Identifier, CategoryData>();

		var categoriesNbt = nbt.getCompound("categories");
		for (var id : categoriesNbt.getKeys()) {
			var elementNbt = categoriesNbt.get(id);
			if (elementNbt instanceof NbtCompound categoryNbt) {
				categories.put(SkillsMod.convertIdentifier(Identifier.of(id)), CategoryData.read(categoryNbt));
			}
		}

		return new PlayerData(categories);
	}

	public NbtCompound writeNbt(NbtCompound nbt) {
		var categoriesNbt = new NbtCompound();
		for (var entry : categories.entrySet()) {
			categoriesNbt.put(
					entry.getKey().toString(),
					entry.getValue().writeNbt(new NbtCompound())
			);
		}
		nbt.put("categories", categoriesNbt);

		return nbt;
	}

	public void unlockCategory(CategoryConfig category) {
		getCategoryData(category).setUnlocked(true);
	}

	public void lockCategory(CategoryConfig category) {
		getCategoryData(category).setUnlocked(false);
	}

	public boolean isCategoryUnlocked(CategoryConfig category) {
		var categoryData = categories.get(category.getId());
		if (categoryData != null) {
			return categoryData.isUnlocked();
		}
		return category.getGeneral().isUnlockedByDefault();
	}

	public CategoryData getCategoryData(CategoryConfig category) {
		return categories.compute(category.getId(), (key, value) -> {
			if (value == null) {
				value = CategoryData.create(category.getGeneral());
			}
			return value;
		});
	}

	public void removeCategoryData(CategoryConfig category) {
		categories.remove(category.getId());
	}
}
