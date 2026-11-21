package com.example.documentqa;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads the evaluation question set. Kept separate from the Evaluator so the
 * file format can be tested without Ollama.
 */
public class EvalQuestionLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<EvalQuestion> load(Path file) throws IOException {
        // TypeReference keeps the generic type (List<EvalQuestion>) at runtime;
        // without it Jackson would only know it's "a List" and build Maps.
        // Unknown JSON fields throw by default, which catches typos in the file.
        return mapper.readValue(file.toFile(), new TypeReference<List<EvalQuestion>>() {});
    }
}