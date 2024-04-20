package net.puffish.skillsmod.impl;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Experience;
import net.puffish.skillsmod.api.Skill;

import java.util.Optional;
import java.util.stream.Stream;

public class CategoryImpl implements Category {
	private final Identifier categoryId;

	public CategoryImpl(Identifier categoryId) {
		this.categoryId = categoryId;
	}

	@Override
	public Identifier getId() {
		return categoryId;
	}

	@Override
	public Optional<Experience> getExperience() {
		if (SkillsMod.getInstance().hasExperience(categoryId).orElseThrow()) {
			return Optional.of(new ExperienceImpl(categoryId));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public Optional<Skill> getSkill(String skillId) {
		if (SkillsMod.getInstance().hasSkill(categoryId, skillId)) {
			return Optional.of(new SkillImpl(this, skillId));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public Stream<Skill> streamSkills() {
		return SkillsMod.getInstance()
				.getSkills(categoryId)
				.orElseThrow()
				.stream()
				.map(skillId -> new SkillImpl(this, skillId));
	}

	@Override
	public Stream<Skill> streamUnlockedSkills(ServerPlayerEntity player) {
		return SkillsMod.getInstance()
				.getUnlockedSkills(player, categoryId)
				.orElseThrow()
				.stream()
				.map(skillId -> new SkillImpl(this, skillId));
	}

	@Override
	public void openScreen(ServerPlayerEntity player) {
		SkillsMod.getInstance().openScreen(player, Optional.of(categoryId));
	}

	@Override
	public void unlock(ServerPlayerEntity player) {
		SkillsMod.getInstance().unlockCategory(player, categoryId);
	}

	@Override
	public void lock(ServerPlayerEntity player) {
		SkillsMod.getInstance().lockCategory(player, categoryId);
	}

	@Override
	public boolean isUnlocked(ServerPlayerEntity player) {
		return SkillsMod.getInstance().isCategoryUnlocked(player, categoryId).orElseThrow();
	}

	@Override
	public void erase(ServerPlayerEntity player) {
		SkillsMod.getInstance().eraseCategory(player, categoryId);
	}

	@Override
	public void resetSkills(ServerPlayerEntity player) {
		SkillsMod.getInstance().resetSkills(player, categoryId);
	}

	@Override
	public int getExtraPoints(ServerPlayerEntity player) {
		return SkillsMod.getInstance().getExtraPoints(player, categoryId).orElseThrow();
	}

	@Override
	public void setExtraPoints(ServerPlayerEntity player, int count) {
		SkillsMod.getInstance().setExtraPoints(player, categoryId, count);
	}

	@Override
	public void addExtraPoints(ServerPlayerEntity player, int count) {
		SkillsMod.getInstance().addExtraPoints(player, categoryId, count);
	}

	@Override
	public int getPointsLeft(ServerPlayerEntity player) {
		return SkillsMod.getInstance().getPointsLeft(player, categoryId).orElseThrow();
	}
}
