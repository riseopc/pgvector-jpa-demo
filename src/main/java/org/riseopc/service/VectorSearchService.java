package org.riseopc.service;

import org.riseopc.dto.BenchmarkReportDto;
import org.riseopc.dto.SearchResultDto;
import org.riseopc.dto.VectorSearchRequest;
import org.riseopc.repository.KnowledgeDocRepository;
import org.riseopc.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final KnowledgeDocRepository knowledgeDocRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${pgvector.dimension:1536}")
    private int defaultDimension;

    private static final String[] CATEGORIES = {"AI技术", "后端开发", "微服务架构", "数据库内核", "前端工程", "DevOps运维"};

    /**
     * 执行多模式向量检索
     */
    @Transactional(readOnly = true)
    public List<SearchResultDto> search(VectorSearchRequest request) {
        float[] vector = request.getVector();
        if (vector == null || vector.length == 0) {
            vector = VectorUtils.randomNormalizedVector(defaultDimension);
        }
        String vectorStr = VectorUtils.toVectorString(vector);
        int limit = Math.max(1, request.getLimit());

        List<SearchResultDto> results;
        String metric = request.getMetric() == null ? "COSINE" : request.getMetric().toUpperCase();

        if (request.getCategory() != null || request.getMinViewCount() != null || request.getMinSimilarity() > 0) {
            // 混合过滤检索
            results = knowledgeDocRepository.searchHybrid(
                    vectorStr,
                    request.getCategory(),
                    request.getMinViewCount(),
                    request.getMinSimilarity(),
                    limit
            ).stream().map(SearchResultDto::fromProjection).toList();
        } else {
            results = switch (metric) {
                case "L2" -> knowledgeDocRepository.searchByL2Distance(vectorStr, limit)
                        .stream().map(SearchResultDto::fromProjection).toList();
                case "INNER_PRODUCT" -> knowledgeDocRepository.searchByInnerProduct(vectorStr, limit)
                        .stream().map(SearchResultDto::fromProjection).toList();
                case "COSINE" -> knowledgeDocRepository.searchByCosine(vectorStr, limit)
                        .stream().map(SearchResultDto::fromProjection).toList();
                default -> throw new IllegalArgumentException("Unsupported metric: " + metric);
            };
        }
        return results;
    }

    /**
     * 批量导入测试向量数据 (使用 JdbcTemplate 批量写入，大幅提升导入吞吐)
     */
    @Transactional
    public long generateAndBatchInsert(int count) {
        log.info("开始生成并批量写入 {} 条向量测试数据...", count);
        long startTime = System.currentTimeMillis();

        String sql = "INSERT INTO knowledge_doc (title, category, tags, view_count, content, embedding) VALUES (?, ?, ?, ?, ?, ?::vector)";

        int batchSize = 500;
        List<Object[]> batchArgs = new ArrayList<>(batchSize);
        Random random = new Random();

        for (int i = 1; i <= count; i++) {
            String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
            String title = "深入理解 " + category + " 核心原理与最佳实践 #" + i;
            String tags = category + ",技术架构,专题" + (i % 10);
            int viewCount = random.nextInt(10000);
            String content = "这是关于 " + title + " 的详细内容摘要与嵌入式语义特征数据，用于验证 pgvector 检索性能与召回率。";
            float[] embedding = VectorUtils.randomNormalizedVector(defaultDimension);
            String vectorStr = VectorUtils.toVectorString(embedding);

            batchArgs.add(new Object[]{title, category, tags, viewCount, content, vectorStr});

            if (batchArgs.size() >= batchSize) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
            batchArgs.clear();
        }

        long duration = System.currentTimeMillis() - startTime;
        long totalCount = knowledgeDocRepository.countAllDocs();
        log.info("批量写入完成！新增条数: {}, 数据库现有总条数: {}, 耗时: {} ms, 写入吞吐量: {} 条/秒",
                count, totalCount, duration, (count * 1000L) / Math.max(1, duration));
        return totalCount;
    }

    /**
     * 清空表数据
     */
    @Transactional
    public void clearAllData() {
        knowledgeDocRepository.truncateTable();
        log.info("已清空 knowledge_doc 表数据。");
    }

    /**
     * 运行 Flat 暴力检索 vs HNSW 索引检索基准评测
     */
    public BenchmarkReportDto runBenchmark(int queryIterations, int topK) {
        long totalCount = knowledgeDocRepository.countAllDocs();
        if (totalCount == 0) {
            log.warn("当前数据库暂无数据，自动灌入 2,000 条测试数据...");
            generateAndBatchInsert(2000);
            totalCount = knowledgeDocRepository.countAllDocs();
        }

        log.info("开始执行基准测试: 数据量={}, 维度={}, 查询轮数={}, TopK={}", totalCount, defaultDimension, queryIterations, topK);

        List<float[]> queryVectors = new ArrayList<>(queryIterations);
        for (int i = 0; i < queryIterations; i++) {
            queryVectors.add(VectorUtils.randomNormalizedVector(defaultDimension));
        }

        // 1. 禁用索引以测试 Flat 暴力全表扫描（精确 KNN）
        jdbcTemplate.execute("SET enable_indexscan = off;");
        List<Long> flatLatencies = new ArrayList<>();
        Map<Integer, List<Long>> groundTruthResults = new HashMap<>();

        try {
            for (int i = 0; i < queryIterations; i++) {
                String vectorStr = VectorUtils.toVectorString(queryVectors.get(i));
                long start = System.nanoTime();
                List<Long> ids = jdbcTemplate.query(
                        "SELECT id FROM knowledge_doc ORDER BY embedding <=> ?::vector LIMIT ?",
                        (rs, rowNum) -> rs.getLong("id"),
                        vectorStr, topK
                );
                long elapsed = (System.nanoTime() - start) / 1_000_000; // ms
                flatLatencies.add(elapsed);
                groundTruthResults.put(i, ids);
            }
        } finally {
            jdbcTemplate.execute("SET enable_indexscan = on;");
        }

        // 2. 启用 HNSW 索引测试（ANN 近似检索）
        jdbcTemplate.execute("SET enable_indexscan = on;");
        jdbcTemplate.execute("SET hnsw.ef_search = 100;");
        List<Long> hnswLatencies = new ArrayList<>();
        int totalOverlap = 0;

        for (int i = 0; i < queryIterations; i++) {
            String vectorStr = VectorUtils.toVectorString(queryVectors.get(i));
            long start = System.nanoTime();
            List<Long> ids = jdbcTemplate.query(
                    "SELECT id FROM knowledge_doc ORDER BY embedding <=> ?::vector LIMIT ?",
                    (rs, rowNum) -> rs.getLong("id"),
                    vectorStr, topK
            );
            long elapsed = (System.nanoTime() - start) / 1_000_000; // ms
            hnswLatencies.add(elapsed);

            // 计算召回率对比
            List<Long> truth = groundTruthResults.get(i);
            Set<Long> truthSet = new HashSet<>(truth != null ? truth : Collections.emptyList());
            long overlap = ids.stream().filter(truthSet::contains).count();
            totalOverlap += overlap;
        }

        double flatAvg = flatLatencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double flatP99 = percentile(flatLatencies, 0.99);

        double hnswAvg = hnswLatencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double hnswP99 = percentile(hnswLatencies, 0.99);

        double speedup = flatAvg > 0 && hnswAvg > 0 ? (flatAvg / hnswAvg) : 1.0;
        double recall = (double) totalOverlap / (queryIterations * topK);

        return BenchmarkReportDto.builder()
                .totalDataCount(totalCount)
                .vectorDimension(defaultDimension)
                .queryIterations(queryIterations)
                .flatAvgLatencyMs(Math.round(flatAvg * 100.0) / 100.0)
                .flatP99LatencyMs(Math.round(flatP99 * 100.0) / 100.0)
                .hnswAvgLatencyMs(Math.round(hnswAvg * 100.0) / 100.0)
                .hnswP99LatencyMs(Math.round(hnswP99 * 100.0) / 100.0)
                .speedupRatio(Math.round(speedup * 100.0) / 100.0)
                .hnswRecallRate(Math.round(recall * 10000.0) / 100.0)
                .description(String.format("在 %d 条 %d 维向量数据上测试，HNSW 索引相比 Flat 暴力检索提速 %.1f 倍，Top-%d 召回率达到 %.2f%%",
                        totalCount, defaultDimension, speedup, topK, recall * 100.0))
                .build();
    }

    private double percentile(List<Long> values, double p) {
        if (values.isEmpty()) return 0.0;
        List<Long> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}

