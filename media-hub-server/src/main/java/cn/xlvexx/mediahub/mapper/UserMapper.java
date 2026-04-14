package cn.xlvexx.mediahub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.xlvexx.mediahub.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 *
 * @author 林风自在
 * @date 2026-03-28
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
