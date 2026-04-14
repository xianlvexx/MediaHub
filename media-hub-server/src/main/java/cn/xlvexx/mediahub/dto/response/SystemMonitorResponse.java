package cn.xlvexx.mediahub.dto.response;

import lombok.Data;

/**
 * 系统监控响应 DTO（CPU/内存/磁盘/JVM/任务统计）
 *
 * @author 林风自在
 * @date 2026-03-29
 */
@Data
public class SystemMonitorResponse {

    private Cpu cpu;
    private Memory systemMemory;
    private JvmMemory jvmMemory;
    private Disk disk;
    private Runtime runtime;
    private Tasks tasks;

    @Data
    public static class Cpu {
        private double usage;
        private int processors;
    }

    @Data
    public static class Memory {
        private long total;
        private long used;
    }

    @Data
    public static class JvmMemory {
        private long used;
        private long total;
        private long max;
    }

    @Data
    public static class Disk {
        private long total;
        private long used;
    }

    @Data
    public static class Runtime {
        private long uptime;
        private long startTime;
        private long pid;
        private String osName;
        private int threads;
        private int daemonThreads;
    }

    @Data
    public static class Tasks {
        private long total;
        private long running;
        private long pending;
        private long completed;
        private long failed;
        private long cancelled;
    }
}
