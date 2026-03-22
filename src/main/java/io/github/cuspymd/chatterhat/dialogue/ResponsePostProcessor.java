package io.github.cuspymd.chatterhat.dialogue;

public final class ResponsePostProcessor {
	private ResponsePostProcessor() {
	}

	public static String trim(String text, int maxChars) {
		String sanitized = text == null ? "" : text.trim().replaceAll("\\s+", " ");
		if (sanitized.length() <= maxChars) {
			return sanitized;
		}
		return sanitized.substring(0, Math.max(0, maxChars - 1)).trim() + "…";
	}
}
