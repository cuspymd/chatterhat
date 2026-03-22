package io.github.cuspymd.chatterhat.hat;

import io.github.cuspymd.chatterhat.config.ChatLanguage;
import io.github.cuspymd.chatterhat.config.ModConfig;
import io.github.cuspymd.chatterhat.config.ModConfigManager;
import io.github.cuspymd.chatterhat.dialogue.DialogueCoordinator;
import io.github.cuspymd.chatterhat.dialogue.DialogueRequest;
import io.github.cuspymd.chatterhat.dialogue.DialogueResponse;
import io.github.cuspymd.chatterhat.dialogue.DialogueType;
import io.github.cuspymd.chatterhat.memory.WorldMemoryData;
import io.github.cuspymd.chatterhat.memory.WorldMemoryRepository;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.network.ServerPlayerEntity;

public class HatSpeechController {
	private final ModConfigManager configManager;
	private final HatManager hatManager;
	private final WorldMemoryRepository worldMemoryRepository;
	private final DialogueCoordinator dialogueCoordinator;

	public HatSpeechController(
		ModConfigManager configManager,
		HatManager hatManager,
		WorldMemoryRepository worldMemoryRepository,
		DialogueCoordinator dialogueCoordinator
	) {
		this.configManager = configManager;
		this.hatManager = hatManager;
		this.worldMemoryRepository = worldMemoryRepository;
		this.dialogueCoordinator = dialogueCoordinator;
	}

	public boolean canSpeak(ServerPlayerEntity player) {
		return this.hatManager.isHatEquipped(player);
	}

	public CompletableFuture<DialogueResponse> speakToPlayer(ServerPlayerEntity player, String message) {
		ModConfig config = this.configManager.get();
		WorldMemoryData memory = this.worldMemoryRepository.getOrCreate(player.getEntityWorld().getServer());
		ChatLanguage detected = ChatLanguage.fromPlayerText(message);
		memory.appendRecent("player", message, player.getEntityWorld().getTime(), detected, config);
		this.worldMemoryRepository.save(player.getEntityWorld().getServer());

		DialogueRequest request = new DialogueRequest(
			player,
			DialogueType.PLAYER_CHAT,
			message,
			describePlayerState(player),
			config.chatLanguage
		);

		return this.dialogueCoordinator.requestResponse(request).thenApply(response -> {
			player.getEntityWorld().getServer().execute(() -> {
				HatOutputChannel.sendChat(player, response.text());
				memory.appendRecent("hat", response.text(), player.getEntityWorld().getTime(), response.language(), config);
				this.worldMemoryRepository.save(player.getEntityWorld().getServer());
			});
			return response;
		});
	}

	public void sendWarning(ServerPlayerEntity player, String cooldownKey, int cooldownSeconds, String message, ChatLanguage language) {
		WorldMemoryData memory = this.worldMemoryRepository.getOrCreate(player.getEntityWorld().getServer());
		long now = player.getEntityWorld().getServer().getTicks();
		long nextAllowed = memory.cooldowns.getOrDefault(cooldownKey, 0L);
		if (now < nextAllowed || !canSpeak(player)) {
			return;
		}

		memory.cooldowns.put(cooldownKey, now + cooldownSeconds * 20L);
		HatOutputChannel.sendActionBar(player, message);
		memory.appendRecent("hat", message, now, language, this.configManager.get());
		this.worldMemoryRepository.save(player.getEntityWorld().getServer());
	}

	public void sendProactiveComment(ServerPlayerEntity player, DialogueType type, String contextHint) {
		if (!canSpeak(player)) {
			return;
		}

		DialogueRequest request = new DialogueRequest(player, type, contextHint, contextHint, this.configManager.get().chatLanguage);
		this.dialogueCoordinator.requestResponse(request).thenAccept(response ->
			player.getEntityWorld().getServer().execute(() -> {
				HatOutputChannel.sendChat(player, response.text());
				WorldMemoryData memory = this.worldMemoryRepository.getOrCreate(player.getEntityWorld().getServer());
				memory.appendRecent("hat", response.text(), player.getEntityWorld().getServer().getTicks(), response.language(), this.configManager.get());
				this.worldMemoryRepository.save(player.getEntityWorld().getServer());
			})
		);
	}

	private String describePlayerState(ServerPlayerEntity player) {
		return "health=%.1f hunger=%d biome=%s pos=%s"
			.formatted(
				player.getHealth(),
				player.getHungerManager().getFoodLevel(),
				player.getEntityWorld().getBiome(player.getBlockPos()).getKey().map(key -> key.getValue().toString()).orElse("unknown"),
				player.getBlockPos().toShortString()
			);
	}
}
