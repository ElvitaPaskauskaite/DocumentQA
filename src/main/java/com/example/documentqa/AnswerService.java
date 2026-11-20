package com.example.documentqa;

import java.util.List;

public class AnswerService {

	private final SemanticSearchService search;
	private final PromptBuilder promptBuilder;
	private final ChatModel chatModel;

	// Search settings now come from outside instead of being hard-coded
	private final int topK;
	private final double minScore;

	public AnswerService(SemanticSearchService search, PromptBuilder promptBuilder, ChatModel chatModel,
			int topK, double minScore) {
		this.search = search;
		this.promptBuilder = promptBuilder;
		this.chatModel = chatModel;
		this.topK = topK;
		this.minScore = minScore;
	}

	// Returns the answer text AND the chunks it was based on,
	// so callers can show sources without searching a second time
	public Answer answer(String question) throws Exception {

		List<EmbeddingScore> results = search.search(question, topK, minScore);

		// Nothing relevant: don't call the LLM at all, and there are no sources
		if (results.isEmpty()) {
			return new Answer("I couldn't find anything relevant in the documents.", List.of());
		}

		String prompt = promptBuilder.build(question, results);
		String text = chatModel.generate(prompt);

		// Keep the chunks the LLM saw, together with what it said
		return new Answer(text, results);
	}
}
