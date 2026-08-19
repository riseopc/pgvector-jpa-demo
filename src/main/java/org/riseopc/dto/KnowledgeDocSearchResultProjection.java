package org.riseopc.dto;

import java.time.LocalDateTime;

/**
 * Spring Data JPA 原生 SQL 检索投影接口
 */
public interface KnowledgeDocSearchResultProjection {

    Long getId();

    String getTitle();

    String getCategory();

    String getTags();

    Integer getViewCount();

    String getContent();

    Double getSimilarity();

    Double getDistance();

    LocalDateTime getCreatedAt();
}

