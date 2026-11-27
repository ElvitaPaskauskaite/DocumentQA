package com.example.documentqa;

import java.util.ArrayList;
import java.util.List;

// Turns text into vectors (EmbeddingModel) and asks the VectorStore for the closest ones.
// It no longer stores or scans vectors itself.
public class SemanticSearchService {

	// Can swap BagOfWords for Ollama without changing this class
	private final EmbeddingModel model;

	// Can swap in-memory for a database without changing this class
	private final VectorStore store;

	// Both dependencies are passed in: this class only knows the interfaces
	public SemanticSearchService(EmbeddingModel model, VectorStore store) {
		this.model = model;
		this.store = store;
	}

	// Embed every chunk first, then replace the store's contents. If an embedding fails,
	// the store keeps the chunks it had before instead of being left half-filled.
	public void index(List<TextChunk> chunks) throws Exception {
		List<double[]> vectors = new ArrayList<>();
		for (TextChunk chunk : chunks) {
			vectors.add(model.embed(chunk.getContent())); // text -> numbers
		}

		store.clear(); // start over if index is called again
		store.addAll(chunks, vectors);
	}

	// Embed the question and let the store return the best topK
	public List<EmbeddingScore> search(String query, int topK) throws Exception {

		// Searching before indexing would silently return nothing useful
		if (store.size() == 0) {
			throw new IllegalStateException("No chunks indexed. Call index() first.");
		}

		// Embed the question with the same model used for the chunks
		double[] queryVector = model.embed(query);

		return store.search(queryVector, topK);
	}

	// Same as search(), but drops chunks scoring below minScore,
	// so off-topic questions return nothing instead of noise
	public List<EmbeddingScore> search(String query, int topK, double minScore) throws Exception {

		List<EmbeddingScore> top = search(query, topK);

		List<EmbeddingScore> filtered = new ArrayList<>();
		for (EmbeddingScore s : top) {
			if (s.getScore() >= minScore) {
				filtered.add(s);
			}
		}
		return filtered;
	}
}
