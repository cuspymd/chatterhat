package io.github.cuspymd.chatterhat.event;

import io.github.cuspymd.chatterhat.config.ChatLanguage;
import io.github.cuspymd.chatterhat.config.ModConfig;
import io.github.cuspymd.chatterhat.config.ModConfigManager;
import io.github.cuspymd.chatterhat.dialogue.DialogueType;
import io.github.cuspymd.chatterhat.hat.HatManager;
import io.github.cuspymd.chatterhat.hat.HatSpeechController;
import io.github.cuspymd.chatterhat.memory.WorldMemoryData;
import io.github.cuspymd.chatterhat.memory.WorldMemoryRepository;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

public class PlayerStateDetector {
	private final ModConfigManager configManager;
	private final HatSpeechController hatSpeechController;
	private final HatManager hatManager;
	private final WorldMemoryRepository worldMemoryRepository;

	public PlayerStateDetector(
		ModConfigManager configManager,
		HatSpeechController hatSpeechController,
		HatManager hatManager,
		WorldMemoryRepository worldMemoryRepository
	) {
		this.configManager = configManager;
		this.hatSpeechController = hatSpeechController;
		this.hatManager = hatManager;
		this.worldMemoryRepository = worldMemoryRepository;
	}

	public void tick(MinecraftServer server) {
		if (server.getTicks() % 20 != 0) {
			return;
		}

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (!this.hatManager.isHatEquipped(player) || player.isSpectator()) {
				continue;
			}
			evaluate(player);
		}
	}

	private void evaluate(ServerPlayerEntity player) {
		ModConfig config = this.configManager.get();
		ChatLanguage language = resolveWarningLanguage(config);
		if (player.getHealth() <= 8.0F) {
			this.hatSpeechController.sendWarning(
				player,
				"LOW_HEALTH",
				config.lowHealthCooldownSeconds,
				language == ChatLanguage.KOREAN ? "체력이 너무 낮아. 지금은 물러나는 게 좋아." : "Your health is getting low. Back off for a moment.",
				language
			);
		}

		if (player.getHungerManager().getFoodLevel() <= 6) {
			this.hatSpeechController.sendWarning(
				player,
				"LOW_HUNGER",
				config.lowHungerCooldownSeconds,
				language == ChatLanguage.KOREAN ? "배가 많이 고파 보여. 음식부터 챙기자." : "You're getting hungry. Let's find food first.",
				language
			);
		}

		if (!player.getEntityWorld().isDay()) {
			WorldMemoryData memory = this.worldMemoryRepository.getOrCreate(player.getEntityWorld().getServer());
			if (!memory.cooldowns.containsKey("FIRST_NIGHT")) {
				this.hatSpeechController.sendWarning(
					player,
					"FIRST_NIGHT",
					config.firstNightCooldownSeconds,
					language == ChatLanguage.KOREAN ? "첫 밤이야. 횃불과 은신처를 먼저 챙기자." : "Night has fallen. Let's secure light and shelter first.",
					language
				);
			}
		}

		boolean hostileNearby = !player.getEntityWorld()
			.getEntitiesByClass(
				HostileEntity.class,
				new Box(player.getX() - 6.0, player.getY() - 4.0, player.getZ() - 6.0, player.getX() + 6.0, player.getY() + 4.0, player.getZ() + 6.0),
				entity -> entity.isAlive()
			)
			.isEmpty();
		if (hostileNearby) {
			this.hatSpeechController.sendWarning(
				player,
				"MONSTER_NEARBY",
				config.monsterNearbyCooldownSeconds,
				language == ChatLanguage.KOREAN ? "근처에 적대적인 몹이 있어. 주변을 먼저 살펴봐." : "Hostile mobs are nearby. Check your surroundings first.",
				language
			);
		}
	}

	private ChatLanguage resolveWarningLanguage(ModConfig config) {
		if (!config.warningsFollowChatLanguage) {
			return ChatLanguage.KOREAN;
		}
		return config.chatLanguage == ChatLanguage.AUTO ? ChatLanguage.ENGLISH : config.chatLanguage;
	}
}
