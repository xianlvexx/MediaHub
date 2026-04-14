package cn.xlvexx.mediahub.common;

/**
 * ThreadLocal 用户会话上下文
 *
 * @author 林风自在
 * @date 2026-03-31
 */
public class UserContextHolder {

    private static final ThreadLocal<UserSession> CONTEXT = new ThreadLocal<>();

    public static void set(UserSession session) { CONTEXT.set(session); }

    public static UserSession get() { return CONTEXT.get(); }

    public static void clear() { CONTEXT.remove(); }
}
