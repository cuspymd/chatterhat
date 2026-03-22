---
title: 수다쟁이 모자 Fabric 구조 설계와 Ollama 모델 추천
tags:
  - minecraft
  - fabric
  - modding
  - ollama
  - llm
  - architecture
  - model-selection
created: 2026-03-19
updated: 2026-03-19
---

# 수다쟁이 모자 Fabric 구조 설계와 Ollama 모델 추천

이 문서는 [[수다쟁이-모자-minecraft-mod-개발-계획서]]를 바탕으로,
실제 구현에 바로 들어갈 수 있도록 **패키지 구조 / 클래스 설계 / 데이터 포맷 / 이벤트 흐름 / Ollama 모델 선택 기준**까지 구체화한 문서다.

---

## 1. 문서 목적

이 문서의 목표는 다음 3가지다.

1. Fabric 모드 구현을 위한 **현실적인 코드 구조**를 제안한다.
2. 싱글플레이 + Ollama 환경에서 **과하지 않은 MVP 설계선**을 고정한다.
3. 한국어/영어 대화, 페르소나, 능동 발화, 메모리까지 감안했을 때 **적합한 Ollama 모델 후보**를 추천한다.

---

## 2. 전체 구현 방향 요약

### 확정 전제
- 최신 Minecraft + Fabric
- 싱글플레이 우선
- 로컬 Ollama 호출
- 모자는 헬멧 슬롯 장착 아이템
- 대화 언어 설정 지원 (`ko`, `en`, `auto`)
- 능동 발화는 **규칙 기반 + 선택적 LLM 호출** 혼합
- 메모리는 **최근 대화 + 중요 facts + 요약** 3층 구조

### 설계 철학
이 모드는 "LLM을 붙인 아이템"이 아니라,
**게임 상태를 감지하고, 캐릭터성을 유지하며, 과도하지 않게 개입하는 동료 시스템**으로 설계해야 한다.

즉, 핵심은:
- LLM 호출 빈도 제어
- 상황 판단 로직 분리
- 메모리 압축
- 언어 일관성 유지
- 디버깅 쉬운 구조

---

## 3. 권장 패키지 구조

패키지명 예시:
`io.github.cuspymd.chatterhat`

### 최상위 패키지 구조

```text
io.github.cuspymd.chatterhat
├─ ChatterHatMod.java
├─ config/
├─ item/
├─ hat/
├─ event/
├─ dialogue/
├─ llm/
├─ memory/
├─ ui/
├─ client/
├─ command/
├─ data/
├─ util/
└─ debug/
```

---

## 4. 패키지별 역할

## 4.1 `config/`
설정 로드/저장과 기본값 관리.

### 주요 클래스 제안
- `ModConfig`
  - 전체 설정 루트
- `ChatConfig`
  - 응답 길이, 수다 빈도, 능동성, 전투 중 제한
- `OllamaConfig`
  - baseUrl, modelName, timeout, keepAlive, context 제한
- `LanguageConfig`
  - chatLanguage, systemLanguageMode, allowMixedLanguage
- `PersonaConfig`
  - 현재 persona id, custom persona text
- `MemoryConfig`
  - 최근 메시지 수, fact 개수 제한, summary 길이

### 설계 포인트
- config는 **전역 기본값**
- world save는 **월드별 기억 상태**
- 즉, 설정과 기억을 섞지 않는다

---

## 4.2 `item/`
실제 아이템 정의.

### 주요 클래스 제안
- `ChatterHatItem`
  - 모자 아이템 본체
- `ModItems`
  - 아이템 등록

### 역할
- 장비 가능 아이템 정의
- 툴팁/희귀도/아이콘 설정
- 장비 상태 확인을 위한 진입점 제공

---

## 4.3 `hat/`
"모자 시스템"의 런타임 상태 관리.

### 주요 클래스 제안
- `HatState`
  - 현재 장착 여부, 침묵 상태, 최근 발화 시각, 활성 persona 등
