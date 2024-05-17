package net.puffish.skillsmod.impl.rewards;

import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElementWrapper;
import net.puffish.skillsmod.api.rewards.RewardConfigContext;
import net.puffish.skillsmod.api.utils.Failure;
import net.puffish.skillsmod.api.utils.Result;

public record RewardConfigContextImpl(
		ConfigContext context,
		Result<JsonElementWrapper, Failure> maybeDataElement
) implements RewardConfigContext {

	@Override
	public MinecraftServer getServer() {
		return context.getServer();
	}

	@Override
	public DynamicRegistryManager getDynamicRegistryManager() {
		return context.getDynamicRegistryManager();
	}

	@Override
	public ResourceManager getResourceManager() {
		return context.getResourceManager();
	}

	@Override
	public void addWarning(String message) {
		context.addWarning(message);
	}

	@Override
	public Result<JsonElementWrapper, Failure> getData() {
		return maybeDataElement;
	}
}
