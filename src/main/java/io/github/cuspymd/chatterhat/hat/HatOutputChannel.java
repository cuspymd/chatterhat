package io.github.cuspymd.chatterhat.hat;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class HatOutputChannel {
	private HatOutputChannel() {
	}

	public static void sendChat(ServerPlayerEntity player, String message) {
		player.sendMessage(Text.literal("[ChatterHat] " + message), false);
	}

	public static void sendActionBar(ServerPlayerEntity player, String message) {
		player.sendMessage(Text.literal("[ChatterHat] " + message), true);
	}
}