- `HatManager`
  - 플레이어 기준 모자 활성 상태 조회
- `HatSpeechController`
  - 실제 발화 요청 수락/거절, 쿨다운 체크
- `HatOutputChannel`
  - chat / actionbar / toast 등 출력 방법 추상화

### 역할
- "지금 모자가 켜져 있는가"
- "지금 말해도 되는가"
- "어디로 말할 것인가"

이 레이어가 있으면 이벤트 시스템과 대화 엔진이 직접 얽히지 않는다.

---

## 4.4 `event/`
게임 이벤트 감지 및 정규화.

### 하위 구조 예시

```text
event/
├─ detector/
├─ model/
├─ rule/
└─ scheduler/
```

### 주요 클래스 제안
- `PlayerStateDetector`
  - 체력, 허기, 장비 상태, 위치, 이동 패턴 감지
- `ThreatDetector`
  - 근처 몹, 낙사 위험, 폭발 위험, 저체력 위험
- `ExplorationDetector`
  - 새 바이옴, 구조물, 광물 발견 등
- `LifecycleDetector`
  - 첫 스폰, 사망, 부활, 첫 밤
- `HatEvent`
  - 정규화된 내부 이벤트 모델
- `HatEventType`
  - `LOW_HEALTH`, `FIRST_NIGHT`, `MONSTER_NEARBY` 등
- `EventPriority`
  - LOW / NORMAL / HIGH / CRITICAL
- `EventRuleEngine`
  - 이벤트 dedupe, debounce, 중요도 보정
- `EventScheduler`
  - 이벤트를 주기적으로/지연해서 처리

### 핵심 포인트
Minecraft 이벤트는 noisy 하다.
따라서 detector는 감지하고,
rule engine은 **정말 말할 가치가 있는 이벤트로 정제**해야 한다.

---

## 4.5 `dialogue/`
대화 컨텍스트 조립과 프롬프트 생성 핵심 레이어.

### 주요 클래스 제안
- `DialogueCoordinator`
  - 전체 대화 요청 파이프라인 오케스트레이션
- `PromptBuilder`
  - 프롬프트 생성
- `ContextAssembler`
  - 현재 상황 + 최근 대화 + 메모리 + 설정 조합
- `ResponsePostProcessor`
  - 길이 제한, 반복 제거, 위험 시 축약
- `LanguagePolicy`
  - ko/en/auto 규칙 처리
- `PersonaResolver`
  - 프리셋/커스텀 persona 적용
- `DialogueRequest`
- `DialogueResponse`

### 구분해야 할 대화 타입
- `PLAYER_CHAT`
- `PROACTIVE_COMMENT`
- `DANGER_WARNING`
- `GUIDE_HINT`
- `DEATH_REACTION`
- `IDLE_BANTER`

이 타입 분리는 매우 중요하다.
같은 모델이라도 위험 경고와 잡담은 프롬프트가 달라야 한다.

---

## 4.6 `llm/`
Ollama 연동 전담.

### 하위 구조 예시

```text
llm/
├─ ollama/
├─ model/
└─ transport/
```

### 주요 클래스 제안
- `LlmClient`
  - 추상 인터페이스
- `OllamaClient`
  - 실제 구현체
- `OllamaChatRequest`
- `OllamaChatResponse`
- `OllamaModelInfo`
- `OllamaHealthChecker`
- `LlmRequestExecutor`
  - 비동기 실행기
- `LlmRateLimiter`
  - 중복 요청 억제
- `LlmFallbackPolicy`
  - 실패 시 로컬 템플릿 fallback

### API 선택
가능하면 초기에 `/api/chat` 중심이 더 자연스럽다.
이유:
- role 기반 대화 구조가 명확함
- persona/system/user 구분이 쉬움
- 추후 provider 확장 시도 덜 아픔

