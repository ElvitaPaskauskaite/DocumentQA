package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Tests for CachingEmbeddingModel. FakeEmbeddingModel plays the role of Ollama, and its
// call counter shows whether the cache answered or the underlying model was used.
// @TempDir gives each test an empty folder that JUnit deletes afterwards,
// so the real cache/embeddings.json is never touched.
class CachingEmbeddingModelTest {

    @TempDir
    Path tempDir;

    private Path cacheFile() {
        return tempDir.resolve("embeddings.json");
    }

    @Test
    void firstCallIsAMissAndSecondCallIsAHit() throws Exception {
        FakeEmbeddingModel inner = new FakeEmbeddingModel().with("hello", 0.1, 0.2);
        CachingEmbeddingModel cache = new CachingEmbeddingModel(inner, "model-a", cacheFile());

        double[] first = cache.embed("hello");
        double[] second = cache.embed("hello");

        assertArrayEquals(first, second);
        assertEquals(1, cache.getMisses());
        assertEquals(1, cache.getHits());
        assertEquals(1, inner.getCalls());   // the underlying model was only called once
    }

    @Test
    void differentTextIsAMiss() throws Exception {
        FakeEmbeddingModel inner = new FakeEmbeddingModel()
                .with("hello", 0.1, 0.2)
                .with("goodbye", 0.3, 0.4);
        CachingEmbeddingModel cache = new CachingEmbeddingModel(inner, "model-a", cacheFile());

        cache.embed("hello");
        cache.embed("goodbye");

        assertEquals(2, cache.getMisses());
        assertEquals(0, cache.getHits());
    }

    @Test
    void savedVectorsAreReloadedFromTheFile() throws Exception {
        FakeEmbeddingModel firstModel = new FakeEmbeddingModel().with("hello", 0.1, 0.2);
        CachingEmbeddingModel firstRun = new CachingEmbeddingModel(firstModel, "model-a", cacheFile());
        firstRun.embed("hello");
        firstRun.save();

        // Simulates restarting the program. This model has no vectors configured,
        // so it would throw if the cache called it. The vector must come from the file.
        FakeEmbeddingModel secondModel = new FakeEmbeddingModel();
        CachingEmbeddingModel secondRun = new CachingEmbeddingModel(secondModel, "model-a", cacheFile());

        assertArrayEquals(new double[] {0.1, 0.2}, secondRun.embed("hello"));
        assertEquals(1, secondRun.getHits());
        assertEquals(0, secondModel.getCalls());
    }

    @Test
    void changingTheModelNameMeansNoCacheHit() throws Exception {
        FakeEmbeddingModel modelA = new FakeEmbeddingModel().with("hello", 0.1, 0.2);
        CachingEmbeddingModel cacheA = new CachingEmbeddingModel(modelA, "model-a", cacheFile());
        cacheA.embed("hello");
        cacheA.save();

        // Same text and same file, different model name. The model name is part of
        // the cache key, so vectors from model-a must never be returned for model-b.
        FakeEmbeddingModel modelB = new FakeEmbeddingModel().with("hello", 0.9, 0.9);
        CachingEmbeddingModel cacheB = new CachingEmbeddingModel(modelB, "model-b", cacheFile());

        assertArrayEquals(new double[] {0.9, 0.9}, cacheB.embed("hello"));
        assertEquals(1, cacheB.getMisses());
        assertEquals(1, modelB.getCalls());
    }

    @Test
    void missingCacheFileStartsWithAnEmptyCache() throws Exception {
        CachingEmbeddingModel cache = new CachingEmbeddingModel(new FakeEmbeddingModel(), "model-a", cacheFile());

        assertEquals(0, cache.getHits());
        assertFalse(Files.exists(cacheFile()));   // creating the cache does not create the file
    }

    @Test
    void nothingIsWrittenUntilSaveIsCalled() throws Exception {
        FakeEmbeddingModel inner = new FakeEmbeddingModel().with("hello", 0.1, 0.2);
        CachingEmbeddingModel cache = new CachingEmbeddingModel(inner, "model-a", cacheFile());

        cache.embed("hello");
        assertFalse(Files.exists(cacheFile()));   // by design: one write after indexing, not one per chunk

        cache.save();
        assertTrue(Files.exists(cacheFile()));
    }

    @Test
    void saveCreatesTheCacheFolderIfItDoesNotExist() throws Exception {
        // Same situation as a fresh clone of the repository, where cache/ is git-ignored
        Path nestedFile = tempDir.resolve("cache").resolve("embeddings.json");
        FakeEmbeddingModel inner = new FakeEmbeddingModel().with("hello", 0.1, 0.2);
        CachingEmbeddingModel cache = new CachingEmbeddingModel(inner, "model-a", nestedFile);

        cache.embed("hello");
        cache.save();

        assertTrue(Files.exists(nestedFile));
    }

    @Test
    void knownLimitation_corruptCacheFileThrowsOnStartup() throws Exception {
        // KNOWN LIMITATION: an unreadable cache file stops the program instead of
        // being ignored and rebuilt. If that is changed later, update this test.
        Files.writeString(cacheFile(), "this is not JSON");

        assertThrows(IOException.class,
                () -> new CachingEmbeddingModel(new FakeEmbeddingModel(), "model-a", cacheFile()));
    }
}
