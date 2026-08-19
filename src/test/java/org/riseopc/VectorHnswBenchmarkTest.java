package org.riseopc;

import org.riseopc.dto.BenchmarkReportDto;
import org.riseopc.repository.KnowledgeDocRepository;
import org.riseopc.service.VectorSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("PgVector HNSW 索引 vs Flat 暴力检索性能与召回率基准测试")
public class VectorHnswBenchmarkTest {

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private KnowledgeDocRepository knowledgeDocRepository;

    @Test
    @DisplayName("执行 Flat vs HNSW 基准压测，输出延迟对比与召回率评估")
    void testHnswVsFlatBenchmark() {
        // 如果数据量少于 1000 条，则批量写入 3000 条进行测试
        if (knowledgeDocRepository.countAllDocs() < 1000) {
            vectorSearchService.generateAndBatchInsert(3000);
        }

        // 运行 30 次随机向量查询对比
        BenchmarkReportDto report = vectorSearchService.runBenchmark(30, 10);

        assertNotNull(report);
        System.out.println("==========================================================");
        System.out.println("            PgVector 检索性能基准评测报告                   ");
        System.out.println("==========================================================");
        System.out.printf("测试数据集总行数   : %d 条%n", report.getTotalDataCount());
        System.out.printf("向量维度           : %d 维%n", report.getVectorDimension());
        System.out.printf("Flat 暴力扫描平均耗时: %.2f ms (P99: %.2f ms)%n", report.getFlatAvgLatencyMs(), report.getFlatP99LatencyMs());
        System.out.printf("HNSW 索引检索平均耗时: %.2f ms (P99: %.2f ms)%n", report.getHnswAvgLatencyMs(), report.getHnswP99LatencyMs());
        System.out.printf("HNSW 检索提速倍数  : %.2f 倍%n", report.getSpeedupRatio());
        System.out.printf("Top-10 召回率 (Recall) : %.2f%%%n", report.getHnswRecallRate());
        System.out.println("评测结论: " + report.getDescription());
        System.out.println("==========================================================");

        // 验证性能指标正常返回
        assertTrue(report.getHnswRecallRate() >= 0.0, "HNSW 召回率应有效计算");
        assertTrue(report.getHnswAvgLatencyMs() >= 0.0, "HNSW 检索耗效应为有效数值");
    }
}

