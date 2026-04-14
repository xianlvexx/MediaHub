package cn.xlvexx.mediahub.exception;

import cn.xlvexx.mediahub.dto.ApiResult;
import cn.xlvexx.mediahub.executor.YtDlpExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;

/**
 * 全局异常处理器，统一转换异常为标准 API 响应
 *
 * @author 林风自在
 * @date 2026-04-03
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return ApiResult.error(1001, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return ApiResult.error(1002, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBind(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        return ApiResult.error(1002, message);
    }

    @ExceptionHandler(YtDlpExecutor.YtDlpException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleYtDlp(YtDlpExecutor.YtDlpException e) {
        log.error("yt-dlp执行失败: {}", e.getMessage());
        return ApiResult.error(2001, "视频解析失败：" + e.getMessage());
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResult<Void> handleRateLimit(RateLimitException e) {
        return ApiResult.error(4290, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResult<Void> handleIllegalState(IllegalStateException e) {
        log.warn("服务不可用: {}", e.getMessage());
        return ApiResult.error(5030, e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleIO(IOException e) {
        log.error("IO操作失败: {}", e.getMessage());
        return ApiResult.error(5000, "操作失败: " + e.getMessage());
    }

    @ExceptionHandler(RejectedExecutionException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResult<Void> handleRejected(RejectedExecutionException e) {
        log.warn("下载队列已满，拒绝新任务");
        return ApiResult.error(3001, "服务器繁忙，下载队列已满，请稍后重试");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResult<Void> handleNoResource(NoResourceFoundException e) {
        log.debug("资源不存在: {}", e.getResourcePath());
        return ApiResult.error(4040, "接口不存在: " + e.getResourcePath());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleGeneral(Exception e) {
        log.error("未知异常", e);
        return ApiResult.error(9999, "服务器内部错误");
    }
}
