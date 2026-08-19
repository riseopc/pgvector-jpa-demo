package org.riseopc.entity;

import org.riseopc.converter.PgVectorConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 知识库/文档向量测试实体类
 */
@Entity
@Table(name = "knowledge_doc")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "embedding")
public class KnowledgeDocEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 64)
    private String category;

    @Column(length = 255)
    private String tags;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 1536 维特征向量，使用自定义 Converter 映射至 PostgreSQL 的 vector(1536) 字段
     */
    @Convert(converter = PgVectorConverter.class)
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

