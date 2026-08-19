-- ==============================================================================
-- 知识库向量检索数据库初始化脚本 (Knowledge Base Vector Schema)
-- 包含：pgvector 扩展开启、数据表结构定义、中文表/字段注释、标量索引与 HNSW 向量索引
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. 启用 pgvector 向量扩展
-- ------------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS vector;

-- ------------------------------------------------------------------------------
-- 2. 创建知识库文档向量核心表
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge_doc (
    id          BIGSERIAL PRIMARY KEY,                                      -- 主键ID（自增）
    title       VARCHAR(255) NOT NULL,                                      -- 文档标题
    category    VARCHAR(64)  NOT NULL,                                      -- 业务分类（如：AI技术、云计算、后端架构等）
    tags        VARCHAR(255),                                               -- 业务标签（英文逗号分隔）
    view_count  INT          DEFAULT 0,                                     -- 浏览量/点击数（用于标量过滤条件）
    content     TEXT,                                                       -- 文档文本正文/内容摘要
    embedding   vector(1536),                                               -- 高维特征向量（1536维，对齐 OpenAI/大模型常用维度）
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP                      -- 创建时间
);

-- ------------------------------------------------------------------------------
-- 3. 添加表及字段级中文注释（元数据注释，便于 DBeaver / Navicat / DataGrip 等工具查看）
-- ------------------------------------------------------------------------------
COMMENT ON TABLE  knowledge_doc             IS '知识库文档向量测试表（集成高维向量与标量业务数据）';
COMMENT ON COLUMN knowledge_doc.id          IS '主键ID（自增）';
COMMENT ON COLUMN knowledge_doc.title       IS '文档标题';
COMMENT ON COLUMN knowledge_doc.category    IS '业务分类（如：AI技术、云计算、后端架构等）';
COMMENT ON COLUMN knowledge_doc.tags        IS '业务标签（英文逗号分隔，如：spring,jpa,vector）';
COMMENT ON COLUMN knowledge_doc.view_count  IS '文档浏览量/点击数（用于标量过滤与混合检索）';
COMMENT ON COLUMN knowledge_doc.content     IS '文档文本正文或内容摘要';
COMMENT ON COLUMN knowledge_doc.embedding   IS '高维特征向量（1536维，支持余弦、欧氏距离与负内积计算）';
COMMENT ON COLUMN knowledge_doc.created_at  IS '数据记录创建时间';

-- ------------------------------------------------------------------------------
-- 4. 创建标量业务字段的 B-Tree 索引（加速混合过滤 Hybrid Search: category / view_count）
-- ------------------------------------------------------------------------------
-- 分类字段索引：用于 WHERE category = :category 快速命中
CREATE INDEX IF NOT EXISTS idx_kdoc_category ON knowledge_doc (category);

-- 浏览量字段索引：用于 WHERE view_count >= :minViews 范围扫描过滤
CREATE INDEX IF NOT EXISTS idx_kdoc_view_count ON knowledge_doc (view_count);

-- ------------------------------------------------------------------------------
-- 5. 创建 HNSW 向量近似最近邻索引（Hierarchical Navigable Small World）
-- ------------------------------------------------------------------------------

-- 5.1 【默认/推荐】余弦距离 HNSW 索引（操作符：<=>，对应 vector_cosine_ops）
-- 适用场景：文本语义相似度匹配、RAG 问答知识库、语义检索
-- 参数调优说明：
--   m = 16: 每个图节点的最大双向连接数（范围 2-100，越大内存与构建时间增加，但提高检索质量）
--   ef_construction = 64: 构建索引时的候选探索集大小（范围 4-1000，越大建索引越慢但图质量更高）
CREATE INDEX IF NOT EXISTS idx_kdoc_embedding_hnsw_cosine 
ON knowledge_doc USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- 5.2 【备选】L2 欧氏距离 HNSW 索引（操作符：<->，对应 vector_l2_ops）
-- 适用场景：图像特征检索、人脸特征比对、几何空间聚类
CREATE INDEX IF NOT EXISTS idx_kdoc_embedding_hnsw_l2 
ON knowledge_doc USING hnsw (embedding vector_l2_ops)
WITH (m = 16, ef_construction = 64);

-- 5.3 【备选】负内积 HNSW 索引（操作符：<#>，对应 vector_ip_ops）
-- 适用场景：已做过单位归一化的向量极速检索、推荐系统协同过滤打分
CREATE INDEX IF NOT EXISTS idx_kdoc_embedding_hnsw_ip 
ON knowledge_doc USING hnsw (embedding vector_ip_ops)
WITH (m = 16, ef_construction = 64);
