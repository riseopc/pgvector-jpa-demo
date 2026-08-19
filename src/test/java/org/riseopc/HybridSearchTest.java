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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("PgVector 混合过滤检索测试 (标量条件 + 向量相似度)")
public class HybridSearchTest {

    @Autowired
    private KnowledgeDocRepository knowledgeDocRepository;

    private static final int DIMENSION = 1536;

    @BeforeEach
    void setUp() {
        knowledgeDocRepository.truncateTable();

        // 插入不同分类和浏览量的测试数据
        for (int i = 1; i <= 20; i++) {
            String category = (i % 2 == 0) ? "AI技术" : "后端开发";
            int viewCount = i * 100;
            KnowledgeDocEntity doc = KnowledgeDocEntity.builder()
                    .title("混合检索样本 #" + i)
                    .category(category)
                    .viewCount(viewCount)
                    .content("样本内容 " + i)
                    .embedding(VectorUtils.randomNormalizedVector(DIMENSION))
                    .build();
            knowledgeDocRepository.save(doc);
        }
    }

    @Test
    @DisplayName("测试：按分类过滤 + 浏览量过滤 + 向量相似度排序")
    void testHybridFilterAndSort() {
        float[] queryVector = VectorUtils.randomNormalizedVector(DIMENSION);
        String vectorStr = VectorUtils.toVectorString(queryVector);

        // 过滤 category = "AI技术" 且 view_count >= 1000
        List<KnowledgeDocSearchResultProjection> results = knowledgeDocRepository.searchHybrid(
                vectorStr,
                "AI技术",
                1000,
                0.0,
                5
        );

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 5);

        for (KnowledgeDocSearchResultProjection res : results) {
            assertEquals("AI技术", res.getCategory());
            assertTrue(res.getViewCount() >= 1000);
            assertNotNull(res.getSimilarity());
            System.out.printf("匹配文档: ID=%d, Title=%s, Category=%s, Views=%d, 相似度=%.4f%n",
                    res.getId(), res.getTitle(), res.getCategory(), res.getViewCount(), res.getSimilarity());
        }
    }
}

