package com.example.documentqa;

public class VectorMath {
	public static double cosineSimilarity(double[] a, double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Vectors must be the same length");
		}
		double dot = 0, normA = 0, normB = 0;
		for (int i = 0; i < a.length; i++) {
			dot+= a[i] * b[i];
			normA += a[i] * a[i];
			normB += b[i] * b[i];
			}
		if (normA == 0 || normB == 0) return 0;
		return dot / (Math.sqrt(normA) * Math.sqrt(normB));
		}
	}


