package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Integration test: calls the real Ollama server instead of a test double.
// If Ollama is not reachable (for example on GitHub Actions), each test is reported
// as skipped rather than failed, so the build still passes.
// Replaces checks 4 and 5 from the old ManualChecks class.
class OllamaIntegrationTest {

    private static boolean ollamaAvailable;
    private static AppConfig config;

    // Runs once: check Ollama a single time instead of before every test
    @BeforeAll
    static void checkOllama() throws Exception {
        ollamaAvailable = ollamaIsReachable();
        config = new AppConfig("application.properties");   // same model names as Main
    }

    // Runs before each test. Assuming here (not in @BeforeAll) means each test
    // is reported as skipped, so the report shows "Skipped: 3" instead of "Tests run: 0"
    @BeforeEach
    void requireOllama() {
        assumeTrue(ollamaAvailable, "Ollama is not running at localhost:11434, skipping");
    }

    // Quick check with a 1-second timeout, so the test run doesn't hang when Ollama is closed
    private static boolean ollamaIsReachable() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:11434/api/tags"))
                    .timeout(Duration.ofSeconds(1))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void embeddingHasTheExpectedNumberOfDimensions() throws Exception {
        EmbeddingModel model = new OllamaEmbeddingModel(config.getEmbeddingModel());

        // nomic-embed-text returns 768 numbers per text
        assertEquals(768, model.embed("hotel limit").length);
    }

    @Test
    void relatedTextsScoreHigherThanUnrelatedTexts() throws Exception {
        EmbeddingModel model = new OllamaEmbeddingModel(config.getEmbeddingModel());

        double[] hotel = model.embed("What is the hotel limit for business travel?");
        double[] accommodation = model.embed("Accommodation costs are reimbursed up to a nightly maximum.");
        double[] weather = model.embed("The weather in Paris is sunny today.");

        double related = VectorMath.cosineSimilarity(hotel, accommodation);
        double unrelated = VectorMath.cosineSimilarity(hotel, weather);

        // Only the ordering is checked; exact scores depend on the model version
        assertTrue(related > unrelated, "related " + related + " should beat unrelated " + unrelated);
    }

    @Test
    void chatModelReturnsANonEmptyReply() throws Exception {
        ChatModel chat = new OllamaChatModel(config.getChatModel());

        String reply = chat.generate("Reply with one short sentence: what is a travel policy?");

        // LLM wording varies between runs, so only check that a reply came back
        assertFalse(reply.isBlank());
    }
}
