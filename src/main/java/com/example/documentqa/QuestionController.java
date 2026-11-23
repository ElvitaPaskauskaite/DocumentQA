package com.example.documentqa;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
		return QuestionResponse.from(answerService.answer(request.question()));
	}
}