### 비동기 원칙
- 게임 메인 스레드에서 직접 HTTP 호출 금지
- 응답 수신 후 메인 스레드 안전 구간에서 출력 반영
- timeout, cancel, duplicate suppression 필수

---

## 4.7 `memory/`
기억 시스템.

### 주요 클래스 제안
- `MemoryStore`
  - 전체 메모리 루트
- `RecentConversationBuffer`
  - 최근 대화 ring buffer
- `FactMemoryStore`
  - 중요 사실 저장
- `SummaryMemoryStore`
  - 요약 메모리 저장
- `MemoryExtractor`
  - fact 후보 추출
- `MemorySummarizer`
  - 요약 생성
- `MemoryRetrievalService`
  - 프롬프트에 넣을 메모리 선택
- `WorldMemoryRepository`
  - 월드 저장/로드

### 메모리 계층

#### 최근 대화
- 최근 10~20 턴
- 플레이어 발화 / 모자 발화 / 이벤트 간단 기록

#### facts
- 구조화 저장
- 중복 병합 가능
- importance / lastUpdated / confidence 포함

#### summary
- 오래된 대화를 압축
- 현재 관계성/플레이 스타일/장기 목표만 남김

---

## 4.8 `ui/`
게임 내 설정 및 정보 노출.

### 주요 클래스 제안
- `HatConfigScreen`
- `ModelSettingsScreen`
- `PersonaEditorScreen`
- `MemoryInspectorScreen`
- `ConnectionTestWidget`
- `LanguageSelectorWidget`

### MVP에서 꼭 필요한 화면
1. 메인 설정 화면
2. Ollama 연결 테스트 화면
3. Persona 선택/편집 화면
4. Memory 요약 보기/초기화 화면

---

## 4.9 `command/`
디버깅과 초기 입력 실험.

### 주요 명령 제안
- `/hat say <message>`
- `/hat status`
- `/hat debug event <type>`
- `/hat memory dump`
- `/hat memory clear`
- `/hat test ollama`

### 이유
초기엔 일반 채팅 후킹보다 명령이 디버깅에 훨씬 유리하다.

---

## 4.10 `debug/`
로그/트레이싱.

### 주요 클래스 제안
- `DebugFlags`
- `PromptLogFormatter`
- `EventTraceLogger`
- `LatencyMetrics`

이 모드는 디버깅이 중요하다.
왜 이 타이밍에 말했는지, 왜 침묵했는지, 왜 한국어 대신 영어가 나왔는지 추적 가능해야 한다.

---

## 5. 핵심 클래스 관계도(개념)

```text
Player Input / Game Event
        ↓
EventDetector / Command
        ↓
HatSpeechController
        ↓
DialogueCoordinator
   ├─ ContextAssembler
   ├─ PersonaResolver
   ├─ LanguagePolicy
   ├─ MemoryRetrievalService
   └─ PromptBuilder
        ↓
OllamaClient (async)
        ↓
ResponsePostProcessor
        ↓
HatOutputChannel
        ↓
MemoryStore update
```

---

## 6. 데이터 저장 설계

## 6.1 저장 위치 전략

### A. 전역 config
저장 대상:
- Ollama URL
- 기본 모델
- 기본 응답 길이
- 기본 언어
- 기본 수다 빈도

### B. 월드 저장 데이터
저장 대상:
- 월드별 기억
- 월드별 현재 persona override
- 최근 이벤트 상태
- 최근 대화
- fact memory
- summary memory

### 권장 이유
- 설정은 모든 월드에 재사용
- 기억은 월드별 서사 유지

---

## 6.2 JSON 예시

