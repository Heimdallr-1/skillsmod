package net.puffish.skillsmod.experience.builtin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.calculation.Calculation;
import net.puffish.skillsmod.api.calculation.operation.OperationFactory;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.api.experience.ExperienceSource;
import net.puffish.skillsmod.api.experience.ExperienceSourceConfigContext;
import net.puffish.skillsmod.api.utils.Failure;
import net.puffish.skillsmod.api.utils.Result;
import net.puffish.skillsmod.calculation.LegacyCalculation;
import net.puffish.skillsmod.calculation.operation.LegacyOperationRegistry;
import net.puffish.skillsmod.calculation.operation.builtin.AttributeOperation;
import net.puffish.skillsmod.calculation.operation.builtin.DamageTypeCondition;
import net.puffish.skillsmod.calculation.operation.builtin.EffectOperation;
import net.puffish.skillsmod.calculation.operation.builtin.EntityTypeCondition;
import net.puffish.skillsmod.calculation.operation.builtin.legacy.LegacyDamageTypeTagCondition;
import net.puffish.skillsmod.calculation.operation.builtin.legacy.LegacyEntityTypeTagCondition;

import java.util.Optional;

public class TakeDamageExperienceSource implements ExperienceSource {
	private static final Identifier ID = SkillsMod.createIdentifier("take_damage");
	private static final Prototype<Data> PROTOTYPE = Prototype.create(ID);

	static {
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("player"),
				BuiltinPrototypes.PLAYER,
				OperationFactory.create(Data::player)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("damage_source"),
				BuiltinPrototypes.DAMAGE_SOURCE,
				OperationFactory.create(Data::damageSource)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("damage"),
				BuiltinPrototypes.NUMBER,
				OperationFactory.create(data -> (double) data.damage())
		);

		// Backwards compatibility.
		var legacy = new LegacyOperationRegistry<>(PROTOTYPE);
		legacy.registerBooleanFunction(
				"damage_type",
				DamageTypeCondition::parse,
				data -> data.damageSource().getType()
		);
		legacy.registerBooleanFunction(
				"damage_type_tag",
				LegacyDamageTypeTagCondition::parse,
				data -> data.damageSource().getType()
		);
		legacy.registerOptionalBooleanFunction(
				"attacker",
				EntityTypeCondition::parse,
				data -> Optional.ofNullable(data.damageSource().getAttacker()).map(Entity::getType)
		);
		legacy.registerOptionalBooleanFunction(
				"attacker_tag",
				LegacyEntityTypeTagCondition::parse,
				data -> Optional.ofNullable(data.damageSource().getAttacker()).map(Entity::getType)
		);
		legacy.registerOptionalBooleanFunction(
				"source",
				EntityTypeCondition::parse,
				data -> Optional.ofNullable(data.damageSource().getSource()).map(Entity::getType)
		);
		legacy.registerOptionalBooleanFunction(
				"source_tag",
				LegacyEntityTypeTagCondition::parse,
				data -> Optional.ofNullable(data.damageSource().getSource()).map(Entity::getType)
		);
		legacy.registerNumberFunction(
				"player_effect",
				effect -> (double) (effect.getAmplifier() + 1),
				EffectOperation::parse,
				Data::player
		);
		legacy.registerNumberFunction(
				"player_attribute",
				EntityAttributeInstance::getValue,
				AttributeOperation::parse,
				Data::player
		);
		legacy.registerNumberFunction(
				"damage",
				data -> (double) data.damage()
		);
	}

	private final Calculation<Data> calculation;

	private TakeDamageExperienceSource(Calculation<Data> calculation) {
		this.calculation = calculation;
	}

	public static void register() {
		SkillsAPI.registerExperienceSource(
				ID,
				TakeDamageExperienceSource::parse
		);
	}

	private static Result<TakeDamageExperienceSource, Failure> parse(ExperienceSourceConfigContext context) {
		return context.getData().andThen(rootElement ->
				LegacyCalculation.parse(rootElement, PROTOTYPE, context)
						.mapSuccess(TakeDamageExperienceSource::new)
		);
	}

	private record Data(ServerPlayerEntity player, float damage, DamageSource damageSource) { }

	public int getValue(ServerPlayerEntity player, float damage, DamageSource damageSource) {
		return (int) Math.round(calculation.evaluate(
				new Data(player, damage, damageSource)
		));
	}

	@Override
	public void dispose(MinecraftServer server) {

	}
}
