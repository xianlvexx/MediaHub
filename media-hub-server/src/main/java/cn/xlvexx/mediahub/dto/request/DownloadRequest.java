package cn.xlvexx.mediahub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 视频下载请求 DTO
 *
 * @author 林风自在
 * @date 2026-03-31
 */
@Data
public class DownloadRequest {

    @NotBlank(message = "视频URL不能为空")
    private String url;

    @NotBlank(message = "格式ID不能为空")
    private String formatId;

    /** 平台标识，可选 */
    private String platform;
}
