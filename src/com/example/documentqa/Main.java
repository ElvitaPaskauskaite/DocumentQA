package com.example.documentqa;

import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import java.nio.file.Path;

public class Main {

	public static void main(String[] args) throws Exception {
		// Load settings from application.properties (in the project folder)
		AppConfig config = new AppConfig("application.properties");

		// Title banner
		printLine();
		System.out.println("  DOCUMENT Q&A SYSTEM");
		System.out.println("  Retrieval-Augmented Generation in Java");
		printLine();

		// Load every document in the folder and split them all into chunks
		System.out.println();
		System.out.println("STEP 1: Loading and splitting the documents");
		printLine();

		// Load every .txt file in the documents folder
		DocumentLoader loader = new DocumentLoader();
		List<Document> documents = loader.loadFolder(config.getDocumentsFolder());

		// Split each document and collect ALL chunks in one list.
		// Keys stay unique because each chunk carries its file name (e.g. git_notes.txt#2)
		// Chunk size limits come from application.properties
		TextSplitter splitter = new TextSplitter(config.getChunkMaxWords(), config.getChunkOverlapWords());
		List<TextChunk> chunks = new ArrayList<>();

		for (Document document : documents) {
			List<TextChunk> documentChunks = splitter.split(document);
			chunks.addAll(documentChunks);
			System.out.println("  File   : " + document.getTitle() + " (" + documentChunks.size() + " chunks)");
		}

		System.out.println("  Total  : " + chunks.size() + " chunks from " + documents.size() + " files");

		// Ask the question
		Scanner scanner = new Scanner(System.in);
		System.out.print("\n  Enter a question: ");
		String query = scanner.nextLine();
		scanner.close();

		// Version 1: keyword search, shown for comparison
		System.out.println();
		System.out.println("STEP 2: Keyword search (Version 1: exact text match)");
		printLine();

		SearchService keywordSearch = new SearchService();
		List<TextChunk> keywordResults = keywordSearch.search(query, chunks);

		if (keywordResults.isEmpty()) {
			System.out.println("  No chunks matched. Keyword search needs the exact phrase.");
		} else {
			System.out.println("  Found " + keywordResults.size() + " matching chunk(s):");
			for (TextChunk chunk : keywordResults) {
				System.out.println("\n  [Chunk " + chunk.getKey() + "]");
				System.out.println("  " + chunk.getContent());
			}
		}

		// Version 2: semantic search
		System.out.println();
		System.out.println("STEP 3: Semantic search (Version 2: embeddings + cosine similarity)");
		printLine();
		
		// The real model, wrapped in a cache: only new or changed chunks are sent to Ollama.
		// The variable type is CachingEmbeddingModel (not EmbeddingModel) because Main needs
		// save() and the hit/miss counts, which aren't part of the interface.
		EmbeddingModel ollama = new OllamaEmbeddingModel(config.getEmbeddingModel());
		CachingEmbeddingModel model = new CachingEmbeddingModel(
				ollama, config.getEmbeddingModel(), Path.of(config.getEmbeddingCacheFile()));
		SemanticSearchService semantic = new SemanticSearchService(model, new InMemoryVectorStore());

		// Time the indexing, so the effect of the cache can be measured
		long start = System.nanoTime();
		semantic.index(chunks);
		long indexMillis = (System.nanoTime() - start) / 1_000_000;   // nanoseconds -> milliseconds

		// Write the vectors to disk for the next run
		model.save();

		System.out.println("  Indexed " + chunks.size() + " chunks in " + indexMillis + " ms ("
				+ model.getHits() + " from cache, " + model.getMisses() + " newly embedded)");

		// Build the answer pipeline: search -> prompt -> LLM
		PromptBuilder promptBuilder = new PromptBuilder();
		ChatModel chat = new OllamaChatModel(config.getChatModel());
		AnswerService answerService = new AnswerService(semantic, promptBuilder, chat, config.getTopK(), config.getMinScore());

		// ONE search: AnswerService searches, calls the LLM, and returns the chunks it used
		System.out.println("  Searching and generating an answer... (this can take a few seconds)");
		Answer answer = answerService.answer(query);

		// Show the retrieved chunks. They come from the Answer, so there's no second search
		if (!answer.hasSources()) {
			System.out.println("  No relevant chunks found.");
		} else {
			System.out.println("  Top " + answer.sources().size() + " most relevant chunks:");
			for (EmbeddingScore source : answer.sources()) {
				System.out.println("\n  [Chunk " + source.getChunk().getKey() + "]  score: "
						+ String.format("%.3f", source.getScore()));
				System.out.println("  " + source.getChunk().getContent());
			}
		}

		// Version 3: the LLM's answer, followed by where it came from
		System.out.println();
		System.out.println("STEP 4: The answer (Version 3: retrieved chunks + LLM)");
		printLine();
		System.out.println("  QUESTION: " + query);
		System.out.println("  ANSWER:   " + answer.text());

		// Sources: what the answer cited, then what was retrieved but not cited
		if (answer.hasSources()) {
			System.out.println();
			if (answer.citedSources().isEmpty()) {
				System.out.println("  Sources: none cited");
			} else {
				System.out.println("  Sources:");
				for (EmbeddingScore source : answer.citedSources()) {
					printSource(source);
				}
			}

			// Shown so problems like the €4,000 case are easy to spot
			if (!answer.uncitedSources().isEmpty()) {
				System.out.println("  Retrieved but not cited:");
				for (EmbeddingScore source : answer.uncitedSources()) {
					printSource(source);
				}
			}
		}
		printLine();
	}

	private static void printLine() {
		System.out.println("----------------------------------------------------------------------");
	}
	// One line per source, e.g. "    travel_expenses_policy.txt#5  (0.811)"
	private static void printSource(EmbeddingScore source) {
		System.out.println("    " + source.getChunk().getKey()
				+ "  (" + String.format("%.3f", source.getScore()) + ")");
	}
}
	