```json
{
  "schemaVersion": 1,
  "worldId": "new-world-01",
  "personaId": "guide_friendly",
  "chatLanguage": "ko",
  "lastSpokenAt": 0,
  "silenceMode": false,
  "recentMessages": [
    {
      "role": "player",
      "text": "오늘 철 찾으러 가자",
      "ts": 1742310000,
      "language": "ko"
    },
    {
      "role": "hat",
      "text": "좋아, 너무 깊게 내려가기 전에 횃불부터 챙기자.",
      "ts": 1742310002,
      "language": "ko"
    }
  ],
  "facts": [
    {
      "id": "goal_001",
      "type": "goal",
      "value": "철과 다이아 확보",
      "importance": 0.85,
      "confidence": 0.91,
      "lastUpdated": 1742310002
    }
  ],
  "summary": {
    "text": "플레이어는 초반 생존 중이며 광물 탐사를 선호한다. 위험 경고와 실용적인 조언에 잘 반응한다.",
    "language": "ko",
    "updatedAt": 1742310100
  },
  "cooldowns": {
    "LOW_HEALTH": 1742310200,
    "MONSTER_NEARBY": 1742310210
  }
}
```

---

## 7. 대화 요청 흐름 설계

## 7.1 플레이어가 말을 건 경우

### 흐름
1. 플레이어 입력 수집
2. 모자 장착 여부 확인
3. 현재 대화 가능 상태 확인
4. 최근 대화/기억/상황 조합
5. 언어 설정 적용
6. Ollama 호출
7. 응답 후처리
8. 채팅 출력
9. 메모리 업데이트

### 포인트
- 플레이어 대화는 가장 우선순위 높은 LLM 호출 경로
- 이 경로는 가능한 가장 안정적으로 만들어야 함

---

## 7.2 능동 이벤트가 발생한 경우

### 흐름
1. detector가 raw event 감지
2. rule engine이 중요도/중복/쿨다운 평가
3. 긴급 여부 판단
4. 긴급하면 로컬 템플릿 즉시 출력
5. 긴급하지 않지만 캐릭터성 있는 반응이 필요하면 LLM 호출
6. 출력 후 event memory 갱신

### 포인트
- 위험 경고는 속도가 우선
- 잡담/서사 코멘트는 분위기가 우선

---

## 8. 프롬프트 설계 템플릿

## 8.1 시스템 프롬프트 골격

```text
너는 Minecraft 세계에서 플레이어가 착용한 "수다쟁이 모자"다.
플레이어를 돕는 동료처럼 행동하되, 시스템이나 AI 정체성을 꺼내지 마라.
반드시 설정된 대화 언어로만 말하라.
위험 상황에서는 짧고 즉각적으로 말하라.
반복하지 마라.
장황한 설명보다 게임에 맞는 한두 문장 조언을 우선하라.
현재 페르소나: {persona}
현재 대화 언어: {languageRule}
```

## 8.2 유저 입력형 프롬프트 컨텍스트
- 현재 월드 상황 요약
- 최근 대화 5~10개
- 중요 facts 3~8개
- 요약 메모리 1개
- 현재 사용자 입력

## 8.3 능동 발화형 프롬프트 컨텍스트
- 현재 이벤트 종류
- 현재 위험도
- 최근 같은 이벤트 발생 여부
- 짧은 메모리 요약
- 출력 길이 제한(예: 1문장)

---

## 9. 언어 설정 상세 설계

## 9.1 추천 정책

### `ko`
- 시스템 프롬프트에서 한국어 고정
- 로컬 경고 템플릿도 한국어
- 메모리 summary도 한국어 생성

### `en`
- 시스템 프롬프트에서 영어 고정
- 로컬 경고 템플릿도 영어
- 메모리 summary도 영어 생성

### `auto`
우선순위 추천:
1. 최근 플레이어 입력 언어
2. 게임 locale
3. 이전 모자 응답 언어
4. 기본 fallback = 영어 또는 한국어 중 config 기본값

## 9.2 내부 저장 전략
- facts는 언어 중립 구조 데이터
- recentMessages는 원문 저장
- summary는 현재 언어 기준으로 저장
- 필요 시 언어 전환 직후 summary 재생성 가능

