package cn.xlvexx.mediahub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.xlvexx.mediahub.config.AppProperties;
import cn.xlvexx.mediahub.dto.response.ParseResponse;
import cn.xlvexx.mediahub.entity.VideoInfo;
import cn.xlvexx.mediahub.executor.YtDlpExecutor;
import cn.xlvexx.mediahub.manager.VideoInfoManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 视频解析服务
 *
 * @author 林风自在
 * @date 2026-04-01
 */
@Slf4j
@Service
public class VideoParseService {

    @Resource
    private VideoInfoManager videoInfoManager;
    @Resource
    private YtDlpExecutor ytDlpExecutor;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private AppProperties appProperties;

    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("^https?://.+");

    /** yt-dlp extractor_key → 平台标识 */
    private static final Map<String, String> EXTRACTOR_PLATFORM_MAP = Map.of(
            "BiliBili",    "bilibili",
            "Youtube",     "youtube",
            "Douyin",      "douyin",
            "WeixinVideo", "shipinhao"
    );

    /** URL 前缀模式兜底映射 */
    private static final List<Map.Entry<Pattern, String>> URL_PLATFORM_PATTERNS = List.of(
            Map.entry(Pattern.compile("bilibili\\.com/video/"), "bilibili"),
            Map.entry(Pattern.compile("(youtube\\.com/watch|youtu\\.be/)"),  "youtube"),
            Map.entry(Pattern.compile("douyin\\.com/video/"),                "douyin"),
            Map.entry(Pattern.compile("(channels\\.weixin\\.qq\\.com|finder\\.video\\.qq\\.com)"), "shipinhao")
    );

    public ParseResponse parseVideo(String url) {
        validateUrl(url);

        String urlHash = sha256(url);
        VideoInfo cached = videoInfoManager.findValidCache(urlHash);
        if (cached != null) {
            log.info("命中视频缓存: urlHash={}", urlHash);
            return convertToResponse(cached);
        }

        log.info("调用yt-dlp解析视频: url={}", url);
        JsonNode jsonNode = ytDlpExecutor.parseVideoInfo(url);
        String platform = detectPlatform(url, jsonNode);
        VideoInfo videoInfo = saveToCache(url, urlHash, platform, jsonNode);
        return convertToResponse(videoInfo);
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("视频URL不能为空");
        }
        if (!HTTP_URL_PATTERN.matcher(url.trim()).find()) {
            throw new IllegalArgumentException("无效的视频URL格式");
        }
    }

    private String detectPlatform(String url, JsonNode json) {
        // 优先取 yt-dlp 返回的 extractor_key（最准确）
        String extractorKey = json.path("extractor_key").asText("");
        if (!extractorKey.isBlank()) {
            String platform = EXTRACTOR_PLATFORM_MAP.get(extractorKey);
            if (platform != null) {
                return platform;
            }
            // yt-dlp 有值但不在已知列表中，用小写 extractor_key 作为兜底
            return extractorKey.toLowerCase();
        }
        // 兜底：按 URL 正则匹配
        for (Map.Entry<Pattern, String> entry : URL_PLATFORM_PATTERNS) {
            if (entry.getKey().matcher(url).find()) {
                return entry.getValue();
            }
        }
        return "unknown";
    }

    private VideoInfo saveToCache(String url, String urlHash, String platform, JsonNode json) {
        VideoInfo info = new VideoInfo();
        info.setUrlHash(urlHash);
        info.setOriginalUrl(url);
        info.setPlatform(platform);
        info.setVideoId(json.path("id").asText(""));
        info.setTitle(json.path("title").asText("未知标题"));
        info.setDuration(json.path("duration").asInt(0));
        info.setThumbnail(json.path("thumbnail").asText(""));
        info.setUploader(json.path("uploader").asText("未知UP主"));
        info.setViewCount(json.path("view_count").asLong(0));
        try {
            info.setFormatsJson(objectMapper.writeValueAsString(json.path("formats")));
        } catch (Exception e) {
            info.setFormatsJson("[]");
        }
        info.setExpireAt(LocalDateTime.now().plusMinutes(appProperties.getCache().getVideoInfoTtlMinutes()));
        // 先查询是否存在旧记录（含已过期），有则更新，避免唯一键冲突
        VideoInfo existing = videoInfoManager.getOne(new LambdaQueryWrapper<VideoInfo>().eq(VideoInfo::getUrlHash, urlHash));
        if (existing != null) {
            info.setId(existing.getId());
            videoInfoManager.updateById(info);
        } else {
            videoInfoManager.save(info);
        }
        return info;
    }

    private ParseResponse convertToResponse(VideoInfo videoInfo) {
        ParseResponse resp = new ParseResponse();
        resp.setVideoId(videoInfo.getVideoId());
        resp.setTitle(videoInfo.getTitle());
        resp.setThumbnail(videoInfo.getThumbnail());
        resp.setDuration(videoInfo.getDuration());
        resp.setUploader(videoInfo.getUploader());
        resp.setPlatform(videoInfo.getPlatform());

        List<ParseResponse.FormatItem> formats = new ArrayList<>();
        try {
            JsonNode formatsNode = objectMapper.readTree(videoInfo.getFormatsJson());
            if (formatsNode.isArray()) {
                for (JsonNode f : formatsNode) {
                    String vcodec = f.path("vcodec").asText("none");
                    // 只返回有视频流的格式
                    if ("none".equals(vcodec)) {
                        continue;
                    }

                    ParseResponse.FormatItem item = new ParseResponse.FormatItem();
                    item.setFormatId(f.path("format_id").asText());
                    item.setFormatNote(buildFormatNote(f));
                    item.setExt(f.path("ext").asText("mp4"));
                    item.setFilesize(f.path("filesize").asLong(f.path("filesize_approx").asLong(0)));
                    item.setVcodec(vcodec);
                    item.setAcodec(f.path("acodec").asText("none"));
                    item.setTbr(f.path("tbr").asDouble(0));
                    formats.add(item);
                }
            }
        } catch (Exception e) {
            log.warn("解析formats JSON失败", e);
        }
        resp.setFormats(formats);
        return resp;
    }

    private String buildFormatNote(JsonNode f) {
        String note = f.path("format_note").asText("");
        // format_note 有值且不是纯数字，直接使用
        if (!note.isEmpty() && !note.matches("\\d+")) {
            return note;
        }

        // 尝试用 height 构建，如 "1080p"
        int height = f.path("height").asInt(0);
        if (height > 0) {
            return height + "p";
        }

        // 尝试用 resolution 字段
        String resolution = f.path("resolution").asText("");
        if (!resolution.isEmpty() && !"unknown".equals(resolution)) {
            return resolution;
        }

        // 最终兜底：用 format 字段（通常形如 "30080 - 1920x1080"）
        String format = f.path("format").asText("");
        if (!format.isEmpty()) {
            // 去掉前缀数字ID，只保留描述部分
            int dashIdx = format.indexOf(" - ");
            if (dashIdx >= 0 && dashIdx + 3 < format.length()) {
                return format.substring(dashIdx + 3);
            }
            return format;
        }
        return f.path("format_id").asText("未知");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256不可用", e);
        }
    }
}
