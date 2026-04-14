package cn.xlvexx.mediahub.manager;

import cn.xlvexx.mediahub.entity.ParseRateLimit;
import cn.xlvexx.mediahub.mapper.ParseRateLimitMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * IP 解析频率数据层
 *
 * @author 林风自在
 * @date 2026-03-29
 */
@Component
public class ParseRateLimitManager extends ServiceImpl<ParseRateLimitMapper, ParseRateLimit> {

    public void upsertIncrement(String ip, LocalDate date) {
        baseMapper.upsertIncrement(ip, date);
    }

    public Integer selectCount(String ip, LocalDate date) {
        ParseRateLimit record = lambdaQuery()
                .eq(ParseRateLimit::getIp, ip)
                .eq(ParseRateLimit::getStatDate, date)
                .one();
        return record != null ? record.getCount() : null;
    }

    public void deleteExpiredBefore(LocalDate date) {
        lambdaUpdate()
                .lt(ParseRateLimit::getStatDate, date)
                .remove();
    }
}
