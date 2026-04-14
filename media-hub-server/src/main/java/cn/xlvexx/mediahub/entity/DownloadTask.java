package cn.xlvexx.mediahub.entity;

import com.baomidou.mybatisplus.annotation.*;
import cn.xlvexx.mediahub.enums.TaskStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下载任务实体
 *
 * @author 林风自在
 * @date 2026-04-01
 */
@Data
@TableName("download_task")
public class DownloadTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private String videoUrl;

    private String platform;

    private String videoTitle;

    private String videoId;

    private String thumbnail;

    private String formatId;

    private String formatNote;

    @TableField("status")
    private TaskStatus status;

    private BigDecimal progress;

    private String speed;

    private String eta;

    private String filePath;

    private String fileName;

    private Long fileSize;

    private String errorMsg;

    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;
}
