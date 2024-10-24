package net.puffish.skillsmod.reward.builtin;

import net.minecraft.util.Identifier;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;

public class ScoreboardReward implements Reward {
	public static final Identifier ID = SkillsMod.createIdentifier("scoreboard");

	private final String objectiveName;

	private ScoreboardReward(String objectiveName) {
		this.objectiveName = objectiveName;
	}

	public static void register() {
		SkillsAPI.registerReward(
				ID,
				ScoreboardReward::parse
		);
	}

	private static Result<ScoreboardReward, Problem> parse(RewardConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, context), context));
	}

	private static Result<ScoreboardReward, Problem> parse(JsonObject rootObject, RewardConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optObjective = rootObject.getString("objective")
				.orElse(LegacyUtils.wrapDeprecated(
						() -> rootObject.getString("scoreboard"),
						3,
						context
				))
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new ScoreboardReward(
					optObjective.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public void update(RewardUpdateContext context) {
		var player = context.getPlayer();
		var scoreboard = player.getScoreboard();
		var objective = scoreboard.getNullableObjective(objectiveName);
		if (objective != null) {
			scoreboard.getOrCreateScore(player, objective).setScore(context.getCount());
		}
	}

	@Override
	public void dispose(RewardDisposeContext context) {
		// Nothing to do.
	}
}
