package cn.xlvexx.mediahub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.xlvexx.mediahub.dto.response.ProgressEvent;
import cn.xlvexx.mediahub.enums.TaskStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 推送服务
 *
 * @author 林风自在
 * @date 2026-04-02
 */
@Slf4j
@Service
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    @Resource
    private ObjectMapper objectMapper;

    public SseEmitter createEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(3600_000L); // 1小时超时
        emitter.onCompletion(() -> {
            log.debug("SSE连接完成: taskId={}", taskId);
            emitters.remove(taskId);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE连接超时: taskId={}", taskId);
            emitters.remove(taskId);
        });
        emitter.onError(e -> {
            log.debug("SSE连接异常: taskId={}, error={}", taskId, e.getMessage());
            emitters.remove(taskId);
        });
        emitters.put(taskId, emitter);
        return emitter;
    }

    public void sendProgress(String taskId, double progress, String speed, String eta, TaskStatus status) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter == null) {
            return;
        }
        try {
            ProgressEvent event = ProgressEvent.builder()
                    .taskId(taskId).status(status).progress(progress).speed(speed).eta(eta).build();
            emitter.send(SseEmitter.event().name("progress").data(objectMapper.writeValueAsString(event)));
            // 任务完成或失败后关闭连接
            if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
                emitter.complete();
                emitters.remove(taskId);
            }
        } catch (IOException e) {
            log.warn("SSE推送失败: taskId={}, error={}", taskId, e.getMessage());
            emitters.remove(taskId);
        }
    }

    public void sendError(String taskId, String errorMsg) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter == null) {
            return;
        }
        try {
            ProgressEvent event = ProgressEvent.builder()
                    .taskId(taskId).status(TaskStatus.FAILED).progress(0.0).errorMsg(errorMsg).build();
            emitter.send(SseEmitter.event().name("error").data(objectMapper.writeValueAsString(event)));
            emitter.complete();
        } catch (IOException e) {
            log.warn("SSE错误推送失败: taskId={}", taskId);
        } finally {
            emitters.remove(taskId);
        }
    }
}
