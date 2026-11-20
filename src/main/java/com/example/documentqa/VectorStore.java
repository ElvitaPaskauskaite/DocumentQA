package com.example.documentqa;

import java.util.List;

// Stores chunk vectors and finds the most similar ones.
// SemanticSearchService only talks to this interface, so the in-memory version
// can later be swapped for a database (e.g. HANA Cloud) without changing the service.
public interface VectorStore {

	// Save one chunk and its vector. Adding the same key again replaces the old entry.
	void add(TextChunk chunk, double[] vector);

	// Return the topK chunks most similar to queryVector, best first.
	List<EmbeddingScore> search(double[] queryVector, int topK);

	// How many chunks are stored
	int size();

	// Remove everything
	void clear();
}