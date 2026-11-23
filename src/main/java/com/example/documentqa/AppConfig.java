package com.example.documentqa;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

// Reads application.properties once and returns typed settings.
// The only class that knows the settings come from a file.
public class AppConfig {

	private final Properties props = new Properties();

	public AppConfig(String filePath) throws IOException {
		// try-with-resources closes the file even if loading fails
		try (Reader reader = Files.newBufferedReader(Path.of(filePath))) {
			props.load(reader);
		}
	}

	// Fail loudly with a clear message instead of a NullPointerException later
	private String get(String key) {
		String value = props.getProperty(key);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Missing setting '" + key + "' in application.properties");
		}
		return value.trim();
	}

	public String getDocumentsFolder() { return get("documents.folder"); }
	public String getEmbeddingModel()  { return get("ollama.embedding.model"); }
	public String getChatModel()       { return get("ollama.chat.model"); }
	public int getTopK()               { return Integer.parseInt(get("search.top-k")); }
	public double getMinScore()        { return Double.parseDouble(get("search.min-score")); }
	public String getEmbeddingCacheFile() { return get("embeddings.cache-file"); }
	public int getChunkMaxWords()      { return Integer.parseInt(get("chunk.max-words")); }
	public int getChunkOverlapWords()  { return Integer.parseInt(get("chunk.overlap-words")); }

	// Where chunk vectors are kept: "memory" (nothing leaves the Mac) or "hana" (SAP HANA Cloud).
	// Any other value is a typo, so it fails at start-up instead of silently using the wrong store
	public String getVectorStore() {
		String value = get("vector.store");
		if (!value.equals("memory") && !value.equals("hana")) {
			throw new IllegalStateException("Setting 'vector.store' must be memory or hana, not '" + value + "'");
		}
		return value;
	}
}