package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvalQuestionLoaderTest {

    private final EvalQuestionLoader loader = new EvalQuestionLoader();

    @Test
    void readsEveryFieldFromJson(@TempDir Path dir) throws IOException {
        // Includes € to check the file is read as UTF-8
        Path file = dir.resolve("questions.json");
        Files.writeString(file, """
                [
                  {
                    "id": "q1",
                    "question": "How many quotes for €4,000?",
                    "answerable": true,
                    "expectedSources": ["procurement_policy.txt#4"],
                    "expectedFacts": [["three", "3"]],
                    "tests": "range comparison"
                  }
                ]
                """);

        List<EvalQuestion> questions = loader.load(file);

        assertEquals(1, questions.size());
        EvalQuestion q = questions.get(0);
        assertEquals("q1", q.id());
        assertEquals("How many quotes for €4,000?", q.question());
        assertTrue(q.answerable());
        assertEquals(List.of("procurement_policy.txt#4"), q.expectedSources());
        assertEquals(List.of(List.of("three", "3")), q.expectedFacts());
        assertEquals("range comparison", q.tests());
    }

    @Test
    void misspelledFieldFailsInsteadOfBeingIgnored(@TempDir Path dir) throws IOException {
        // "expectedSource" (missing s): if this were silently ignored, the
        // question would have no expected source and score wrongly
        Path file = dir.resolve("questions.json");
        Files.writeString(file, """
                [
                  {
                    "id": "q1",
                    "question": "Q?",
                    "answerable": true,
                    "expectedSource": ["a.txt#1"],
                    "expectedFacts": [],
                    "tests": "typo"
                  }
                ]
                """);

        assertThrows(IOException.class, () -> loader.load(file));
    }

    @Test
    void realQuestionFileIsWellFormed() throws IOException {
        // Guards the real evaluation set: a broken file would make every
        // results table meaningless
        List<EvalQuestion> questions = loader.load(Path.of("eval/questions.json"));

        assertEquals(20, questions.size());
        assertEquals(15L, questions.stream().filter(EvalQuestion::answerable).count());
        assertEquals(20L, questions.stream().map(EvalQuestion::id).distinct().count());

        for (EvalQuestion q : questions) {
            if (q.answerable()) {
                assertFalse(q.expectedSources().isEmpty(), q.id() + " has no expected source");
                assertFalse(q.expectedFacts().isEmpty(), q.id() + " has no expected facts");
            } else {
                assertTrue(q.expectedSources().isEmpty(), q.id() + " should have no sources");
                assertTrue(q.expectedFacts().isEmpty(), q.id() + " should have no facts");
            }
        }
    }
}