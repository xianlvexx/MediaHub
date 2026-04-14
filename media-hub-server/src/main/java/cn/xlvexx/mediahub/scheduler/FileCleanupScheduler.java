package cn.xlvexx.mediahub.scheduler;

import cn.xlvexx.mediahub.config.AppProperties;
import cn.xlvexx.mediahub.entity.DownloadTask;
import cn.xlvexx.mediahub.manager.DownloadTaskManager;
import cn.xlvexx.mediahub.service.DownloadService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件清理定时任务，定期扫描并删除已完成或失败任务的过期下载文件
 *
 * @author 林风自在
 * @date 2026-04-01
 */
@Slf4j
@Component
public class FileCleanupScheduler {

    @Resource
    private DownloadTaskManager taskManager;
    @Resource
    private DownloadService downloadService;
    @Resource
    private AppProperties appProperties;

    @Scheduled(fixedDelayString = "${app.cleanup.scan-interval-ms:600000}")
    public void cleanupExpiredFiles() {
        int retainHours = appProperties.getCleanup().getFileRetainHours();
        List<DownloadTask> expiredTasks = taskManager.findExpiredTasks(retainHours);
        if (expiredTasks.isEmpty()) {
            return;
        }

        log.info("开始清理过期文件，共{}个任务", expiredTasks.size());
        for (DownloadTask task : expiredTasks) {
            try {
                downloadService.deleteTaskFiles(task);
                log.info("清理完成: taskId={}", task.getTaskId());
            } catch (Exception e) {
                log.error("清理文件失败: taskId={}", task.getTaskId(), e);
            }
        }
    }
}
