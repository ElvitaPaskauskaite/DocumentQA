package com.example.documentqa;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

//A real embedder: sends text to the local ollama server and returns the vector it replies with
public class OllamaEmbeddingModel implements EmbeddingModel {
	
	// The address ollama listens on
	private static final String URL = "http://localhost:11434/api/embed";
	
	// Which model to use. Passed in so its easy to swap
	private final String modelName;
	
	// Send the HTTP requests
    private final HttpClient client = HttpClient.newHttpClient();

    // Jackson's ObjectMapper converts between Java objects and JSON text
    private final ObjectMapper mapper = new ObjectMapper();	
	
    public OllamaEmbeddingModel(String modelName) {
    	this.modelName = modelName;
    }
    
    @Override
    public double[] embed(String text) throws Exception {
        // 1. Build the JSON request body
        ObjectNode body = mapper.createObjectNode();
        body.put("model", modelName);
        body.put("input", text);

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

        // 5. Pull the vector out of the JSON reply
        JsonNode vectorNode = mapper.readTree(response.body())
                .get("embeddings")
                .get(0);

        // 6. Copy the JSON numbers into a double[]
        double[] vector = new double[vectorNode.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vectorNode.get(i).asDouble();
        }
        return vector;    	
    }

}
