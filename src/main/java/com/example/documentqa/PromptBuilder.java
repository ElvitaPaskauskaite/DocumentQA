package com.example.documentqa;

import java.util.List;

public class PromptBuilder {
	// Add instructions for chat model
	// A text block (""" ... """) keeps the prompt readable over several lines.
	// The model must copy the label of each paragraph it uses, so Java can check
	// which retrieved chunks were actually cited 
	private static final String INSTRUCTIONS = """
			Answer the question in one or two complete sentences, using ONLY the context below.
			Each context paragraph starts with a label in square brackets, such as [example.txt#3].
			After each fact, write the label of the paragraph it came from, in square brackets.
			If the question contains an amount or a date, compare it with the limits in each paragraph and answer from the paragraph whose range includes it.
			If the answer is not found in the context, say "I don't know based on the documents." and do not write any labels.""";
	
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
