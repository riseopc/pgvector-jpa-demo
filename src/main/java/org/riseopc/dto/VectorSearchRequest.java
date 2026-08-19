package org.riseopc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchRequest {
    /**
     * 查询向量（float数组），如未传则可自动生成随机向量用于测试
     */
    private float[] vector;

    /**
     * 距离度量算法: COSINE (默认), L2, INNER_PRODUCT
     */
    @Builder.Default
    private String metric = "COSINE";

    /**
     * 业务分类过滤条件（可选）
     */
    private String category;

    /**
     * 最小阅读量过滤（可选）
     */
    private Integer minViewCount;

    /**
     * 最小相似度阈值过滤（0.0 ~ 1.0）
     */
    @Builder.Default
    private double minSimilarity = 0.0;

    /**
     * 返回 Top-K 数量
     */
    @Builder.Default
    private int limit = 10;
}

