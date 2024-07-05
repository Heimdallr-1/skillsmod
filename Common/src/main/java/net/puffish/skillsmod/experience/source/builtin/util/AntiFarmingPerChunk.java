package net.puffish.skillsmod.experience.source.builtin.util;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record AntiFarmingPerChunk(int limitPerChunk, int resetAfterSeconds) {
	public static Result<Optional<AntiFarmingPerChunk>, Problem> parse(JsonElement rootElement) {
		return rootElement.getAsObject()
				.andThen(AntiFarmingPerChunk::parse);
	}

	public static Result<Optional<AntiFarmingPerChunk>, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		// Deprecated
		var enabled = rootObject.getBoolean("enabled")
				.getSuccess()
				.orElse(true);

		var optLimitPerChunk = rootObject.getInt("limit_per_chunk")
				.ifFailure(problems::add)
				.getSuccess();

		var optResetAfterSeconds = rootObject.getInt("reset_after_seconds")
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			if (enabled) {
				return Result.success(Optional.of(new AntiFarmingPerChunk(
						optLimitPerChunk.orElseThrow(),
						optResetAfterSeconds.orElseThrow()
				)));
			} else {
				return Result.success(Optional.empty());
			}
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	public static class Data {
		private final Map<AntiFarmingPerChunk, LongList> antiFarmingData = new HashMap<>();

		public boolean tryIncrement(AntiFarmingPerChunk antiFarming) {
			var data = antiFarmingData.computeIfAbsent(antiFarming, key -> new LongArrayList());

			if (data.size() < antiFarming.limitPerChunk()) {
				data.add(System.currentTimeMillis() + antiFarming.resetAfterSeconds() * 1000L);
				return true;
			}

			return false;
		}

		public void removeOutdated() {
			var currentTime = System.currentTimeMillis();

			antiFarmingData.values().removeIf(data -> {
				data.removeIf(time -> time < currentTime);
				return data.isEmpty();
			});
		}
	}
}
