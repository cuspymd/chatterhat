# ChatterHat

ChatterHat is a Fabric mod for Minecraft `1.21.11` that adds a talkative companion hat powered by Ollama.

The hat is designed as a wearable companion item rather than a generic chatbot:

- it only becomes active while equipped in the helmet slot
- it responds to player input
- it warns about danger with lightweight local rules
- it keeps short world-specific conversation memory
- it supports Korean, English, and automatic language selection

## Current Status

This repository is in early MVP development.

Implemented today:

- `ChatterHat` item registration
- helmet-slot activation
- Ollama chat integration via `/api/chat`
- `/hat` command-based interaction
- recent conversation memory
- world-specific memory save file
- proactive warnings for:
  - low health
  - low hunger
  - first night
  - nearby hostile mobs

Planned next:

- richer memory facts and summary memory
- more world and exploration events
- in-game config UI
- stronger persona controls
- better debugging tools

## Target Stack

- Minecraft `1.21.11`
- Fabric Loader `0.18.4`
- Fabric API `0.141.3+1.21.11`
- Java `21`
- Gradle `9.3.0` via wrapper
- Yarn mappings `1.21.11+build.4`
- Ollama local API

## Project Documents

Design and planning docs live in [`docs/`](/home/cuspymd/work/chatterhat/docs):

- [수다쟁이-모자-minecraft-mod-개발-계획서.md](/home/cuspymd/work/chatterhat/docs/수다쟁이-모자-minecraft-mod-개발-계획서.md)
- [수다쟁이-모자-fabric-구조설계와-ollama-모델-추천.md](/home/cuspymd/work/chatterhat/docs/수다쟁이-모자-fabric-구조설계와-ollama-모델-추천.md)
- [수다쟁이-모자-다음-단계-구현-계획.md](/home/cuspymd/work/chatterhat/docs/수다쟁이-모자-다음-단계-구현-계획.md)

## Quick Start

### 1. Start Ollama

Install Ollama and start the local service.

Recommended default model for the current MVP:

```bash
ollama pull qwen3:8b
ollama serve
```

If you want a lighter test model, you can switch the config later.

### 2. Build the Mod

```bash
./gradlew build
```

The built jar will be created under:

```text
build/libs/
```

### 3. Run in Development

```bash
./gradlew runClient
```

### 4. Equip the Hat

In a development world, give yourself the item and place it in the helmet slot.

```mcfunction
/give @s chatterhat:chatter_hat
```

## How to Test In Game

1. Start a singleplayer world.
2. Equip `ChatterHat` in the helmet slot.
3. Test Ollama connection:

```mcfunction
/hat test ollama
```

4. Talk to the hat:

```mcfunction
/hat say hello
/hat say 안녕
```

5. Check current status:

```mcfunction
/hat status
```

6. Trigger natural warnings by lowering health, hunger, or waiting for night.

## Commands

### Dialogue

```mcfunction
/hat say <message>
```

Sends a message to the hat. This only works while the hat is equipped.

### Status

```mcfunction
/hat status
```

Shows current activation and memory-related status.

### Ollama Connectivity

```mcfunction
/hat test ollama
```

Sends a short test request to the configured Ollama model.

### Memory

```mcfunction
/hat memory dump
/hat memory clear
```

### Config

```mcfunction
/hat config language <auto|ko|en>
/hat config persona <guide_friendly|chatty_friend|nervous_coach|boastful_wizard>
/hat config model <ollama-model-name>
```

## Configuration

Global config is stored at:

```text
config/chatterhat.json
```

Current main fields:

- `ollamaBaseUrl`
- `ollamaModel`
- `ollamaTimeoutSeconds`
- `chatLanguage`
- `allowMixedLanguage`
- `warningsFollowChatLanguage`
- `personaId`
- `customPersona`
- `responseMaxChars`
- `recentConversationLimit`

Default values are defined in [ModConfig.java](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/config/ModConfig.java).

## World Memory

World-specific memory is stored under the save folder:

```text
<world>/data/chatterhat-memory.json
```

Current memory includes:

- recent messages
- simple facts list
- summary text placeholder
- per-event cooldown data

This is still an early format and may evolve as the memory system gets upgraded.

## Project Structure

Main package:

```text
io.github.cuspymd.chatterhat
```

Important areas:

- [`config/`](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/config)
- [`item/`](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/item)
- [`hat/`](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/hat)
- [`dialogue/`](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/dialogue)
- [`llm/`](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/llm)
- [`memory/`](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/memory)
- [`event/`](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/event)
- [`command/`](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/command)

Useful entry points:

- [ChatterHatMod.java](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/ChatterHatMod.java)
- [HatCommand.java](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/command/HatCommand.java)
- [DialogueCoordinator.java](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/dialogue/DialogueCoordinator.java)
- [OllamaClient.java](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/llm/ollama/OllamaClient.java)
- [PlayerStateDetector.java](/home/cuspymd/work/chatterhat/src/main/java/io/github/cuspymd/chatterhat/event/PlayerStateDetector.java)

## Known Limitations

- singleplayer-first design
- no in-game config screen yet
- no general chat hook yet
- no advanced long-term memory retrieval yet
- no multiplayer support yet
- current item art is placeholder-level

## Development Notes

- Do not perform HTTP requests on the Minecraft main thread.
- Ollama requests are currently executed asynchronously.
- Warning events use local templates for responsiveness.
- Dialogue is command-driven for debugging stability in the current MVP.

## Recommended Next Work

If you continue development from the current state, the best next milestone is:

1. upgrade the memory model from simple storage to structured facts and summary retrieval
2. improve response quality control and repetition suppression
3. add more exploration and lifecycle events
4. add in-game configuration screens

The detailed next-step plan is documented in [수다쟁이-모자-다음-단계-구현-계획.md](/home/cuspymd/work/chatterhat/docs/수다쟁이-모자-다음-단계-구현-계획.md).
