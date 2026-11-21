package com.example.documentqa;

import java.util.Locale;

/**
 * Scoring rules for the evaluation. Each method compares one Answer with the
 * result written in advance in an EvalQuestion. They are pure functions (no
 * model calls, no files), so every rule can be unit tested exactly.
 */
public final class AnswerScorer {

    // Utility class: only static methods, so it is never instantiated
    private AnswerScorer() {
    }

    /** Retrieval check: was at least one expected chunk among the retrieved sources? */
    public static boolean retrievedExpectedSource(EvalQuestion question, Answer answer) {
        return answer.sources().stream()
                .map(source -> source.getChunk().getKey())
                .anyMatch(question.expectedSources()::contains);
    }

    /**
     * Citation check: did the answer cite at least one expected chunk?
     * Reuses Answer.citedSources() so "cited" has one definition in the project.
     */
    public static boolean citedExpectedSource(EvalQuestion question, Answer answer) {
        return answer.citedSources().stream()
                .map(source -> source.getChunk().getKey())
                .anyMatch(question.expectedSources()::contains);
    }

    /**
     * Answer check: every fact group must appear, but any one alternative in
     * a group is enough (["three", "3"]). Case-insensitive substring match:
     * crude on purpose, so the results are checked by hand as well.
     */
    public static boolean containsExpectedFacts(EvalQuestion question, Answer answer) {
        String text = answer.text().toLowerCase(Locale.ROOT);
        return question.expectedFacts().stream()
                .allMatch(group -> group.stream()
                        .anyMatch(fact -> text.contains(fact.toLowerCase(Locale.ROOT))));
    }

    /**
     * Refusal check: either the model's refusal sentence from PromptBuilder, or
     * AnswerService's message when nothing passed the minimum score.
     */
    public static boolean refused(Answer answer) {
        // Models sometimes write a curly apostrophe (’) instead of '
        String text = answer.text().toLowerCase(Locale.ROOT).replace('\u2019', '\'');
        return text.contains("i don't know")
                || text.contains("couldn't find anything relevant");
    }
}