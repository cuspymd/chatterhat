package io.github.cuspymd.chatterhat.llm.ollama;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cuspymd.chatterhat.config.ModConfig;
import io.github.cuspymd.chatterhat.config.ModConfigManager;
import io.github.cuspymd.chatterhat.llm.LlmClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OllamaClient implements LlmClient {
	private final ModConfigManager configManager;
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	public OllamaClient(ModConfigManager configManager) {
		this.configManager = configManager;
	}

	@Override
	public String chat(String systemPrompt, String userPrompt) throws Exception {
		ModConfig config = this.configManager.get();
		JsonObject payload = new JsonObject();
		payload.addProperty("model", config.ollamaModel);
		payload.addProperty("stream", false);

		JsonArray messages = new JsonArray();
		messages.add(message("system", systemPrompt));
		messages.add(message("user", userPrompt));
		payload.add("messages", messages);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(config.ollamaBaseUrl + "/api/chat"))
			.timeout(Duration.ofSeconds(config.ollamaTimeoutSeconds))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
			.build();

		HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IllegalStateException("Ollama returned status " + response.statusCode());
		}

		JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
		JsonObject message = json.getAsJsonObject("message");
		if (message == null || !message.has("content")) {
			throw new IllegalStateException("Ollama response did not contain message.content");
		}

		return message.get("content").getAsString();
	}

	private static JsonObject message(String role, String content) {
		JsonObject json = new JsonObject();
		json.addProperty("role", role);
		json.addProperty("content", content);
		return json;
	}
}