## 9.3 구현 팁
언어 강제는 단순히 "한국어로 대답해" 한 줄보다,
- allowed language
- forbidden behavior
- output style
를 함께 넣는 게 더 낫다.

예:
```text
출력은 반드시 한국어로만 작성하라.
영어 문장이나 영어 메타 표현을 섞지 마라.
아이템 이름/고유명사 외에는 한국어를 유지하라.
```

---

## 10. 능동 발화 제어 설계

이 모드에서 UX를 좌우하는 핵심.

## 10.1 쿨다운 체계
이벤트마다 개별 쿨다운 필요.

예:
- `LOW_HEALTH`: 20~40초
- `MONSTER_NEARBY`: 15~30초
- `FIRST_NIGHT`: 월드당 1회
- `NEW_BIOME`: biome별 1회 또는 긴 쿨다운
- `IDLE_BANTER`: 3~10분

## 10.2 발화 우선순위
- CRITICAL: 즉시 출력, 로컬 템플릿 우선
- HIGH: 짧은 발화, 필요 시 LLM
- NORMAL: 상황 따라 LLM
- LOW: 침묵 가능

## 10.3 침묵 조건
- 방금 말함
- 전투 중인데 중요도 낮음
- 같은 이벤트 반복
- 플레이어가 침묵 모드 설정
- Ollama 응답 대기 중

---

## 11. MVP용 실제 클래스 목록

여기서부터는 "이번 주말에 바로 만들기" 수준으로 줄인 목록이다.

### 필수 클래스
- `ChatterHatMod`
- `ModItems`
- `ChatterHatItem`
- `ModConfig`
- `HatManager`
- `HatSpeechController`
- `DialogueCoordinator`
- `PromptBuilder`
- `OllamaClient`
- `LlmRequestExecutor`
- `RecentConversationBuffer`
- `WorldMemoryRepository`
- `PlayerStateDetector`
- `ThreatDetector`
- `HatConfigScreen`
- `/hat say` command handler

### 2차 클래스
- `FactMemoryStore`
- `MemoryExtractor`
- `SummaryMemoryStore`
- `ExplorationDetector`
- `LanguagePolicy`
- `ResponsePostProcessor`

---

## 12. 개발 순서 제안

## Step 1. 대화 루프만 먼저 완성
- `/hat say 안녕`
- Ollama 응답 받기
- 채팅 출력
- 최근 대화 저장

## Step 2. 모자 장착과 연결
- 모자를 착용했을 때만 `/hat say` 동작
- 장착/해제 상태 표시

## Step 3. 언어 설정 추가
- ko/en/auto
- 한국어/영어 강제 프롬프트
- 로컬 경고 템플릿 2언어 준비

## Step 4. 첫 번째 능동 이벤트 추가
- 저체력
- 몹 접근
- 첫 밤

## Step 5. fact memory 추가
- 목표
- 선호
- 중요한 이벤트

## Step 6. UI 추가
- Ollama 연결 테스트
- model 입력
- persona 선택
- 언어 선택
- 수다 빈도

---

## 13. Ollama 모델 조사 기준

모델 추천은 아래 기준으로 판단해야 한다.

### 이 프로젝트에 중요한 기준
1. **한국어/영어 대화 품질**
2. **roleplay / 캐릭터 유지력**
3. **instruction following**
4. **짧은 응답에서도 자연스러운 말투**
5. **로컬 실행 가능성**
6. **지연 시간**
7. **장기 컨텍스트 처리**

### 덜 중요한 기준
- 복잡한 수학/코딩 성능
- tool use 최적화
- 비전 입력

즉, 이 프로젝트는 순수 reasoning benchmark보다,
**멀티턴 대화, 말투 유지, 다국어 안정성**이 더 중요하다.

---

## 14. 조사 요약

공식/공개 설명 기준으로 볼 때, 이번 프로젝트에서 특히 눈에 띄는 후보는 다음 계열이다.

