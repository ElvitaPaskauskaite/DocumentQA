package com.example.documentqa;

import java.util.List;

public class PromptBuilder {
	// Add instructions for chat model
	private static final String INSTRUCTIONS = "Answer the question in one or two complete sentences, using ONLY the context below.\nIf the answer is not found in the context say \"I don't know based on the documents.\" ";
	
	public String build(String question, List<EmbeddingScore> results) {
		
		// build a string for the chat model to use as a prompt
		StringBuilder sb = new StringBuilder();
		
		sb.append(INSTRUCTIONS);
		sb.append("\n\nContext:\n");
		
		for (EmbeddingScore result : results) {
		    sb.append("[");  
		    sb.append(result.getChunk().getKey());   // the chunk's id
		    sb.append("] ");  
		    sb.append(result.getChunk().getContent());  // the chunk's content
		    sb.append("\n\n");   
		}
		
		sb.append("Question: ").append(question);
		sb.append("\n\nAnswer:");
		
		return sb.toString();
	}

}
