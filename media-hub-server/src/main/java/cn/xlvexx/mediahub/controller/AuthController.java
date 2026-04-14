package cn.xlvexx.mediahub.controller;

import cn.xlvexx.mediahub.common.CaptchaStore;
import cn.xlvexx.mediahub.common.TokenStore;
import cn.xlvexx.mediahub.common.UserContextHolder;
import cn.xlvexx.mediahub.common.UserSession;
import cn.xlvexx.mediahub.dto.ApiResult;
import cn.xlvexx.mediahub.dto.request.LoginRequest;
import cn.xlvexx.mediahub.dto.response.CaptchaResponse;
import cn.xlvexx.mediahub.dto.response.LoginResponse;
import cn.xlvexx.mediahub.dto.response.UserVO;
import cn.xlvexx.mediahub.entity.User;
import cn.xlvexx.mediahub.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author 林风自在
 * @date 2026-04-02
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private UserService userService;
    @Resource
    private TokenStore tokenStore;
    @Resource
    private CaptchaStore captchaStore;

    @GetMapping("/captcha")
    public ApiResult<CaptchaResponse> captcha() {
        CaptchaStore.CaptchaResult result = captchaStore.generate();
        return ApiResult.success(CaptchaResponse.of(result.captchaId(), result.imageDataUrl()));
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        if (!captchaStore.verify(req.getCaptchaId(), req.getCaptchaCode())) {
            return ApiResult.error(4012, "验证码错误或已过期");
        }
        User user = userService.authenticate(req.getUsername(), req.getPassword());
        if (user == null) {
            return ApiResult.error(4011, "用户名或密码错误");
        }
        String token = tokenStore.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("用户登录成功：username={}, role={}", user.getUsername(), user.getRole());
        return ApiResult.success(LoginResponse.of(token, user.getId(), user.getUsername(), user.getRole()));
    }

    @GetMapping("/me")
    public ApiResult<UserVO> me() {
        UserSession session = UserContextHolder.get();
        if (session == null) {
            return ApiResult.error(4010, "未登录");
        }
        User user = userService.getById(session.userId());
        if (user == null) {
            return ApiResult.error(4010, "用户不存在");
        }
        return ApiResult.success(UserVO.from(user));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenStore.revoke(authHeader.substring(7));
        }
        return ApiResult.success(null);
    }
}
