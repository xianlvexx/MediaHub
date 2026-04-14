package cn.xlvexx.mediahub.manager;

import cn.xlvexx.mediahub.entity.SysToken;
import cn.xlvexx.mediahub.mapper.SysTokenMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Component;

/**
 * 系统令牌数据访问层，封装 Token 的查询、删除和过期清理操作
 *
 * @author 林风自在
 * @date 2026-04-01
 */
@Component
public class SysTokenManager extends ServiceImpl<SysTokenMapper, SysToken> {

    public SysToken findByToken(String token) {
        return lambdaQuery()
                .eq(SysToken::getToken, token)
                .last("LIMIT 1")
                .one();
    }

    public boolean deleteByToken(String token) {
        return lambdaUpdate()
                .eq(SysToken::getToken, token)
                .remove();
    }

    public boolean deleteExpired(long now) {
        return lambdaUpdate()
                .lt(SysToken::getExpireAt, now)
                .remove();
    }
}
