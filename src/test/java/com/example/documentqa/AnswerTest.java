package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

// Tests for Answer (src/main/java/.../Answer.java): citations and immutability.
class AnswerTest {

    // Two chunks from the same file. #1 and #13 share the prefix "doc.txt#1"
    private final EmbeddingScore chunk1  = new EmbeddingScore(new TextChunk("doc.txt", 1, "Rule one."), 0.8);
    private final EmbeddingScore chunk13 = new EmbeddingScore(new TextChunk("doc.txt", 13, "Contact."), 0.6);

    @Test
    void citedLabelGoesToCitedAndTheRestToUncited() {
        Answer answer = new Answer("The rule is one [doc.txt#1].", List.of(chunk1, chunk13));

        assertEquals(List.of(chunk1), answer.citedSources());
        assertEquals(List.of(chunk13), answer.uncitedSources());
    }

    @Test
    void bracketsStopShortKeyMatchingInsideLongerKey() {
        // Only #13 is cited. Without brackets, "doc.txt#1" would also be found inside "doc.txt#13"
        Answer answer = new Answer("Email us [doc.txt#13].", List.of(chunk1, chunk13));

        assertEquals(List.of(chunk13), answer.citedSources());
        assertEquals(List.of(chunk1), answer.uncitedSources());
    }

    @Test
    void keyWithoutBracketsDoesNotCount() {
        Answer answer = new Answer("See doc.txt#1 for details.", List.of(chunk1));

        assertTrue(answer.citedSources().isEmpty());
    }

    @Test
    void refusalCitesNothingButStillHasSources() {
        // The LLM was called (chunks passed min-score) but refused, so no labels in the text
        Answer answer = new Answer("I don't know based on the documents.", List.of(chunk1, chunk13));

        assertTrue(answer.hasSources());
        assertTrue(answer.citedSources().isEmpty());
        assertEquals(2, answer.uncitedSources().size());
    }

    @Test
    void noSourcesMeansHasSourcesIsFalse() {
        // The "nothing relevant" path in AnswerService: LLM never called
        Answer answer = new Answer("I couldn't find anything relevant in the documents.", List.of());

        assertFalse(answer.hasSources());
        assertTrue(answer.citedSources().isEmpty());
    }

    @Test
    void changingTheOriginalListDoesNotChangeTheAnswer() {
        List<EmbeddingScore> original = new ArrayList<>(List.of(chunk1));
        Answer answer = new Answer("Text [doc.txt#1].", original);

        original.add(chunk13);   // change the caller's list AFTER creating the Answer

        assertEquals(1, answer.sources().size());   // List.copyOf took a snapshot
    }

    @Test
    void sourcesCannotBeModified() {
        Answer answer = new Answer("Text.", List.of(chunk1));

        assertThrows(UnsupportedOperationException.class, () -> answer.sources().add(chunk13));
    }

    @Test
    void knownLimitation_severalLabelsInOneBracketAreNotDetected() {
        // KNOWN LIMITATION: the check looks for "[key]" exactly, so "[a, b]" matches neither.
        // If this is fixed later, this test should fail and be updated to expect both cited.
        Answer answer = new Answer("Both apply [doc.txt#1, doc.txt#13].", List.of(chunk1, chunk13));

        assertTrue(answer.citedSources().isEmpty());
    }
}