package net.puffish.skillsmod.experience;

import net.puffish.skillsmod.json.JsonElementWrapper;
import net.puffish.skillsmod.utils.Result;
import net.puffish.skillsmod.utils.error.Error;

public interface ExperienceSourceWithDataFactory {
	Result<? extends ExperienceSource, Error> create(JsonElementWrapper json);
}