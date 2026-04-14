package cn.xlvexx.mediahub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 林风自在
 * @date 2026-04-03
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Ytdlp ytdlp = new Ytdlp();
    private Download download = new Download();
    private Cleanup cleanup = new Cleanup();
    private RateLimit rateLimit = new RateLimit();
    private Cache cache = new Cache();
    private Auth auth = new Auth();
    private Inpaint inpaint = new Inpaint();

    @Data
    public static class Ytdlp {
        private String path = "/usr/local/bin/yt-dlp";
        private String cookiesFile;
    }

    @Data
    public static class Download {
        private String dir = "/tmp/video-downloads";
        private int maxConcurrent = 5;
        private int queueCapacity = 10;
    }

    @Data
    public static class Cleanup {
        private int fileRetainHours = 720;
        private long scanIntervalMs = 600000;
    }

    @Data
    public static class RateLimit {
        private int parsePerIpPerMinute = 10;
    }

    @Data
    public static class Cache {
        private int videoInfoTtlMinutes = 60;
    }

    @Data
    public static class Auth {
        private int tokenExpireHours = 24;
    }

    @Data
    public static class Inpaint {
        private String pythonPath = "python3";
        private int timeoutSeconds = 300;
    }
}
