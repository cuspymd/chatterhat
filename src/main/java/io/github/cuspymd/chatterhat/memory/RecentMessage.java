package io.github.cuspymd.chatterhat.memory;

import io.github.cuspymd.chatterhat.config.ChatLanguage;

public record RecentMessage(String role, String text, long timestamp, ChatLanguage language) {
}
