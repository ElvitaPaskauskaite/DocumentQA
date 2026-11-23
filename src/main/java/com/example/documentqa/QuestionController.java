package com.example.documentqa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// POST /questions: the REST version of what Main does for one Console question.
// It only translates between JSON and the existing classes; the answering logic
// stays in AnswerService, so the API can't drift from what was evaluated.
@RestController
public class QuestionController {

	// The JSON body the client sends: {"question": "..."}
	public record QuestionRequest(String question) {}

	// One retrieved paragraph. "cited" says whether the answer actually used it,
	// so a client can show the real sources and flag the rest, as Main does
	public record SourceResponse(String key, double score, boolean cited, String content) {}

	// What the client gets back: {"answer": "...", "sources": [...]}
	public record QuestionResponse(String answer, List<SourceResponse> sources) {

		// Converts the internal Answer into the API's JSON shape. Kept separate from
		// Answer so internal classes can change without changing what clients receive
		static QuestionResponse from(Answer answer) {
			List<SourceResponse> sources = new ArrayList<>();
			for (EmbeddingScore source : answer.citedSources()) {
				sources.add(toSource(source, true));
			}
			for (EmbeddingScore source : answer.uncitedSources()) {
				sources.add(toSource(source, false));
			}
			return new QuestionResponse(answer.text(), sources);
		}

		private static SourceResponse toSource(EmbeddingScore source, boolean cited) {
			TextChunk chunk = source.getChunk();
			return new SourceResponse(chunk.getKey(), source.getScore(), cited, chunk.getContent());
		}
	}

	private final AnswerService answerService;

	// Spring passes in the AnswerService bean from PipelineConfig
	public QuestionController(AnswerService answerService) {
		this.answerService = answerService;
	}

	@PostMapping("/questions")
	public QuestionResponse ask(@RequestBody QuestionRequest request) throws Exception {
		// Reject a missing or empty question before any work. The client made the mistake,
		// so it gets 400 Bad Request instead of a 500 that looks like a server bug
		if (request.question() == null || request.question().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question must not be blank");
		}
		return QuestionResponse.from(answerService.answer(request.question()));
	}

	// Both Ollama classes throw IOException when Ollama can't be reached or returns an error.
	// That's a failure of a service this API depends on, not of the request, so the client
	// gets 503 and knows a retry later may work. A fixed message avoids exposing internal addresses
	@ExceptionHandler(IOException.class)
	public ResponseEntity<String> ollamaUnavailable(IOException e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body("The language model service is unavailable. Try again later.");
	}
}