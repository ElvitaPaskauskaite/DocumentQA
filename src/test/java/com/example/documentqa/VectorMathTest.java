package com.example.documentqa;   // same package as VectorMath, but in the test folder

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
// Tests for VectorMath.cosineSimilarity (src/main/java/.../VectorMath.java).
// No Ollama needed: plain arrays in, a number out.
class VectorMathTest {

    @Test   // JUnit finds and runs every method marked @Test
    void identicalVectorsHaveSimilarityOne() {
        double[] v = {1.0, 2.0, 3.0};

        double score = VectorMath.cosineSimilarity(v, v);

        // Third argument = tolerance. Doubles are rarely exact, so never compare them with ==
        assertEquals(1.0, score, 1e-9);
    }
    @Test
    void orthogonalVectorsHaveSimilarityZero() {
        // 90 degrees apart: nothing in common
        double score = VectorMath.cosineSimilarity(new double[] {1, 0}, new double[] {0, 1});
        assertEquals(0.0, score, 1e-9);
    }

    @Test
    void oppositeVectorsHaveSimilarityMinusOne() {
        double score = VectorMath.cosineSimilarity(new double[] {1, 2}, new double[] {-1, -2});
        assertEquals(-1.0, score, 1e-9);
    }

    @Test
    void lengthDoesNotMatterOnlyDirection() {
        // {2,4} is {1,2} doubled: same direction, so similarity is still 1
        double score = VectorMath.cosineSimilarity(new double[] {1, 2}, new double[] {2, 4});
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void knownAngleGivesExpectedValue() {
        // 45 degrees apart: cos(45°) = 1/sqrt(2) ≈ 0.7071. Checks the actual formula, not just 0/1/-1
        double score = VectorMath.cosineSimilarity(new double[] {1, 0}, new double[] {1, 1});
        assertEquals(1 / Math.sqrt(2), score, 1e-9);
    }

    @Test
    void zeroVectorReturnsZeroInsteadOfNaN() {
        // Line 14 of VectorMath: without it this would be 0/0 = NaN, which breaks ranking
        double score = VectorMath.cosineSimilarity(new double[] {0, 0}, new double[] {1, 2});
        assertEquals(0.0, score, 1e-9);
    }

    @Test
    void differentLengthsThrow() {
        // assertThrows passes only if the code inside the lambda throws this exception type
        assertThrows(IllegalArgumentException.class,
                () -> VectorMath.cosineSimilarity(new double[] {1, 2}, new double[] {1, 2, 3}));
    }
}