package com.example.documentqa;

import java.util.List;

/**
 * One evaluation question from eval/questions.json, with the result that is
 * expected before the system is run. Field names match the JSON keys, so
 * Jackson can build it directly.
 *
 * expectedFacts is a list of groups: every group must appear in the answer,
 * but any one word from a group is enough (e.g. ["three", "3"]).
 */
public record EvalQuestion(
        String id,
        String question,
        boolean answerable,
        List<String> expectedSources,
        List<List<String>> expectedFacts,
        String tests) {

    // Defensive copies make the question immutable, so scoring code can't
    // change the expected results by accident. List.copyOf also rejects
    // nulls, so a missing field in the JSON fails at load time.
    public EvalQuestion {
        expectedSources = List.copyOf(expectedSources);
        expectedFacts = expectedFacts.stream().map(List::copyOf).toList();
    }
}
