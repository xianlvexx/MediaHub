package cn.xlvexx.mediahub.mapper;

import cn.xlvexx.mediahub.entity.ParseRateLimit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * IP 解析频率 Mapper，扩展 upsert 自增操作
 *
 * @author 林风自在
 * @date 2026-03-30
 */
@Mapper
public interface ParseRateLimitMapper extends BaseMapper<ParseRateLimit> {

    void upsertIncrement(@Param("ip") String ip, @Param("date") LocalDate date);
}
