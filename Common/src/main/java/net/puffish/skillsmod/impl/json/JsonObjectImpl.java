package net.puffish.skillsmod.impl.json;

import net.puffish.skillsmod.api.json.JsonArray;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.util.JsonPathFailure;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class JsonObjectImpl implements JsonObject {
	private final com.google.gson.JsonObject json;
	protected final JsonPath path;

	public JsonObjectImpl(com.google.gson.JsonObject json, JsonPath path) {
		this.json = json;
		this.path = path;
	}

	@Override
	public Result<JsonElement, Problem> get(String key) {
		var newPath = path.getObject(key);
		var element = json.get(key);
		if (element == null) {
			return Result.failure(JsonPathFailure.expectedToExist(newPath));
		} else {
			return Result.success(new JsonElementImpl(element, newPath));
		}
	}

	@Override
	public Result<JsonObject, Problem> getObject(String key) {
		return get(key)
				.mapFailure(e -> JsonPathFailure.expectedToExistAndBe(path.getObject(key), "an object"))
				.andThen(JsonElement::getAsObject);
	}

	@Override
	public Result<JsonArray, Problem> getArray(String key) {
		return get(key)
				.mapFailure(e -> JsonPathFailure.expectedToExistAndBe(path.getObject(key), "an array"))
				.andThen(JsonElement::getAsArray);
	}

	@Override
	public Result<String, Problem> getString(String key) {
		return get(key)
				.mapFailure(e -> JsonPathFailure.expectedToExistAndBe(path.getObject(key), "a string"))
				.andThen(JsonElement::getAsString);
	}

	@Override
	public Result<Float, Problem> getFloat(String key) {
		return get(key)
				.mapFailure(e -> JsonPathFailure.expectedToExistAndBe(path.getObject(key), "a float"))
				.andThen(JsonElement::getAsFloat);
	}

	@Override
	public Result<Double, Problem> getDouble(String key) {
		return get(key)
				.mapFailure(e -> JsonPathFailure.expectedToExistAndBe(path.getObject(key), "a double"))
				.andThen(JsonElement::getAsDouble);
	}

	@Override
	public Result<Integer, Problem> getInt(String key) {
		return get(key)
				.mapFailure(e -> JsonPathFailure.expectedToExistAndBe(path.getObject(key), "an int"))
				.andThen(JsonElement::getAsInt);
	}

	@Override
	public Result<Boolean, Problem> getBoolean(String key) {
		return get(key)
				.mapFailure(e -> JsonPathFailure.expectedToExistAndBe(path.getObject(key), "a boolean"))
				.andThen(JsonElement::getAsBoolean);
	}

	@Override
	public Stream<Map.Entry<String, JsonElement>> stream() {
		return json.entrySet()
				.stream()
				.map(entry -> Map.entry(
						entry.getKey(),
						new JsonElementImpl(entry.getValue(), path.getObject(entry.getKey()))
				));
	}

	@Override
	public <S, F> Result<Map<String, S>, Map<String, F>> getAsMap(BiFunction<String, JsonElement, Result<S, F>> function) {
		var successes = new HashMap<String, S>();
		var failures = new HashMap<String, F>();

		json.entrySet().forEach(entry ->
				function.apply(entry.getKey(), new JsonElementImpl(entry.getValue(), path.getObject(entry.getKey())))
						.ifSuccess(t -> successes.put(entry.getKey(), t))
						.ifFailure(t -> failures.put(entry.getKey(), t))
		);

		if (failures.isEmpty()) {
			return Result.success(successes);
		} else {
			return Result.failure(failures);
		}
	}

	@Override
	public com.google.gson.JsonObject getJson() {
		return json;
	}

	public JsonPath getPath() {
		return path;
	}
}
