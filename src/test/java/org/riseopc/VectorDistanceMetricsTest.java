package org.riseopc;

import org.riseopc.dto.KnowledgeDocSearchResultProjection;
import org.riseopc.entity.KnowledgeDocEntity;
import org.riseopc.repository.KnowledgeDocRepository;
import org.riseopc.util.VectorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("PgVector 距离度量算法一致性与准确度测试")
public class VectorDistanceMetricsTest {

    @Autowired
    private KnowledgeDocRepository knowledgeDocRepository;

    private static final int DIMENSION = 1536;

    @BeforeEach
    void setUp() {
        knowledgeDocRepository.truncateTable();
    }

    @Test
    @DisplayName("测试 1：余弦相似度 (<=>) 计算结果与 Java 本地数学理论值一致性")
    void testCosineSimilarityAccuracy() {
        // 创建基准向量和目标向量
        float[] baseVector = VectorUtils.randomNormalizedVector(DIMENSION);
        float[] targetVector = VectorUtils.randomNormalizedVector(DIMENSION);

        KnowledgeDocEntity doc = KnowledgeDocEntity.builder()
                .title("余弦相似度精度测试文档")
                .category("AI技术")
                .content("用于测试余弦距离与Java本地计算一致性")
                .embedding(targetVector)
                .build();
        knowledgeDocRepository.save(doc);

        // 使用 PgVector 的 <=> 进行检索
        String baseVectorStr = VectorUtils.toVectorString(baseVector);
        List<KnowledgeDocSearchResultProjection> results = knowledgeDocRepository.searchByCosine(baseVectorStr, 1);

        assertFalse(results.isEmpty());
        KnowledgeDocSearchResultProjection result = results.get(0);

        // Java 本地计算的理论余弦相似度
        double expectedSimilarity = VectorUtils.cosineSimilarity(baseVector, targetVector);
        double actualSimilarity = result.getSimilarity();

        System.out.printf("PgVector 余弦相似度: %.6f, Java 本地理论计算值: %.6f, 误差: %.8f%n",
                actualSimilarity, expectedSimilarity, Math.abs(actualSimilarity - expectedSimilarity));

        // 验证浮点精度误差在 1e-4 以内
        assertEquals(expectedSimilarity, actualSimilarity, 1e-4);
    }

    @Test
    @DisplayName("测试 2：欧氏距离 (<->) 计算结果与 Java 本地理论值一致性")
    void testEuclideanDistanceAccuracy() {
        float[] baseVector = VectorUtils.randomVector(DIMENSION);
        float[] targetVector = VectorUtils.randomVector(DIMENSION);

        KnowledgeDocEntity doc = KnowledgeDocEntity.builder()
                .title("欧氏距离精度测试文档")
                .category("数学计算")
                .content("用于测试L2距离与Java本地计算一致性")
                .embedding(targetVector)
                .build();
        knowledgeDocRepository.save(doc);

        String baseVectorStr = VectorUtils.toVectorString(baseVector);
        List<KnowledgeDocSearchResultProjection> results = knowledgeDocRepository.searchByL2Distance(baseVectorStr, 1);

        assertFalse(results.isEmpty());
        KnowledgeDocSearchResultProjection result = results.get(0);

        double expectedDistance = VectorUtils.euclideanDistance(baseVector, targetVector);
        double actualDistance = result.getDistance();

        System.out.printf("PgVector 欧氏距离 (<->): %.6f, Java 本地理论计算值: %.6f%n",
                actualDistance, expectedDistance);

        assertEquals(expectedDistance, actualDistance, 1e-3);
    }

    @Test
    @DisplayName("测试 3：完全相同向量的余弦相似度应为 1.0，距离为 0.0")
    void testIdenticalVectorCosine() {
        float[] vector = VectorUtils.randomNormalizedVector(DIMENSION);

        KnowledgeDocEntity doc = KnowledgeDocEntity.builder()
                .title("完全匹配向量测试")
                .category("AI技术")
                .embedding(vector)
                .build();
        knowledgeDocRepository.save(doc);

        String vectorStr = VectorUtils.toVectorString(vector);
        List<KnowledgeDocSearchResultProjection> results = knowledgeDocRepository.searchByCosine(vectorStr, 1);

        assertFalse(results.isEmpty());
        assertEquals(1.0, results.get(0).getSimilarity(), 1e-5);
        assertEquals(0.0, results.get(0).getDistance(), 1e-5);
    }
}

