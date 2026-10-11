package com.example.documentqa;

public interface EmbeddingModel {
	double[] embed(String text) throws Exception;
	
}
