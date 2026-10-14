package com.example.documentqa;

import java.util.List;

public class AnswerService {
	
	private final SemanticSearchService search;
	private final PromptBuilder promptBuilder;
	private final ChatModel chatModel;
	
	public AnswerService(SemanticSearchService search, PromptBuilder promptBuilder, ChatModel chatModel) {
		this.search = search;
		this.promptBuilder = promptBuilder;
		this.chatModel = chatModel;
	}
	
	public String answer(String question) throws Exception {
			
		List<EmbeddingScore> results = search.search(question, 3, 0.5);
		
		if (results.isEmpty()) {
			return " I couldnt find anything relevant in the document";
		}
		
		String prompt = promptBuilder.build(question, results);
		
		System.out.println("----- PROMPT -----");
		System.out.println(prompt);
		System.out.println("------------------");
		return chatModel.generate(prompt);
	}

}
