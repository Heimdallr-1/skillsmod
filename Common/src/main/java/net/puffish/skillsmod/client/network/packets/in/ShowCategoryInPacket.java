package net.puffish.skillsmod.client.network.packets.in;

import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.network.PacketByteBuf;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.client.config.ClientBackgroundConfig;
import net.puffish.skillsmod.client.config.ClientCategoryConfig;
import net.puffish.skillsmod.client.config.ClientFrameConfig;
import net.puffish.skillsmod.client.config.ClientIconConfig;
import net.puffish.skillsmod.client.config.colors.ClientColorConfig;
import net.puffish.skillsmod.client.config.colors.ClientColorsConfig;
import net.puffish.skillsmod.client.config.colors.ClientConnectionsColorsConfig;
import net.puffish.skillsmod.client.config.colors.ClientFillStrokeColorsConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConnectionConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillDefinitionConfig;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.common.BackgroundPosition;
import net.puffish.skillsmod.common.FrameType;
import net.puffish.skillsmod.common.IconType;
import net.puffish.skillsmod.network.InPacket;

import java.util.stream.Collectors;

public class ShowCategoryInPacket implements InPacket {
	private final ClientCategoryData category;

	private ShowCategoryInPacket(ClientCategoryData category) {
		this.category = category;
	}

	public static ShowCategoryInPacket read(PacketByteBuf buf) {
		var category = readCategory(buf);

		return new ShowCategoryInPacket(category);
	}

	public static ClientCategoryData readCategory(PacketByteBuf buf) {
		var id = buf.readIdentifier();

		var title = buf.readText();
		var icon = readSkillIcon(buf);
		var background = readBackground(buf);
		var colors = readColors(buf);
		var exclusiveRoot = buf.readBoolean();
		var spentPointsLimit = buf.readInt();

		var definitions = buf.readList(ShowCategoryInPacket::readDefinition)
				.stream()
				.collect(Collectors.toMap(ClientSkillDefinitionConfig::id, definition -> definition));

		var skills = buf.readList(ShowCategoryInPacket::readSkill)
				.stream()
				.collect(Collectors.toMap(ClientSkillConfig::id, skill -> skill));

		var normalConnections = buf.readList(ShowCategoryInPacket::readSkillConnection);
		var exclusiveConnections = buf.readList(ShowCategoryInPacket::readSkillConnection);

		var skillsStates = buf.readMap(
				PacketByteBuf::readString,
				buf1 -> buf1.readEnumConstant(Skill.State.class)
		);

		var spentPoints = buf.readInt();
		var earnedPoints = buf.readInt();

		var currentLevel = Integer.MIN_VALUE;
		var currentExperience = Integer.MIN_VALUE;
		var requiredExperience = Integer.MIN_VALUE;
		if (buf.readBoolean()) {
			currentLevel = buf.readInt();
			currentExperience = buf.readInt();
			requiredExperience = buf.readInt();
		}

		var category = new ClientCategoryConfig(
				id,
				title,
				icon,
				background,
				colors,
				exclusiveRoot,
				spentPointsLimit,
				definitions,
				skills,
				normalConnections,
				exclusiveConnections
		);

		return new ClientCategoryData(
				category,
				skillsStates,
				spentPoints,
				earnedPoints,
				currentLevel,
				currentExperience,
				requiredExperience
		);
	}

	public static ClientSkillDefinitionConfig readDefinition(PacketByteBuf buf) {
		var id = buf.readString();
		var title = buf.readText();
		var description = buf.readText();
		var extraDescription = buf.readText();
		var frame = readFrameIcon(buf);
		var icon = readSkillIcon(buf);
		var size = buf.readFloat();
		var cost = buf.readInt();
		var requiredSkills = buf.readInt();
		var requiredPoints = buf.readInt();
		var requiredSpentPoints = buf.readInt();

		return new ClientSkillDefinitionConfig(
				id,
				title,
				description,
				extraDescription,
				icon,
				frame,
				size,
				cost,
				requiredSkills,
				requiredPoints,
				requiredSpentPoints
		);
	}

	public static ClientIconConfig readSkillIcon(PacketByteBuf buf) {
		var type = buf.readEnumConstant(IconType.class);
		return switch (type) {
			case EFFECT -> {
				var effect = StatusEffect.byRawId(buf.readInt());
				yield new ClientIconConfig.EffectIconConfig(effect);
			}
			case ITEM -> {
				var itemStack = buf.readItemStack();
				yield new ClientIconConfig.ItemIconConfig(itemStack);
			}
			case TEXTURE -> {
				var texture = buf.readIdentifier();
				yield new ClientIconConfig.TextureIconConfig(texture);
			}
		};
	}

	public static ClientFrameConfig readFrameIcon(PacketByteBuf buf) {
		var type = buf.readEnumConstant(FrameType.class);
		return switch (type) {
			case ADVANCEMENT -> {
				var advancementFrame = buf.readEnumConstant(AdvancementFrame.class);
				yield new ClientFrameConfig.AdvancementFrameConfig(advancementFrame);
			}
			case TEXTURE -> {
				var lockedTexture = buf.readOptional(PacketByteBuf::readIdentifier);
				var availableTexture = buf.readIdentifier();
				var affordableTexture = buf.readOptional(PacketByteBuf::readIdentifier);
				var unlockedTexture = buf.readIdentifier();
				var excludedTexture = buf.readOptional(PacketByteBuf::readIdentifier);
				yield new ClientFrameConfig.TextureFrameConfig(
						lockedTexture,
						availableTexture,
						affordableTexture,
						unlockedTexture,
						excludedTexture
				);
			}
		};
	}

	public static ClientBackgroundConfig readBackground(PacketByteBuf buf) {
		var texture = buf.readIdentifier();
		var width = buf.readInt();
		var height = buf.readInt();
		var position = buf.readEnumConstant(BackgroundPosition.class);

		return new ClientBackgroundConfig(texture, width, height, position);
	}

	public static ClientColorsConfig readColors(PacketByteBuf buf) {
		var connections = readConnectionsColors(buf);
		var points = readFillStrokeColors(buf);

		return new ClientColorsConfig(connections, points);
	}

	public static ClientConnectionsColorsConfig readConnectionsColors(PacketByteBuf buf) {
		var locked = readFillStrokeColors(buf);
		var available = readFillStrokeColors(buf);
		var affordable = readFillStrokeColors(buf);
		var unlocked = readFillStrokeColors(buf);
		var excluded = readFillStrokeColors(buf);

		return new ClientConnectionsColorsConfig(locked, available, affordable, unlocked, excluded);
	}

	public static ClientFillStrokeColorsConfig readFillStrokeColors(PacketByteBuf buf) {
		var fill = readColor(buf);
		var stroke = readColor(buf);

		return new ClientFillStrokeColorsConfig(fill, stroke);
	}

	public static ClientColorConfig readColor(PacketByteBuf buf) {
		var argb = buf.readInt();

		return new ClientColorConfig(argb);
	}

	public static ClientSkillConfig readSkill(PacketByteBuf buf) {
		var id = buf.readString();
		var x = buf.readInt();
		var y = buf.readInt();
		var definition = buf.readString();
		var isRoot = buf.readBoolean();

		return new ClientSkillConfig(id, x, y, definition, isRoot);
	}

	public static ClientSkillConnectionConfig readSkillConnection(PacketByteBuf buf) {
		var skillAId = buf.readString();
		var skillBId = buf.readString();
		var bidirectional = buf.readBoolean();

		return new ClientSkillConnectionConfig(skillAId, skillBId, bidirectional);
	}

	public ClientCategoryData getCategory() {
		return category;
	}
}
