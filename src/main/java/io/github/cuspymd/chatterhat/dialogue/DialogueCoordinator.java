package io.github.cuspymd.chatterhat.dialogue;

import io.github.cuspymd.chatterhat.ChatterHatMod;
import io.github.cuspymd.chatterhat.config.ChatLanguage;
import io.github.cuspymd.chatterhat.config.ModConfig;
import io.github.cuspymd.chatterhat.config.ModConfigManager;
import io.github.cuspymd.chatterhat.llm.LlmClient;
import io.github.cuspymd.chatterhat.llm.LlmRequestExecutor;
import io.github.cuspymd.chatterhat.memory.WorldMemoryData;
import io.github.cuspymd.chatterhat.memory.WorldMemoryRepository;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.network.ServerPlayerEntity;

public class DialogueCoordinator {
	private final ModConfigManager configManager;
	private final WorldMemoryRepository worldMemoryRepository;
	private final LlmClient llmClient;
	private final LlmRequestExecutor requestExecutor;

	public DialogueCoordinator(
		ModConfigManager configManager,
		WorldMemoryRepository worldMemoryRepository,
		LlmClient llmClient,
		LlmRequestExecutor requestExecutor
	) {
		this.configManager = configManager;
		this.worldMemoryRepository = worldMemoryRepository;
		this.llmClient = llmClient;
		this.requestExecutor = requestExecutor;
	}

	public CompletableFuture<DialogueResponse> requestResponse(DialogueRequest request) {
		ServerPlayerEntity player = request.player();
		WorldMemoryData memory = this.worldMemoryRepository.getOrCreate(player.getEntityWorld().getServer());
		ModConfig config = this.configManager.get();
		ChatLanguage language = LanguagePolicy.resolve(config, memory, request);
		String persona = PersonaResolver.resolve(config);
		String systemPrompt = PromptBuilder.buildSystemPrompt(config, persona, LanguagePolicy.buildRule(language, config.allowMixedLanguage), request.type());
		String userPrompt = PromptBuilder.buildUserPrompt(request, memory);

		return this.requestExecutor.submit(() -> {
			String raw = this.llmClient.chat(systemPrompt, userPrompt);
			String processed = ResponsePostProcessor.trim(raw, config.responseMaxChars);
			return new DialogueResponse(processed, language);
		}).exceptionally(exception -> {
			ChatterHatMod.LOGGER.warn("Falling back to template response", exception);
			return new DialogueResponse(buildFallback(request.type(), language), language);
		});
	}

	public CompletableFuture<String> testConnection() {
		ModConfig config = this.configManager.get();
		return this.requestExecutor.submit(() ->
			this.llmClient.chat(
				"Reply with a short status line only.",
				"Model '%s' is being checked by the ChatterHat Minecraft mod. Reply with one short greeting.".formatted(config.ollamaModel)
			)
		);
	}

	private String buildFallback(DialogueType type, ChatLanguage language) {
		return switch (type) {
			case DANGER_WARNING -> language == ChatLanguage.KOREAN ? "지금은 조심하는 게 좋겠어." : "Careful, this looks dangerous.";
			case GUIDE_HINT -> language == ChatLanguage.KOREAN ? "준비를 조금 더 하고 움직이자." : "Let's prepare a bit more before moving.";
			case PROACTIVE_COMMENT -> language == ChatLanguage.KOREAN ? "오늘도 제법 바쁜 모험이 되겠는걸." : "This is turning into quite the adventure.";
			case PLAYER_CHAT -> language == ChatLanguage.KOREAN ? "응, 듣고 있어." : "I'm listening.";
		};
	}
}
