package cn.xlvexx.mediahub.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.xlvexx.mediahub.config.AppProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * yt-dlp 外部进程封装器，提供视频信息解析和带进度回调的视频下载能力
 *
 * @author 林风自在
 * @date 2026-04-02
 */
@Slf4j
@Component
public class YtDlpExecutor {

    @Resource
    private AppProperties appProperties;
    @Resource
    private ObjectMapper objectMapper;

    private static final Pattern PROGRESS_PATTERN = Pattern.compile(
            "\\[download\\]\\s+([\\d.]+)%\\s+of.*?at\\s+([\\S]+)\\s+ETA\\s+(\\S+)");

    private static final Pattern PROGRESS_TEMPLATE_PATTERN = Pattern.compile("PROGRESS\\|([\\d.]+)%?\\|([^|]*)\\|([^|\\n]*)");

    /**
     * 解析视频信息（不下载）
     */
    public JsonNode parseVideoInfo(String url) {
        List<String> cmd = buildBaseCommand();
        cmd.add("--dump-json");
        cmd.add("--no-playlist");
        cmd.add("--socket-timeout");
        cmd.add("30");
        cmd.add(url);

        ProcessResult result = executeProcess(cmd);
        if (result.exitCode() != 0) {
            throw new YtDlpException("视频解析失败: " + result.stderr());
        }

        try {
            return objectMapper.readTree(result.stdout());
        } catch (Exception e) {
            throw new YtDlpException("解析yt-dlp输出JSON失败: " + e.getMessage());
        }
    }

    /**
     * 下载视频，通过回调报告进度
     */
    public void downloadVideo(String url, String formatId, String outputDir,
                              ProgressCallback progressCallback) {
        String outputTemplate = outputDir + "/%(title)s.%(ext)s";

        List<String> cmd = buildBaseCommand();
        cmd.add("--format");
        cmd.add(formatId + "+bestaudio/best");
        cmd.add("--merge-output-format");
        cmd.add("mp4");
        cmd.add("--output");
        cmd.add(outputTemplate);
        cmd.add("--progress-template");
        cmd.add("download:PROGRESS|%(progress._percent_str)s|%(progress._speed_str)s|%(progress._eta_str)s");
        cmd.add("--no-playlist");
        cmd.add("--socket-timeout");
        cmd.add("60");
        cmd.add("--retries");
        cmd.add("3");
        cmd.add("--no-overwrites");
        cmd.add(url);

        executeProcessWithProgress(cmd, progressCallback);
    }

    private List<String> buildBaseCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add(appProperties.getYtdlp().getPath());
        String cookiesFile = appProperties.getYtdlp().getCookiesFile();
        if (cookiesFile != null && !cookiesFile.isBlank()) {
            cmd.add("--cookies");
            cmd.add(cookiesFile);
        }
        return cmd;
    }

    private void executeProcessWithProgress(List<String> cmd, ProgressCallback callback) {
        log.info("执行yt-dlp下载命令: {}", String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("yt-dlp输出: {}", line);

                    // 先尝试 --progress-template 格式
                    Matcher m = PROGRESS_TEMPLATE_PATTERN.matcher(line);
                    if (m.find()) {
                        String percentStr = m.group(1).trim().replace("%", "");
                        try {
                            double percent = Double.parseDouble(percentStr);
                            callback.onProgress(percent, m.group(2).trim(), m.group(3).trim());
                        } catch (NumberFormatException ignored) {
                        }
                        continue;
                    }

                    // 兜底匹配 yt-dlp 默认进度格式
                    m = PROGRESS_PATTERN.matcher(line);
                    if (m.find()) {
                        try {
                            double percent = Double.parseDouble(m.group(1));
                            callback.onProgress(percent, m.group(2), m.group(3));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new YtDlpException("yt-dlp退出码: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new YtDlpException("yt-dlp进程执行异常: " + e.getMessage());
        }
    }

    private ProcessResult executeProcess(List<String> cmd) {
        log.info("执行yt-dlp命令: {}", String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);

        try {
            Process process = pb.start();

            String stdout;
            String stderr;
            try (var stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                 var stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {

                stdout = String.join("\n", stdoutReader.lines().toList());
                stderr = String.join("\n", stderrReader.lines().toList());
            }

            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, stdout, stderr);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new YtDlpException("yt-dlp进程执行异常: " + e.getMessage());
        }
    }

    public record ProcessResult(int exitCode, String stdout, String stderr) {}

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(double percent, String speed, String eta);
    }

    public static class YtDlpException extends RuntimeException {
        public YtDlpException(String message) { super(message); }
    }
}
