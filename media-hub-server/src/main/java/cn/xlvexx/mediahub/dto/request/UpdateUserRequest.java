package cn.xlvexx.mediahub.dto.request;

import lombok.Data;

/**
 * 更新用户请求 DTO（字段均可选）
 *
 * @author 林风自在
 * @date 2026-04-02
 */
@Data
public class UpdateUserRequest {
    private String password;
    private String role;
    private Boolean enabled;
}
