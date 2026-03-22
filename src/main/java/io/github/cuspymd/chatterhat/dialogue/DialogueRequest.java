package io.github.cuspymd.chatterhat.dialogue;

import io.github.cuspymd.chatterhat.config.ChatLanguage;
import net.minecraft.server.network.ServerPlayerEntity;

public record DialogueRequest(
	ServerPlayerEntity player,
	DialogueType type,
	String userMessage,
	String contextHint,
	ChatLanguage requestedLanguage
) {
}
