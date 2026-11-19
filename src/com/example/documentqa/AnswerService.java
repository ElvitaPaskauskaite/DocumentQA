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

	public String answer(String question) throws Exception {

		List<EmbeddingScore> results = search.search(question, topK, minScore);

		// Nothing relevant: don't call the LLM at all
		if (results.isEmpty()) {
			return "I couldn't find anything relevant in the documents.";
		}

		String prompt = promptBuilder.build(question, results);

		return chatModel.generate(prompt);
	}
}
