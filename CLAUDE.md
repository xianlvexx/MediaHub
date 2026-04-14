# MediaHub 后端

图像处理工具平台的 Java 后端，基于 Spring Boot 3.2.3 + MyBatis-Plus。当前已实现视频处理（解析/下载）和图片处理（水印修复）两大工具模块，架构设计支持持续扩展更多工具类功能。

## 构建与运行

```bash
# 构建 JAR（产物：media-hub-server/target/media-hub-server-1.0.0.jar）
mvn package -DskipTests

# 运行测试
mvn test

# Docker 部署
docker compose up -d --build
```

## 项目结构

多模块 Maven 工程，base package `cn.xlvexx.mediahub`：

```
media-hub/
├── media-hub-common/        # 公共模块（供 server 依赖）
├── media-hub-server/        # Spring Boot 应用层
│   └── src/main/
│       ├── java/cn/xlvexx/media-hub/
│       │   ├── auth/          # Token 认证、验证码、用户上下文
│       │   ├── config/        # AppProperties、CORS、异步、MyBatisPlus
│       │   ├── controller/    # REST 入口
│       │   ├── dto/           # 请求 / 响应对象
│       │   ├── entity/        # 数据库实体
│       │   ├── enums/         # 枚举（TaskStatus 等）
│       │   ├── exception/     # GlobalExceptionHandler
│       │   ├── executor/      # YtDlpExecutor（外部进程封装）
│       │   ├── manager/       # 数据访问层（extends ServiceImpl）
│       │   ├── mapper/        # MyBatis-Plus Mapper 接口
│       │   ├── scheduler/     # 定时任务
│       │   └── service/       # 业务逻辑
│       └── resources/
│           ├── application.yml
│           ├── application-prod.yml
│           ├── mapper/        # MyBatis XML
│           └── scripts/inpaint.py  # 水印修复 Python 脚本（打包进 JAR）
├── sql/vidsiilo-init.sql  # 数据库初始化 SQL
├── Dockerfile
├── docker-compose.yml
└── .env.example
```

## 分层约定

| 层 | 职责 |
|----|------|
| `controller/` | 接收请求、参数校验、调用 service、返回 `ApiResult<T>` |
| `service/` | 业务逻辑，注入 manager 操作数据 |
| `manager/` | 继承 `ServiceImpl<Mapper, Entity>`，封装常用 DB 操作 |
| `mapper/` | MyBatis-Plus Mapper 接口，复杂 SQL 在 `resources/mapper/*.xml` |
| `executor/` | 封装外部进程（yt-dlp），不含业务逻辑 |
| `scheduler/` | `@Scheduled` 定时任务（文件清理、指标采样） |

统一响应体为 `ApiResult<T>`，异常统一由 `GlobalExceptionHandler` 处理。

## 工具模块

### 视频工具
- **视频解析**：`YtDlpExecutor.parseVideoInfo()` 调用 yt-dlp `--dump-json` 获取视频元信息
- **视频下载**：`YtDlpExecutor.downloadVideo()` 带进度回调，通过 `--progress-template` 解析实时进度
- **进度推送**：`SseService` 将 `ProgressEvent` 以 SSE 实时推送给前端
- **任务管理**：`DownloadTaskManager` 维护任务状态（PENDING / RUNNING / COMPLETED / FAILED）

### 图片工具
- **水印修复（Inpaint）**：`InpaintService` 调用 `scripts/inpaint.py` 对图片指定区域进行内容填充
  - 优先使用 LaMa AI 模型（`simple-lama-inpainting`），效果更优
  - 未安装 LaMa 时自动降级到 OpenCV 算法
  - 脚本打包进 JAR，启动时解压到临时目录，无需单独部署
  - 超时上限 300 秒；Python 路径由 `app.inpaint.python-path` 配置

> 新增工具模块时：在 `controller/`、`service/` 下新建对应类，公共能力（文件处理、限流等）复用 `media-hub-common`。

### 基础能力
- **认证**：Token + 验证码，`AuthInterceptor` 拦截校验，Token 存储于内存，有效期可配置
- **限流**：`ParseRateLimitManager` 按 IP 限制解析频率
- **文件清理**：`FileCleanupScheduler` 定期清理过期下载文件
- **系统监控**：`SystemMetricsSampler` 定期采样 CPU / 内存指标

## 关键配置项（application.yml）

```yaml
app:
  ytdlp:
    path: /usr/local/bin/yt-dlp       # yt-dlp 可执行路径
    cookies-file: /home/video/cookies.txt  # 可选，需要登录的站点
  download:
    dir: /home/video/downloads         # 下载目录
    max-concurrent: 5
  cleanup:
    file-retain-hours: 2               # 完成/失败任务文件保留时长
  rate-limit:
    parse-per-ip-per-minute: 10        # 解析接口限流
  auth:
    token-expire-hours: 48
  inpaint:
    python-path: /opt/media-hub/venv/bin/python  # prod 使用 venv
```

## 环境依赖

| 依赖 | 模块 | 说明 |
|------|------|------|
| JDK 17 | 核心 | 运行时 |
| MySQL 8.0 | 核心 | 数据库，库名 `video` |
| yt-dlp | 视频工具 | 视频解析/下载 |
| ffmpeg | 视频工具 | 音视频合并 |
| Python 3 + opencv-python | 图片工具 | inpaint 基础算法 |
| simple-lama-inpainting + torch | 图片工具 | inpaint LaMa 模式（可选，效果更好）|

Docker 镜像已在 `/opt/media-hub/venv` 中安装 `opencv-python-headless`。
