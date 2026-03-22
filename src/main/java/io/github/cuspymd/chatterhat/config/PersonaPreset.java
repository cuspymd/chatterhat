package io.github.cuspymd.chatterhat.config;

public enum PersonaPreset {
	GUIDE_FRIENDLY(
		"guide_friendly",
		"친절한 초보자 가이드",
		"따뜻하고 믿음직한 생존 가이드다. 실용적인 조언을 짧게 전하며 플레이어를 응원한다."
	),
	CHATTY_FRIEND(
		"chatty_friend",
		"수다 많은 친구",
		"말수가 조금 많지만 부담스럽지 않은 친구다. 상황을 재치 있게 해설하며 친근하게 반응한다."
	),
	NERVOUS_COACH(
		"nervous_coach",
		"겁 많은 생존 코치",
		"위험에 민감하고 조심성이 많다. 안전을 최우선으로 두며 긴장한 말투로 경고한다."
	),
	BOASTFUL_WIZARD(
		"boastful_wizard",
		"허풍쟁이 마법사",
		"스스로를 대단한 현자처럼 소개한다. 과장된 표현을 쓰지만 도움이 되는 팁도 준다."
	);

	private final String id;
	private final String displayName;
	private final String description;

	PersonaPreset(String id, String displayName, String description) {
		this.id = id;
		this.displayName = displayName;
		this.description = description;
	}

	public String id() {
		return this.id;
	}

	public String displayName() {
		return this.displayName;
	}

	public String description() {
		return this.description;
	}

	public static PersonaPreset fromId(String value) {
		for (PersonaPreset preset : values()) {
			if (preset.id.equalsIgnoreCase(value)) {
				return preset;
			}
		}
		return GUIDE_FRIENDLY;
	}
}
