package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AnswerScorerTest {

    private static final EvalQuestion LAPTOP = question(
            List.of("procurement_policy.txt#4"), List.of(List.of("three", "3")));

    // A retrieved chunk with a made-up score; only its key matters here
    private static EmbeddingScore source(String file, int position) {
        return new EmbeddingScore(new TextChunk(file, position, "text"), 0.7);
    }

    private static EvalQuestion question(List<String> sources, List<List<String>> facts) {
        return new EvalQuestion("t", "Q?", true, sources, facts, "test");
    }

    @Test
    void retrievalHitWhenExpectedChunkIsAmongSources() {
        Answer answer = new Answer("text", List.of(
                source("procurement_policy.txt", 3), source("procurement_policy.txt", 4)));
        assertTrue(AnswerScorer.retrievedExpectedSource(LAPTOP, answer));
    }

    @Test
    void retrievalMissWhenExpectedChunkIsNotRetrieved() {
        Answer answer = new Answer("text", List.of(source("procurement_policy.txt", 3)));
        assertFalse(AnswerScorer.retrievedExpectedSource(LAPTOP, answer));
    }

    @Test
    void citedWhenAnswerContainsTheExpectedLabel() {
        Answer answer = new Answer("You need three quotes. [procurement_policy.txt#4]",
                List.of(source("procurement_policy.txt", 4)));
        assertTrue(AnswerScorer.citedExpectedSource(LAPTOP, answer));
    }

    @Test
    void retrievedButCitingTheWrongChunkIsNotCited() {
        // The real laptop failure: #4 was retrieved, but the answer cited #3
        Answer answer = new Answer("You need one quote. [procurement_policy.txt#3]",
                List.of(source("procurement_policy.txt", 4), source("procurement_policy.txt", 3)));
        assertTrue(AnswerScorer.retrievedExpectedSource(LAPTOP, answer));
        assertFalse(AnswerScorer.citedExpectedSource(LAPTOP, answer));
    }

    @Test
    void anyAlternativeInAFactGroupCounts() {
        Answer answer = new Answer("You need 3 written quotes.", List.of());
        assertTrue(AnswerScorer.containsExpectedFacts(LAPTOP, answer));
    }

    @Test
    void everyFactGroupMustAppear() {
        EvalQuestion carryOver = question(List.of("annual_leave_policy.txt#7"),
                List.of(List.of("5", "five"), List.of("31 march")));
        Answer missingDeadline = new Answer("You can carry over 5 days.", List.of());
        assertFalse(AnswerScorer.containsExpectedFacts(carryOver, missingDeadline));
    }

    @Test
    void factMatchIgnoresCase() {
        EvalQuestion carryOver = question(List.of("annual_leave_policy.txt#7"),
                List.of(List.of("31 march")));
        Answer answer = new Answer("Use them by 31 MARCH.", List.of());
        assertTrue(AnswerScorer.containsExpectedFacts(carryOver, answer));
    }

    @Test
    void recognisesBothKindsOfRefusal() {
        assertTrue(AnswerScorer.refused(new Answer("I don't know based on the documents.", List.of())));
        assertTrue(AnswerScorer.refused(new Answer("I don\u2019t know based on the documents.", List.of())));
        assertTrue(AnswerScorer.refused(
                new Answer("I couldn't find anything relevant in the documents.", List.of())));
        assertFalse(AnswerScorer.refused(new Answer("You need three quotes.", List.of())));
    }

    @Test
    void knownLimitationSubstringMatchAcceptsDigitsInsideOtherNumbers() {
        // "3" is found inside "€300", so a wrong answer passes the fact check.
        // Pinned on purpose: this is why the results are also checked by hand.
        Answer wrong = new Answer("The limit is €300.", List.of());
        assertTrue(AnswerScorer.containsExpectedFacts(LAPTOP, wrong));
    }
}