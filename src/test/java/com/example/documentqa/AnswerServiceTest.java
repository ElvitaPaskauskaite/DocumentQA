package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Tests for AnswerService: search -> prompt -> LLM -> Answer.
// Embeddings and the LLM are test doubles, so no Ollama is needed.
// Scores for the hotel question {1,0}: hotel = 1.0, meals = 0.707, quotes = 0.0
class AnswerServiceTest {

    private SemanticSearchService search;

    @BeforeEach
    void setUp() throws Exception {
        FakeEmbeddingModel embeddings = new FakeEmbeddingModel()
                .with("Hotel limit is €215.", 1, 0)
                .with("Meals are €62 per day.", 1, 1)
                .with("Two quotes over €1,000.", 0, 1)
                .with("What is the hotel limit?", 1, 0)
                .with("What is the parental leave policy?", 0, 0);   // scores 0 against every chunk

        search = new SemanticSearchService(embeddings, new InMemoryVectorStore());
        search.index(List.of(
                new TextChunk("travel.txt", 5, "Hotel limit is €215."),
                new TextChunk("travel.txt", 11, "Meals are €62 per day."),
                new TextChunk("procurement.txt", 4, "Two quotes over €1,000.")));
    }

    // Builds the service with the given chat model and search settings
    private AnswerService service(ChatModel chat, int topK, double minScore) {
        return new AnswerService(search, new PromptBuilder(), chat, topK, minScore);
    }

    private List<String> keys(List<EmbeddingScore> results) {
        return results.stream().map(r -> r.getChunk().getKey()).toList();
    }

    @Test
    void nothingRelevantMeansTheLlmIsNotCalled() throws Exception {
        FakeChatModel chat = new FakeChatModel("should not be used");

        Answer answer = service(chat, 3, 0.5).answer("What is the parental leave policy?");

        // Without context the LLM could only guess, so it is skipped entirely
        assertEquals(0, chat.getCalls());
        assertNull(chat.getLastPrompt());
        assertEquals("I couldn't find anything relevant in the documents.", answer.text());
        assertFalse(answer.hasSources());
    }

    @Test
    void relevantChunksAreSentToTheLlmAndReturnedAsSources() throws Exception {
        FakeChatModel chat = new FakeChatModel("The limit is €215 [travel.txt#5].");

        Answer answer = service(chat, 3, 0.5).answer("What is the hotel limit?");

        assertEquals(1, chat.getCalls());
        assertEquals("The limit is €215 [travel.txt#5].", answer.text());
        assertEquals(List.of("travel.txt#5", "travel.txt#11"), keys(answer.sources()));   // quotes is below 0.5
    }

    @Test
    void promptContainsTheQuestionAndOnlyTheRetrievedChunks() throws Exception {
        FakeChatModel chat = new FakeChatModel("Some answer.");

        service(chat, 3, 0.5).answer("What is the hotel limit?");

        String prompt = chat.getLastPrompt();
        assertTrue(prompt.contains("What is the hotel limit?"));
        assertTrue(prompt.contains("[travel.txt#5] Hotel limit is €215."));
        assertFalse(prompt.contains("procurement.txt#4"));   // filtered out by min score, so never sent
    }

    @Test
    void topKLimitsTheNumberOfSources() throws Exception {
        FakeChatModel chat = new FakeChatModel("Some answer.");

        Answer answer = service(chat, 1, 0.5).answer("What is the hotel limit?");

        assertEquals(List.of("travel.txt#5"), keys(answer.sources()));
    }

    @Test
    void citationsInTheReplyAreMatchedToSources() throws Exception {
        // End-to-end check of the citation logic: the reply cites #5 but not #11
        FakeChatModel chat = new FakeChatModel("The limit is €215 [travel.txt#5].");

        Answer answer = service(chat, 3, 0.5).answer("What is the hotel limit?");

        assertEquals(List.of("travel.txt#5"), keys(answer.citedSources()));
        assertEquals(List.of("travel.txt#11"), keys(answer.uncitedSources()));
    }
}