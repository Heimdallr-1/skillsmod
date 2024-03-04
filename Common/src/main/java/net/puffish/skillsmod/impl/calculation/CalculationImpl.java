package net.puffish.skillsmod.impl.calculation;

import net.puffish.skillsmod.api.calculation.Calculation;
import net.puffish.skillsmod.api.calculation.Variables;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonArrayWrapper;
import net.puffish.skillsmod.api.json.JsonElementWrapper;
import net.puffish.skillsmod.api.utils.Failure;
import net.puffish.skillsmod.api.utils.Result;
import net.puffish.skillsmod.calculation.CalculationCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CalculationImpl<T> implements Calculation<T> {
	private final Variables<T, Double> variables;
	private final List<CalculationCase> cases;

	private CalculationImpl(Variables<T, Double> variables, List<CalculationCase> cases) {
		this.variables = variables;
		this.cases = cases;
	}

	@Override
	public double evaluate(T t) {
		return cases.stream()
				.flatMap(calc -> calc.getValue(variables.evaluate(t)).stream())
				.findFirst()
				.orElse(0.0);
	}

	public static <T> Result<CalculationImpl<T>, Failure> create(
			JsonElementWrapper rootElement,
			Variables<T, Double> variables,
			ConfigContext context
	) {
		var variableNames = variables.streamNames().collect(Collectors.toSet());

		return rootElement.getAsArray()
				.andThen(rootObject -> create(rootObject, variables, variableNames, context))
				.orElse(failure -> CalculationCase.parseSimplified(rootElement, variableNames)
						.mapSuccess(calculationCase -> new CalculationImpl<>(variables, List.of(calculationCase)))
				);
	}

	private static <T> Result<CalculationImpl<T>, Failure> create(
			JsonArrayWrapper rootArray,
			Variables<T, Double> variables,
			Set<String> variableNames,
			ConfigContext context
	) {
		var failures = new ArrayList<Failure>();

		var optCalculations = rootArray.getAsList((i, element) -> CalculationCase.parse(element, variableNames))
						.mapFailure(Failure::fromMany)
						.ifFailure(failures::add)
						.getSuccess();

		if (failures.isEmpty()) {
			return Result.success(new CalculationImpl<>(
					variables,
					optCalculations.orElseThrow()
			));
		} else {
			return Result.failure(Failure.fromMany(failures));
		}
	}

}
