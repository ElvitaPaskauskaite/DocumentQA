package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Tests for SemanticSearchService with a fake embedder (no Ollama) and the real in-memory store.
// Query vector {1,0}:  hotel {1,0} -> 1.0,  meals {1,1} -> 0.707,  quotes {0,1} -> 0.0
class SemanticSearchServiceTest {

    private final TextChunk hotel  = new TextChunk("travel.txt", 5, "Hotel limit is €215.");
    private final TextChunk meals  = new TextChunk("travel.txt", 11, "Meals are €62 per day.");
    private final TextChunk quotes = new TextChunk("procurement.txt", 4, "Two quotes over €1,000.");
    private final List<TextChunk> chunks = List.of(hotel, meals, quotes);

    private FakeEmbeddingModel model;
    private VectorStore store;
    private SemanticSearchService service;

    @BeforeEach   // runs before EVERY test: each test gets a fresh model, store and service
    void setUp() {
        model = new FakeEmbeddingModel()
                .with("Hotel limit is €215.", 1, 0)
                .with("Meals are €62 per day.", 1, 1)
                .with("Two quotes over €1,000.", 0, 1)
                .with("hotel question", 1, 0)
                .with("off-topic question", 0, 0);   // zero vector: scores 0 against everything
        store = new InMemoryVectorStore();
        service = new SemanticSearchService(model, store);
    }

    private List<String> keys(List<EmbeddingScore> results) {
        return results.stream().map(r -> r.getChunk().getKey()).toList();
    }

    @Test
    void searchBeforeIndexThrows() {
        // Fail loudly instead of returning nothing
        assertThrows(IllegalStateException.class, () -> service.search("hotel question", 3));
    }

    @Test
    void searchReturnsChunksBestFirst() throws Exception {
        service.index(chunks);

        List<EmbeddingScore> results = service.search("hotel question", 3);

        assertEquals(List.of("travel.txt#5", "travel.txt#11", "procurement.txt#4"), keys(results));
    }

    @Test
    void minScoreDropsLowScoringChunks() throws Exception {
        service.index(chunks);

        // 1.0 and 0.707 pass, 0.0 is dropped
        List<EmbeddingScore> results = service.search("hotel question", 3, 0.5);

        assertEquals(List.of("travel.txt#5", "travel.txt#11"), keys(results));
    }

    @Test
    void scoreEqualToMinScoreIsKept() throws Exception {
        service.index(chunks);

        // Boundary: the filter uses >=, so a score of exactly 1.0 passes minScore 1.0
        List<EmbeddingScore> results = service.search("hotel question", 3, 1.0);

        assertEquals(List.of("travel.txt#5"), keys(results));
    }

    @Test
    void offTopicQuestionReturnsNothing() throws Exception {
        // Regression test for DEBUGGING.md entry 1: off-topic questions used to
        // return the top 3 chunks at score 0.000
        service.index(chunks);

        assertTrue(service.search("off-topic question", 3, 0.5).isEmpty());
    }

    @Test
    void indexingTwiceDoesNotDuplicateChunks() throws Exception {
        service.index(chunks);
        service.index(chunks);   // index() clears the store first, so the second call starts over   

        assertEquals(3, store.size());
    }

    @Test
    void eachChunkIsEmbeddedOnceAndTheQueryOnce() throws Exception {
        service.index(chunks);
        service.search("hotel question", 3);

        // 3 chunks + 1 query. With Ollama, every extra call costs about 30 ms
        assertEquals(4, model.getCalls());
    }
    

    // A failed embedding during index() must not wipe out the chunks indexed before it
    @Test
    void failedEmbeddingKeepsThePreviousChunks() throws Exception {
        service.index(chunks);

        // The fake has no vector for this text, so embedding it throws partway through
        TextChunk unknown = new TextChunk("travel.txt", 20, "Text with no fake vector.");

        assertThrows(IllegalArgumentException.class, () -> service.index(List.of(hotel, unknown)));

        assertEquals(3, store.size());
        assertEquals(List.of("travel.txt#5", "travel.txt#11", "procurement.txt#4"),
                keys(service.search("hotel question", 3)));
    }
}
