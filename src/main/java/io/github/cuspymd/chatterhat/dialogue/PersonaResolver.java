package io.github.cuspymd.chatterhat.dialogue;

import io.github.cuspymd.chatterhat.config.ModConfig;
import io.github.cuspymd.chatterhat.config.PersonaPreset;

public final class PersonaResolver {
	private PersonaResolver() {
	}

	public static String resolve(ModConfig config) {
		if (config.customPersona != null && !config.customPersona.isBlank()) {
			return config.customPersona.trim();
		}

		PersonaPreset preset = PersonaPreset.fromId(config.personaId);
		return preset.displayName() + ": " + preset.description();
	}
}
