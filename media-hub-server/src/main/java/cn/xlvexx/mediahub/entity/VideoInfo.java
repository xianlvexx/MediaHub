package cn.xlvexx.mediahub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频信息实体，缓存 yt-dlp 解析结果
 *
 * @author 林风自在
 * @date 2026-03-31
 */
@Data
@TableName("video_info")
public class VideoInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String urlHash;

    private String originalUrl;

    private String platform;

    private String videoId;

    private String title;

    private Integer duration;

    private String thumbnail;

    private String uploader;

    private Long viewCount;

    private String formatsJson;

    private LocalDateTime expireAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
