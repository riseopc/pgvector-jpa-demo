package org.riseopc.repository;

import org.riseopc.dto.KnowledgeDocSearchResultProjection;
import org.riseopc.entity.KnowledgeDocEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 基于 Spring Data JPA 的 PgVector 检索持久层接口
 */
@Repository
public interface KnowledgeDocRepository extends JpaRepository<KnowledgeDocEntity, Long> {

    /**
     * 1. 余弦相似度 Top-K 检索（操作符 <=> 计算余弦距离，余弦相似度 = 1 - 余弦距离）
     */
    @Query(value = """
        SELECT d.id AS id,
               d.title AS title,
               d.category AS category,
               d.tags AS tags,
               d.view_count AS viewCount,
               d.content AS content,
               (1 - (d.embedding <=> CAST(:vectorStr AS vector))) AS similarity,
               (d.embedding <=> CAST(:vectorStr AS vector)) AS distance,
               d.created_at AS createdAt
        FROM knowledge_doc d
        ORDER BY d.embedding <=> CAST(:vectorStr AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeDocSearchResultProjection> searchByCosine(
        @Param("vectorStr") String vectorStr,
        @Param("limit") int limit
    );

    /**
     * 2. 欧氏距离 (L2 Distance) Top-K 检索（操作符 <->）
     */
    @Query(value = """
        SELECT d.id AS id,
               d.title AS title,
               d.category AS category,
               d.tags AS tags,
               d.view_count AS viewCount,
               d.content AS content,
               NULL AS similarity,
               (d.embedding <-> CAST(:vectorStr AS vector)) AS distance,
               d.created_at AS createdAt
        FROM knowledge_doc d
        ORDER BY d.embedding <-> CAST(:vectorStr AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeDocSearchResultProjection> searchByL2Distance(
        @Param("vectorStr") String vectorStr,
        @Param("limit") int limit
    );

    /**
     * 3. 负内积 (Negative Inner Product) Top-K 检索（操作符 <#>）
     */
    @Query(value = """
        SELECT d.id AS id,
               d.title AS title,
               d.category AS category,
               d.tags AS tags,
               d.view_count AS viewCount,
               d.content AS content,
               ((d.embedding <#> CAST(:vectorStr AS vector)) * -1) AS similarity,
               (d.embedding <#> CAST(:vectorStr AS vector)) AS distance,
               d.created_at AS createdAt
        FROM knowledge_doc d
        ORDER BY d.embedding <#> CAST(:vectorStr AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeDocSearchResultProjection> searchByInnerProduct(
        @Param("vectorStr") String vectorStr,
        @Param("limit") int limit
    );

    /**
     * 4. 混合过滤检索 (Hybrid Search)：业务标量条件 + 向量相似度 + 阈值截断
     */
    @Query(value = """
        SELECT d.id AS id,
               d.title AS title,
               d.category AS category,
               d.tags AS tags,
               d.view_count AS viewCount,
               d.content AS content,
               (1 - (d.embedding <=> CAST(:vectorStr AS vector))) AS similarity,
               (d.embedding <=> CAST(:vectorStr AS vector)) AS distance,
               d.created_at AS createdAt
        FROM knowledge_doc d
        WHERE (CAST(:category AS varchar) IS NULL OR d.category = CAST(:category AS varchar))
          AND (CAST(:minViewCount AS integer) IS NULL OR d.view_count >= CAST(:minViewCount AS integer))
          AND (1 - (d.embedding <=> CAST(:vectorStr AS vector))) >= :minSimilarity
        ORDER BY d.embedding <=> CAST(:vectorStr AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeDocSearchResultProjection> searchHybrid(
        @Param("vectorStr") String vectorStr,
        @Param("category") String category,
        @Param("minViewCount") Integer minViewCount,
        @Param("minSimilarity") double minSimilarity,
        @Param("limit") int limit
    );

    /**
     * 5. 统计总数
     */
    @Query(value = "SELECT COUNT(*) FROM knowledge_doc", nativeQuery = true)
    long countAllDocs();

    /**
     * 6. 清空测试数据
     */
    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query(value = "TRUNCATE TABLE knowledge_doc RESTART IDENTITY", nativeQuery = true)
    void truncateTable();
}

