package org.riseopc;

import org.riseopc.service.VectorSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("PgVector 批量向量写入吞吐量测试")
public class BatchInsertPerformanceTest {

    @Autowired
    private VectorSearchService vectorSearchService;

    @Test
    @DisplayName("测试：批量灌入 5,000 条 1536 维向量数据并统计写入吞吐")
    void testBatchInsertThroughput() {
        int insertCount = 5000;
        long start = System.currentTimeMillis();
        long total = vectorSearchService.generateAndBatchInsert(insertCount);
        long elapsed = System.currentTimeMillis() - start;

        double qps = (insertCount * 1000.0) / Math.max(1, elapsed);
        System.out.println("==========================================================");
        System.out.printf("成功写入 %d 条 1536 维向量, 总耗时: %d ms, 写入 QPS: %.2f 条/秒%n",
                insertCount, elapsed, qps);
        System.out.printf("数据库当前总数据量: %d 条%n", total);
        System.out.println("==========================================================");

        assertTrue(total >= insertCount);
    }
}

