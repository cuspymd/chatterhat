package io.github.cuspymd.chatterhat.config;

import java.util.Locale;

public enum ChatLanguage {
	AUTO("auto"),
	KOREAN("ko"),
	ENGLISH("en");

	private final String id;

	ChatLanguage(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	public static ChatLanguage fromId(String value) {
		for (ChatLanguage language : values()) {
			if (language.id.equalsIgnoreCase(value)) {
				return language;
			}
		}
		return AUTO;
	}

	public static ChatLanguage fromPlayerText(String text) {
		for (int index = 0; index < text.length(); index++) {
			char character = text.charAt(index);
			if (character >= '\uAC00' && character <= '\uD7A3') {
				return KOREAN;
			}
		}

		for (int index = 0; index < text.length(); index++) {
			if (Character.isLetter(text.charAt(index))) {
				return ENGLISH;
			}
		}

		return AUTO;
	}

	public static ChatLanguage fromLocale(String locale) {
		return locale != null && locale.toLowerCase(Locale.ROOT).startsWith("ko") ? KOREAN : ENGLISH;
	}
}
