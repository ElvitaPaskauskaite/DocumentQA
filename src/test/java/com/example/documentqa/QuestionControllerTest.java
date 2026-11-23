package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// Tests the HTTP side of POST /questions: status codes and the JSON shape.
// MockMvc sends requests straight to the controller without starting Tomcat, and the
// pipeline uses test doubles, so no Ollama is needed and these run in CI.
// Scores for the hotel question {1,0}: hotel = 1.0, quotes = 0.0 (below min-score 0.5)
class QuestionControllerTest {

    private SemanticSearchService search;

    @BeforeEach
    void setUp() throws Exception {
        FakeEmbeddingModel embeddings = new FakeEmbeddingModel()
                .with("Hotel limit is €215.", 1, 0)
                .with("Two quotes over €1,000.", 0, 1)
                .with("What is the hotel limit?", 1, 0);

        search = new SemanticSearchService(embeddings, new InMemoryVectorStore());
        search.index(List.of(
                new TextChunk("travel.txt", 5, "Hotel limit is €215."),
                new TextChunk("procurement.txt", 4, "Two quotes over €1,000.")));
    }

    // Sends {"question": ...} or any raw JSON body to a controller whose LLM is the given chat model
    private ResultActions send(ChatModel chat, String jsonBody) throws Exception {
        AnswerService service = new AnswerService(search, new PromptBuilder(), chat, 3, 0.5);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new QuestionController(service)).build();
        return mvc.perform(post("/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody));
    }

    @Test
    void answerComesBackWithItsCitedSource() throws Exception {
    	String json = send(new FakeChatModel("The limit is €215 [travel.txt#5]."),
                        "{\"question\":\"What is the hotel limit?\"}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(json.contains("\"answer\":\"The limit is €215 [travel.txt#5].\""));
        assertTrue(json.contains("\"key\":\"travel.txt#5\""));
        assertTrue(json.contains("\"cited\":true"));
        // Scored 0.0, below min-score, so it must not be listed as a source
        assertFalse(json.contains("procurement.txt#4"));
    }

    @Test
    void blankQuestionIsABadRequestAndNothingIsCalled() throws Exception {
        FakeChatModel chat = new FakeChatModel("should not be used");

        send(chat, "{\"question\":\"   \"}").andExpect(status().isBadRequest());

        // Rejected before search: the fake embedder would have thrown for an unknown text
        assertEquals(0, chat.getCalls());
    }

    @Test
    void missingQuestionFieldIsABadRequest() throws Exception {
    	send(new FakeChatModel("should not be used"), "{}").andExpect(status().isBadRequest());
    }

    @Test
    void ollamaUnreachableIsServiceUnavailable() throws Exception {
        // Stands in for OllamaChatModel when Ollama isn't running
        ChatModel down = prompt -> { throw new IOException("Could not reach Ollama"); };

        send(down, "{\"question\":\"What is the hotel limit?\"}")
                .andExpect(status().isServiceUnavailable());
    }
}