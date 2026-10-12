package com.example.documentqa;

import java.util.List;

public class VectorMathDemo {


	    public static void main(String[] args) throws Exception {

	        // Same direction, different length -> expect 1.0
	        double[] a = {1, 2};
	        double[] b = {2, 4};
	        System.out.println("Same direction: " + VectorMath.cosineSimilarity(a, b));

	        // Perpendicular -> expect 0.0
	        double[] c = {1, 0};
	        double[] d = {0, 1};
	        System.out.println("Perpendicular: " + VectorMath.cosineSimilarity(c, d));

	        // Opposite direction -> expect -1.0
	        double[] e = {1, 2};
	        double[] f = {-1, -2};
	        System.out.println("Opposite: " + VectorMath.cosineSimilarity(e, f));

	        // Zero vector -> expect 0.0 (handled by your guard)
	        double[] zero = {0, 0};
	        System.out.println("Zero vector: " + VectorMath.cosineSimilarity(a, zero));

	        // Mismatched lengths -> expect an exception
	        try {
	            VectorMath.cosineSimilarity(new double[]{1, 2}, new double[]{1, 2, 3});
	            System.out.println("Mismatched lengths: no exception (this is a bug)");
	        } catch (IllegalArgumentException ex) {
	            System.out.println("Mismatched lengths: threw exception as expected -> " + ex.getMessage());
	        }
	        
	        System.out.println("\nBagOfWords Embedding model test ----------------\n");
	        List<TextChunk> chunks = List.of(
	        	    new TextChunk(1, "java is fun"),
	        	    new TextChunk(2, "java is hard"));
	        
	        EmbeddingModel model = new BagOfWordsEmbeddingModel(chunks);
	        double[] vecFun = model.embed("java is fun");
	        double[] vecHard = model.embed("java is hard");
	        System.out.println(VectorMath.cosineSimilarity(vecFun, vecHard)); // expected value 0.667
	        
	        System.out.println("\nOllama Embedding model Demo --------------------\n");
	        EmbeddingModel ollama = new OllamaEmbeddingModel("nomic-embed-text");

	        double[] inheritance = ollama.embed("inheritance in java");
	        double[] polymorphism = ollama.embed("polymorphism in java");
	        double[] weather = ollama.embed("the weather in Paris");

	        System.out.println("Vector length: " + inheritance.length);  // expect 768
	        System.out.println("inheritance vs polymorphism: " + VectorMath.cosineSimilarity(inheritance, polymorphism));
	        System.out.println("inheritance vs weather:     " + VectorMath.cosineSimilarity(inheritance, weather));
	    }
	    
	    
	}