### 14.1 Qwen 계열
#### Qwen2.5
Ollama 라이브러리 설명상:
- 128K 컨텍스트
- 29개 이상 언어 지원
- instruction following, long-text generation, structured output, role-play 개선 명시
- 한국어 지원 명시

즉, **안정적인 기본 추천 후보**다.

#### Qwen3
Ollama 라이브러리 설명상:
- creative writing, role-playing, multi-turn dialogues, instruction following에서 강점 명시
- 100+ 언어/방언 지원
- small dense / larger variants / MoE variants 폭넓음

즉, **현재 시점 기준 가장 유망한 대화형 후보**로 보인다.
특히 이 모드처럼 페르소나와 다국어, 멀티턴이 중요한 경우 잘 맞는다.

---

### 14.2 Gemma 3 계열
Ollama 라이브러리 설명상:
- 128K 컨텍스트(4B 이상)
- 140개 이상 언어 지원
- 비교적 경량
- 작은 장치에서도 돌리기 쉬운 포지션

장점:
- 가벼운 하드웨어에도 유리
- 범용 챗/요약에 무난

주의:
- roleplay 몰입감이나 캐릭터 유지 면에서는 Qwen 계열보다 항상 우세하다고 보긴 어려움
- 아주 작은 변형은 캐릭터 일관성이 흔들릴 수 있음

---

### 14.3 Llama 3.1 계열
Ollama 설명상:
- multilingual
- 128K 컨텍스트
- steerability 강점

장점:
- 생태계 성숙
- 전반적으로 안정적

주의:
- 한국어/영어 혼합 설정, roleplay, 경쾌한 캐릭터성 측면에서는 Qwen 계열이 더 매력적일 수 있음
- 8B도 무난하지만, 이 프로젝트 특화 추천 1순위까진 아님

---

### 14.4 Mistral Small 계열
Ollama 설명상:
- 24B
- 다국어 지원(한국어 포함 명시)
- 빠른 conversational agent에 적합
- system prompt adherence 강점

장점:
- 시스템 프롬프트 잘 따르는 편이라는 포지션
- 채팅형 에이전트에 적합

주의:
- 24B라서 경량 하드웨어에겐 다소 무거울 수 있음
- 싱글플레이 사이드 프로젝트의 기본값으론 다소 부담될 수 있음

---

## 15. 최종 추천 모델 랭킹

여기서는 **수다쟁이 모자 모드**라는 목적에 맞춰 추천한다.

## 15.1 1순위: `qwen3:8b`
### 추천 이유
- role-playing, creative writing, multi-turn dialogue 강점이 공식 설명에 직접 드러남
- 다국어 강점이 큼
- 한국어/영어 설정 대응에 유리
- 8B는 현실적인 로컬 실행선
- instruction following이 좋아서 persona + 언어 강제 + 짧은 응답 제약을 비교적 잘 따를 가능성이 높음

### 이 모드에 특히 잘 맞는 점
- 잡담형 캐릭터성을 살리기 좋음
- 짧은 멘트와 분위기 있는 말 모두 가능성이 높음
- 페르소나 바뀌는 실험에 적합

### 우려점
- reasoning/thinking 모드 성격이 섞이면 응답이 과해질 수 있으니 프롬프트를 짧고 단단하게 잡아야 함
- 8B라도 로컬 환경에 따라 응답 지연 체감 가능

### 추천 포지션
**기본 추천 모델**

---

## 15.2 2순위: `qwen2.5:7b` 또는 `qwen2.5:14b`
### 추천 이유
- 이미 검증된 계열로 보수적으로 안정적
- role-play와 시스템 프롬프트 강점이 공식 설명에 포함
- 한국어 지원 명시
- 7B와 14B 사이에서 하드웨어 맞춤 선택 가능

