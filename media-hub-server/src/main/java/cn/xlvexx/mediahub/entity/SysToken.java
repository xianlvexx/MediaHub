package cn.xlvexx.mediahub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统令牌实体
 *
 * @author 林风自在
 * @date 2026-03-28
 */
@Data
@TableName("sys_token")
public class SysToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String token;
    private Long userId;
    private String username;
    private String role;

    private Long expireAt;

    private LocalDateTime createdAt;
}
