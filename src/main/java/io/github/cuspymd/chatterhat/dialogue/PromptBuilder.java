package io.github.cuspymd.chatterhat.dialogue;

import io.github.cuspymd.chatterhat.config.ModConfig;
import io.github.cuspymd.chatterhat.memory.RecentMessage;
import io.github.cuspymd.chatterhat.memory.WorldMemoryData;
import java.util.List;

public final class PromptBuilder {
	private PromptBuilder() {
	}

	public static String buildSystemPrompt(ModConfig config, String persona, String languageRule, DialogueType type) {
		String urgency = switch (type) {
			case DANGER_WARNING -> "위험 경고 상황이다. 반드시 1문장 또는 2문장 이내로 짧고 즉각적으로 말하라.";
			case GUIDE_HINT -> "짧고 실용적인 조언을 우선하라.";
			case PROACTIVE_COMMENT -> "분위기를 살리는 짧은 코멘트를 하되 과하게 길어지지 마라.";
			case PLAYER_CHAT -> "플레이어에게 동료처럼 반응하되 장황하게 늘어놓지 마라.";
		};

		return """
			너는 Minecraft 세계에서 플레이어가 착용한 "수다쟁이 모자"다.
			플레이어를 돕는 동료처럼 행동하되, 시스템이나 AI 정체성을 드러내지 마라.
			반복하지 마라.
			%s
			현재 페르소나: %s
			언어 규칙: %s
			최대 응답 길이: %d자 안팎
			""".formatted(urgency, persona, languageRule, config.responseMaxChars);
	}

	public static String buildUserPrompt(DialogueRequest request, WorldMemoryData memory) {
		StringBuilder builder = new StringBuilder();
		builder.append("Current event/context: ").append(request.contextHint()).append('\n');
		if (memory.summary() != null && !memory.summary().isBlank()) {
			builder.append("Summary memory: ").append(memory.summary()).append('\n');
		}
		appendRecentMessages(builder, memory.recentMessages());
		builder.append("Player input: ").append(request.userMessage());
		return builder.toString();
	}

	private static void appendRecentMessages(StringBuilder builder, List<RecentMessage> messages) {
		if (messages.isEmpty()) {
			return;
		}
		builder.append("Recent conversation:\n");
		for (RecentMessage message : messages) {
			builder.append("- ")
				.append(message.role())
				.append(": ")
				.append(message.text())
				.append('\n');
		}
	}
}
