package net.puffish.skillsmod.impl.json;

import com.google.common.collect.Streams;
import net.puffish.skillsmod.api.json.JsonArray;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.api.util.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class JsonArrayImpl implements JsonArray {
	private final com.google.gson.JsonArray json;
	private final JsonPath path;

	public JsonArrayImpl(com.google.gson.JsonArray json, JsonPath path) {
		this.json = json;
		this.path = path;
	}

	@Override
	public Stream<JsonElement> stream() {
		return Streams.mapWithIndex(
				Streams.stream(json.iterator()),
				(jsonElement, i) -> new JsonElementImpl(jsonElement, path.getArray(i))
		);
	}

	@Override
	public <S, F> Result<List<S>, List<F>> getAsList(BiFunction<Integer, JsonElement, Result<S, F>> function) {
		var successes = new ArrayList<S>();
		var failures = new ArrayList<F>();

		for (var i = 0; i < json.size(); i++) {
			function.apply(i, new JsonElementImpl(json.get(i), path.getArray(i)))
					.ifSuccess(successes::add)
					.ifFailure(failures::add);
		}

		if (failures.isEmpty()) {
			return Result.success(successes);
		} else {
			return Result.failure(failures);
		}
	}

	@Override
	public int getSize() {
		return json.size();
	}

	@Override
	public com.google.gson.JsonArray getJson() {
		return json;
	}

	@Override
	public JsonPath getPath() {
		return path;
	}
}
