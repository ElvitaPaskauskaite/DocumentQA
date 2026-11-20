package com.example.documentqa;

import java.util.ArrayList;
import java.util.List;

// simple keyword based search over a list of Textchunk objects
public class SearchService {

    public List<TextChunk> search(String query, List<TextChunk> chunks) {
    	
    	//create an empty list to collect chunks that match the query and return even if its empty
        List<TextChunk> results = new ArrayList<>();
        
        //ignore upper cases in questions
        String searchQuery = query.toLowerCase();
        
        //loop through each chunk in the provided list
        for (TextChunk chunk : chunks) {
        	
        	// get the text content of the current chunk and lowercase it too
            String content = chunk.getContent().toLowerCase();
            
            //check if chunk content contains the search query as a substring
            if (content.contains(searchQuery)) {
            	//if it matches add this to results list
                results.add(chunk);
            }
        }

        return results;
    }
}
