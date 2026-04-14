package cn.xlvexx.mediahub.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.xlvexx.mediahub.dto.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * 认证拦截器
 *
 * @author 林风自在
 * @date 2026-04-01
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private TokenStore tokenStore;
    @Resource
    private ObjectMapper objectMapper;

    /** 校验 Token，/api/admin/** 接口额外要求管理员权限 */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = extractToken(request);
        UserSession session = tokenStore.getSession(token);
        if (session != null) {
            UserContextHolder.set(session);
        }

        if (request.getRequestURI().startsWith("/api/admin/")) {
            if (session == null || !session.isAdmin()) {
                write401(response, "需要管理员权限");
                return false;
            }
        }
        return true;
    }

    /** 请求结束后清理 ThreadLocal 用户上下文 */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    /** 从 Authorization 头提取 Bearer Token，未携带返回 null */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void write401(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.error(4010, message)));
    }
}
