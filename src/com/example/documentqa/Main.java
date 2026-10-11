package com.example.documentqa;

import java.util.List;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) throws Exception {
		
		//load the document
		DocumentLoader loader = new DocumentLoader();
		
		Document document =
				loader.load("documents/java_notes.txt");
		
		System.out.println("Document loaded!");
		System.out.println("Title:" + document.getTitle());
		
		System.out.println("\n--- Text Chunks ---");
		
		// Split the document
		TextSplitter splitter = new TextSplitter();
		
		List<TextChunk> chunks =
				splitter.split(document);
		
		
		// ask the user for a search term and run the search 
		Scanner scanner = new Scanner(System.in);
		System.out.print("\nEnter a question about Java: ");
		String query = scanner.nextLine();
		scanner.close();
		
		
		// Run Semantic search: the vocabulary is built from the chunks, then each chunk is embedded once (Version 2)
		EmbeddingModel model = new BagOfWordsEmbeddingModel(chunks);
		SemanticSearchService semantic = new SemanticSearchService(model);
		semantic.index(chunks);

		List<EmbeddingScore> semanticResults = semantic.search(query, 3);

		System.out.println("\nSemantic search results for: " + query);
		for (EmbeddingScore result : semanticResults) {
		    System.out.println("\nChunk " + result.getChunk().getId()
		            + " (score: " + String.format("%.3f", result.getScore()) + ")");
		    System.out.println(result.getChunk().getContent());
		}
		
		
		
		
		
	}

}
