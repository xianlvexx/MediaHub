package cn.xlvexx.mediahub.dto.response;

import cn.xlvexx.mediahub.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

/**
 * SSE 下载进度推送事件
 *
 * @author 林风自在
 * @date 2026-03-30
 */
@Data
@Builder
public class ProgressEvent {

    private String taskId;
    private TaskStatus status;
    private Double progress;
    private String speed;
    private String eta;
    private String errorMsg;
}
