# MediaHub

[中文文档](README_zh.md)

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
