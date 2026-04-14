package cn.xlvexx.mediahub.dto.response;

import lombok.Data;

/**
 * 登录响应 DTO
 *
 * @author 林风自在
 * @date 2026-04-03
 */
@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String role;

    public static LoginResponse of(String token, Long userId, String username, String role) {
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUserId(userId);
        resp.setUsername(username);
        resp.setRole(role);
        return resp;
    }
}
