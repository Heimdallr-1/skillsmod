package net.puffish.skillsmod.main;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.event.ClientEventListener;
import net.puffish.skillsmod.client.event.ClientEventReceiver;
import net.puffish.skillsmod.client.keybinding.KeyBindingHandler;
import net.puffish.skillsmod.client.keybinding.KeyBindingReceiver;
import net.puffish.skillsmod.client.network.ClientPacketHandler;
import net.puffish.skillsmod.client.network.ClientPacketSender;
import net.puffish.skillsmod.client.setup.ClientRegistrar;
import net.puffish.skillsmod.network.InPacket;
import net.puffish.skillsmod.network.OutPacket;

import java.util.function.Function;

public class FabricClientMain implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		SkillsClientMod.setup(
				new ClientRegistrarImpl(),
				new ClientEventReceiverImpl(),
				new KeyBindingReceiverImpl(),
				new ClientPacketSenderImpl()
		);
	}

	private static class ClientRegistrarImpl implements ClientRegistrar {
		@Override
		public <T extends InPacket> void registerInPacket(Identifier id, Function<PacketByteBuf, T> reader, ClientPacketHandler<T> handler) {
			ClientPlayNetworking.registerGlobalReceiver(
					id,
					(client, handler2, buf, responseSender) -> {
						var packet = reader.apply(buf);
						client.execute(() -> handler.handle(packet));
					}
			);
		}

		@Override
		public void registerOutPacket(Identifier id) { }
	}

	private static class ClientEventReceiverImpl implements ClientEventReceiver {
		@Override
		public void registerListener(ClientEventListener eventListener) {
			ClientPlayConnectionEvents.JOIN.register(
					(handler, sender, client) -> eventListener.onPlayerJoin()
			);
		}
	}

	private static class KeyBindingReceiverImpl implements KeyBindingReceiver {
		@Override
		public void registerKeyBinding(KeyBinding keyBinding, KeyBindingHandler handler) {
			ClientTickEvents.END_CLIENT_TICK.register(
					client -> {
						if (keyBinding.wasPressed()) {
							handler.handle();
						}
					}
			);
			KeyBindingHelper.registerKeyBinding(keyBinding);
		}
	}

	private static class ClientPacketSenderImpl implements ClientPacketSender {
		@Override
		public void send(OutPacket packet) {
			var buf = new PacketByteBuf(Unpooled.buffer());
			packet.write(buf);
			ClientPlayNetworking.send(packet.getId(), buf);
		}
	}
}