### 어떻게 고를까
- **응답 속도 / 가벼움 우선** -> `qwen2.5:7b`
- **캐릭터 안정성 / 문장 질 우선** -> `qwen2.5:14b`

### 추천 포지션
**안정형 대안**

---

## 15.3 3순위: `gemma3:4b` 또는 `gemma3:12b`
### 추천 이유
- 가벼운 환경에서 시작하기 좋음
- 다국어 폭이 넓음
- 4B 이상은 128K 컨텍스트

### 어떻게 고를까
- **테스트/저사양 MVP** -> `gemma3:4b`
- **좀 더 자연스러운 응답** -> `gemma3:12b`

### 주의
- roleplay 몰입감은 Qwen보다 덜 인상적일 수 있음
- 아주 개성 강한 모자 캐릭터에는 살짝 밋밋할 수 있음

### 추천 포지션
**경량/입문형 대안**

---

## 15.4 4순위: `mistral-small:24b`
### 추천 이유
- system prompt adherence가 강점
- 다국어와 빠른 conversational agent 포지션
- 정교한 페르소나 제어에 잠재력 있음

### 주의
- 24B는 기본 추천으로는 무겁다
- 대부분의 개인 환경에서 초기 개발 피드백 루프가 느려질 수 있음

### 추천 포지션
**고성능 PC용 상위 옵션**

---

## 15.5 보조 후보: `llama3.1:8b`
### 추천 이유
- 생태계가 익숙하고 안정적
- 전반적으로 무난

### 주의
- 이 프로젝트의 핵심인 다국어 roleplay 관점에선 Qwen 쪽이 더 매력적

### 추천 포지션
**무난한 fallback**

---

## 16. 하드웨어 기준 추천 조합

정확한 VRAM/RAM은 양자화와 환경에 따라 달라지므로 여기서는 대략적인 실전 기준으로 본다.

## 16.1 가벼운 환경
예:
- 노트북
- 내장 GPU
- 적은 메모리

### 추천
- `gemma3:4b`
- `qwen3:4b`
- `qwen2.5:3b` 또는 `7b`

### 목적
- 먼저 컨셉 검증
- UI/이벤트 흐름 개발
- 한국어/영어 전환 테스트

---

## 16.2 현실적인 개인 개발 환경
예:
- 16GB~32GB RAM
- 중급 이상 GPU 또는 꽤 괜찮은 CPU

### 추천
- `qwen3:8b`
- `qwen2.5:7b`
- `gemma3:12b` (속도 괜찮으면)

### 목적
- 실제 플레이 테스트
- 페르소나 튜닝
- 메모리/능동 발화 품질 평가

---

## 16.3 여유 있는 고성능 환경
예:
- 24GB급 VRAM
- 32GB 이상 RAM
- 로컬 추론에 익숙한 환경

### 추천
- `qwen2.5:14b`
- `mistral-small:24b`
- `qwen3:14b` 또는 상위급

### 목적
- 더 정교한 캐릭터성
- 더 안정적인 멀티턴 대화
- 요약 메모리 품질 향상

---

## 17. 프로젝트용 모델 추천 결론

### 가장 추천하는 시작점
**`qwen3:8b`**

이유:
- 이 프로젝트는 "게임 안에서 떠드는 캐릭터"가 핵심이라,
  단순 질의응답보다 **roleplay / multi-turn / 말투 제어 / 언어 전환**이 중요하다.
- Qwen3는 공식 소개에서 이 강점을 직접 강조하고 있다.
- 8B는 개인 개발자 기준으로 성능과 품질의 균형이 꽤 괜찮은 편이다.

### 가장 안전한 대안
**`qwen2.5:7b`**

이유:
- 비교적 보수적이고 검증된 선택지 느낌
- 한국어/영어 모두 무난할 가능성이 높음
- 프롬프트 튜닝이 단순해도 결과가 크게 망가질 확률이 낮음

### 저사양 테스트용
**`gemma3:4b`**

