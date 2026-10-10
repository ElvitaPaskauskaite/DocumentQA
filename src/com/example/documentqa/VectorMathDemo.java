package com.example.documentqa;

public class VectorMathDemo {


	    public static void main(String[] args) {

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
	    }
	}
