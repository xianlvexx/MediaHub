# ===== 构建阶段 =====
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 分层复制 pom 文件，优先利用 Docker 层缓存
COPY pom.xml .
COPY media-hub-common/pom.xml media-hub-common/pom.xml
COPY media-hub-server/pom.xml media-hub-server/pom.xml
RUN mvn dependency:go-offline -q

# 复制源码并构建
COPY media-hub-common/src media-hub-common/src
COPY media-hub-server/src media-hub-server/src
RUN mvn package -DskipTests -q

# ===== 运行阶段 =====
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 安装系统依赖：ffmpeg、python3、yt-dlp
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    python3 \
    python3-pip \
    python3-venv \
    curl \
    && curl -fsSL https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp \
       -o /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 创建 Python 虚拟环境并安装 opencv（与 prod 配置 python-path 一致）
RUN python3 -m venv /opt/mediahub/venv \
    && /opt/mediahub/venv/bin/pip install --no-cache-dir opencv-python-headless

# 复制 JAR（多模块构建产物在 media-hub-server/target/）
COPY --from=builder /build/media-hub-server/target/media-hub-server-*.jar app.jar

# 下载目录（与 prod 配置 app.download.dir 一致）
RUN mkdir -p /home/video/downloads
VOLUME ["/home/video/downloads"]

EXPOSE 8080

# JVM 参数必须在 -jar 之前
ENTRYPOINT ["java", "-Xmx512m", "-Xms128m", "-jar", "app.jar", "--spring.profiles.active=prod"]
