package cn.xlvexx.mediahub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IP 解析频率限制实体
 *
 * @author 林风自在
 * @date 2026-03-30
 */
@Data
@TableName("parse_rate_limit")
public class ParseRateLimit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ip;
    private LocalDate statDate;
    private Integer count;
    private LocalDateTime updatedAt;
}
