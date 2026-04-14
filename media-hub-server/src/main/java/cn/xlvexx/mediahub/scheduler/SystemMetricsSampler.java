package cn.xlvexx.mediahub.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

/**
 * 系统指标采样器，定时采集 CPU 使用率和内存信息供监控接口使用
 *
 * @author 林风自在
 * @date 2026-03-29
 */
@Slf4j
@Component
public class SystemMetricsSampler {

    private final CentralProcessor processor;
    private final GlobalMemory memory;

    private long[] prevCpuTicks;
    private volatile double cpuUsage = 0.0;

    public SystemMetricsSampler() {
        SystemInfo si = new SystemInfo();
        this.processor = si.getHardware().getProcessor();
        this.memory = si.getHardware().getMemory();
        this.prevCpuTicks = processor.getSystemCpuLoadTicks();
    }

    @Scheduled(fixedDelay = 3000)
    public void sampleCpu() {
        long[] ticks = processor.getSystemCpuLoadTicks();
        this.cpuUsage = processor.getSystemCpuLoadBetweenTicks(prevCpuTicks) * 100;
        this.prevCpuTicks = ticks;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public int getLogicalProcessorCount() {
        return processor.getLogicalProcessorCount();
    }

    public long getMemoryTotal() {
        return memory.getTotal();
    }

    public long getMemoryUsed() {
        return memory.getTotal() - memory.getAvailable();
    }
}
