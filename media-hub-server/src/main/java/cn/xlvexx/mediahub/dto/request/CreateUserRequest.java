package cn.xlvexx.mediahub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建用户请求 DTO
 *
 * @author 林风自在
 * @date 2026-03-29
 */
@Data
public class CreateUserRequest {
    @NotBlank private String username;
    @NotBlank private String password;
    @NotBlank private String role;
}
