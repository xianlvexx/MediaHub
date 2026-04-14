package cn.xlvexx.mediahub.service;

import cn.xlvexx.mediahub.entity.User;
import cn.xlvexx.mediahub.manager.UserManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务
 *
 * @author 林风自在
 * @date 2026-03-30
 */
@Slf4j
@Service
public class UserService {

    @Resource
    private UserManager userManager;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User authenticate(String username, String rawPassword) {
        User user = userManager.findByUsername(username);
        if (user == null) return null;
        if (!Boolean.TRUE.equals(user.getEnabled())) return null;
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) return null;
        return user;
    }

    public Page<User> listUsers(int page, int pageSize) {
        return userManager.page(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
    }

    public User createUser(String username, String rawPassword, String role) {
        if (userManager.findByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role.toUpperCase());
        user.setEnabled(true);
        userManager.save(user);
        return user;
    }

    public void updateUser(Long id, String rawPassword, String role, Boolean enabled) {
        User user = userManager.getById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        if (role != null && !role.isBlank()) {
            user.setRole(role.toUpperCase());
        }
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        userManager.updateById(user);
    }

    public void deleteUser(Long id) {
        User user = userManager.getById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if ("ADMIN".equals(user.getRole())) {
            long adminCount = userManager.count(
                    new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
            if (adminCount <= 1) throw new IllegalArgumentException("不能删除最后一个管理员账号");
        }
        userManager.removeById(id);
    }

    public User getById(Long id) {
        return userManager.getById(id);
    }
}
