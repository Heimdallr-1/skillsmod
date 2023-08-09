package net.puffish.skillsmod.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.puffish.skillsmod.SkillsAPI;

public class PointsCommand {
	public static LiteralArgumentBuilder<ServerCommandSource> create() {
		return CommandManager.literal("points")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.literal("add")
						.then(CommandManager.argument("players", EntityArgumentType.players())
								.then(CommandManager.argument("category", IdentifierArgumentType.identifier())
										.then(CommandManager.argument("count", IntegerArgumentType.integer())
												.executes(context -> {
													var players = EntityArgumentType.getPlayers(context, "players");
													var categoryId = IdentifierArgumentType.getIdentifier(context, "category");
													var count = IntegerArgumentType.getInteger(context, "count");
													for (var player : players) {
														SkillsAPI.addExtraPoints(player, categoryId, count);
													}
													return players.size();
												})
										)
								)
						)
				)
				.then(CommandManager.literal("set")
						.then(CommandManager.argument("players", EntityArgumentType.players())
								.then(CommandManager.argument("category", IdentifierArgumentType.identifier())
										.then(CommandManager.argument("count", IntegerArgumentType.integer())
												.executes(context -> {
													var players = EntityArgumentType.getPlayers(context, "players");
													var categoryId = IdentifierArgumentType.getIdentifier(context, "category");
													var count = IntegerArgumentType.getInteger(context, "count");
													for (var player : players) {
														SkillsAPI.setPointsLeft(player, categoryId, count);
													}
													return players.size();
												})
										)
								)
						)
				);
	}
}
