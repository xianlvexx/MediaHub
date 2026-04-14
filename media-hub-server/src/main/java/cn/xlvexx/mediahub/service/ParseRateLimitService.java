package cn.xlvexx.mediahub.service;

import cn.xlvexx.mediahub.exception.RateLimitException;
import cn.xlvexx.mediahub.manager.ParseRateLimitManager;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 解析频率限制服务
 *
 * @author 林风自在
 * @date 2026-03-31
 */
@Service
public class ParseRateLimitService {

    private static final int GUEST_DAILY_LIMIT = 3;

    @Resource
    private ParseRateLimitManager rateLimitManager;

    public void checkAndIncrement(String ip) {
        LocalDate today = LocalDate.now();
        rateLimitManager.upsertIncrement(ip, today);
        Integer count = rateLimitManager.selectCount(ip, today);
        if (count != null && count > GUEST_DAILY_LIMIT) {
            throw new RateLimitException(
                "今日解析次数已达上限（" + GUEST_DAILY_LIMIT + " 次），登录后可无限使用"
            );
        }
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void cleanUp() {
        rateLimitManager.deleteExpiredBefore(LocalDate.now().minusDays(7));
    }
}
