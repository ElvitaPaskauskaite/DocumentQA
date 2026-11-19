package com.example.documentqa;

import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;

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
		TextSplitter splitter = new TextSplitter();
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

		// Each chunk is turned into a vector once, using the Ollama embedding model
		EmbeddingModel model = new OllamaEmbeddingModel(config.getEmbeddingModel());
		SemanticSearchService semantic = new SemanticSearchService(model, new InMemoryVectorStore());
		semantic.index(chunks);

		// Top-K chunks, dropping anything below the minimum score (from application.properties)
		List<EmbeddingScore> semanticResults = semantic.search(query, config.getTopK(), config.getMinScore());

		if (semanticResults.isEmpty()) {
			System.out.println("  No relevant chunks found.");
		} else {
			System.out.println("  Top " + semanticResults.size() + " most relevant chunks:");
			for (EmbeddingScore result : semanticResults) {
				System.out.println("\n  [Chunk " + result.getChunk().getKey() + "]  score: "
						+ String.format("%.3f", result.getScore()));
				System.out.println("  " + result.getChunk().getContent());
			}
		}

		// Version 3: the LLM writes the answer
		System.out.println();
		System.out.println("STEP 4: Generating an answer (Version 3: retrieved chunks + LLM)");
		printLine();

		PromptBuilder promptBuilder = new PromptBuilder();
		ChatModel chat = new OllamaChatModel(config.getChatModel());
		AnswerService answerService = new AnswerService(semantic, promptBuilder, chat, config.getTopK(), config.getMinScore());

		System.out.println("  Thinking... (this can take a few seconds)");
		String answer = answerService.answer(query);

		System.out.println();
		printLine();
		System.out.println("  QUESTION: " + query);
		System.out.println("  ANSWER:   " + answer);
		printLine();
	}

	private static void printLine() {
		System.out.println("----------------------------------------------------------------------");
	}
}
	

