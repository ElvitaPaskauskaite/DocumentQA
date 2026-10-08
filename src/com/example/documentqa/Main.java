package com.example.documentqa;

public class Main {

	public static void main(String[] args) throws Exception {
		
		DocumentLoader loader = new DocumentLoader();
		
		Document document =
				loader.load("documents/java_notes.txt");
		
		System.out.println("Title:");
		System.out.println(document.getTitle());
		
		System.out.println();
		
		System.out.println("Content:");
		System.out.println(document.getContent());

	}

}
