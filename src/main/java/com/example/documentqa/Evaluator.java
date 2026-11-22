package com.example.documentqa;

import java.nio.file.Path;
import java.util.ArrayList;
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

        // Same preparation as Main, so the evaluation measures the real system
        List<Document> documents = new DocumentLoader().loadFolder(config.getDocumentsFolder());
        TextSplitter splitter = new TextSplitter(config.getChunkMaxWords(), config.getChunkOverlapWords());
        List<TextChunk> chunks = new ArrayList<>();
        for (Document document : documents) {
            chunks.addAll(splitter.split(document));
        }

        // Optional second argument "bag-of-words" swaps in the word-count baseline,
        // to measure how much the real embedding model adds. Both implement
        // EmbeddingModel, so nothing else in the pipeline changes.
        boolean bagOfWords = args.length > 1 && args[1].equals("bag-of-words");
        CachingEmbeddingModel cache = new CachingEmbeddingModel(
                new OllamaEmbeddingModel(config.getEmbeddingModel()),
                config.getEmbeddingModel(), Path.of(config.getEmbeddingCacheFile()));
        EmbeddingModel embedder = bagOfWords ? new BagOfWordsEmbeddingModel(chunks) : cache;
        String embedderName = bagOfWords ? "bag-of-words" : config.getEmbeddingModel();
        SemanticSearchService search = new SemanticSearchService(embedder, new InMemoryVectorStore());
        search.index(chunks);

        AnswerService answerService = new AnswerService(search, new PromptBuilder(),
                new OllamaChatModel(config.getChatModel()), config.getTopK(), config.getMinScore());

        // The question file can be given as a program argument, so the held-out
        // set (eval/heldout.json) is scored by exactly the same code
        String questionFile = args.length > 0 ? args[0] : "eval/questions.json";
        List<EvalQuestion> questions = new EvalQuestionLoader().load(Path.of(questionFile));

        // Printed with every table: chunk keys and scores only mean something
        // for these exact settings, so a table without them can't be compared
        System.out.println("Questions: " + questionFile + " (" + questions.size() + ")");
        System.out.println("Embedding model: " + embedderName
                + " | Chat model: " + config.getChatModel() + " (temperature 0)");
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
        cache.save();

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
