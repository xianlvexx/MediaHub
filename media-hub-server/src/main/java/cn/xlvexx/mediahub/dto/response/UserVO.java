package cn.xlvexx.mediahub.dto.response;

import cn.xlvexx.mediahub.entity.User;
import lombok.Data;

import java.time.format.DateTimeFormatter;

/**
 * 用户视图对象（屏蔽密码等敏感字段）
 *
 * @author 林风自在
 * @date 2026-03-31
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String role;
    private Boolean enabled;
    private String createdAt;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setEnabled(user.getEnabled());
        vo.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().format(FMT) : null);
        return vo;
    }
}
