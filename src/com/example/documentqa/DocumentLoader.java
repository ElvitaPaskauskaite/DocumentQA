package com.example.documentqa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DocumentLoader {
	
	public Document load(String filePath) throws IOException {
		
		String content = Files.readString(Path.of(filePath));
		
		return new Document(filePath, content);
	}
	

}
