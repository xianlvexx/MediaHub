package cn.xlvexx.mediahub.dto.response;

import lombok.Data;

/**
 * Cookie 文件信息响应 DTO
 *
 * @author 林风自在
 * @date 2026-04-01
 */
@Data
public class CookieInfoResponse {
    private boolean configured;
    private String path;
    private Boolean exists;
    private Long size;
    private Long lastModified;

    public static CookieInfoResponse notConfigured() {
        CookieInfoResponse resp = new CookieInfoResponse();
        resp.setConfigured(false);
        return resp;
    }

    public static CookieInfoResponse configured(String path, boolean exists, Long size, Long lastModified) {
        CookieInfoResponse resp = new CookieInfoResponse();
        resp.setConfigured(true);
        resp.setPath(path);
        resp.setExists(exists);
        resp.setSize(size);
        resp.setLastModified(lastModified);
        return resp;
    }
}
