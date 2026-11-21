package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

// Tests for PromptBuilder: checks the parts other code depends on, not the exact wording.
class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    private final List<EmbeddingScore> results = List.of(
            new EmbeddingScore(new TextChunk("travel.txt", 5, "Hotel limit outside Ireland is €215."), 0.81),
            new EmbeddingScore(new TextChunk("travel.txt", 3, "Book through the travel portal."), 0.71));

    private final String prompt = builder.build("What is the hotel limit?", results);

    @Test
    void everyChunkAppearsWithItsLabel() {
        // Answer.citedSources() only works if the model sees these exact labels
        assertTrue(prompt.contains("[travel.txt#5] Hotel limit outside Ireland is €215."));
        assertTrue(prompt.contains("[travel.txt#3] Book through the travel portal."));
    }

    @Test
    void chunksKeepTheirRankingOrder() {
        // Best match first, as SemanticSearchService returned them
        assertTrue(prompt.indexOf("[travel.txt#5]") < prompt.indexOf("[travel.txt#3]"));
    }

    @Test
    void questionIsIncludedAfterTheContext() {
        // Regression test for DEBUGGING.md entry 2: build() once never appended the question
        assertTrue(prompt.contains("Question: What is the hotel limit?"));
        assertTrue(prompt.indexOf("[travel.txt#3]") < prompt.indexOf("Question:"));
    }

    @Test
    void endsWithAnswerCue() {
        assertTrue(prompt.endsWith("Answer:"));
    }

    @Test
    void containsTheExactRefusalSentence() {
        // The evaluation will look for this exact sentence to count refusals
        assertTrue(prompt.contains("I don't know based on the documents."));
    }
}