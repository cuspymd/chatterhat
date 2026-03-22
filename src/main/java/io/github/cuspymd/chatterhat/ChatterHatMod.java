package io.github.cuspymd.chatterhat;

import io.github.cuspymd.chatterhat.command.HatCommand;
import io.github.cuspymd.chatterhat.config.ModConfigManager;
import io.github.cuspymd.chatterhat.dialogue.DialogueCoordinator;
import io.github.cuspymd.chatterhat.event.PlayerStateDetector;
import io.github.cuspymd.chatterhat.hat.HatManager;
import io.github.cuspymd.chatterhat.hat.HatSpeechController;
import io.github.cuspymd.chatterhat.item.ModItems;
import io.github.cuspymd.chatterhat.llm.LlmRequestExecutor;
import io.github.cuspymd.chatterhat.llm.ollama.OllamaClient;
import io.github.cuspymd.chatterhat.memory.WorldMemoryRepository;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatterHatMod implements ModInitializer {
	public static final String MOD_ID = "chatterhat";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ModConfigManager configManager;
	private static WorldMemoryRepository worldMemoryRepository;
	private static LlmRequestExecutor llmRequestExecutor;
	private static DialogueCoordinator dialogueCoordinator;
	private static HatSpeechController hatSpeechController;
	private static PlayerStateDetector playerStateDetector;

	@Override
	public void onInitialize() {
		configManager = new ModConfigManager();
		configManager.load();

		worldMemoryRepository = new WorldMemoryRepository();
		llmRequestExecutor = new LlmRequestExecutor();

		OllamaClient ollamaClient = new OllamaClient(configManager);
		HatManager hatManager = new HatManager();
		dialogueCoordinator = new DialogueCoordinator(configManager, worldMemoryRepository, ollamaClient, llmRequestExecutor);
		hatSpeechController = new HatSpeechController(configManager, hatManager, worldMemoryRepository, dialogueCoordinator);
		playerStateDetector = new PlayerStateDetector(configManager, hatSpeechController, hatManager, worldMemoryRepository);

		ModItems.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			HatCommand.register(dispatcher, configManager, worldMemoryRepository, hatManager, hatSpeechController, dialogueCoordinator)
		);

		ServerLifecycleEvents.SERVER_STARTED.register(worldMemoryRepository::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			worldMemoryRepository.save(server);
			llmRequestExecutor.shutdown();
		});
		ServerTickEvents.END_SERVER_TICK.register(playerStateDetector::tick);

		LOGGER.info("ChatterHat initialized.");
	}

	public static ModConfigManager getConfigManager() {
		return configManager;
	}
}
