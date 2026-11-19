package com.example.documentqa;

import java.util.List;

// A scratchpad for checking each piece of the project by hand.
// Run it to see every check in the console. Phase 3 will replace this with JUnit tests.
public class ManualChecks {

	public static void main(String[] args) throws Exception {

		checkVectorMath();               // Check 1: needs nothing running
		checkBagOfWords();               // Check 2: needs nothing running
		checkPromptBuilder();            // Check 3: needs nothing running
		checkOllamaEmbeddings();         // Check 4: needs the Ollama app open
		checkOllamaChat();               // Check 5: needs the Ollama app open
	}

	// ---------------------------------------------------------------
	// CHECK 1: VectorMath.cosineSimilarity
	// Tests the maths on simple vectors where the right answer is known.
	// ---------------------------------------------------------------
	private static void checkVectorMath() {
		printTitle("CHECK 1: VectorMath (cosine similarity)");

		// Same direction, different length -> expect 1.0
		double[] a = {1, 2};
		double[] b = {2, 4};
		System.out.println("Same direction (expect 1.0):  " + VectorMath.cosineSimilarity(a, b));

		// Perpendicular, nothing in common -> expect 0.0
		double[] c = {1, 0};
		double[] d = {0, 1};
		System.out.println("Perpendicular  (expect 0.0):  " + VectorMath.cosineSimilarity(c, d));

		// Opposite direction -> expect -1.0
		double[] e = {1, 2};
		double[] f = {-1, -2};
		System.out.println("Opposite       (expect -1.0): " + VectorMath.cosineSimilarity(e, f));

		// Zero vector -> expect 0.0 (handled by a guard in VectorMath)
		double[] zero = {0, 0};
		System.out.println("Zero vector    (expect 0.0):  " + VectorMath.cosineSimilarity(a, zero));

		// Different lengths -> expect an exception
		try {
			VectorMath.cosineSimilarity(new double[]{1, 2}, new double[]{1, 2, 3});
			System.out.println("Different lengths: FAIL, no exception was thrown");
		} catch (IllegalArgumentException ex) {
			System.out.println("Different lengths: PASS, threw exception -> " + ex.getMessage());
		}
	}

	// ---------------------------------------------------------------
	// CHECK 2: BagOfWordsEmbeddingModel
	// Tests the word-counting embedder. No Ollama needed.
	// ---------------------------------------------------------------
	private static void checkBagOfWords() throws Exception {
		printTitle("CHECK 2: BagOfWordsEmbeddingModel (word counts)");

		// Two small chunks, as if the splitter had produced them
		List<TextChunk> chunks = List.of(
				new TextChunk("demo.txt", 1, "java is fun"),
				new TextChunk("demo.txt", 2, "java is hard"));

		// The vocabulary is built from these chunks
		EmbeddingModel model = new BagOfWordsEmbeddingModel(chunks);
		double[] vecFun = model.embed("java is fun");
		double[] vecHard = model.embed("java is hard");

		// They share 2 of 3 words, so expect about 0.667
		System.out.println("'java is fun' vs 'java is hard' (expect about 0.667): "
				+ VectorMath.cosineSimilarity(vecFun, vecHard));
	}

	// ---------------------------------------------------------------
	// CHECK 3: PromptBuilder
	// Tests the prompt layout using hand-made chunks. No Ollama needed.
	// ---------------------------------------------------------------
	private static void checkPromptBuilder() {
		printTitle("CHECK 3: PromptBuilder (prompt layout)");

		PromptBuilder builder = new PromptBuilder();

		// Two fake chunks, as if the splitter had produced them
		TextChunk chunk1 = new TextChunk("demo.txt", 1, "Inheritance lets a class reuse fields and methods of a parent class.");
		TextChunk chunk2 = new TextChunk("demo.txt", 2, "A subclass is declared with the extends keyword.");

		// Wrap each in an EmbeddingScore, as if the search had ranked them
		List<EmbeddingScore> fakeResults = List.of(
				new EmbeddingScore(chunk1, 0.8),
				new EmbeddingScore(chunk2, 0.7));

		// Expect: instructions, Context with two labelled chunks, the Question, then Answer:
		System.out.println(builder.build("what is inheritance?", fakeResults));
	}

	// ---------------------------------------------------------------
	// CHECK 4: OllamaEmbeddingModel
	// Tests real embeddings. Needs the Ollama app open and nomic-embed-text pulled.
	// ---------------------------------------------------------------
	private static void checkOllamaEmbeddings() throws Exception {
		printTitle("CHECK 4: OllamaEmbeddingModel (real embeddings)");

		EmbeddingModel ollama = new OllamaEmbeddingModel("nomic-embed-text");

		double[] inheritance = ollama.embed("inheritance in java");
		double[] polymorphism = ollama.embed("polymorphism in java");
		double[] weather = ollama.embed("the weather in Paris");

		// nomic-embed-text should give 768 numbers per text
		System.out.println("Vector length (expect 768): " + inheritance.length);

		// Related topics should score higher than unrelated ones
		System.out.println("inheritance vs polymorphism (related, should be higher):  "
				+ VectorMath.cosineSimilarity(inheritance, polymorphism));
		System.out.println("inheritance vs weather      (unrelated, should be lower): "
				+ VectorMath.cosineSimilarity(inheritance, weather));
	}

	// ---------------------------------------------------------------
	// CHECK 5: OllamaChatModel
	// Tests the LLM on its own, with no search. Needs llama3.2 pulled.
	// ---------------------------------------------------------------
	private static void checkOllamaChat() throws Exception {
		printTitle("CHECK 5: OllamaChatModel (LLM reply)");

		ChatModel chat = new OllamaChatModel("llama3.2");

		// Expect one sensible sentence about encapsulation
		System.out.println("What is encapsulation in Java?");
		System.out.println(chat.generate("In one sentence what is encapsulation in Java?"));
	}

	// Prints a blank line and a title with a line under it
	private static void printTitle(String title) {
		System.out.println();
		System.out.println(title);
		System.out.println("----------------------------------------------------------------------");
	}
}