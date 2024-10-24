package net.puffish.skillsmod.experience.source.builtin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.calculation.Calculation;
import net.puffish.skillsmod.api.calculation.operation.OperationFactory;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.experience.source.ExperienceSource;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceConfigContext;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceDisposeContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.calculation.LegacyCalculation;
import net.puffish.skillsmod.experience.source.builtin.util.AntiFarmingPerChunk;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

public class SharedKillEntityExperienceSource implements ExperienceSource {
	private static final Identifier ID = SkillsMod.createIdentifier("shared_kill_entity");
	private static final Prototype<Data> PROTOTYPE = Prototype.create(ID);

	static {
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_player"),
				BuiltinPrototypes.PLAYER,
				OperationFactory.create(Data::player)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_weapon_item_stack"),
				BuiltinPrototypes.ITEM_STACK,
				OperationFactory.create(Data::weapon)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_killed_living_entity"),
				BuiltinPrototypes.LIVING_ENTITY,
				OperationFactory.create(Data::entity)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_damage_source"),
				BuiltinPrototypes.DAMAGE_SOURCE,
				OperationFactory.create(Data::damageSource)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_dropped_experience"),
				BuiltinPrototypes.NUMBER,
				OperationFactory.create(Data::entityDroppedXp)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_total_dealt_damage"),
				BuiltinPrototypes.NUMBER,
				OperationFactory.create(Data::totalDamage)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_participants"),
				BuiltinPrototypes.NUMBER,
				OperationFactory.create(data -> (double) data.participants)
		);
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_share"),
				BuiltinPrototypes.NUMBER,
				OperationFactory.create(Data::share)
		);
	}

	private final Calculation<Data> calculation;
	private final Optional<AntiFarmingPerChunk> optAntiFarming;

	private SharedKillEntityExperienceSource(Calculation<Data> calculation, Optional<AntiFarmingPerChunk> optAntiFarming) {
		this.calculation = calculation;
		this.optAntiFarming = optAntiFarming;
	}

	public static void register() {
		SkillsAPI.registerExperienceSource(
				ID,
				SharedKillEntityExperienceSource::parse
		);
	}

	private static Result<SharedKillEntityExperienceSource, Problem> parse(ExperienceSourceConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, context), context));
	}
	private static Result<SharedKillEntityExperienceSource, Problem> parse(JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optCalculation = LegacyCalculation.parse(rootObject, PROTOTYPE, context)
				.ifFailure(problems::add)
				.getSuccess();

		var optAntiFarming = rootObject.get("anti_farming")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> AntiFarmingPerChunk.parse(element, context)
						.ifFailure(problems::add)
						.getSuccess()
						.flatMap(Function.identity())
				);

		if (problems.isEmpty()) {
			return Result.success(new SharedKillEntityExperienceSource(
					optCalculation.orElseThrow(),
					optAntiFarming
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	private record Data(
			ServerPlayerEntity player,
			LivingEntity entity,
			ItemStack weapon,
			DamageSource damageSource,
			double entityDroppedXp,
			double totalDamage,
			int participants,
			double share
	) { }

	public int getValue(
			ServerPlayerEntity player,
			LivingEntity entity,
			ItemStack weapon,
			DamageSource damageSource,
			double entityDroppedXp,
			double totalDamage,
			int participants,
			double share
	) {
		return (int) Math.round(calculation.evaluate(
				new Data(player, entity, weapon, damageSource, entityDroppedXp, totalDamage, participants, share)
		));
	}

	public Optional<AntiFarmingPerChunk> getAntiFarming() {
		return optAntiFarming;
	}

	@Override
	public void dispose(ExperienceSourceDisposeContext context) {
		// Nothing to do.
	}
}
