package cn.xlvexx.mediahub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author 林风自在
 * @date 2026-03-30
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("cn.xlvexx.mediahub.mapper")
public class MediaHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaHubApplication.class, args);
    }
}
