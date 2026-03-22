package io.github.cuspymd.chatterhat.memory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.cuspymd.chatterhat.ChatterHatMod;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public class WorldMemoryRepository {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "chatterhat-memory.json";

	private WorldMemoryData cache;

	public void load(MinecraftServer server) {
		Path path = getFilePath(server);
		if (!Files.exists(path)) {
			this.cache = createDefault(server);
			save(server);
			return;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			WorldMemoryData loaded = GSON.fromJson(reader, WorldMemoryData.class);
			this.cache = loaded != null ? loaded : createDefault(server);
		} catch (IOException exception) {
			ChatterHatMod.LOGGER.error("Failed to load ChatterHat memory {}", path, exception);
			this.cache = createDefault(server);
		}
	}

	public void save(MinecraftServer server) {
		if (server == null) {
			return;
		}

		WorldMemoryData data = getOrCreate(server);
		Path path = getFilePath(server);
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException exception) {
			ChatterHatMod.LOGGER.error("Failed to save ChatterHat memory {}", path, exception);
		}
	}

	public WorldMemoryData getOrCreate(MinecraftServer server) {
		if (this.cache == null) {
			this.cache = createDefault(server);
		}
		return this.cache;
	}

	public void clear(MinecraftServer server) {
		this.cache = createDefault(server);
		save(server);
	}

	private WorldMemoryData createDefault(MinecraftServer server) {
		WorldMemoryData data = new WorldMemoryData();
		data.worldId = server != null ? server.getSaveProperties().getLevelName() : "unknown-world";
		return data;
	}

	private Path getFilePath(MinecraftServer server) {
		return server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve(FILE_NAME);
	}
}
