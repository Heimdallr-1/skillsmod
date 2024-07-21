package net.puffish.skillsmod.server.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.puffish.skillsmod.api.SkillsAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerData extends PersistentState {
	private final Map<UUID, PlayerData> players = new HashMap<>();

	private ServerData() {

	}

	private static ServerData read(NbtCompound tag) {
		var playersData = new ServerData();

		var playersNbt = tag.getCompound("players");
		playersNbt.getKeys().forEach(key -> playersData.players.put(
				UUID.fromString(key),
				PlayerData.read(playersNbt.getCompound(key))
		));

		return playersData;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		var playersNbt = new NbtCompound();
		for (var entry : players.entrySet()) {
			playersNbt.put(
					entry.getKey().toString(),
					entry.getValue().writeNbt(new NbtCompound())
			);
		}
		nbt.put("players", playersNbt);

		return nbt;
	}

	public static ServerData getOrCreate(MinecraftServer server) {
		var persistentStateManager = server.getOverworld().getPersistentStateManager();

		return persistentStateManager.getOrCreate(
				ServerData::read,
				ServerData::new,
				SkillsAPI.MOD_ID
		);
	}

	public PlayerData getPlayerData(ServerPlayerEntity player) {
		return players.computeIfAbsent(player.getUuid(), uuid -> PlayerData.empty());
	}

	public void putPlayerData(ServerPlayerEntity player, PlayerData data) {
		players.put(player.getUuid(), data);
	}

	@Override
	public boolean isDirty() {
		return true;
	}
}
