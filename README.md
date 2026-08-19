# 🚀 Spring Boot + PgVector 向量检索实战 Demo

> 💡 **基于 Spring Boot 4.0.7 + Spring Data JPA + PostgreSQL 18 (pgvector) 的轻量级高维向量检索与混合查询实战示例。**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.7-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg?logo=openjdk)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue.svg?logo=postgresql)](https://www.postgresql.org/)
[![PgVector](https://img.shields.io/badge/pgvector-0.8.x-orange.svg)](https://github.com/pgvector/pgvector)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

## ✨ 核心特性

* 🎯 **JPA 透明映射**：使用 `PgVectorConverter` 实现 Java `float[]` 数组与数据库 `vector(1536)` 字段的无感双向转换。
* 📐 **三种距离算法**：支持 **余弦距离 (`<=>`)**、**L2 欧氏距离 (`<->`)** 与 **负内积 (`<#>`)**。
* ⚡ **HNSW 索引压测**：内置 Flat 暴力全表扫描与 HNSW 索引的延迟及召回率（Recall）对比压测。
* 🔍 **混合过滤检索**：支持业务字段（分类、阅读量）过滤 + 向量相似度排序的复合查询。
* 🐳 **全容器化多阶段构建**：Dockerfile 内置 Maven 自动编译打包，电脑**无需安装 Maven 与 JDK** 即可一键运行。
* 📮 **开箱即用测试**：内置 IntelliJ IDEA 原生 HTTP Client 测试脚本 (`http/vector-search.http`)，点击绿色箭头即可直接发起请求。

---

## 🚀 快速上手

### 方案 A：🐳 纯 Docker 一键全自动编译与启动（最省事，推荐）

无需在本地电脑安装 Maven 和 JDK，直接通过 Docker Compose 完成**源码编译 + PostgreSQL 启动 + 应用启动**：

```bash
docker compose up -d --build
```

---

### 方案 B：💻 本地 IDEA / 命令行开发运行

```bash
# 1. 仅启动 PostgreSQL 数据库容器
docker compose up -d postgres

# 2. 运行单元测试（包含算法精度与 HNSW 压测）
mvn test

# 3. 本地启动 Web 服务（端口 8099）
mvn spring-boot:run
```

---

## 📮 使用 IDEA 直接测试接口

在 IntelliJ IDEA 中打开 [`http/vector-search.http`](http/vector-search.http)，点击请求旁边的绿色箭头即可直接测试各个接口：

```http
### 1. 批量导入 1000 条 1536 维测试向量
POST http://localhost:8099/api/vectors/mock-batch?count=1000

### 2. 向量相似度检索（支持业务分类与浏览量过滤）
POST http://localhost:8099/api/vectors/search
Content-Type: application/json

{
  "category": "AI技术",
  "minViewCount": 500,
  "metric": "COSINE",
  "minSimilarity": 0.1,
  "limit": 5
}

### 3. 运行 Flat vs HNSW 耗时与召回率压测
GET http://localhost:8099/api/vectors/benchmark?iterations=20&topK=10

### 4. 清空测试数据
DELETE http://localhost:8099/api/vectors/clear
```

---

## 📂 项目结构

```
pgvector-jpa-demo
├── 📁 docs                      # 架构说明
│   └── ARCHITECTURE.md          # 分层架构与核心原理简析
├── 📁 http                      # IDEA HTTP Client 接口测试
│   ├── http-client.env.json     # 环境配置 (local / docker / dev)
│   └── vector-search.http       # 接口测试请求脚本
├── 📁 src
│   ├── 📁 main
│   │   ├── 📁 java/org/riseopc
│   │   │   ├── PgvectorJpaDemoApplication.java        # 启动类
│   │   │   ├── 📁 controller/VectorSearchController.java # REST 接口
│   │   │   ├── 📁 converter/PgVectorConverter.java    # JPA float[] 向量转换器
│   │   │   ├── 📁 dto/                                # 请求/响应 DTO
│   │   │   ├── 📁 entity/KnowledgeDocEntity.java      # JPA 实体
│   │   │   ├── 📁 repository/KnowledgeDocRepository.java # 向量查询 Native SQL
│   │   │   ├── 📁 service/VectorSearchService.java    # 检索逻辑与压测服务
│   │   │   └── 📁 util/VectorUtils.java               # 向量数学工具
│   │   └── 📁 resources
│   │       ├── application.yml                        # 配置文件
│   │       └── 📁 db/schema.sql                       # 建表与 HNSW 索引 SQL
│   └── 📁 test/java/org/riseopc                       # 单元测试与基准评测用例
├── 🐳 Dockerfile                # 多阶段构建：自动拉取 Maven 镜像在容器内编译打包
├── 🐳 docker-compose.yml        # PostgreSQL 18 + App 一键编排
└── 📄 pom.xml                   # Maven 依赖
```

---

## 💡 核心代码一览

### JPA 向量类型转换器
```java
@Converter
public class PgVectorConverter implements AttributeConverter<float[], String> {
    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        return VectorUtils.toVectorString(attribute); // 转换为 "[0.1, 0.2, ...]"
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        return VectorUtils.parseVectorString(dbData);
    }
}
```

### Native SQL 向量检索与混合过滤
```sql
SELECT d.id, d.title, d.category, d.view_count,
       (1 - (d.embedding <=> CAST(:vectorStr AS vector))) AS similarity
FROM knowledge_doc d
WHERE (CAST(:category AS varchar) IS NULL OR d.category = CAST(:category AS varchar))
  AND (CAST(:minViewCount AS integer) IS NULL OR d.view_count >= CAST(:minViewCount AS integer))
ORDER BY d.embedding <=> CAST(:vectorStr AS vector)
LIMIT :limit;
```

---

## ⚖️ 开源协议

本项目采用 [Apache License 2.0](LICENSE) 开源协议。
