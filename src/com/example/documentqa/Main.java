package com.example.documentqa;

import java.util.List;

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
		

	}

}
