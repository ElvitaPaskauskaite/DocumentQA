package com.example.documentqa;

public interface ChatModel {
	double[] generate(String prompt) throws Exception;
}
