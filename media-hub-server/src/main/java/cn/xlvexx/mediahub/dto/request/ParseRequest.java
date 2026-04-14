package cn.xlvexx.mediahub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 视频解析请求 DTO
 *
 * @author 林风自在
 * @date 2026-03-28
 */
@Data
public class ParseRequest {

    @NotBlank(message = "视频URL不能为空")
    private String url;

    /** 平台标识，可选，前端传入用于辅助校验；服务端以 yt-dlp extractor 为准 */
    private String platform;
}
