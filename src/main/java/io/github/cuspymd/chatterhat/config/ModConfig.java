package io.github.cuspymd.chatterhat.config;

public class ModConfig {
	public int schemaVersion = 1;
	public String ollamaBaseUrl = "http://127.0.0.1:11434";
	public String ollamaModel = "qwen3:8b";
	public int ollamaTimeoutSeconds = 45;
	public ChatLanguage chatLanguage = ChatLanguage.AUTO;
	public boolean allowMixedLanguage = false;
	public boolean warningsFollowChatLanguage = true;
	public String personaId = PersonaPreset.GUIDE_FRIENDLY.id();
	public String customPersona = "";
	public int responseMaxChars = 220;
	public int recentConversationLimit = 12;
	public int lowHealthCooldownSeconds = 30;
	public int lowHungerCooldownSeconds = 40;
	public int monsterNearbyCooldownSeconds = 20;
	public int firstNightCooldownSeconds = 600;
}
