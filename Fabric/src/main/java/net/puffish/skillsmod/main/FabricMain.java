package net.puffish.skillsmod.main;

import com.mojang.brigadier.arguments.ArgumentType;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import net.minecraft.util.registry.Registry;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.mixin.GameRulesAccessor;
import net.puffish.skillsmod.network.InPacket;
import net.puffish.skillsmod.network.OutPacket;
import net.puffish.skillsmod.server.event.ServerEventListener;
import net.puffish.skillsmod.server.event.ServerEventReceiver;
import net.puffish.skillsmod.server.network.ServerPacketHandler;
import net.puffish.skillsmod.server.network.ServerPacketSender;
import net.puffish.skillsmod.server.setup.ServerRegistrar;

import java.util.function.Function;

public class FabricMain implements ModInitializer {

	@Override
	public void onInitialize() {
		SkillsMod.setup(
				FabricLoader.getInstance().getConfigDir(),
				new ServerRegistrarImpl(),
				new ServerEventReceiverImpl(),
				new ServerPacketSenderImpl()
		);

	}

	private static class ServerRegistrarImpl implements ServerRegistrar {
		@Override
		public <V, T extends V> void register(Registry<V> registry, Identifier id, T entry) {
			Registry.register(registry, id, entry);
		}

		@Override
		public <T extends GameRules.Rule<T>> void registerGameRule(GameRules.Key<T> key, GameRules.Type<T> type) {
			GameRulesAccessor.getRuleTypes().put(key, type);
		}

		@Override
		public <A extends ArgumentType<?>> void registerArgumentType(Identifier id, Class<A> clazz, ArgumentSerializer<A> serializer) {
			ArgumentTypes.register(id.toString(), clazz, serializer);
		}

		@Override
		public <T extends InPacket> void registerInPacket(Identifier id, Function<PacketByteBuf, T> reader, ServerPacketHandler<T> handler) {
			ServerPlayNetworking.registerGlobalReceiver(
					id,
					(server, player, handler2, buf, responseSender) -> {
						var packet = reader.apply(buf);
						server.execute(
								() -> handler.handle(player, packet)
						);
					}
			);
		}

		@Override
		public void registerOutPacket(Identifier id) { }
	}

	private static class ServerEventReceiverImpl implements ServerEventReceiver {
		@Override
		public void registerListener(ServerEventListener eventListener) {
			ServerLifecycleEvents.SERVER_STARTING.register(eventListener::onServerStarting);

			ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
					(server, resourceManager, success) -> eventListener.onServerReload(server)
			);

			ServerPlayConnectionEvents.JOIN.register(
					(handler, sender, server) -> eventListener.onPlayerJoin(handler.player)
			);

			CommandRegistrationCallback.EVENT.register(
					(dispatcher, dedicated) -> eventListener.onCommandsRegister(dispatcher)
			);
		}
	}

	private static class ServerPacketSenderImpl implements ServerPacketSender {
		@Override
		public void send(ServerPlayerEntity player, OutPacket packet) {
			var buf = new PacketByteBuf(Unpooled.buffer());
			packet.write(buf);
			ServerPlayNetworking.send(player, packet.getId(), buf);
		}
	}
}
