package net.puffish.skillsmod.calculation.operation.builtin;

import net.minecraft.entity.Entity;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.calculation.operation.Operation;
import net.puffish.skillsmod.api.calculation.operation.OperationConfigContext;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.ArrayList;
import java.util.Optional;

public final class ScoreboardOperation implements Operation<Entity, Double> {
	private final String objectiveName;

	private ScoreboardOperation(String objectiveName) {
		this.objectiveName = objectiveName;
	}

	public static void register() {
		BuiltinPrototypes.ENTITY.registerOperation(
				SkillsMod.createIdentifier("scoreboard"),
				BuiltinPrototypes.NUMBER,
				ScoreboardOperation::parse
		);
	}

	public static Result<ScoreboardOperation, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(ScoreboardOperation::parse);
	}

	public static Result<ScoreboardOperation, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optScoreboard = rootObject.getString("objective")
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new ScoreboardOperation(
					optScoreboard.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Double> apply(Entity entity) {
		var scoreboard = entity.getWorld().getScoreboard();
		return Optional.ofNullable(scoreboard.getNullableObjective(objectiveName))
				.map(objective -> Optional.ofNullable(scoreboard.getScore(entity, objective))
						.map(score -> (double) score.getScore())
						.orElse(0.0));
	}
}
