package cn.xlvexx.mediahub.common;

import cn.xlvexx.mediahub.config.AppProperties;
import cn.xlvexx.mediahub.entity.SysToken;
import cn.xlvexx.mediahub.manager.SysTokenManager;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token 存储器
 *
 * @author 林风自在
 * @date 2026-04-02
 */
@Component
public class TokenStore {

    @Resource
    private AppProperties appProperties;
    @Resource
    private SysTokenManager sysTokenManager;

    public String generateToken(Long userId, String username, String role) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + (long) appProperties.getAuth().getTokenExpireHours() * 3600_000;
        SysToken record = new SysToken();
        record.setToken(token);
        record.setUserId(userId);
        record.setUsername(username);
        record.setRole(role);
        record.setExpireAt(expireAt);
        record.setCreatedAt(LocalDateTime.now());
        sysTokenManager.save(record);
        return token;
    }

    public UserSession getSession(String token) {
        if (token == null) {
            return null;
        }
        SysToken record = sysTokenManager.findByToken(token);
        if (record == null) {
            return null;
        }
        if (System.currentTimeMillis() > record.getExpireAt()) {
            sysTokenManager.deleteByToken(token);
            return null;
        }
        return new UserSession(record.getUserId(), record.getUsername(), record.getRole(), record.getExpireAt());
    }

    public void revoke(String token) {
        if (token != null) {
            sysTokenManager.deleteByToken(token);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanExpired() {
        sysTokenManager.deleteExpired(System.currentTimeMillis());
    }
}
