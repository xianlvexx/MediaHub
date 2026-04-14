package cn.xlvexx.mediahub.controller;

import cn.xlvexx.mediahub.service.SseService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 控制器
 *
 * @author 林风自在
 * @date 2026-03-28
 */
@RestController
@RequestMapping("/api")
public class SseController {

    @Resource
    private SseService sseService;

    @GetMapping(value = "/progress/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(@PathVariable String taskId) {
        return sseService.createEmitter(taskId);
    }
}
