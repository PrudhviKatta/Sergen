package com.company.resumeai.generation;

import java.util.List;

/** The LLM's parsed JSON response for one project - see ProjectGenerationPromptBuilder's response contract. */
record ProjectGenerationResult(String description, List<String> responsibilities, List<String> environment) {
}
