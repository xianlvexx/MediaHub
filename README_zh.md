# MediaHub - 流媒体处理工具

[English Documentation](README.md)

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
1. 克隆仓库
git clone https://github.com/xianlvexx/MediaHub.git cd MediaHub
2. 初始化数据库
mysql -u root -p media_hub < docs/media-hub-init.sql
3. 构建
mvn package -DskipTests
4. 启动
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

