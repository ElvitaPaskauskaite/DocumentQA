package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

// Tests for BagOfWordsEmbeddingModel, the word-count baseline embedder.
// The vocabulary is built from the chunks in order of first appearance:
// java = 0, is = 1, fun = 2, hard = 3
class BagOfWordsEmbeddingModelTest {

    private final BagOfWordsEmbeddingModel model = new BagOfWordsEmbeddingModel(List.of(
            new TextChunk("demo.txt", 1, "java is fun"),
            new TextChunk("demo.txt", 2, "java is hard")));

    @Test
    void vectorLengthEqualsVocabularySize() {
        // 4 distinct words across both chunks
        assertEquals(4, model.embed("java").length);
    }

    @Test
    void eachPositionCountsHowOftenThatWordAppears() {
        assertArrayEquals(new double[] {2, 0, 1, 0}, model.embed("java java fun"), 1e-9);
    }

    @Test
    void caseAndPunctuationAreIgnored() {
        assertArrayEquals(model.embed("java is fun"), model.embed("Java, IS fun!"), 1e-9);
    }

    @Test
    void wordsOutsideTheVocabularyAreIgnored() {
        // No known words, so the result is a zero vector (VectorMath scores it 0 against everything)
        assertArrayEquals(new double[] {0, 0, 0, 0}, model.embed("python rocks"), 1e-9);
    }

    @Test
    void sharedWordsGiveExpectedSimilarity() {
        // 2 of 3 words in common: 2 / (sqrt(3) * sqrt(3)) = 0.667. Same example as the earlier manual check
        double score = VectorMath.cosineSimilarity(model.embed("java is fun"), model.embed("java is hard"));

        assertEquals(2.0 / 3.0, score, 1e-9);
    }
}