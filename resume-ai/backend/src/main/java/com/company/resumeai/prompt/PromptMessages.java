package com.company.resumeai.prompt;

/** A built system/user message pair, ready to hand to an LlmClient. */
public record PromptMessages(String system, String user) {
}
