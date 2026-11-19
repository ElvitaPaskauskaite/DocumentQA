package com.example.documentqa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DocumentLoader {
	
	public Document load(String filePath) throws IOException {
		
		// Turn the String into a Path object once, and reuse it
		Path path = Path.of(filePath);
		
		// Read the whole file as text
		String content = Files.readString(path);
		
		// Keep only the last part of the path: "documents/java_notes.txt" -> "java_notes.txt"
		// getFileName() returns a Path, so toString() turns it back into a String
		String title = path.getFileName().toString();
		
		return new Document(title, content);
	}
	
	// Loads every .txt file in a folder (not subfolders) as a Document
		public List<Document> loadFolder(String folderPath) throws IOException {
			
			List<Document> documents = new ArrayList<>();
			
			// Files.list opens the folder; try-with-resources closes it automatically
			try (Stream<Path> files = Files.list(Path.of(folderPath))) {
				
				List<Path> txtFiles = files
						.filter(p -> p.toString().endsWith(".txt"))  // skip .DS_Store and other files
						.sorted()                                    // same order on every run
						.toList();
				
				// A normal loop, because load() throws IOException,
				// which a lambda inside the stream can't throw
				for (Path p : txtFiles) {
					documents.add(load(p.toString()));  // reuses load(), so titles are short names
				}
			}
			
			return documents;
		}
	

}
