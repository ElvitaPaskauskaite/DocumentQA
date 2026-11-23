package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// The same scenarios as InMemoryVectorStoreTest, run against the real SAP HANA Cloud
// instance, so both stores must behave the same. Uses its own 2-D test table so it never
// touches the real chunk vectors. Skipped when the HANA_* variables aren't set (e.g. CI).
// Query {1,0}:  {1,0} -> 1.0,  {1,1} -> 0.707,  {0,1} -> 0.0
class HanaVectorStoreTest {

    private static final String TABLE = "TEST_CHUNK_VECTORS";
    private final double[] query = {1, 0};
    private HanaVectorStore store;

    @BeforeEach
    void connectAndEmpty() throws Exception {
        assumeTrue(HanaSettings.isConfigured(), "HANA_* environment variables not set, skipping");
        store = new HanaVectorStore(HanaSettings.fromEnvironment(), TABLE, 2);
        store.clear();   // each test starts from an empty table
    }

    @AfterEach
    void disconnect() throws Exception {
        if (store != null) {
            store.close();   // closing twice is allowed, so the reconnect test is safe too
        }
    }

    private TextChunk chunk(int position) {
        return new TextChunk("doc.txt", position, "Chunk " + position);
    }

    private List<String> keys(List<EmbeddingScore> results) {
        return results.stream().map(r -> r.getChunk().getKey()).toList();
    }

    @Test
    void returnsBestMatchFirst() {
        store.add(chunk(1), new double[] {0, 1});   // 0.0
        store.add(chunk(2), new double[] {1, 1});   // 0.707
        store.add(chunk(3), new double[] {1, 0});   // 1.0

        List<EmbeddingScore> results = store.search(query, 3);

        assertEquals(List.of("doc.txt#3", "doc.txt#2", "doc.txt#1"), keys(results));
        // HANA stores single-precision floats, so compare to 6 decimal places, not 9
        assertEquals(1.0, results.get(0).getScore(), 1e-6);
        assertEquals("Chunk 3", results.get(0).getChunk().getContent());
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

        assertEquals(1, store.search(query, 10).size());
    }

    @Test
    void emptyStoreReturnsEmptyList() {
        assertTrue(store.search(query, 3).isEmpty());
    }

    @Test
    void addingSameKeyReplacesTheOldEntry() {
        store.add(new TextChunk("doc.txt", 1, "Old text"), new double[] {0, 1});
        store.add(new TextChunk("doc.txt", 1, "New text"), new double[] {1, 0});

        assertEquals(1, store.size());
        EmbeddingScore top = store.search(query, 1).get(0);
        assertEquals("New text", top.getChunk().getContent());
        assertEquals(1.0, top.getScore(), 1e-6);
    }

    @Test
    void tiedScoresKeepInsertionOrder() {
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

    // What the database adds over InMemoryVectorStore: the vectors outlive the connection
    @Test
    void vectorsAreStillThereAfterReconnecting() throws Exception {
        store.add(chunk(1), new double[] {1, 0});
        store.close();

        try (HanaVectorStore reopened = new HanaVectorStore(HanaSettings.fromEnvironment(), TABLE, 2)) {
            assertEquals(1, reopened.size());
            assertEquals("Chunk 1", reopened.search(query, 1).get(0).getChunk().getContent());
        }
    }
}