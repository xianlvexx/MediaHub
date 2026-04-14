package cn.xlvexx.mediahub.service;

import cn.xlvexx.mediahub.config.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 图像修复服务
 *
 * @author 林风自在
 * @date 2026-03-28
 */
@Slf4j
@Service
public class InpaintService {

    @Resource
    private AppProperties appProperties;
    private Path scriptPath;

    @PostConstruct
    private void init() {
        extractScript();
    }

    private void extractScript() {
        try {
            ClassPathResource resource = new ClassPathResource("scripts/inpaint.py");
            if (!resource.exists()) {
                log.warn("scripts/inpaint.py 未找到，后端水印去除不可用");
                return;
            }
            Path tmp = Files.createTempFile("mediahub_inpaint_", ".py");
            try (InputStream is = resource.getInputStream()) {
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            tmp.toFile().setExecutable(true);
            scriptPath = tmp;
            log.info("inpaint.py 已解压至: {}", scriptPath);
        } catch (IOException e) {
            log.error("inpaint.py 解压失败: {}", e.getMessage());
        }
    }

    public byte[] inpaint(byte[] imageBytes, int x, int y, int w, int h)
            throws IOException {
        if (scriptPath == null) {
            throw new IllegalStateException("inpaint 脚本不可用，请检查服务器部署");
        }

        String pythonPath = appProperties.getInpaint().getPythonPath();
        Path inputFile  = Files.createTempFile("mediahub_in_",  ".png");
        Path outputFile = Files.createTempFile("mediahub_out_", ".png");

        try {
            Files.write(inputFile, imageBytes);

            List<String> cmd = List.of(
                    pythonPath, scriptPath.toString(),
                    inputFile.toString(),
                    String.valueOf(x), String.valueOf(y),
                    String.valueOf(w), String.valueOf(h),
                    outputFile.toString()
            );

            log.info("执行 inpaint: x={} y={} w={} h={}", x, y, w, h);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // 异步读取 stderr，防止缓冲区写满导致进程阻塞
            StringBuilder stderrBuf = new StringBuilder();
            Thread stderrThread = new Thread(() -> {
                try {
                    stderrBuf.append(new String(process.getErrorStream().readAllBytes()));
                } catch (IOException ignored) {}
            });
            stderrThread.setDaemon(true);
            stderrThread.start();

            int timeoutSeconds = appProperties.getInpaint().getTimeoutSeconds();
            boolean finished;
            try {
                finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
                throw new IOException("inpaint 进程被中断");
            }

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("inpaint 超时（超过 " + timeoutSeconds + " 秒），请检查模型或减小图片尺寸");
            }

            try { stderrThread.join(2000); } catch (InterruptedException ignored) {}
            String stderr  = stderrBuf.toString();
            int    exitCode = process.exitValue();

            if (exitCode != 0) {
                log.error("inpaint 退出码={}, stderr={}", exitCode, stderr);
                if (exitCode == 2) {
                    throw new IllegalStateException("服务器缺少依赖，请执行: pip install simple-lama-inpainting torch torchvision");
                }
                throw new IOException("inpaint 处理失败: " + stderr);
            }

            log.info("inpaint 完成: {}", stderr.lines().filter(l -> !l.isBlank()).reduce((a, b) -> b).orElse("ok"));

            byte[] result = Files.readAllBytes(outputFile);
            log.info("inpaint 输出大小: {} bytes", result.length);
            return result;
        } finally {
            Files.deleteIfExists(inputFile);
            Files.deleteIfExists(outputFile);
        }
    }
}
