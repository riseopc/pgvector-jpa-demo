package org.riseopc.util;

import java.util.Arrays;
import java.util.Random;

/**
 * 向量数学计算与模拟工具类
 */
public class VectorUtils {

    private static final Random RANDOM = new Random(42); // 固定随机种子以便可复现

    /**
     * 生成指定维度的随机浮点向量
     */
    public static float[] randomVector(int dimension) {
        float[] vector = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            vector[i] = RANDOM.nextFloat() * 2.0f - 1.0f; // [-1.0, 1.0]
        }
        return vector;
    }

    /**
     * 生成已归一化（L2 单位长度）的随机向量
     */
    public static float[] randomNormalizedVector(int dimension) {
        float[] vector = randomVector(dimension);
        return normalize(vector);
    }

    /**
     * 向量 L2 归一化
     */
    public static float[] normalize(float[] v) {
        double sumSquares = 0.0;
        for (float val : v) {
            sumSquares += val * val;
        }
        double norm = Math.sqrt(sumSquares);
        if (norm == 0.0) {
            return v;
        }
        float[] normalized = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            normalized[i] = (float) (v[i] / norm);
        }
        return normalized;
    }

    /**
     * 基于 Java 本地计算两个向量的余弦相似度 (Cosine Similarity: range [-1.0, 1.0])
     */
    public static double cosineSimilarity(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 基于 Java 本地计算两个向量的欧氏距离 (L2 Distance)
     */
    public static double euclideanDistance(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }
        double sum = 0.0;
        for (int i = 0; i < v1.length; i++) {
            double diff = v1[i] - v2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * 将 float[] 转换为 PostgreSQL vector 字符串表示形式: "[0.123, -0.456, ...]"
     */
    public static String toVectorString(float[] vector) {
        if (vector == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 将 PostgreSQL vector 字符串 "[0.1, 0.2]" 解析为 float[]
     */
    public static float[] parseVectorString(String vectorStr) {
        if (vectorStr == null || vectorStr.isBlank()) {
            return new float[0];
        }
        String clean = vectorStr.replace("[", "").replace("]", "").trim();
        if (clean.isEmpty()) {
            return new float[0];
        }
        String[] parts = clean.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}

