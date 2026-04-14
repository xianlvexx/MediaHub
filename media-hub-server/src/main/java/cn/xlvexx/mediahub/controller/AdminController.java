package cn.xlvexx.mediahub.controller;

import cn.xlvexx.mediahub.config.AppProperties;
import cn.xlvexx.mediahub.dto.ApiResult;
import cn.xlvexx.mediahub.dto.request.CreateUserRequest;
import cn.xlvexx.mediahub.dto.request.UpdateUserRequest;
import cn.xlvexx.mediahub.dto.response.CookieInfoResponse;
import cn.xlvexx.mediahub.dto.response.CookieUploadResponse;
import cn.xlvexx.mediahub.dto.response.PageResult;
import cn.xlvexx.mediahub.dto.response.SystemMonitorResponse;
import cn.xlvexx.mediahub.dto.response.UserVO;
import cn.xlvexx.mediahub.entity.DownloadTask;
import cn.xlvexx.mediahub.entity.User;
import cn.xlvexx.mediahub.manager.DownloadTaskManager;
import cn.xlvexx.mediahub.scheduler.SystemMetricsSampler;
import cn.xlvexx.mediahub.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员控制器
 *
 * @author 林风自在
 * @date 2026-03-29
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Resource
    private AppProperties appProperties;
    @Resource
    private UserService userService;
    @Resource
    private DownloadTaskManager taskManager;
    @Resource
    private SystemMetricsSampler metricsSampler;

    @PostMapping("/cookie")
    public ApiResult<CookieUploadResponse> uploadCookie(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ApiResult.error(4000, "文件不能为空");
        }

        String cookiesFilePath = appProperties.getYtdlp().getCookiesFile();
        if (cookiesFilePath == null || cookiesFilePath.isBlank()) {
            return ApiResult.error(5000, "服务端未配置 cookies 文件路径");
        }

        Path target = Paths.get(cookiesFilePath);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Cookie 文件已更新: {}, 大小: {} bytes", cookiesFilePath, file.getSize());
        return ApiResult.success(CookieUploadResponse.of(cookiesFilePath, String.valueOf(file.getSize())));
    }

    @GetMapping("/cookie/info")
    public ApiResult<CookieInfoResponse> getCookieInfo() throws Exception {
        String cookiesFilePath = appProperties.getYtdlp().getCookiesFile();
        if (cookiesFilePath == null || cookiesFilePath.isBlank()) {
            return ApiResult.success(CookieInfoResponse.notConfigured());
        }
        Path path = Paths.get(cookiesFilePath);
        boolean exists = Files.exists(path);
        Long size = null;
        Long lastModified = null;
        if (exists) {
            size = Files.size(path);
            lastModified = Files.getLastModifiedTime(path).toMillis();
        }
        return ApiResult.success(CookieInfoResponse.configured(cookiesFilePath, exists, size, lastModified));
    }

    @GetMapping("/users")
    public ApiResult<PageResult<UserVO>> listUsers(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        Page<User> result = userService.listUsers(page, pageSize);
        List<UserVO> list = result.getRecords().stream().map(UserVO::from).toList();
        return ApiResult.success(PageResult.of(list, result.getTotal(), page, pageSize));
    }

    @PostMapping("/users")
    public ApiResult<UserVO> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ApiResult.success(UserVO.from(userService.createUser(req.getUsername(), req.getPassword(), req.getRole())));
    }

    @PutMapping("/users/{id}")
    public ApiResult<Void> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        userService.updateUser(id, req.getPassword(), req.getRole(), req.getEnabled());
        return ApiResult.success(null);
    }

    @DeleteMapping("/users/{id}")
    public ApiResult<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResult.success(null);
    }

    @GetMapping("/monitor")
    public ApiResult<SystemMonitorResponse> getSystemMonitor() {
        SystemMonitorResponse data = new SystemMonitorResponse();

        SystemMonitorResponse.Cpu cpu = new SystemMonitorResponse.Cpu();
        cpu.setUsage(metricsSampler.getCpuUsage());
        cpu.setProcessors(metricsSampler.getLogicalProcessorCount());
        data.setCpu(cpu);

        SystemMonitorResponse.Memory systemMemory = new SystemMonitorResponse.Memory();
        systemMemory.setTotal(metricsSampler.getMemoryTotal());
        systemMemory.setUsed(metricsSampler.getMemoryUsed());
        data.setSystemMemory(systemMemory);

        Runtime javaRuntime = Runtime.getRuntime();
        long jvmTotal = javaRuntime.totalMemory();
        long jvmUsed = jvmTotal - javaRuntime.freeMemory();
        SystemMonitorResponse.JvmMemory jvmMemory = new SystemMonitorResponse.JvmMemory();
        jvmMemory.setUsed(jvmUsed);
        jvmMemory.setTotal(jvmTotal);
        jvmMemory.setMax(javaRuntime.maxMemory());
        data.setJvmMemory(jvmMemory);

        File diskRoot = new File(appProperties.getDownload().getDir());
        if (!diskRoot.exists()) {
            diskRoot = new File("/");
        }
        SystemMonitorResponse.Disk disk = new SystemMonitorResponse.Disk();
        disk.setTotal(diskRoot.getTotalSpace());
        disk.setUsed(diskRoot.getTotalSpace() - diskRoot.getUsableSpace());
        data.setDisk(disk);

        OperatingSystemMXBean osMx = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadMx = ManagementFactory.getThreadMXBean();
        RuntimeMXBean runtimeMx = ManagementFactory.getRuntimeMXBean();
        SystemMonitorResponse.Runtime runtime = new SystemMonitorResponse.Runtime();
        runtime.setUptime(runtimeMx.getUptime());
        runtime.setStartTime(runtimeMx.getStartTime());
        runtime.setPid(ProcessHandle.current().pid());
        runtime.setOsName(osMx.getName() + " " + osMx.getArch());
        runtime.setThreads(threadMx.getThreadCount());
        runtime.setDaemonThreads(threadMx.getDaemonThreadCount());
        data.setRuntime(runtime);

        // 单次 GROUP BY 保证任务统计快照的一致性
        List<Map<String, Object>> rows = taskManager.listMaps(new QueryWrapper<DownloadTask>().select("status", "count(*) as cnt").groupBy("status"));
        Map<String, Long> countByStatus = rows.stream().collect(Collectors.toMap(r -> (String) r.get("status"), r -> ((Number) r.get("cnt")).longValue()));
        SystemMonitorResponse.Tasks tasks = new SystemMonitorResponse.Tasks();
        tasks.setTotal(countByStatus.values().stream().mapToLong(Long::longValue).sum());
        tasks.setRunning(countByStatus.getOrDefault("RUNNING", 0L));
        tasks.setPending(countByStatus.getOrDefault("PENDING", 0L));
        tasks.setCompleted(countByStatus.getOrDefault("COMPLETED", 0L));
        tasks.setFailed(countByStatus.getOrDefault("FAILED", 0L));
        tasks.setCancelled(countByStatus.getOrDefault("CANCELLED", 0L));
        data.setTasks(tasks);

        return ApiResult.success(data);
    }
}
