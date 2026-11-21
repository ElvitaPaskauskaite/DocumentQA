package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Tests for TextSplitter (src/main/java/.../TextSplitter.java).
// Uses in-memory Documents, so no files are read.
class TextSplitterTest {

    // ---------- helpers ----------

    // Wraps text in a Document called "doc.txt" and splits it
    private List<TextChunk> split(TextSplitter splitter, String text) {
        return splitter.split(new Document("doc.txt", text));
    }

    // Just the chunk texts, so one assertEquals can check them all in order
    private List<String> contents(List<TextChunk> chunks) {
        return chunks.stream().map(TextChunk::getContent).toList();
    }

    // ---------- paragraphs ----------

    @Test
    void splitsAtBlankLines() {
        TextSplitter splitter = new TextSplitter(120, 20);

        List<TextChunk> chunks = split(splitter, "First paragraph.\n\nSecond paragraph.");

        assertEquals(List.of("First paragraph.", "Second paragraph."), contents(chunks));
        assertEquals("doc.txt#1", chunks.get(0).getKey());
        assertEquals("doc.txt#2", chunks.get(1).getKey());
    }

    @Test
    void emptyParagraphsAreSkippedAndDoNotUseUpPositions() {
        TextSplitter splitter = new TextSplitter(120, 20);
        // Leading blank lines, a "blank" line with spaces, several blank lines in a row, trailing blank lines
        String text = "\n\nFirst\n   \n\n\nSecond\n\n";

        List<TextChunk> chunks = split(splitter, text);

        assertEquals(List.of("First", "Second"), contents(chunks));
        assertEquals(2, chunks.get(1).getPosition());   // not 3 or 4: skipped paragraphs don't count
    }

    @Test
    void shortParagraphIsKeptExactlyAsWritten() {
        TextSplitter splitter = new TextSplitter(120, 20);

        // A single line break inside a paragraph is NOT a split point
        List<TextChunk> chunks = split(splitter, "Line one\nline two");

        assertEquals(List.of("Line one\nline two"), contents(chunks));
    }

    @Test
    void paragraphOfExactlyMaxWordsIsOneChunk() {
        // Boundary: 5 words with maxWords 5 must NOT be cut (line 59 uses <=)
        TextSplitter splitter = new TextSplitter(5, 2);

        List<TextChunk> chunks = split(splitter, "w1 w2 w3 w4 w5");

        assertEquals(List.of("w1 w2 w3 w4 w5"), contents(chunks));
    }

    // ---------- word windows ----------

    @Test
    void longParagraphIsCutIntoOverlappingWindows() {
        // maxWords 5, overlap 2 -> each window moves forward 3 words
        TextSplitter splitter = new TextSplitter(5, 2);

        List<TextChunk> chunks = split(splitter, "w1 w2 w3 w4 w5 w6 w7 w8 w9");

        assertEquals(List.of(
                "w1 w2 w3 w4 w5",
                "w4 w5 w6 w7 w8",   // starts with the last 2 words of the window before
                "w7 w8 w9"),        // last window can be shorter
                contents(chunks));
    }

    @Test
    void noFinalWindowThatOnlyRepeatsOverlapWords() {
        // 8 words: windows w1-w5 and w4-w8. Without the break on line 73
        // there would be a third chunk "w7 w8" with nothing new in it
        TextSplitter splitter = new TextSplitter(5, 2);

        List<TextChunk> chunks = split(splitter, "w1 w2 w3 w4 w5 w6 w7 w8");

        assertEquals(List.of("w1 w2 w3 w4 w5", "w4 w5 w6 w7 w8"), contents(chunks));
    }

    @Test
    void positionsCountAcrossTheWholeDocument() {
        // Short, long (cut into 3), short -> positions 1..5, so keys stay unique
        TextSplitter splitter = new TextSplitter(5, 2);
        String text = "Intro.\n\nw1 w2 w3 w4 w5 w6 w7 w8 w9\n\nContact.";

        List<TextChunk> chunks = split(splitter, text);

        List<String> keys = chunks.stream().map(TextChunk::getKey).toList();
        assertEquals(List.of("doc.txt#1", "doc.txt#2", "doc.txt#3", "doc.txt#4", "doc.txt#5"), keys);
    }

    // ---------- constructor ----------

    // Runs once per line in @CsvSource: "maxWords, overlapWords"
    @ParameterizedTest
    @CsvSource({
            "0, 0",    // maxWords must be > 0
            "5, -1",   // overlap can't be negative
            "5, 5",    // overlap == maxWords: window would never move forward (infinite loop)
            "5, 6"     // overlap > maxWords: same problem
    })
    void invalidSettingsThrow(int maxWords, int overlapWords) {
        assertThrows(IllegalArgumentException.class, () -> new TextSplitter(maxWords, overlapWords));
    }

    @Test
    void edgeOfValidSettingsIsAccepted() {
        assertDoesNotThrow(() -> new TextSplitter(5, 0));   // no overlap
        assertDoesNotThrow(() -> new TextSplitter(5, 4));   // largest allowed overlap
    }
}
