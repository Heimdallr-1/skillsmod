package net.puffish.skillsmod.client.network.packets.out;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.network.OutPacket;
import net.puffish.skillsmod.network.Packets;

public record SkillClickOutPacket(Identifier categoryId, String skillId) implements OutPacket {
	@Override
	public void write(RegistryByteBuf buf) {
		buf.writeIdentifier(categoryId);
		buf.writeString(skillId);
	}

	@Override
	public Identifier getId() {
		return Packets.SKILL_CLICK;
	}
}
