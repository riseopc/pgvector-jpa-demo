package org.riseopc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkReportDto {
    private long totalDataCount;
    private int vectorDimension;
    private int queryIterations;
    
    // Flat 暴力搜索基准
    private double flatAvgLatencyMs;
    private double flatP99LatencyMs;

    // HNSW 索引搜索
    private double hnswAvgLatencyMs;
    private double hnswP99LatencyMs;

    // 性能提升倍数与召回率
    private double speedupRatio;
    private double hnswRecallRate; // Top-10 召回率 (对比 Flat 结果)

    private String description;
}

