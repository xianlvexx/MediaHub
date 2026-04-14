package cn.xlvexx.mediahub.dto;

import lombok.Data;

/**
 * 统一 API 响应体
 *
 * @author 林风自在
 * @date 2026-03-30
 */
@Data
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;

    private static <T> ApiResult<T> of(int code, String message, T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = code;
        r.message = message;
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> success(T data) {
        return of(0, "success", data);
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return of(code, message, null);
    }

}
