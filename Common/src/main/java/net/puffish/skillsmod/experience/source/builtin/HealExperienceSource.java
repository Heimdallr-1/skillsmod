package net.puffish.skillsmod.experience.source.builtin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.calculation.Calculation;
import net.puffish.skillsmod.api.calculation.Variables;
import net.puffish.skillsmod.api.calculation.operation.OperationFactory;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.prototype.Prototype;
import net.puffish.skillsmod.api.experience.source.ExperienceSource;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceConfigContext;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceDisposeContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

public class HealExperienceSource implements ExperienceSource {
	private static final Identifier ID = SkillsMod.createIdentifier("heal");
	private static final Prototype<Data> PROTOTYPE = Prototype.create(ID);

	static {
		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_player"),
				BuiltinPrototypes.PLAYER,
				OperationFactory.create(Data::player)
		);

		PROTOTYPE.registerOperation(
				SkillsMod.createIdentifier("get_heal_amount"),
				BuiltinPrototypes.NUMBER,
				OperationFactory.create(data -> (double) data.damage())
		);
	}

	private final Calculation<Data> calculation;

	private HealExperienceSource(Calculation<Data> calculation) {
		this.calculation = calculation;
	}

	public static void register() {
		SkillsAPI.registerExperienceSource(
				ID,
				HealExperienceSource::parse
		);
	}

	private static Result<HealExperienceSource, Problem> parse(ExperienceSourceConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(rootObject -> rootObject.get("variables")
						.andThen(variablesElement -> Variables.parse(
								variablesElement,
								PROTOTYPE,
								context
						))
						.andThen(variables -> rootObject.get("experience")
								.andThen(experienceElement -> Calculation.parse(
										experienceElement,
										variables,
										context
								))
						)
						.mapSuccess(HealExperienceSource::new)
				);
	}

	private record Data(ServerPlayerEntity player, float damage) { }

	public int getValue(ServerPlayerEntity player, float damage) {
		return (int) Math.round(calculation.evaluate(
				new Data(player, damage)
		));
	}

	@Override
	public void dispose(ExperienceSourceDisposeContext context) {
		// Nothing to do.
	}
}