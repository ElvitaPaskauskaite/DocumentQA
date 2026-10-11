package com.example.documentqa;

public class EmbeddingScore {
	private final TextChunk chunk;
	private final double score;
	
	public EmbeddingScore(TextChunk chunk, double score) {
		this.chunk = chunk;
		this.score = score;
	}
	
	public TextChunk getChunk() { return chunk; }
	public double getSscore() { return score; }

}
