package com.example.documentqa;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Answers GET /health so a person or a monitoring tool can check the server is running
// without asking a question (which would need Ollama and take seconds).
// It only reports that the web layer is up; it does not check Ollama.
@RestController
public class HealthController {

	// Spring converts the returned record to JSON: {"status":"UP"}
	public record HealthResponse(String status) {}

	@GetMapping("/health")
	public HealthResponse health() {
		return new HealthResponse("UP");
	}
}