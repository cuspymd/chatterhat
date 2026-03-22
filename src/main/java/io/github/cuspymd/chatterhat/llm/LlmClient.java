package io.github.cuspymd.chatterhat.llm;

public interface LlmClient {
	String chat(String systemPrompt, String userPrompt) throws Exception;
}
