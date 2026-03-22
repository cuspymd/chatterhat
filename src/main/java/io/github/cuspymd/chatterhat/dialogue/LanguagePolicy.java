package io.github.cuspymd.chatterhat.dialogue;

import io.github.cuspymd.chatterhat.config.ChatLanguage;
import io.github.cuspymd.chatterhat.config.ModConfig;
import io.github.cuspymd.chatterhat.memory.RecentMessage;
import io.github.cuspymd.chatterhat.memory.WorldMemoryData;

public final class LanguagePolicy {
	private LanguagePolicy() {
	}

	public static ChatLanguage resolve(ModConfig config, WorldMemoryData memory, DialogueRequest request) {
		if (request.requestedLanguage() != ChatLanguage.AUTO) {
			return request.requestedLanguage();
		}
		if (config.chatLanguage != ChatLanguage.AUTO) {
			return config.chatLanguage;
		}

		RecentMessage recentPlayerMessage = memory.findLatestByRole("player");
		if (recentPlayerMessage != null && recentPlayerMessage.language() != ChatLanguage.AUTO) {
			return recentPlayerMessage.language();
		}

		return ChatLanguage.ENGLISH;
	}

	public static String buildRule(ChatLanguage language, boolean allowMixedLanguage) {
		if (language == ChatLanguage.KOREAN) {
			return allowMixedLanguage
				? "Respond mostly in Korean. Keep foreign words to item names or unavoidable proper nouns."
				: "출력은 반드시 한국어로만 작성하라. 영어 메타 표현을 섞지 마라.";
		}
		if (language == ChatLanguage.ENGLISH) {
			return allowMixedLanguage
				? "Respond primarily in English. You may keep Minecraft item names as-is."
				: "Respond only in English. Do not switch into Korean unless quoting the player.";
		}
		return "Match the player's most recent language and stay consistent inside a single reply.";
	}
}
