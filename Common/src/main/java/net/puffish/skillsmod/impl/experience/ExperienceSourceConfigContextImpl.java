package net.puffish.skillsmod.impl.experience;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.experience.ExperienceSourceConfigContext;
import net.puffish.skillsmod.api.json.JsonElementWrapper;
import net.puffish.skillsmod.api.utils.Failure;
import net.puffish.skillsmod.api.utils.Result;

public record ExperienceSourceConfigContextImpl(
		ConfigContext context,
		Result<JsonElementWrapper, Failure> maybeDataElement
) implements ExperienceSourceConfigContext {

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
