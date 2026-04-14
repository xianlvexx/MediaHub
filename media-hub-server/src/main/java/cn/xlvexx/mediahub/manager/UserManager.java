package cn.xlvexx.mediahub.manager;

import cn.xlvexx.mediahub.entity.User;
import cn.xlvexx.mediahub.mapper.UserMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Component;

/**
 * 用户数据访问层，封装用户信息的常用查询操作
 *
 * @author 林风自在
 * @date 2026-03-31
 */
@Component
public class UserManager extends ServiceImpl<UserMapper, User> {

    public User findByUsername(String username) {
        return lambdaQuery()
                .eq(User::getUsername, username)
                .last("LIMIT 1")
                .one();
    }
}
