package com.example.documentqa;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class OllamaChatModel implements ChatModel {
	// The address ollama listens on
	private static final String URL = "http://localhost:11434/api/chat";
	
	// Which model to use. Passed in so its easy to swap
	private final String modelName;
	
	// Send the HTTP requests
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();	
	
    public OllamaChatModel(String modelName) {
    	this.modelName = modelName;
    }
    
    
    @Override
    public String generate(String prompt) throws Exception {
        // 1. Build the JSON request body
        ObjectNode body = mapper.createObjectNode();
        body.put("model", modelName);
        body.put("stream", false);
        ArrayNode messages = body.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        
        // 2. Wrap it in an HTTP POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        // 3. Send it and wait for the reply
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (ConnectException e) {
            throw new IOException("Could not reach Ollama at " + URL
                    + ". Is the Ollama app running?", e);
        }

        // 4. Check the reply was successful
        if (response.statusCode() != 200) {
            throw new IOException("Ollama returned " + response.statusCode()
                    + ": " + response.body());
        }

		return mapper.readTree(response.body())
				.get("message")
				.get("content")
				.asText();
        }  
}
