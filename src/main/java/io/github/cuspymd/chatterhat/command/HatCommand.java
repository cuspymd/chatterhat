package io.github.cuspymd.chatterhat.command;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.cuspymd.chatterhat.config.ChatLanguage;
import io.github.cuspymd.chatterhat.config.ModConfigManager;
import io.github.cuspymd.chatterhat.config.PersonaPreset;
import io.github.cuspymd.chatterhat.dialogue.DialogueCoordinator;
import io.github.cuspymd.chatterhat.hat.HatManager;
import io.github.cuspymd.chatterhat.hat.HatSpeechController;
import io.github.cuspymd.chatterhat.memory.WorldMemoryData;
import io.github.cuspymd.chatterhat.memory.WorldMemoryRepository;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class HatCommand {
	private HatCommand() {
	}

	public static void register(
		CommandDispatcher<ServerCommandSource> dispatcher,
		ModConfigManager configManager,
		WorldMemoryRepository worldMemoryRepository,
		HatManager hatManager,
		HatSpeechController hatSpeechController,
		DialogueCoordinator dialogueCoordinator
	) {
		dispatcher.register(literal("hat")
			.then(literal("say")
				.then(argument("message", StringArgumentType.greedyString())
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
						if (!hatManager.isHatEquipped(player)) {
							context.getSource().sendError(Text.literal("ChatterHat must be equipped in the helmet slot."));
							return 0;
						}

						String message = StringArgumentType.getString(context, "message");
						context.getSource().sendFeedback(() -> Text.literal("ChatterHat is thinking..."), false);
						hatSpeechController.speakToPlayer(player, message);
						return 1;
					})
				)
			)
			.then(literal("status")
				.executes(context -> {
					ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
					WorldMemoryData memory = worldMemoryRepository.getOrCreate(player.getEntityWorld().getServer());
					String status = """
						equipped=%s
						language=%s
						persona=%s
						model=%s
						recentMessages=%d
						facts=%d
						""".formatted(
						hatManager.isHatEquipped(player),
						configManager.get().chatLanguage.id(),
						configManager.get().personaId,
						configManager.get().ollamaModel,
						memory.recentMessages.size(),
						memory.facts.size()
					);
					context.getSource().sendFeedback(() -> Text.literal(status), false);
					return 1;
				})
			)
			.then(literal("test")
				.then(literal("ollama")
					.executes(context -> {
						ServerCommandSource source = context.getSource();
						source.sendFeedback(() -> Text.literal("Testing Ollama connection..."), false);
						dialogueCoordinator.testConnection().thenAccept(result ->
							source.getServer().execute(() ->
								source.sendFeedback(() -> Text.literal("Ollama reply: " + result), false)
							)
						).exceptionally(exception -> {
							source.getServer().execute(() ->
								source.sendError(Text.literal("Ollama test failed: " + exception.getMessage()))
							);
							return null;
						});
						return 1;
					})
				)
			)
			.then(literal("memory")
				.then(literal("dump")
					.executes(context -> {
						WorldMemoryData memory = worldMemoryRepository.getOrCreate(context.getSource().getServer());
						context.getSource().sendFeedback(() -> Text.literal(
							"summary=" + memory.summary + "\nrecentMessages=" + memory.recentMessages + "\nfacts=" + memory.facts + "\ncooldowns=" + memory.cooldowns
						), false);
						return 1;
					})
				)
				.then(literal("clear")
					.executes(context -> {
						worldMemoryRepository.clear(context.getSource().getServer());
						context.getSource().sendFeedback(() -> Text.literal("ChatterHat world memory cleared."), true);
						return 1;
					})
				)
			)
			.then(literal("config")
				.then(literal("language")
					.then(argument("value", StringArgumentType.word())
						.executes(context -> {
							configManager.get().chatLanguage = ChatLanguage.fromId(StringArgumentType.getString(context, "value"));
							configManager.save();
							context.getSource().sendFeedback(() -> Text.literal("chatLanguage=" + configManager.get().chatLanguage.id()), false);
							return 1;
						})
					)
				)
				.then(literal("persona")
					.then(argument("value", StringArgumentType.word())
						.executes(context -> {
							String personaId = StringArgumentType.getString(context, "value");
							configManager.get().personaId = PersonaPreset.fromId(personaId).id();
							configManager.save();
							context.getSource().sendFeedback(() -> Text.literal("persona=" + configManager.get().personaId), false);
							return 1;
						})
					)
				)
				.then(literal("model")
					.then(argument("value", StringArgumentType.greedyString())
						.executes(context -> {
							configManager.get().ollamaModel = StringArgumentType.getString(context, "value");
							configManager.save();
							context.getSource().sendFeedback(() -> Text.literal("model=" + configManager.get().ollamaModel), false);
							return 1;
						})
					)
				)
			)
		);
	}
}
