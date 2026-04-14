package cn.xlvexx.mediahub.service;

import cn.xlvexx.mediahub.config.AppProperties;
import cn.xlvexx.mediahub.dto.response.TaskListResponse;
import cn.xlvexx.mediahub.entity.DownloadTask;
import cn.xlvexx.mediahub.enums.TaskStatus;
import cn.xlvexx.mediahub.executor.YtDlpExecutor;
import cn.xlvexx.mediahub.manager.DownloadTaskManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 下载服务
 *
 * @author 林风自在
 * @date 2026-04-03
 */
@Slf4j
@Service
public class DownloadService {

    @Resource
    private DownloadTaskManager taskManager;
    @Resource
    private YtDlpExecutor ytDlpExecutor;
    @Resource
    private SseService sseService;
    @Resource
    private AppProperties appProperties;

    public String createTask(String url, String formatId, String videoTitle,
                             String thumbnail, String videoId, String formatNote,
                             String platform, String ipAddress) {
        String taskId = UUID.randomUUID().toString();
        DownloadTask task = new DownloadTask();
        task.setTaskId(taskId);
        task.setVideoUrl(url);
        task.setVideoTitle(videoTitle);
        task.setVideoId(videoId);
        task.setThumbnail(thumbnail);
        task.setFormatId(formatId);
        task.setFormatNote(formatNote);
        task.setPlatform(platform != null ? platform : "");
        task.setStatus(TaskStatus.PENDING);
        task.setProgress(BigDecimal.ZERO);
        task.setIpAddress(ipAddress);
        taskManager.save(task);
        return taskId;
    }

    @Async("downloadTaskExecutor")
    public void executeDownload(String taskId) {
        DownloadTask task = taskManager.findByTaskId(taskId);
        if (task == null) {
            log.error("下载任务不存在: taskId={}", taskId);
            return;
        }

        String outputDir = appProperties.getDownload().getDir() + "/" + taskId;
        new File(outputDir).mkdirs();

        taskManager.updateStatus(taskId, TaskStatus.RUNNING);
        sseService.sendProgress(taskId, 0.0, "", "", TaskStatus.RUNNING);

        try {
            ytDlpExecutor.downloadVideo(task.getVideoUrl(), task.getFormatId(), outputDir,
                    (percent, speed, eta) -> {
                        taskManager.updateProgress(taskId, percent, speed, eta);
                        sseService.sendProgress(taskId, percent, speed, eta, TaskStatus.RUNNING);
                    });

            File downloadedFile = findDownloadedFile(outputDir);
            if (downloadedFile == null) {
                throw new RuntimeException("下载完成但未找到文件");
            }

            taskManager.updateCompleted(taskId, downloadedFile.getAbsolutePath(), downloadedFile.getName(), downloadedFile.length());
            sseService.sendProgress(taskId, 100.0, "", "", TaskStatus.COMPLETED);
            log.info("下载完成: taskId={}, file={}", taskId, downloadedFile.getName());
        } catch (Exception e) {
            String errMsg = e.getMessage() != null ? e.getMessage() : "任务执行异常";
            log.error("下载失败: taskId={}, error={}", taskId, errMsg);
            taskManager.updateFailed(taskId, errMsg);
            sseService.sendError(taskId, errMsg);
        }
    }

    public DownloadTask getTask(String taskId) {
        return taskManager.findByTaskId(taskId);
    }

    public List<TaskListResponse> getTaskList(int page, int pageSize) {
        return taskManager.findPagedTasks(page, pageSize).stream().map(TaskListResponse::from).toList();
    }

    public long getTaskCount() {
        return taskManager.count();
    }

    public boolean deleteTask(String taskId) {
        DownloadTask task = taskManager.findByTaskId(taskId);
        if (task == null) {
            return false;
        }
        deleteTaskFiles(task);
        taskManager.removeById(task.getId());
        return true;
    }

    public void deleteTaskFiles(DownloadTask task) {
        String taskDir = appProperties.getDownload().getDir() + "/" + task.getTaskId();
        File dir = new File(taskDir);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
            log.info("已删除任务文件目录: {}", taskDir);
        }
    }

    private File findDownloadedFile(String outputDir) {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            return null;
        }

        try (Stream<Path> paths = Files.list(dir.toPath())) {
            return paths
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .filter(f -> !f.getName().endsWith(".part"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            log.warn("查找下载文件失败: dir={}", outputDir, e);
            return null;
        }
    }
}
