package com.example.documentqa;

import java.nio.file.Path;
import java.util.List;

/**
 * Runs every question in eval/questions.json through the real pipeline and
 * prints a results table. Separate from Main so a measurement never depends
 * on typing questions by hand, and every run asks exactly the same questions.
 * Needs Ollama running.
 */
public class Evaluator {

    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig("application.properties");

        // Optional second argument "bag-of-words" swaps in the word-count baseline
        boolean bagOfWords = args.length > 1 && args[1].equals("bag-of-words");

        // Same set-up as Main and the REST API, so the evaluation measures the real system
        Pipeline pipeline = Pipeline.build(config, bagOfWords);
        List<TextChunk> chunks = pipeline.chunks();
        String embedderName = pipeline.embedderName();
        AnswerService answerService = pipeline.answerService();

        // The question file can be given as a program argument, so the held-out
        // set (eval/heldout.json) is scored by exactly the same code
        String questionFile = args.length > 0 ? args[0] : "eval/questions.json";
        List<EvalQuestion> questions = new EvalQuestionLoader().load(Path.of(questionFile));

        // Printed with every table: chunk keys and scores only mean something
        // for these exact settings, so a table without them can't be compared
        System.out.println("Questions: " + questionFile + " (" + questions.size() + ")");
        System.out.println("Embedding model: " + embedderName
                + " | Chat model: " + config.getChatModel() + " (temperature 0) | Vector store: " + pipeline.storeName());
        System.out.println("Chunks: " + chunks.size() + " (max " + config.getChunkMaxWords()
                + " words, overlap " + config.getChunkOverlapWords() + ") | top-k: "
                + config.getTopK() + " | min-score: " + config.getMinScore());
        System.out.println();
        System.out.printf("%-10s %-10s %-6s %-6s %-8s%n", "id", "retrieved", "cited", "facts", "refused");

        int answerable = 0, retrieved = 0, cited = 0, correct = 0, wrongRefusals = 0;
        int unanswerable = 0, correctRefusals = 0;

        for (EvalQuestion question : questions) {
            Answer answer = answerService.answer(question.question());
            boolean refused = AnswerScorer.refused(answer);

            if (question.answerable()) {
                boolean hit = AnswerScorer.retrievedExpectedSource(question, answer);
                boolean citedExpected = AnswerScorer.citedExpectedSource(question, answer);
                // A refusal can't count as correct, even if it happens to contain a fact word
                boolean factsCorrect = AnswerScorer.containsExpectedFacts(question, answer) && !refused;

                answerable++;
                if (hit) retrieved++;
                if (citedExpected) cited++;
                if (factsCorrect) correct++;
                if (refused) wrongRefusals++;

                System.out.printf("%-10s %-10s %-6s %-6s %-8s%n", question.id(),
                        yesNo(hit), yesNo(citedExpected), yesNo(factsCorrect), yesNo(refused));
            } else {
                unanswerable++;
                if (refused) correctRefusals++;

                // Retrieval, citation and facts don't apply when there is nothing to find
                System.out.printf("%-10s %-10s %-6s %-6s %-8s%n", question.id(), "-", "-", "-", yesNo(refused));
            }
            // Full answer for the manual check, because the fact match is crude
            System.out.println("           " + answer.text());
        }

        // The question embeddings were new, so keep them for the next run
        pipeline.cache().save();

        System.out.println();
        System.out.println("Retrieved expected chunk (top " + config.getTopK() + "): " + retrieved + "/" + answerable);
        System.out.println("Cited expected chunk:               " + cited + "/" + answerable);
        System.out.println("Answer contains expected facts:     " + correct + "/" + answerable);
        System.out.println("Refused an answerable question:     " + wrongRefusals + "/" + answerable);
        System.out.println("Refused an unanswerable question:   " + correctRefusals + "/" + unanswerable);
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "NO";
    }
}
