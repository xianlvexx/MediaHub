package cn.xlvexx.mediahub.common;

/**
 * 认证用户会话信息
 *
 * @author 林风自在
 * @date 2026-03-29
 */
public record UserSession(Long userId, String username, String role, long expireAt) {
    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isMember() { return "MEMBER".equals(role); }
}
