package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

// Tests for InMemoryVectorStore using tiny 2-D vectors with easy scores.
// Query {1,0}:  {1,0} -> 1.0,  {1,1} -> 0.707,  {0,1} -> 0.0
class InMemoryVectorStoreTest {

    private final VectorStore store = new InMemoryVectorStore();   // typed as the interface, like SemanticSearchService
    private final double[] query = {1, 0};

    private TextChunk chunk(int position) {
        return new TextChunk("doc.txt", position, "Chunk " + position);
    }

    // Just the keys, so one assertEquals checks the order
    private List<String> keys(List<EmbeddingScore> results) {
        return results.stream().map(r -> r.getChunk().getKey()).toList();
    }

    @Test
    void returnsBestMatchFirst() {
        // Added worst-first on purpose, so insertion order can't make the test pass by luck
        store.add(chunk(1), new double[] {0, 1});   // 0.0
        store.add(chunk(2), new double[] {1, 1});   // 0.707
        store.add(chunk(3), new double[] {1, 0});   // 1.0

        List<EmbeddingScore> results = store.search(query, 3);

        assertEquals(List.of("doc.txt#3", "doc.txt#2", "doc.txt#1"), keys(results));
        assertEquals(1.0, results.get(0).getScore(), 1e-9);
    }

    @Test
    void returnsAtMostTopK() {
        store.add(chunk(1), new double[] {0, 1});
        store.add(chunk(2), new double[] {1, 1});
        store.add(chunk(3), new double[] {1, 0});

        assertEquals(List.of("doc.txt#3", "doc.txt#2"), keys(store.search(query, 2)));
    }

    @Test
    void topKLargerThanStoreReturnsEverythingWithoutError() {
        store.add(chunk(1), new double[] {1, 0});

        assertEquals(1, store.search(query, 10).size());   // Math.min stops subList going out of bounds
    }

    @Test
    void emptyStoreReturnsEmptyList() {
        assertTrue(store.search(query, 3).isEmpty());
    }

    @Test
    void addingSameKeyReplacesTheOldEntry() {
        store.add(new TextChunk("doc.txt", 1, "Old text"), new double[] {0, 1});
        store.add(new TextChunk("doc.txt", 1, "New text"), new double[] {1, 0});   // same key "doc.txt#1"

        assertEquals(1, store.size());
        EmbeddingScore top = store.search(query, 1).get(0);
        assertEquals("New text", top.getChunk().getContent());
        assertEquals(1.0, top.getScore(), 1e-9);   // the new vector was used too
    }

    @Test
    void tiedScoresKeepInsertionOrder() {
        // Same vector -> same score. LinkedHashMap + stable sort keep the order they were added
        store.add(chunk(7), new double[] {1, 0});
        store.add(chunk(2), new double[] {1, 0});

        assertEquals(List.of("doc.txt#7", "doc.txt#2"), keys(store.search(query, 2)));
    }

    @Test
    void clearRemovesEverything() {
        store.add(chunk(1), new double[] {1, 0});

        store.clear();

        assertEquals(0, store.size());
        assertTrue(store.search(query, 3).isEmpty());
    }
}