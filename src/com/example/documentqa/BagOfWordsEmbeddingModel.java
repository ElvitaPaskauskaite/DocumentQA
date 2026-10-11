package com.example.documentqa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BagOfWordsEmbeddingModel implements EmbeddingModel{
	
	private final Map<String, Integer> vocabulary = new HashMap<>();
	
	// Build the vocab from all chunks before embedding anything
	public BagOfWordsEmbeddingModel(List<TextChunk> chunks) {
		for (TextChunk chunk: chunks) {
			for (String word : tokenize(chunk.getContent())) {
				if (!vocabulary.containsKey(word)) {
					vocabulary.put(word, vocabulary.size());
				}
			}
		}
	}
	
	@Override
	public double[] embed(String text) {
		double[] vector = new double[vocabulary.size()];
			for (String word : tokenize(text)) {
				Integer index = vocabulary.get(word);
				if (index != null) {
					vector[index]++;	// ignore words not in the vocabulary
				}
			}
		return vector;
	}
	
	private String[] tokenize(String text) {
		return text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
	}
}
