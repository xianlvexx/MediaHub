package cn.xlvexx.mediahub.dto.response;

import cn.xlvexx.mediahub.entity.DownloadTask;
import cn.xlvexx.mediahub.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下载任务列表 DTO
 *
 * @author 林风自在
 * @date 2026-04-02
 */
@Data
public class TaskListResponse {

    private String taskId;
    private String videoTitle;
    private String thumbnail;
    private String formatNote;
    private String platform;
    private TaskStatus status;
    private Double progress;
    private Long fileSize;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static TaskListResponse from(DownloadTask task) {
        TaskListResponse r = new TaskListResponse();
        r.setTaskId(task.getTaskId());
        r.setVideoTitle(task.getVideoTitle());
        r.setThumbnail(task.getThumbnail());
        r.setFormatNote(task.getFormatNote());
        r.setPlatform(task.getPlatform());
        r.setStatus(task.getStatus());
        r.setProgress(task.getProgress() != null ? task.getProgress().doubleValue() : 0.0);
        r.setFileSize(task.getFileSize());
        r.setCreatedAt(task.getCreatedAt());
        r.setCompletedAt(task.getCompletedAt());
        return r;
    }
}
