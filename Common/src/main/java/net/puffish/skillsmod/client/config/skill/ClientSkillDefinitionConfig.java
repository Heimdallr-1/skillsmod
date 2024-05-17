package net.puffish.skillsmod.client.config.skill;

import net.minecraft.text.Text;
import net.puffish.skillsmod.client.config.ClientFrameConfig;
import net.puffish.skillsmod.client.config.ClientIconConfig;

public record ClientSkillDefinitionConfig(
		String id,
		Text title,
		Text description,
		Text extraDescription,
		ClientFrameConfig frame,
		ClientIconConfig icon,
		float size
) { }
