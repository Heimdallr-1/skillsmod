package net.puffish.skillsmod.impl.calculation.operation;

import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.api.calculation.operation.OperationConfigContext;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

public record OperationConfigContextImpl(
		ConfigContext context,
		Result<JsonElement, Problem> maybeDataElement
) implements OperationConfigContext {

	@Override
	public MinecraftServer getServer() {
		return context.getServer();
	}

	@Override
	public void emitWarning(String message) {
		context.emitWarning(message);
	}

	@Override
	public Result<JsonElement, Problem> getData() {
		return maybeDataElement;
	}
}
