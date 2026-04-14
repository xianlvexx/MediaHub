# MediaHub

**图像处理工具平台 · Image Processing Toolbox**

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

- 后端通过 Docker 独立部署，前端单独部署后将 API 请求代理至后端。
- MySQL 8.0（自行准备，执行 `sql/media-hub-init.sql` 初始化表结构） 
- 后端默认运行在 `http://localhost:8080`。

## Cookie 配置（可选）

部分高画质视频需要账号登录才能下载，可通过配置 Cookie 解锁：

- 用浏览器插件（如 [cookies.txt](https://chromewebstore.google.com/detail/cookies-txt/njabckikapfpffapmjgojcnbfjonfjfg)）导出目标网站的 `cookies.txt`
- 在 `.env` 中设置 `COOKIES_FILE=/your/path/cookies.txt`，或登录管理员后台上传
- 
```dotenv
# yt-dlp cookies 文件路径（可选，需要登录的视频网站才需配置）
# COOKIES_FILE=/path/to/cookies.txt
```

## License

[MIT License](LICENSE) © 2025 林风自在
