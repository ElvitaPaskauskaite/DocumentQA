package com.example.documentqa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Splits a document into chunks:
// 1. split at blank lines (one paragraph = one chunk, as before)
// 2. if a paragraph is longer than maxWords, cut it into overlapping windows
public class TextSplitter {

	private final int maxWords;      // longest chunk allowed
	private final int overlapWords;  // words repeated at the start of the next window

	public TextSplitter(int maxWords, int overlapWords) {
		// Fail early with a clear message: if overlap >= maxWords, the window
		// would never move forward and the loop below would never end
		if (maxWords <= 0 || overlapWords < 0 || overlapWords >= maxWords) {
			throw new IllegalArgumentException("Need maxWords > 0 and 0 <= overlapWords < maxWords, got "
					+ maxWords + " and " + overlapWords);
		}
		this.maxWords = maxWords;
		this.overlapWords = overlapWords;
	}

	public List<TextChunk> split(Document document) {

		List<TextChunk> chunks = new ArrayList<>();

		// Split at blank lines into paragraphs
		String[] paragraphs = document.getContent().split("\\n\\s*\\n");

		// Positions keep counting across the whole document,
		// so keys like "policy.txt#7" stay unique even when a paragraph is cut
		int position = 1;

		for (String paragraph : paragraphs) {
			// Don't create empty chunks
			if (paragraph.trim().isEmpty()) {
				continue;
			}
			// A short paragraph comes back as one piece, a long one as several
			for (String piece : window(paragraph.trim())) {
				chunks.add(new TextChunk(document.getTitle(), position, piece));
				position++;
			}
		}

		return chunks;
	}

	// Cut one paragraph into windows of at most maxWords words,
	// each starting overlapWords before the previous one ended
	private List<String> window(String paragraph) {

		String[] words = paragraph.split("\\s+");

		// Short enough: keep the paragraph exactly as written
		if (words.length <= maxWords) {
			return List.of(paragraph);
		}

		List<String> pieces = new ArrayList<>();
		int step = maxWords - overlapWords;   // how far each window moves forward

		for (int start = 0; start < words.length; start += step) {
			int end = Math.min(start + maxWords, words.length);
			// copyOfRange takes words[start] up to (not including) words[end]
			pieces.add(String.join(" ", Arrays.copyOfRange(words, start, end)));

			// This window reached the last word: stop, or the next window
			// would only repeat words we already have
			if (end == words.length) {
				break;
			}
		}
		return pieces;
	}
}
