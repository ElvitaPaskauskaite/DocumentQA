package com.example.documentqa;

import java.util.List;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) throws Exception {

		// Title banner
		printLine();
		System.out.println("  DOCUMENT Q&A SYSTEM");
		System.out.println("  Retrieval-Augmented Generation in Java");
		printLine();

		// Version 1: load the document and split it into chunks
		System.out.println();
		System.out.println("STEP 1: Loading and splitting the document (Version 1)");
		printLine();

		DocumentLoader loader = new DocumentLoader();
		Document document = loader.load("documents/java_notes.txt");

		TextSplitter splitter = new TextSplitter();
		List<TextChunk> chunks = splitter.split(document);

		System.out.println("  File   : " + document.getTitle());
		System.out.println("  Chunks : " + chunks.size() + " paragraphs");

		// Ask the question
		Scanner scanner = new Scanner(System.in);
		System.out.print("\n  Enter a question about Java: ");
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
		EmbeddingModel model = new OllamaEmbeddingModel("nomic-embed-text");
		SemanticSearchService semantic = new SemanticSearchService(model);
		semantic.index(chunks);

		// Top 3 chunks, dropping anything scoring below 0.5
		List<EmbeddingScore> semanticResults = semantic.search(query, 3, 0.5);

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
		ChatModel chat = new OllamaChatModel("llama3.2");
		AnswerService answerService = new AnswerService(semantic, promptBuilder, chat);

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
	

