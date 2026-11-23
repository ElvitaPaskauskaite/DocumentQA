package com.example.documentqa;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Builds the question-answering pipeline from application.properties in one place,
// so Main, Evaluator and the REST API run exactly the same set-up. Before this,
// Main and Evaluator each had a copy, and a change to one could silently miss the other,
// which would mean the evaluation no longer measured the system people actually use.
public record Pipeline(
		List<Document> documents,
		List<TextChunk> chunks,
		CachingEmbeddingModel cache,
		String embedderName,
		String storeName,
		long indexMillis,
		AnswerService answerService) {

	// The normal set-up: the embedding model from the settings, wrapped in the cache
	public static Pipeline build(AppConfig config) throws Exception {
		return build(config, false);
	}

	// bagOfWords = true swaps in the word-count baseline (used by the Evaluator to
	// measure what the real embedding model adds). Both implement EmbeddingModel,
	// so nothing after this choice changes.
	public static Pipeline build(AppConfig config, boolean bagOfWords) throws Exception {
		List<Document> documents = new DocumentLoader().loadFolder(config.getDocumentsFolder());
		TextSplitter splitter = new TextSplitter(config.getChunkMaxWords(), config.getChunkOverlapWords());
		List<TextChunk> chunks = new ArrayList<>();
		for (Document document : documents) {
			chunks.addAll(splitter.split(document));
		}

		// Created even for bag-of-words, because callers save it after answering
		// (the question embeddings are new and worth keeping)
		CachingEmbeddingModel cache = new CachingEmbeddingModel(
				new OllamaEmbeddingModel(config.getEmbeddingModel()),
				config.getEmbeddingModel(), Path.of(config.getEmbeddingCacheFile()));
		EmbeddingModel embedder = bagOfWords ? new BagOfWordsEmbeddingModel(chunks) : cache;
		String embedderName = bagOfWords ? "bag-of-words" : config.getEmbeddingModel();

		VectorStore store = createStore(config, embedder, chunks);
		SemanticSearchService search = new SemanticSearchService(embedder, store);

		// Timed so the effect of the cache stays measurable
		long start = System.nanoTime();
		search.index(chunks);
		long indexMillis = (System.nanoTime() - start) / 1_000_000;   // nanoseconds -> milliseconds

		// Save straight away, so new chunk vectors survive even if the program stops early
		cache.save();

		AnswerService answerService = new AnswerService(search, new PromptBuilder(),
				new OllamaChatModel(config.getChatModel()), config.getTopK(), config.getMinScore());

		// Copies, so code using the pipeline can't change the lists it was built from
		return new Pipeline(List.copyOf(documents), List.copyOf(chunks), cache,
				embedderName, config.getVectorStore(), indexMillis, answerService);
	}

	// vector.store=memory keeps vectors in a Java map; vector.store=hana stores and searches them
	// in SAP HANA Cloud. The table name includes the vector length, so vectors from different
	// embedders (768 numbers for nomic-embed-text, vocabulary size for bag-of-words) never share a table
	private static VectorStore createStore(AppConfig config, EmbeddingModel embedder,
			List<TextChunk> chunks) throws Exception {
		if (config.getVectorStore().equals("memory")) {
			return new InMemoryVectorStore();
		}
		// One embedding tells us the vector length (it comes from the cache, so it's cheap)
		int dimensions = embedder.embed(chunks.get(0).getContent()).length;
		return new HanaVectorStore(HanaSettings.fromEnvironment(), "CHUNK_VECTORS_" + dimensions, dimensions);
	}
}