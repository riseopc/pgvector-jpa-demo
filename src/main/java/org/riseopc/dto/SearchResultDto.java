package org.riseopc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {
    private Long id;
    private String title;
    private String category;
    private String tags;
    private Integer viewCount;
    private String content;
    private Double similarity;
    private Double distance;
    private LocalDateTime createdAt;

    public static SearchResultDto fromProjection(KnowledgeDocSearchResultProjection proj) {
        return SearchResultDto.builder()
                .id(proj.getId())
                .title(proj.getTitle())
                .category(proj.getCategory())
                .tags(proj.getTags())
                .viewCount(proj.getViewCount())
                .content(proj.getContent())
                .similarity(proj.getSimilarity() != null ? Math.round(proj.getSimilarity() * 10000.0) / 10000.0 : null)
                .distance(proj.getDistance() != null ? Math.round(proj.getDistance() * 10000.0) / 10000.0 : null)
                .createdAt(proj.getCreatedAt())
                .build();
    }
}