이유:
- 개발 초기 반복 테스트에 유리
- UI/이벤트/메모리 흐름 먼저 붙이기에 가벼움

### 가장 정교한 상위 옵션
**`qwen2.5:14b` 또는 `mistral-small:24b`**

이유:
- 캐릭터 일관성과 응답 질을 더 보고 싶을 때 고려할 수 있음
- 다만 초기 MVP 기본값으로는 무거울 수 있음

---

## 18. 권장 모델 운영 전략

처음부터 모델 하나에 올인하지 말고,
**개발 단계별로 모델을 바꾸는 전략**이 좋다.

### 개발 초반
- `gemma3:4b` 또는 `qwen2.5:7b`
- 이유: 빠른 반복 개발

### 컨셉 검증 / 플레이 테스트
- `qwen3:8b`
- 이유: 캐릭터성 평가

### 품질 비교 테스트
- `qwen2.5:14b`
- `mistral-small:24b`
- 이유: 상위 품질 확인

---

## 19. 모델 평가 체크리스트

실제로 2~4개 모델을 같은 프롬프트로 비교해보는 게 좋다.

### 체크 포인트
1. 한국어 응답 자연스러운가?
2. 영어 응답 자연스러운가?
3. ko/en/auto 지시를 잘 따르는가?
4. 같은 persona를 10턴 이상 유지하는가?
5. 위험 상황에서 짧게 말하는가?
6. 지나치게 메타 발언을 하는가?
7. 반복 문장이 많은가?
8. 모자처럼 느껴지는가, 그냥 챗봇처럼 느껴지는가?

### 추천 테스트 시나리오
- 초보자 가이드형 페르소나
- 겁많은 생존 코치형 페르소나
- 수다친구형 페르소나
- 저체력 위험 경고
- 첫 밤 멘트
- 플레이어가 한국어/영어를 섞어 말할 때 auto 처리

---

## 20. 구현 직전 의사결정 요약

### 지금 고정해도 좋은 것
- 기본 패키지 구조는 위 설계 사용
- 초기 입력은 `/hat say` 우선
- 저장은 전역 config + 월드 메모리 분리
- 능동 발화는 규칙 기반 우선
- 메모리는 recent + facts + summary
- 기본 추천 모델은 `qwen3:8b`
- fallback 추천은 `qwen2.5:7b`

### 아직 열어둘 것
- 일반 채팅 후킹 여부
- persona 프리셋 개수
- memory summary 생성 주기
- 상위 모델을 기본값으로 쓸지 여부

---

## 21. 다음 문서 후보

이 다음 단계로 작성하면 좋은 문서는 아래다.

1. **MVP 클래스 스켈레톤 문서**
   - 각 클래스의 필드와 메서드 시그니처 초안
2. **프롬프트 설계 문서**
   - persona / language / warning / memory prompt 템플릿
3. **테스트 시나리오 문서**
   - 이벤트 케이스와 기대 출력 정리
4. **Fabric 개발 착수 체크리스트**
   - Gradle, Yarn, 등록, client init, screen, command까지

---

## 22. 최종 메모

이 프로젝트의 재미는 모델 IQ보다,
**짧고 적절한 순간에, 캐릭터성 있는 한마디를 해주는 것**에서 나온다.

그래서 모델 선택도 "벤치마크 최고점"보다,
다음 질문으로 판단하는 게 맞다.

- 이 모델이 한국어/영어를 안정적으로 유지하는가?
- 이 모델이 모자처럼 떠들 수 있는가?
- 이 모델이 짧고 게임 친화적인 응답을 잘 만드는가?
- 이 모델이 로컬에서 반복 테스트 가능한 속도를 내는가?

현재 판단으로는,
**첫 실전 후보는 `qwen3:8b`, 가장 안정적인 대안은 `qwen2.5:7b`, 저사양 개발용은 `gemma3:4b`** 가 가장 균형이 좋다.
