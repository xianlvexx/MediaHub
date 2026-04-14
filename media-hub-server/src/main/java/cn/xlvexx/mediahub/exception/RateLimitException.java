package cn.xlvexx.mediahub.exception;

/**
 * IP 解析次数超限异常
 *
 * @author 林风自在
 * @date 2026-03-28
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
