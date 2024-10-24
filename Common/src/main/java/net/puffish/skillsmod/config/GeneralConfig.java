package net.puffish.skillsmod.config;

import net.minecraft.text.Text;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.BuiltinJson;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.config.colors.ColorsConfig;
import net.puffish.skillsmod.util.LegacyUtils;

import java.util.ArrayList;

public class GeneralConfig {
	private final Text title;
	private final IconConfig icon;
	private final BackgroundConfig background;
	private final ColorsConfig colors;
	private final boolean unlockedByDefault;
	private final int startingPoints;
	private final boolean exclusiveRoot;
	private final int spentPointsLimit;

	private GeneralConfig(
			Text title,
			IconConfig icon,
			BackgroundConfig background,
			ColorsConfig colors,
			boolean unlockedByDefault,
			int startingPoints,
			boolean exclusiveRoot,
			int spentPointsLimit
	) {
		this.title = title;
		this.icon = icon;
		this.background = background;
		this.colors = colors;
		this.unlockedByDefault = unlockedByDefault;
		this.startingPoints = startingPoints;
		this.exclusiveRoot = exclusiveRoot;
		this.spentPointsLimit = spentPointsLimit;
	}

	public static Result<GeneralConfig, Problem> parse(JsonElement rootElement, ConfigContext context) {
		return rootElement.getAsObject()
				.andThen(LegacyUtils.wrapNoUnused(rootObject -> parse(rootObject, context), context));
	}

	public static Result<GeneralConfig, Problem> parse(JsonObject rootObject, ConfigContext context) {
		var problems = new ArrayList<Problem>();

		var optTitle = rootObject.get("title")
				.andThen(titleElement -> BuiltinJson.parseText(titleElement, context.getServer().getRegistryManager()))
				.ifFailure(problems::add)
				.getSuccess();

		var optIcon = rootObject.get("icon")
				.andThen(element -> IconConfig.parse(element, context))
				.ifFailure(problems::add)
				.getSuccess();

		var optBackground = rootObject.get("background")
				.andThen(element -> BackgroundConfig.parse(element, context))
				.ifFailure(problems::add)
				.getSuccess();

		var colors = rootObject.get("colors")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> ColorsConfig.parse(element, context)
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElseGet(ColorsConfig::createDefault);

		var unlockedByDefault = rootObject.get("unlocked_by_default")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsBoolean()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(true);

		var startingPoints = rootObject.get("starting_points")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsInt()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(0);

		var exclusiveRoot = rootObject.get("exclusive_root")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsBoolean()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(false);

		var spentPointsLimit = rootObject.get("spent_points_limit")
				.getSuccess() // ignore failure because this property is optional
				.flatMap(element -> element.getAsInt()
						.ifFailure(problems::add)
						.getSuccess()
				)
				.orElse(Integer.MAX_VALUE);

		if (problems.isEmpty()) {
			return Result.success(new GeneralConfig(
					optTitle.orElseThrow(),
					optIcon.orElseThrow(),
					optBackground.orElseThrow(),
					colors,
					unlockedByDefault,
					startingPoints,
					exclusiveRoot,
					spentPointsLimit
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	public Text getTitle() {
		return title;
	}

	public boolean isUnlockedByDefault() {
		return unlockedByDefault;
	}

	public int getStartingPoints() {
		return startingPoints;
	}

	public boolean isExclusiveRoot() {
		return exclusiveRoot;
	}

	public IconConfig getIcon() {
		return icon;
	}

	public BackgroundConfig getBackground() {
		return background;
	}

	public ColorsConfig getColors() {
		return colors;
	}

	public int getSpentPointsLimit() {
		return spentPointsLimit;
	}
}
