package io.github.cuspymd.chatterhat.memory;

import io.github.cuspymd.chatterhat.config.ChatLanguage;
import io.github.cuspymd.chatterhat.config.ModConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldMemoryData {
	public int schemaVersion = 1;
	public String worldId = "default-world";
	public String personaId = "guide_friendly";
	public String summary = "";
	public List<RecentMessage> recentMessages = new ArrayList<>();
	public List<String> facts = new ArrayList<>();
	public Map<String, Long> cooldowns = new HashMap<>();

	public void appendRecent(String role, String text, long timestamp, ChatLanguage language, ModConfig config) {
		this.recentMessages.add(new RecentMessage(role, text, timestamp, language));
		int overflow = this.recentMessages.size() - config.recentConversationLimit;
		if (overflow > 0) {
			this.recentMessages.subList(0, overflow).clear();
		}
	}

	public List<RecentMessage> recentMessages() {
		return this.recentMessages;
	}

	public String summary() {
		return this.summary;
	}

	public RecentMessage findLatestByRole(String role) {
		for (int index = this.recentMessages.size() - 1; index >= 0; index--) {
			RecentMessage message = this.recentMessages.get(index);
			if (message.role().equals(role)) {
				return message;
			}
		}
		return null;
	}
}
