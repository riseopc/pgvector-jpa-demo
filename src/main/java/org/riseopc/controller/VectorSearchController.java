package org.riseopc.controller;

import org.riseopc.dto.BenchmarkReportDto;
import org.riseopc.dto.SearchResultDto;
import org.riseopc.dto.VectorSearchRequest;
import org.riseopc.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * PgVector 向量化检索 REST 控制器接口
 */
@RestController
@RequestMapping("/api/vectors")
@RequiredArgsConstructor
public class VectorSearchController {

    private final VectorSearchService vectorSearchService;

    /**
     * 1. 向量相似度检索（支持 Cosine, L2, InnerProduct 及混合业务过滤）
     */
    @PostMapping("/search")
    public ResponseEntity<List<SearchResultDto>> search(@RequestBody(required = false) VectorSearchRequest request) {
        if (request == null) {
            request = new VectorSearchRequest();
        }
        return ResponseEntity.ok(vectorSearchService.search(request));
    }

    /**
     * 2. 批量灌入模拟向量测试数据
     */
    @PostMapping("/mock-batch")
    public ResponseEntity<Map<String, Object>> mockBatch(@RequestParam(defaultValue = "1000") int count) {
        long total = vectorSearchService.generateAndBatchInsert(count);
        return ResponseEntity.ok(Map.of(
                "message", "批量导入成功",
                "insertedCount", count,
                "totalCount", total
        ));
    }

    /**
     * 3. 运行 Flat 暴力搜索 vs HNSW 索引检索性能基准压测
     */
    @GetMapping("/benchmark")
    public ResponseEntity<BenchmarkReportDto> runBenchmark(
            @RequestParam(defaultValue = "20") int iterations,
            @RequestParam(defaultValue = "10") int topK
    ) {
        return ResponseEntity.ok(vectorSearchService.runBenchmark(iterations, topK));
    }

    /**
     * 4. 清空测试表
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clear() {
        vectorSearchService.clearAllData();
        return ResponseEntity.ok(Map.of("message", "测试数据已清空"));
    }
}

