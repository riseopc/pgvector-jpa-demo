package org.riseopc;

import org.riseopc.service.VectorSearchService;
import org.riseopc.util.VectorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@SpringBootTest
@DisplayName("PgVector 执行计划与索引下推分析 (EXPLAIN ANALYZE)")
public class VectorIndexExplainTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VectorSearchService vectorSearchService;

    @Test
    @DisplayName("分析 HNSW 向量索引查询的底层执行计划")
    void testExplainAnalyzeHnsw() {
        vectorSearchService.generateAndBatchInsert(1000);

        float[] queryVector = VectorUtils.randomNormalizedVector(1536);
        String vectorStr = VectorUtils.toVectorString(queryVector);

        String sql = """
            EXPLAIN ANALYZE
            SELECT id, title, category, (1 - (embedding <=> ?::vector)) AS similarity
            FROM knowledge_doc
            WHERE category = 'AI技术'
            ORDER BY embedding <=> ?::vector
            LIMIT 10
            """;

        List<String> explainLines = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString(1),
                vectorStr, vectorStr
        );

        System.out.println("==========================================================");
        System.out.println("            PgVector EXPLAIN ANALYZE 执行计划输出          ");
        System.out.println("==========================================================");
        for (String line : explainLines) {
            System.out.println(line);
        }
        System.out.println("==========================================================");
    }
}

