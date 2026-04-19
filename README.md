# MediaHub

**[English](#english) | [中文](#中文)**

---

<a id="english"></a>

A streaming media processing toolkit for video downloading and image watermark removal.

## Preview

![Video Resolve](image/video-resolve.gif)

![Logo Transfer](image/logo-transfer.gif)

## Features

- **Video Parsing & Downloading** — Powered by yt-dlp, supports multi-platform video parsing, format selection, concurrent downloads, and queue management
- **Image Watermark Removal** — Dual-mode: OpenCV for basic processing, LaMa AI model for superior results on complex textures
- **Real-time Progress** — Live download progress and processing status via Server-Sent Events (SSE)
- **Authentication & Rate Limiting** — Built-in user login, token management, and IP-based rate limiting
- **File Lifecycle Management** — Automatic cleanup of expired files with configurable retention
- **System Monitoring** — Real-time visibility into system resource usage and application health

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17 + Spring Boot 3.2 |
| Data | MySQL 8.0 + MyBatis-Plus |
| Video | yt-dlp + ffmpeg |
| Image | Python + OpenCV / LaMa AI |
## Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/xianlvexx/MediaHub.git
cd MediaHub

# 2. Initialize database
mysql -u root -p media_hub < docs/media-hub-init.sql

# 3. Build
mvn package -DskipTests

# 4. Run
java -jar media-hub-server/target/media-hub-server-1.0.0.jar --spring.profiles.active=prod
```

Visit `http://localhost:8080` after startup.

## Configuration

Edit `resources/application.yml`:

| Key | Default | Description |
|-----|---------|-------------|
| `app.download.dir` | `/home/video/downloads` | File storage directory |
| `app.inpaint.python-path` | `/opt/mediahub/venv/bin/python` | Python executable path |
| `app.ytdlp.path` | `/usr/local/bin/yt-dlp` | yt-dlp executable path |

## License

This project is licensed under the [Apache License 2.0](LICENSE).

---

<a id="中文"></a>

# MediaHub - 流媒体处理工具

**[English](#english) | [中文](#中文)**

MediaHub 是一个流媒体处理工具，提供视频解析下载和图像水印去除功能。

## 效果预览

![视频解析](image/video-resolve.gif)

![水印去除](image/logo-transfer.gif)

## 核心功能

- **视频解析与下载** — 基于 yt-dlp，支持多平台视频解析、多格式选择、并发下载与队列管理
- **图像水印去除** — 支持 OpenCV 基础算法和 LaMa AI 模型两种模式，AI 模式对复杂纹理效果更佳
- **实时进度推送** — 通过 SSE 实时推送下载进度和处理状态
- **用户认证与限流** — 内置登录认证、Token 管理和 IP 级别速率限制
- **文件生命周期管理** — 自动清理过期文件，可配置保留时长
- **系统监控** — 实时查看系统资源使用情况与应用健康状态

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Java 17 + Spring Boot 3.2 |
| 数据层 | MySQL 8.0 + MyBatis-Plus |
| 视频处理 | yt-dlp + ffmpeg |
| 图像处理 | Python + OpenCV / LaMa AI |
## 快速开始

```bash
# 1. 克隆仓库
git clone https://github.com/xianlvexx/MediaHub.git
cd MediaHub

# 2. 初始化数据库
mysql -u root -p media_hub < docs/media-hub-init.sql

# 3. 构建
mvn package -DskipTests

# 4. 启动
java -jar media-hub-server/target/media-hub-server-1.0.0.jar --spring.profiles.active=prod
```

启动后访问 `http://localhost:8080` 即可使用。

## 配置说明

编辑 `resources/application.yml` 修改配置：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `app.download.dir` | `/home/video/downloads` | 文件存储目录 |
| `app.inpaint.python-path` | `/opt/mediahub/venv/bin/python` | Python 路径 |
| `app.ytdlp.path` | `/usr/local/bin/yt-dlp` | yt-dlp 路径 |

## 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源。
