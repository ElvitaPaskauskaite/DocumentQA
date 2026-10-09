package com.example.documentqa;

import java.util.ArrayList;
import java.util.List;

public class TextSplitter {

    public List<TextChunk> split(Document document) {
    	
    	// Create an empty list where each textChunk is stored
        List<TextChunk> chunks = new ArrayList<>();
         
        // Get all the text from the Document and split it into sections looking for blank lines
        String[] sections =
                document.getContent().split("\\n\\s*\\n");
        
        // Give each chunk a unique id
        int id = 1;
        
        // Go through each section of the document one at a time
        for (String section : sections) {
        	// Dont create new sections for empty sections
            if (!section.trim().isEmpty()) {
            	// Create a textchunk containing id and text from the section
                TextChunk chunk =
                        new TextChunk(id, section.trim());
                // add new chunk to list
                chunks.add(chunk);
                // Increment id
                id++;
            }
        }
        
        // Return the completed list of chunks
        return chunks;
    }
}
