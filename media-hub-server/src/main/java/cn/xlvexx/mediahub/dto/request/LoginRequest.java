package cn.xlvexx.mediahub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 *
 * @author 林风自在
 * @date 2026-04-01
 */
@Data
public class LoginRequest {
    @NotBlank private String username;
    @NotBlank private String password;
    @NotBlank private String captchaId;
    @NotBlank private String captchaCode;
}
