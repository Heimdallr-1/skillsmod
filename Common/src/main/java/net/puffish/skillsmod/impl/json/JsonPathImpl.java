package net.puffish.skillsmod.impl.json;

import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.api.util.Problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonPathImpl implements JsonPath {
	private final List<String> path;

	public JsonPathImpl(List<String> path) {
		this.path = path;
	}

	@Override
	public JsonPath getArray(long index) {
		return this.get("index " + index);
	}

	@Override
	public JsonPath getObject(String key) {
		return this.get("`" + key + "`");
	}

	private JsonPath get(String str) {
		var path = new ArrayList<String>();
		path.add(str);
		path.addAll(this.path);
		return new JsonPathImpl(path);
	}

	@Override
	public Optional<JsonPath> getParent() {
		if (path.size() <= 1) {
			return Optional.empty();
		}
		return Optional.of(new JsonPathImpl(
				new ArrayList<>(this.path.subList(1, this.path.size()))
		));
	}

	public String getHead() {
		return this.path.get(0);
	}

	public Problem expectedToExist() {
		return expectedTo("exist");
	}

	public Problem expectedToExistAndBe(String str) {
		return expectedTo("exist and be " + str);
	}

	public Problem expectedToBe(String str) {
		return expectedTo("be " + str);
	}

	@Override
	public Problem createProblem(String message) {
		return Problem.message(message + " at " + this);
	}

	public Problem expectedTo(String str) {
		var parent = getParent();
		if (parent.isPresent()) {
			return Problem.message("Expected " + getHead() + " to " + str + " at " + parent.orElseThrow() + ".");
		} else {
			return Problem.message("Expected " + getHead() + " to " + str + ".");
		}
	}

	@Override
	public String toString() {
		return String.join(" at ", path);
	}
}
