package com.example.documentqa;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// Wraps a real EmbeddingModel and remembers its vectors in a JSON file.
// Same model + same text = same vector, so each chunk only needs embedding once.
// It implements EmbeddingModel itself, so SemanticSearchService can't tell
// it's talking to a cache (the Decorator pattern).
public class CachingEmbeddingModel implements EmbeddingModel {

	private final EmbeddingModel inner;   // the real model, e.g. OllamaEmbeddingModel
	private final String modelName;       // part of the key: different models give different vectors
	private final Path cacheFile;
	private final ObjectMapper mapper = new ObjectMapper();

	// Key: SHA-256 fingerprint of (model name + text). Value: the vector.
	private final Map<String, double[]> cache;

	private int hits = 0;     // found in the cache, no Ollama call
	private int misses = 0;   // not found, so the real model was called

	public CachingEmbeddingModel(EmbeddingModel inner, String modelName, Path cacheFile) throws IOException {
		this.inner = inner;
		this.modelName = modelName;
		this.cacheFile = cacheFile;

		// Load earlier vectors if the file exists; otherwise start empty.
		// TypeReference tells Jackson the exact type to build: a Map of String -> double[]
		if (Files.exists(cacheFile)) {
			cache = mapper.readValue(cacheFile.toFile(), new TypeReference<HashMap<String, double[]>>() {});
		} else {
			cache = new HashMap<>();
		}
	}

	@Override
	public double[] embed(String text) throws Exception {
		// The model name is part of the fingerprint, so switching models
		// never returns the other model's vectors
		String key = sha256(modelName + "\n" + text);

		double[] vector = cache.get(key);
		if (vector != null) {
			hits++;
			return vector;
		}

		// Not seen before: ask the real model and remember the answer
		misses++;
		vector = inner.embed(text);
		cache.put(key, vector);
		return vector;
	}

	// Write the cache to disk. Called once after indexing, not after every embed,
	// because rewriting the file 39 times would be slow.
	public void save() throws IOException {
		Path folder = cacheFile.getParent();
		if (folder != null) {
			Files.createDirectories(folder);   // creates cache/ the first time
		}
		mapper.writeValue(cacheFile.toFile(), cache);
	}

	public int getHits()   { return hits; }
	public int getMisses() { return misses; }

	// Text -> 64-character hex fingerprint. Change one letter and the whole fingerprint changes.
	private static String sha256(String text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);   // bytes -> "a3f9..." (Java 17+)
		} catch (NoSuchAlgorithmException e) {
			// Every Java installation includes SHA-256, so this can't really happen
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}
}