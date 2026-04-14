package cn.xlvexx.mediahub.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author 林风自在
 * @date 2026-03-30
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Resource
    private AppProperties appProperties;

    @Bean("downloadTaskExecutor")
    public Executor downloadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(appProperties.getDownload().getMaxConcurrent());
        executor.setQueueCapacity(appProperties.getDownload().getQueueCapacity());
        executor.setThreadNamePrefix("download-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
