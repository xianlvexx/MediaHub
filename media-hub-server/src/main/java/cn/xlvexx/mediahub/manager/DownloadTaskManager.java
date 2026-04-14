package cn.xlvexx.mediahub.manager;

import cn.xlvexx.mediahub.entity.DownloadTask;
import cn.xlvexx.mediahub.enums.TaskStatus;
import cn.xlvexx.mediahub.mapper.DownloadTaskMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 下载任务数据访问层，封装下载任务的常用查询与状态更新操作
 *
 * @author 林风自在
 * @date 2026-03-30
 */
@Component
public class DownloadTaskManager extends ServiceImpl<DownloadTaskMapper, DownloadTask> {

    public DownloadTask findByTaskId(String taskId) {
        return lambdaQuery()
                .eq(DownloadTask::getTaskId, taskId)
                .one();
    }

    public boolean updateStatus(String taskId, TaskStatus status) {
        return lambdaUpdate()
                .eq(DownloadTask::getTaskId, taskId)
                .set(DownloadTask::getStatus, status)
                .set(DownloadTask::getUpdatedAt, LocalDateTime.now())
                .update();
    }

    public boolean updateProgress(String taskId, double progress, String speed, String eta) {
        return lambdaUpdate()
                .eq(DownloadTask::getTaskId, taskId)
                .set(DownloadTask::getProgress, BigDecimal.valueOf(progress))
                .set(DownloadTask::getSpeed, speed)
                .set(DownloadTask::getEta, eta)
                .set(DownloadTask::getUpdatedAt, LocalDateTime.now())
                .update();
    }

    public boolean updateCompleted(String taskId, String filePath, String fileName, long fileSize) {
        return lambdaUpdate()
                .eq(DownloadTask::getTaskId, taskId)
                .set(DownloadTask::getStatus, TaskStatus.COMPLETED)
                .set(DownloadTask::getProgress, BigDecimal.valueOf(100.0))
                .set(DownloadTask::getFilePath, filePath)
                .set(DownloadTask::getFileName, fileName)
                .set(DownloadTask::getFileSize, fileSize)
                .set(DownloadTask::getCompletedAt, LocalDateTime.now())
                .set(DownloadTask::getUpdatedAt, LocalDateTime.now())
                .update();
    }

    public boolean updateFailed(String taskId, String errorMsg) {
        return lambdaUpdate()
                .eq(DownloadTask::getTaskId, taskId)
                .set(DownloadTask::getStatus, TaskStatus.FAILED)
                .set(DownloadTask::getErrorMsg, errorMsg)
                .set(DownloadTask::getCompletedAt, LocalDateTime.now())
                .set(DownloadTask::getUpdatedAt, LocalDateTime.now())
                .update();
    }

    public List<DownloadTask> findExpiredTasks(int hours) {
        return lambdaQuery()
                .in(DownloadTask::getStatus, TaskStatus.COMPLETED, TaskStatus.FAILED)
                .lt(DownloadTask::getCompletedAt, LocalDateTime.now().minusHours(hours))
                .list();
    }

    public List<DownloadTask> findPagedTasks(int page, int pageSize) {
        return lambdaQuery()
                .orderByDesc(DownloadTask::getCreatedAt)
                .page(new Page<>(page, pageSize))
                .getRecords();
    }
}
