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
		
		// Display the chunks
		
		for (TextChunk chunk : chunks) {
			
			System.out.println("\nChunk" + chunk.getId());
			System.out.println(chunk.getContent());
		}
		
		// ask the user for a search term and run the search 
		Scanner scanner = new Scanner(System.in);
		System.out.print("\nEnter a search term: ");
		String query = scanner.nextLine();
		scanner.close();
		
		// run the search
		SearchService searchService = new SearchService();
		List<TextChunk> results = searchService.search(query, chunks);
		
		System.out.println("\n Search results for " + query);
		if (results.isEmpty()) {
			System.out.println("No chunks matched.");
		} else {
			System.out.println("Found " + results.size() + "matchig chunk(s)");
			
			for (TextChunk chunk : results) {
				System.out.println("\nChunk" + chunk.getId());
				System.out.println(chunk.getContent());
			}
		}
		
		
		
		
		
	}

}
