package cn.xlvexx.mediahub.dto.response;

import lombok.Data;

/**
 * 验证码响应 DTO
 *
 * @author 林风自在
 * @date 2026-03-30
 */
@Data
public class CaptchaResponse {
    private String captchaId;
    private String captchaImage;

    public static CaptchaResponse of(String captchaId, String captchaImage) {
        CaptchaResponse resp = new CaptchaResponse();
        resp.setCaptchaId(captchaId);
        resp.setCaptchaImage(captchaImage);
        return resp;
    }
}
