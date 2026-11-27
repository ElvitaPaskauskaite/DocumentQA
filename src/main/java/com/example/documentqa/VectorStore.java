package com.example.documentqa;

import java.util.List;

// Stores chunk vectors and finds the most similar ones.
// SemanticSearchService only talks to this interface, so the in-memory version
// can later be swapped for a database (e.g. HANA Cloud) without changing the service.
public interface VectorStore {

	// Save one chunk and its vector. Adding the same key again replaces the old entry.
	void add(TextChunk chunk, double[] vector);
	

	// Save many chunks at once. The default calls add() for each one, so every store works
	// straight away. A store that talks to a database can override this to send the whole
	// batch in fewer round trips. The two lists are matched by position.
	default void addAll(List<TextChunk> chunks, List<double[]> vectors) {
		if (chunks.size() != vectors.size()) {
			throw new IllegalArgumentException("Got " + chunks.size() + " chunks but " + vectors.size() + " vectors");
		}
		for (int i = 0; i < chunks.size(); i++) {
			add(chunks.get(i), vectors.get(i));
		}
	}

	// Return the topK chunks most similar to queryVector, best first.
	List<EmbeddingScore> search(double[] queryVector, int topK);

	// How many chunks are stored
	int size();

	// Remove everything
	void clear();
}