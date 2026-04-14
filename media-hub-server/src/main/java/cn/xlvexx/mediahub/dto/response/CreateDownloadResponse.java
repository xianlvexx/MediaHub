package cn.xlvexx.mediahub.dto.response;

import lombok.Data;

/**
 * 创建下载任务响应 DTO
 *
 * @author 林风自在
 * @date 2026-03-31
 */
@Data
public class CreateDownloadResponse {
    private String taskId;

    public static CreateDownloadResponse of(String taskId) {
        CreateDownloadResponse resp = new CreateDownloadResponse();
        resp.setTaskId(taskId);
        return resp;
    }
}
