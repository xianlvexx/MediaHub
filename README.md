<div align="center">

# MediaHub

**图像处理工具平台 · Image Processing Toolbox**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61dafb.svg)](https://react.dev)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ed.svg)](https://docs.docker.com/compose/)

视频下载 · 图片水印修复，持续扩展更多图像处理能力。

[快速开始](#快速开始) · [工具功能](#工具功能) · [配置说明](#配置说明) · [开发指南](#开发指南)

</div>

---

## 工具功能

### 视频工具
- **视频解析与下载** — 粘贴链接，自动解析标题、封面、画质列表，按需下载
- **实时进度推送** — 基于 SSE 实时展示下载进度、速度与预计剩余时间
- **下载历史管理** — 完整的任务记录，支持分页查询与重复下载

### 图片工具
- **水印修复** — 框选水印区域，AI 自动填充内容（LaMa 模型，降级 OpenCV）

### 平台能力
- **系统监控面板** — 实时查看 CPU、内存、磁盘、JVM 使用率与任务统计
- **自动文件清理** — 按配置时长定期清理本地文件，节省磁盘空间
- **解析速率限制** — 按 IP 限流，保护服务稳定性
- **管理员后台** — Cookie 管理、用户账户管理

## 快速开始

后端通过 Docker 独立部署，前端单独部署后将 API 请求代理至后端。

### 前置条件

- Docker & Docker Compose
- MySQL 8.0（自行准备，执行 `sql/vidsiilo-init.sql` 初始化表结构）

### 启动后端

```bash
git clone https://github.com/your-username/media-hub.git
cd media-hub

# 配置环境变量
cp .env.example .env

# 启动后端服务
docker compose up -d --build
```

后端默认运行在 `http://localhost:8080`。

### 环境变量

```dotenv
# 后端对外端口（默认 8080）
BACKEND_PORT=8080

# yt-dlp cookies 文件路径（可选，需要登录的视频网站才需配置）
# COOKIES_FILE=/path/to/cookies.txt
```

> 数据库连接信息在 `media-hub-server/src/main/resources/application-prod.yml` 中配置。

## 配置说明

配置文件：`media-hub-server/src/main/resources/application.yml`，可通过 Spring 环境变量覆盖。

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `app.ytdlp.path` | `/usr/local/bin/yt-dlp` | yt-dlp 可执行路径 |
| `app.ytdlp.cookies-file` | `/home/video/cookies.txt` | Cookie 文件路径（可选） |
| `app.download.dir` | `/home/video/downloads` | 下载文件存储目录 |
| `app.download.max-concurrent` | `5` | 最大并发下载数 |
| `app.download.queue-capacity` | `10` | 等待队列容量 |
| `app.cleanup.file-retain-hours` | `2` | 文件保留时长（小时） |
| `app.rate-limit.parse-per-ip-per-minute` | `10` | 每 IP 每分钟解析次数上限 |
| `app.inpaint.python-path` | `python3` | Python 可执行路径（图片修复） |

## Cookie 配置（可选）

部分高画质视频需要账号登录才能下载，可通过配置 Cookie 解锁：

1. 用浏览器插件（如 [cookies.txt](https://chromewebstore.google.com/detail/cookies-txt/njabckikapfpffapmjgojcnbfjonfjfg)）导出目标网站的 `cookies.txt`
2. 在 `.env` 中设置 `COOKIES_FILE=/your/path/cookies.txt`，或登录管理员后台上传

## 开发指南

### 项目结构

```
media-hub/                       # 后端仓库（本仓库）
├── media-hub-common/              # 公共模块
├── media-hub-server/              # Spring Boot 应用层
│   └── src/main/
│       ├── java/cn/xlvexx/mediahub/
│       │   ├── controller/      # REST API 入口
│       │   ├── service/         # 业务逻辑
│       │   ├── manager/         # 数据访问层
│       │   ├── executor/        # yt-dlp 进程封装
│       │   ├── scheduler/       # 定时任务
│       │   └── auth/            # 认证
│       └── resources/
│           └── scripts/inpaint.py  # 图片修复脚本
├── sql/vidsiilo-init.sql        # 数据库初始化
├── Dockerfile
└── docker-compose.yml

media-hub-web/                     # 前端仓库（单独部署）
```

### 本地开发

**后端**

```bash
# 需本地 MySQL 并执行 sql/vidsiilo-init.sql 初始化
mvn spring-boot:run
# 运行在 http://localhost:8080
```

**前端**（前端仓库）

```bash
npm install
npm run dev
# 运行在 http://localhost:5173，/api 自动代理至后端
```

**构建**

```bash
# 后端打包
mvn package -DskipTests
# 产物：media-hub-server/target/media-hub-server-1.0.0.jar
```

## 常见问题

**Q: 支持哪些视频平台？**
取决于服务器上 yt-dlp 版本所支持的平台，项目本身无平台限制。

**Q: 下载完成后文件在哪里？**
存储在 `app.download.dir` 目录，通过 `/api/tasks/{taskId}/file` 下载，超过 `file-retain-hours` 后自动清理。

**Q: 图片修复效果不好？**
默认使用 LaMa AI 模型，需在服务器虚拟环境中安装：`pip install simple-lama-inpainting torch torchvision`。未安装时自动降级到 OpenCV 算法。

## Contributing

欢迎提交 Issue 和 Pull Request。提交前请确保：

1. 后端改动已通过本地编译：`mvn clean package`
2. 前端改动已通过类型检查：`npm run build`

## License

[MIT License](LICENSE) © 2025 xlvexx
