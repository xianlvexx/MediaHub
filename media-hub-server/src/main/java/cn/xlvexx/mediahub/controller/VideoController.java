package cn.xlvexx.mediahub.controller;

import cn.xlvexx.mediahub.common.UserContextHolder;
import cn.xlvexx.mediahub.dto.ApiResult;
import cn.xlvexx.mediahub.dto.request.DownloadRequest;
import cn.xlvexx.mediahub.dto.request.ParseRequest;
import cn.xlvexx.mediahub.dto.response.CreateDownloadResponse;
import cn.xlvexx.mediahub.dto.response.PageResult;
import cn.xlvexx.mediahub.dto.response.ParseResponse;
import cn.xlvexx.mediahub.dto.response.TaskListResponse;
import cn.xlvexx.mediahub.entity.DownloadTask;
import cn.xlvexx.mediahub.service.DownloadService;
import cn.xlvexx.mediahub.service.ParseRateLimitService;
import cn.xlvexx.mediahub.service.VideoParseService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 视频控制器
 *
 * @author 林风自在
 * @date 2026-04-03
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class VideoController {

    @Resource
    private VideoParseService videoParseService;
    @Resource
    private DownloadService downloadService;
    @Resource
    private ParseRateLimitService parseRateLimitService;

    @PostMapping("/parse")
    public ApiResult<ParseResponse> parseVideo(@Valid @RequestBody ParseRequest request, HttpServletRequest httpRequest) {
        if (UserContextHolder.get() == null) {
            parseRateLimitService.checkAndIncrement(getClientIp(httpRequest));
        }
        return ApiResult.success(videoParseService.parseVideo(request.getUrl()));
    }

    @PostMapping("/download")
    public ApiResult<CreateDownloadResponse> createDownload(@Valid @RequestBody DownloadRequest request, HttpServletRequest httpRequest) {
        ParseResponse videoInfo = videoParseService.parseVideo(request.getUrl());
        String formatNote = videoInfo.getFormats().stream()
                .filter(f -> f.getFormatId().equals(request.getFormatId()))
                .map(ParseResponse.FormatItem::getFormatNote)
                .findFirst().orElse("");
        String taskId = downloadService.createTask(
                request.getUrl(), request.getFormatId(),
                videoInfo.getTitle(), videoInfo.getThumbnail(), videoInfo.getVideoId(),
                formatNote, videoInfo.getPlatform(), getClientIp(httpRequest));
        downloadService.executeDownload(taskId);
        return ApiResult.success(CreateDownloadResponse.of(taskId));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResult<DownloadTask> getTask(@PathVariable String taskId) {
        DownloadTask task = downloadService.getTask(taskId);
        if (task == null) {
            return ApiResult.error(4004, "任务不存在");
        }
        return ApiResult.success(task);
    }

    @GetMapping("/tasks/{taskId}/file")
    public ResponseEntity<?> downloadFile(@PathVariable String taskId) {
        DownloadTask task = downloadService.getTask(taskId);
        if (task == null || task.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(task.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResult.error(4100, "文件已失效"));
        }

        String encodedName = URLEncoder.encode(task.getFileName() != null ? task.getFileName() : file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(new FileSystemResource(file));
    }

    @GetMapping("/tasks")
    public ApiResult<PageResult<TaskListResponse>> getTaskList(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResult.success(PageResult.of(downloadService.getTaskList(page, pageSize), downloadService.getTaskCount(), page, pageSize));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ApiResult<Void> deleteTask(@PathVariable String taskId) {
        if (!downloadService.deleteTask(taskId)) {
            return ApiResult.error(4004, "任务不存在");
        }
        return ApiResult.success(null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
