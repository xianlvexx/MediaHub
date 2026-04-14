package cn.xlvexx.mediahub.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 视频解析响应 DTO
 *
 * @author 林风自在
 * @date 2026-04-01
 */
@Data
public class ParseResponse {

    private String videoId;
    private String title;
    private String thumbnail;
    private Integer duration;
    private String uploader;
    private String platform;
    private List<FormatItem> formats;

    @Data
    public static class FormatItem {
        private String formatId;
        private String formatNote;
        private String ext;
        private Long filesize;
        private String vcodec;
        private String acodec;
        private Double tbr;
    }
}
