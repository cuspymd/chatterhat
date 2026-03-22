package io.github.cuspymd.chatterhat.dialogue;

import io.github.cuspymd.chatterhat.config.ChatLanguage;

public record DialogueResponse(String text, ChatLanguage language) {
}
