package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Tests for AppConfig. Each test writes its own small properties file into a
// temporary folder, except the last one, which checks the real application.properties.
class AppConfigTest {

    @TempDir
    Path tempDir;

    // Writes the given lines to a temporary properties file and loads it
    private AppConfig configWith(String content) throws IOException {
        Path file = tempDir.resolve("test.properties");
        Files.writeString(file, content);
        return new AppConfig(file.toString());
    }

    @Test
    void readsTypedValues() throws Exception {
        AppConfig config = configWith("""
                search.top-k=3
                search.min-score=0.5
                ollama.chat.model=llama3.2
                """);

        assertEquals(3, config.getTopK());
        assertEquals(0.5, config.getMinScore(), 1e-9);
        assertEquals("llama3.2", config.getChatModel());
    }

    @Test
    void trailingSpacesAreTrimmed() throws Exception {
        // A trailing space is invisible in the file and would otherwise break Integer.parseInt
        AppConfig config = configWith("search.top-k=3   \n");

        assertEquals(3, config.getTopK());
    }

    @Test
    void missingKeyGivesAClearError() throws Exception {
        AppConfig config = configWith("search.top-k=3\n");

        IllegalStateException error = assertThrows(IllegalStateException.class, config::getChatModel);

        // The message should tell the user exactly which setting to add
        assertTrue(error.getMessage().contains("ollama.chat.model"));
    }

    @Test
    void blankValueIsTreatedAsMissing() throws Exception {
        AppConfig config = configWith("ollama.chat.model=\n");

        assertThrows(IllegalStateException.class, config::getChatModel);
    }

    @Test
    void missingFileThrows() {
        assertThrows(IOException.class, () -> new AppConfig(tempDir.resolve("does-not-exist.properties").toString()));
    }

    @Test
    void knownLimitation_nonNumericValueDoesNotNameTheKey() throws Exception {
        // KNOWN LIMITATION: the NumberFormatException only says "For input string: three",
        // not which setting was wrong. If AppConfig adds the key name later, update this test.
        AppConfig config = configWith("search.top-k=three\n");

        assertThrows(NumberFormatException.class, config::getTopK);
    }

    @Test
    void realApplicationPropertiesHasEverySetting() throws Exception {
        // Maven and Eclipse both run from the project folder, so this relative path
        // is the same file Main reads. Fails if a setting is renamed or removed.
        AppConfig config = new AppConfig("application.properties");

        assertDoesNotThrow(config::getDocumentsFolder);
        assertDoesNotThrow(config::getEmbeddingModel);
        assertDoesNotThrow(config::getChatModel);
        assertDoesNotThrow(config::getTopK);
        assertDoesNotThrow(config::getMinScore);
        assertDoesNotThrow(config::getEmbeddingCacheFile);
        assertDoesNotThrow(config::getChunkMaxWords);
        assertDoesNotThrow(config::getChunkOverlapWords);
    }
}
