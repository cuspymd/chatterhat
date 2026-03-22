package io.github.cuspymd.chatterhat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.cuspymd.chatterhat.ChatterHatMod;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class ModConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("chatterhat.json");
	private ModConfig config = new ModConfig();

	public void load() {
		if (!Files.exists(this.configPath)) {
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(this.configPath, StandardCharsets.UTF_8)) {
			ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
			this.config = loaded != null ? loaded : new ModConfig();
		} catch (IOException exception) {
			ChatterHatMod.LOGGER.error("Failed to load config {}", this.configPath, exception);
			this.config = new ModConfig();
		}
	}

	public void save() {
		try {
			Files.createDirectories(this.configPath.getParent());
			try (Writer writer = Files.newBufferedWriter(this.configPath, StandardCharsets.UTF_8)) {
				GSON.toJson(this.config, writer);
			}
		} catch (IOException exception) {
			ChatterHatMod.LOGGER.error("Failed to save config {}", this.configPath, exception);
		}
	}

	public ModConfig get() {
		return this.config;
	}
}
