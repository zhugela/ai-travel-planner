# ============================================================
# Dockerfile - 后端 Spring Boot 容器化(Ch 09 §5.4 方案 ①)
# 云端构建:平台拉源码 + Maven 在线打包
# ============================================================

# 预装 Maven + JDK 21 的基础镜像
FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /app

# 先复制 pom.xml 单独下载依赖(利用 Docker 缓存)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码 + 打包
COPY src ./src
RUN mvn clean package -DskipTests

# ============ 第二阶段:运行时镜像 ============
# 轻量 JDK 21(只需要运行时)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 从 builder 阶段复制 jar
COPY --from=builder /app/target/ai-travel-planner-0.0.1-SNAPSHOT.jar app.jar

# 暴露后端端口
EXPOSE 8123

# 健康检查(可选,平台不需要可注释掉)
# HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
#   CMD wget --quiet --tries=1 --spider http://localhost:8123/api/chat || exit 1

# 启动命令:激活 prod profile
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]

# JVM 参数(可调):
#   -Xms512m 初始堆
#   -Xmx1024m 最大堆
#   -XX:+UseG1GC G1 垃圾回收器
#   -Dfile.encoding=UTF-8 文件编码
#   完整版:
#   ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-XX:+UseG1GC",
#               "-Dfile.encoding=UTF-8",
#               "-jar", "/app/app.jar", "--spring.profiles.active=prod"]