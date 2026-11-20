package com.example.documentqa;

import java.util.List;

// The result of asking a question: the answer text plus the chunks it was based on.
// A record: Java generates the constructor, the getters text() and sources(),
// equals() and toString(). Fields are final, so an Answer can't change once created.
public record Answer(String text, List<EmbeddingScore> sources) {

	// Compact constructor: runs before the fields are set.
	// List.copyOf makes an unmodifiable copy, so code that later changes
	// the original list can't change this Answer's sources.
	public Answer {
		sources = List.copyOf(sources);
	}

	// False when nothing scored above min-score, so the LLM was never called
	public boolean hasSources() {
		return !sources.isEmpty();
	}
	
	// Retrieved is not the same as used: these are the chunks whose label
	// appears in the answer text, e.g. "[travel_expenses_policy.txt#5]"
	public List<EmbeddingScore> citedSources() {
		return sources.stream()
				.filter(this::isCited)
				.toList();
	}

	// Chunks the LLM was given but didn't cite
	public List<EmbeddingScore> uncitedSources() {
		return sources.stream()
				.filter(s -> !isCited(s))
				.toList();
	}

	// Look for the label WITH its brackets: "[file#1]" doesn't match "[file#13]",
	// but plain "file#1" would match inside "file#13"
	private boolean isCited(EmbeddingScore source) {
		return text.contains("[" + source.getChunk().getKey() + "]");
	}
}
