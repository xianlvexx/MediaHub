package cn.xlvexx.mediahub.dto.response;

import lombok.Data;

/**
 * Cookie 上传响应 DTO
 *
 * @author 林风自在
 * @date 2026-03-29
 */
@Data
public class CookieUploadResponse {
    private String path;
    private String size;

    public static CookieUploadResponse of(String path, String size) {
        CookieUploadResponse resp = new CookieUploadResponse();
        resp.setPath(path);
        resp.setSize(size);
        return resp;
    }
}
