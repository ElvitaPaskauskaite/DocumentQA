package com.example.documentqa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Keeps every vector in a Map in memory. Simple and fast for a few hundred chunks.
// Everything is lost when the program stops (Phase 2 adds a cache file for that).
public class InMemoryVectorStore implements VectorStore {

	// One stored item: the chunk plus its vector, kept together
	private record Entry(TextChunk chunk, double[] vector) {}

	// Keyed by chunk.getKey() (e.g. "git_notes.txt#5"), so keys are unique across files.
	// LinkedHashMap keeps insertion order, so results are repeatable when scores tie.
	private final Map<String, Entry> entries = new LinkedHashMap<>();

	@Override
	public void add(TextChunk chunk, double[] vector) {
		entries.put(chunk.getKey(), new Entry(chunk, vector));
	}

	@Override
	public List<EmbeddingScore> search(double[] queryVector, int topK) {

		List<EmbeddingScore> scored = new ArrayList<>();

		// Brute force: compare the query with every stored vector
		for (Entry entry : entries.values()) {
			double score = VectorMath.cosineSimilarity(queryVector, entry.vector());
			scored.add(new EmbeddingScore(entry.chunk(), score));
		}

		// Highest score first
		scored.sort(Comparator.comparingDouble(EmbeddingScore::getScore).reversed());

		// Return at most topK results
		int limit = Math.min(topK, scored.size());
		return new ArrayList<>(scored.subList(0, limit));
	}

	@Override
	public int size() {
		return entries.size();
	}

	@Override
	public void clear() {
		entries.clear();
	}
}