# ==============================================================================
# 阶段 1：构建阶段（基于 Maven 容器，启用 BuildKit 依赖持久缓存）
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

# ⚡ 核心提速优化：
# 挂载 Docker BuildKit 本地缓存目录 (/root/.m2)
# 首次构建下载依赖后持久保存在宿主机，后续代码修改构建时直接秒级复用本地依赖缓存，无需重复下载！
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests

# ==============================================================================
# 阶段 2：运行阶段（轻量化 JRE 运行环境）
# ==============================================================================
FROM alibabadragonwell/dragonwell:21-ubuntu

WORKDIR /app

# 设置容器时区为 Asia/Shanghai
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 从构建阶段提取打包好的 JAR 文件
COPY --from=builder /build/target/*.jar app.jar

# 暴露服务端口
EXPOSE 8099

# JVM 优化参数（容器感知、内存百分比、G1GC、快速随机数）
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
