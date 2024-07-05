package net.puffish.skillsmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.puffish.skillsmod.access.DamageSourceAccess;
import net.puffish.skillsmod.access.WorldChunkAccess;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.experience.source.builtin.DealDamageExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.KillEntityExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.SharedKillEntityExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.TakeDamageExperienceSource;
import net.puffish.skillsmod.experience.source.builtin.util.AntiFarmingPerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Unique
	private int entityDroppedXp = 0;

	@Unique
	private final Map<ServerPlayerEntity, Float> damageShare = new WeakHashMap<>();

	@Unique
	private final AntiFarmingPerEntity.Data antiFarmingData = new AntiFarmingPerEntity.Data();

	@Inject(method = "applyDamage", at = @At("TAIL"))
	private void injectAtApplyDamage(DamageSource source, float damage, CallbackInfo ci) {
		var entity = ((LivingEntity) (Object) this);

		if (entity instanceof ServerPlayerEntity player) {
			SkillsAPI.updateExperienceSources(
					player,
					TakeDamageExperienceSource.class,
					experienceSource -> experienceSource.getValue(player, damage, source)
			);
		}

		if (source.getAttacker() instanceof ServerPlayerEntity player) {
			damageShare.compute(player, (key, value) -> {
				if (value == null) {
					return damage;
				} else {
					return value + damage;
				}
			});

			antiFarmingData.removeOutdated();
			SkillsAPI.updateExperienceSources(
					player,
					DealDamageExperienceSource.class,
					experienceSource -> {
						float limitedDamage = experienceSource.getAntiFarming()
								.map(antiFarming -> antiFarmingData.addAndLimit(antiFarming, damage))
								.orElse(damage);
						if (limitedDamage > MathHelper.EPSILON) {
							return experienceSource.getValue(player, entity, limitedDamage, source);
						}
						return 0;
					}
			);
		}
	}

	@Inject(method = "drop", at = @At("TAIL"))
	private void injectAtDrop(DamageSource source, CallbackInfo ci) {
		if (source.getAttacker() instanceof ServerPlayerEntity player) {
			var entity = ((LivingEntity) (Object) this);
			var weapon = ((DamageSourceAccess) source).getWeapon().orElse(ItemStack.EMPTY);

			var antiFarmingData = ((WorldChunkAccess) entity.getWorld()
					.getWorldChunk(entity.getBlockPos()))
					.getAntiFarmingData();
			antiFarmingData.removeOutdated();

			SkillsAPI.updateExperienceSources(
					player,
					KillEntityExperienceSource.class,
					experienceSource -> {
						if (experienceSource
								.getAntiFarming()
								.map(antiFarmingData::tryIncrement)
								.orElse(true)
						) {
							return experienceSource.getValue(
									player,
									entity,
									weapon,
									source,
									entityDroppedXp
							);
						}
						return 0;
					}
			);

			var entries = damageShare.entrySet();
			var totalDamage = entries.stream().mapToDouble(Map.Entry::getValue).sum();
			for (var entry : entries) {
				SkillsAPI.updateExperienceSources(
						entry.getKey(),
						SharedKillEntityExperienceSource.class,
						experienceSource -> {
							if (experienceSource
									.getAntiFarming()
									.map(antiFarmingData::tryIncrement)
									.orElse(true)
							) {
								return experienceSource.getValue(
										entry.getKey(),
										entity,
										weapon,
										source,
										entityDroppedXp,
										totalDamage,
										entries.size(),
										entry.getValue() / totalDamage
								);
							}
							return 0;
						}
				);
			}
		}
	}

	@ModifyArg(
			method = "dropXp",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"
			),
			index = 2
	)
	private int injectAtDropXp(int droppedXp) {
		entityDroppedXp = droppedXp;
		return droppedXp;
	}
}
