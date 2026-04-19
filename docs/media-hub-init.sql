-- ============================================================
-- 表1：video_info - 视频解析信息缓存表
-- 作用：缓存yt-dlp的解析结果，避免重复调用（TTL = 1小时）
-- ============================================================
CREATE TABLE IF NOT EXISTS `video_info` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT         COMMENT '主键ID',
    `url_hash`     VARCHAR(64)   NOT NULL                        COMMENT 'URL的SHA256哈希值，用于快速查找缓存',
    `original_url` VARCHAR(500)  NOT NULL                        COMMENT '原始视频URL',
    `platform`     VARCHAR(50)   NOT NULL DEFAULT ''             COMMENT '视频平台标识（bilibili/youtube/douyin/shipinhao等）',
    `video_id`     VARCHAR(100)  NOT NULL DEFAULT ''             COMMENT '平台视频ID',
    `title`        VARCHAR(500)  NOT NULL DEFAULT ''             COMMENT '视频标题',
    `duration`     INT           NOT NULL DEFAULT 0              COMMENT '视频时长（秒）',
    `thumbnail`    VARCHAR(500)  NOT NULL DEFAULT ''             COMMENT '封面图URL',
    `uploader`     VARCHAR(200)  NOT NULL DEFAULT ''             COMMENT 'UP主名称',
    `view_count`   BIGINT        NOT NULL DEFAULT 0              COMMENT '视频播放量',
    `formats_json` JSON                                          COMMENT '可用画质格式列表（yt-dlp formats字段完整JSON）',
    `expire_at`    DATETIME      NOT NULL                        COMMENT '缓存过期时间（created_at + 1小时）',
    `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_url_hash` (`url_hash`),
    KEY `idx_expire_at` (`expire_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '视频解析信息缓存表';


-- ============================================================
-- 表2：download_task - 下载任务表
-- 作用：记录每个下载任务的完整生命周期（PENDING→RUNNING→COMPLETED/FAILED）
-- ============================================================
CREATE TABLE IF NOT EXISTS `download_task` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT         COMMENT '主键ID',
    `task_id`       VARCHAR(36)   NOT NULL                        COMMENT '任务UUID（对外暴露，前端用于SSE订阅和查询）',
    `video_url`     VARCHAR(500)  NOT NULL                        COMMENT '视频原始URL',
    `platform`      VARCHAR(50)   NOT NULL DEFAULT ''             COMMENT '视频平台标识（bilibili/youtube/douyin/shipinhao等）',
    `video_title`   VARCHAR(500)  NOT NULL DEFAULT ''             COMMENT '视频标题（冗余存储，方便列表展示）',
    `video_id`      VARCHAR(100)  NOT NULL DEFAULT ''             COMMENT '平台视频ID',
    `thumbnail`     VARCHAR(500)  NOT NULL DEFAULT ''             COMMENT '封面图URL（冗余存储）',
    `format_id`     VARCHAR(50)   NOT NULL DEFAULT ''             COMMENT '用户选择的yt-dlp格式ID（如"80"表示1080p）',
    `format_note`   VARCHAR(100)  NOT NULL DEFAULT ''             COMMENT '画质描述（如"1080p", "720p"）',
    `status`        VARCHAR(20)   NOT NULL DEFAULT 'PENDING'      COMMENT '任务状态：PENDING/RUNNING/COMPLETED/FAILED/CANCELLED',
    `progress`      DECIMAL(5,2)  NOT NULL DEFAULT 0.00           COMMENT '下载进度百分比（0.00 ~ 100.00）',
    `speed`         VARCHAR(50)   NOT NULL DEFAULT ''             COMMENT '当前下载速度（如"2.5MiB/s"）',
    `eta`           VARCHAR(20)   NOT NULL DEFAULT ''             COMMENT '预计剩余时间（如"00:30"）',
    `file_path`     VARCHAR(500)          DEFAULT NULL            COMMENT '下载完成后的文件绝对路径',
    `file_name`     VARCHAR(200)          DEFAULT NULL            COMMENT '下载文件名（含扩展名，用于Content-Disposition）',
    `file_size`     BIGINT        NOT NULL DEFAULT 0              COMMENT '文件大小（字节），完成后更新',
    `error_msg`     TEXT                  DEFAULT NULL            COMMENT '失败原因（来自yt-dlp的错误输出）',
    `ip_address`    VARCHAR(45)   NOT NULL DEFAULT ''             COMMENT '发起下载的客户端IP（用于限流统计）',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务创建时间',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `completed_at`  DATETIME              DEFAULT NULL            COMMENT '任务完成时间（COMPLETED或FAILED状态时设置）',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_ip_created` (`ip_address`, `created_at`),
    KEY `idx_completed_at` (`completed_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '下载任务表';


-- ============================================================
-- 表3：system_config - 系统配置表
-- 作用：运行时可修改的配置项，避免每次改配置都需要重启服务
-- ============================================================
CREATE TABLE IF NOT EXISTS `system_config` (
    `id`           INT           NOT NULL AUTO_INCREMENT         COMMENT '主键ID',
    `config_key`   VARCHAR(100)  NOT NULL                        COMMENT '配置键（全局唯一）',
    `config_value` VARCHAR(500)  NOT NULL DEFAULT ''             COMMENT '配置值（字符串，业务层自行转换类型）',
    `description`  VARCHAR(200)  NOT NULL DEFAULT ''             COMMENT '配置说明',
    `is_active`    TINYINT(1)    NOT NULL DEFAULT 1              COMMENT '是否启用：1=启用，0=禁用',
    `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '系统配置表';


-- ============================================================
-- 初始化系统配置数据
-- ============================================================
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
-- 限流配置
('rate_limit.parse_per_ip_per_minute', '10',   '每IP每分钟最多解析视频次数'),
('rate_limit.download_per_ip_per_day', '20',   '每IP每天最多下载次数'),

-- 并发控制
('download.max_concurrent_tasks',      '5',    '全局最大并发下载任务数'),
('download.queue_max_size',            '10',   '下载任务队列最大容量，超过则拒绝新任务'),

-- 缓存配置
('cache.video_info_ttl_minutes',       '60',   '视频信息缓存有效期（分钟）'),

-- 文件清理配置
('cleanup.file_retain_hours',          '2',    '下载完成后文件保留时长（小时），超时自动删除'),
('cleanup.scan_interval_minutes',      '10',   '文件清理定时任务扫描间隔（分钟）'),

-- 下载限制
('download.max_file_size_gb',          '2',    '单次下载最大文件大小（GB）'),
('download.socket_timeout_seconds',    '60',   'yt-dlp网络连接超时（秒）'),
('download.retries',                   '3',    'yt-dlp下载失败重试次数'),

-- yt-dlp路径（Docker环境）
('ytdlp.binary_path',                  '/usr/local/bin/yt-dlp', 'yt-dlp可执行文件路径'),
('ytdlp.ffmpeg_path',                  '/usr/bin/ffmpeg',        'ffmpeg可执行文件路径'),

-- 存储路径
('storage.download_base_dir',          '/app/downloads',         '下载文件存储根目录');

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `username`   VARCHAR(64)  NOT NULL,
    `password`   VARCHAR(128) NOT NULL,
    `role`       VARCHAR(16)  NOT NULL DEFAULT 'MEMBER',
    `enabled`    TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '系统用户表';


-- ============================================================
-- 表5：sys_token - 登录 Token 持久化表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_token` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `token`      VARCHAR(64)  NOT NULL                        COMMENT 'Token 值',
    `user_id`    BIGINT       NOT NULL                        COMMENT '用户ID',
    `username`   VARCHAR(64)  NOT NULL                        COMMENT '用户名（冗余）',
    `role`       VARCHAR(16)  NOT NULL                        COMMENT '角色（冗余）',
    `expire_at`  BIGINT       NOT NULL                        COMMENT '过期时间（Unix 毫秒）',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`),
    KEY `idx_expire_at` (`expire_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '登录 Token 表';


-- ============================================================
-- 表6：parse_rate_limit - 未登录用户每日解析次数限流表
-- ============================================================
CREATE TABLE IF NOT EXISTS `parse_rate_limit` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `ip`         VARCHAR(45)  NOT NULL                        COMMENT '客户端 IP',
    `stat_date`  DATE         NOT NULL                        COMMENT '统计日期',
    `count`      INT          NOT NULL DEFAULT 0              COMMENT '当日解析次数',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ip_date` (`ip`, `stat_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '解析次数限流表';


-- ============================================================
-- 迁移：为已存在的表添加 platform 字段（存量数据库执行）
-- ============================================================
ALTER TABLE `video_info` ADD COLUMN `platform` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '视频平台标识' AFTER `original_url`;

ALTER TABLE `download_task` ADD COLUMN `platform` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '视频平台标识' AFTER `video_url`;

