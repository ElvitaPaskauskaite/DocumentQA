package com.example.documentqa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticSearchService {
	
	// Can swap BagOfWords for Ollama later without chaning this class
	private final EmbeddingModel model;
	
	// Maps a chunk id to its vector, so each chunk is embedded only once
	private final Map<Integer, double[]> chunkVectors = new HashMap<>();
	
	// The chunks indexed, kept so search can return them.
	private final List<TextChunk> indexedChunks = new ArrayList<>();
	
	// the model is passed in, class doesnt know what model is being used, this is the payoff for interface
	public SemanticSearchService(EmbeddingModel model) {
		this.model = model;
	}
	
	// Embed every chunk once and store the results, call this once at startup, before any searching
	public void index(List<TextChunk> chunks) throws Exception {
		chunkVectors.clear(); // Start over if index is called again
		indexedChunks.clear();
		
		for (TextChunk chunk : chunks) {
			double[] vector = model.embed(chunk.getContent()); // Turn text to numbers
			chunkVectors.put(chunk.getId(), vector);
			indexedChunks.add(chunk);
		}
	}
	
	// embed the question, score every chunk, return the best topK	
	public List<EmbeddingScore> search(String query, int topK) throws Exception {
		
        // searching before indexing would silently return nothing useful.
        if (indexedChunks.isEmpty()) {
            throw new IllegalStateException("No chunks indexed. Call index() first.");
        }
        
        // Embed the question with the same model used for the chunks
        double[] queryVector = model.embed(query);
        
        List<EmbeddingScore> scored = new ArrayList<>();
        
        for (TextChunk chunk : indexedChunks) {
            double[] chunkVector = chunkVectors.get(chunk.getId());

            // VectorMath does the comparison: one number per chunk.
            double score = VectorMath.cosineSimilarity(queryVector, chunkVector);

            // EmbeddingScore just bundles the chunk with its score.
            scored.add(new EmbeddingScore(chunk, score));      	
        }
        
        // Sort highest score first. comparingDouble reads each score        // and reversed() flips the default low-to-high order.
        scored.sort(Comparator.comparingDouble(EmbeddingScore::getScore).reversed());

        // Return at most topK results
        int limit = Math.min(topK, scored.size());
        return new ArrayList<>(scored.subList(0, limit));       
	}

}
