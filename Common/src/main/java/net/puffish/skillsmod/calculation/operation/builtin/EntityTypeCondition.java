package net.puffish.skillsmod.calculation.operation.builtin;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.entry.RegistryEntryList;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.calculation.operation.Operation;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;
import net.puffish.skillsmod.api.calculation.operation.OperationConfigContext;
import net.puffish.skillsmod.api.json.BuiltinJson;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.ArrayList;
import java.util.Optional;

public final class EntityTypeCondition implements Operation<EntityType<?>, Boolean> {
	private final RegistryEntryList<EntityType<?>> entityTypeEntries;

	private EntityTypeCondition(RegistryEntryList<EntityType<?>> entityTypeEntries) {
		this.entityTypeEntries = entityTypeEntries;
	}

	public static void register() {
		BuiltinPrototypes.ENTITY_TYPE.registerOperation(
				SkillsMod.createIdentifier("test"),
				BuiltinPrototypes.BOOLEAN,
				EntityTypeCondition::parse
		);
	}

	public static Result<EntityTypeCondition, Problem> parse(OperationConfigContext context) {
		return context.getData()
				.andThen(JsonElement::getAsObject)
				.andThen(EntityTypeCondition::parse);
	}

	public static Result<EntityTypeCondition, Problem> parse(JsonObject rootObject) {
		var problems = new ArrayList<Problem>();

		var optEntityType = rootObject.get("entity") // Backwards compatibility.
				.orElse(problem -> rootObject.get("entity_type"))
				.andThen(BuiltinJson::parseEntityTypeOrEntityTypeTag)
				.ifFailure(problems::add)
				.getSuccess();

		if (problems.isEmpty()) {
			return Result.success(new EntityTypeCondition(
					optEntityType.orElseThrow()
			));
		} else {
			return Result.failure(Problem.combine(problems));
		}
	}

	@Override
	public Optional<Boolean> apply(EntityType<?> entityType) {
		return Optional.of(entityTypeEntries.contains(entityType.getRegistryEntry()));
	}
}
