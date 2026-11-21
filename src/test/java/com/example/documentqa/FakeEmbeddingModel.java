package com.example.documentqa;

import java.util.HashMap;
import java.util.Map;

// Test stand-in for OllamaEmbeddingModel: returns vectors the test chooses,
// with no network. Implements the same interface, so services can't tell the difference.
class FakeEmbeddingModel implements EmbeddingModel {

    private final Map<String, double[]> vectors = new HashMap<>();
    private int calls = 0;

    // Tell the fake which vector to return for a text. Returns this so calls can be chained
    FakeEmbeddingModel with(String text, double... vector) {
        vectors.put(text, vector);
        return this;
    }

    @Override
    public double[] embed(String text) {
        calls++;
        double[] vector = vectors.get(text);
        // Fail loudly if a test forgot to set up a text, instead of returning something random
        if (vector == null) {
            throw new IllegalArgumentException("FakeEmbeddingModel has no vector for: " + text);
        }
        return vector;
    }

    // How many times embed() was called
    int getCalls() {
        return calls;
    }
}
