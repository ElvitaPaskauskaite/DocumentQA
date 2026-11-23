package com.example.documentqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Integration test against the real SAP HANA Cloud instance.
// Skipped (not failed) when the HANA_* environment variables aren't set, e.g. on
// GitHub Actions, the same way OllamaIntegrationTest skips without Ollama.
// The free-tier instance stops every night, so start it in HANA Cloud Central first.
class HanaIntegrationTest {

    @BeforeEach
    void requireHana() {
        assumeTrue(HanaSettings.isConfigured(), "HANA_* environment variables not set, skipping");
    }

    @Test
    void connectsAsTheAppUserNotTheAdmin() throws Exception {
        HanaSettings settings = HanaSettings.fromEnvironment();

        try (Connection connection = settings.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT CURRENT_USER FROM DUMMY")) {

            result.next();
            // Least privilege: the app must never run as DBADMIN
            assertEquals("DOCUMENTQA", result.getString(1));
        }
    }

    @Test
    void hanaCosineSimilarityMatchesMyOwnImplementation() throws Exception {
        // Same 2-D example I checked by hand in the SQL Console: [1,0] vs [1,1] ≈ 0.707
        double mine = VectorMath.cosineSimilarity(new double[] {1, 0}, new double[] {1, 1});

        try (Connection connection = HanaSettings.fromEnvironment().connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COSINE_SIMILARITY(TO_REAL_VECTOR('[1,0]'), TO_REAL_VECTOR('[1,1]')) FROM DUMMY")) {

            result.next();
            // HANA stores single-precision numbers (REAL), my code uses double,
            // so they can differ slightly: compare to 6 decimal places
            assertEquals(mine, result.getDouble(1), 1e-6);
        }
    }

    @Test
    void passwordIsHiddenWhenSettingsArePrinted() {
        HanaSettings settings = HanaSettings.fromEnvironment();
        assertFalse(settings.toString().contains(settings.password()));
    }
}