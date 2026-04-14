package cn.xlvexx.mediahub.common;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码存储器
 *
 * @author 林风自在
 * @date 2026-03-28
 */
@Component
public class CaptchaStore {

    private record Entry(String code, long expireAt) {}

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private static final long TTL_MS = 5 * 60 * 1000L;

    public CaptchaResult generate() {
        String code = String.format("%04d", new Random().nextInt(10000));
        String id = UUID.randomUUID().toString().replace("-", "");
        store.put(id, new Entry(code, System.currentTimeMillis() + TTL_MS));
        return new CaptchaResult(id, buildSvg(code));
    }

    public boolean verify(String captchaId, String inputCode) {
        if (captchaId == null || inputCode == null) {
            return false;
        }
        Entry entry = store.remove(captchaId);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expireAt()) {
            return false;
        }
        return entry.code().equalsIgnoreCase(inputCode.trim());
    }

    private String buildSvg(String code) {
        Random rand = new Random();
        var sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"120\" height=\"40\" viewBox=\"0 0 120 40\">");
        sb.append("<rect width=\"120\" height=\"40\" rx=\"6\" fill=\"#f5f7fa\"/>");

        String[] lineColors = {"#d9d9d9", "#c0c0c0", "#e0e0e0"};
        for (int i = 0; i < 4; i++) {
            sb.append(String.format(
                    "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"%s\" stroke-width=\"1.2\" opacity=\"0.7\"/>",
                    rand.nextInt(120), rand.nextInt(40),
                    rand.nextInt(120), rand.nextInt(40),
                    lineColors[rand.nextInt(lineColors.length)]));
        }
        for (int i = 0; i < 15; i++) {
            sb.append(String.format(
                    "<circle cx=\"%d\" cy=\"%d\" r=\"1\" fill=\"#bfbfbf\" opacity=\"0.5\"/>",
                    rand.nextInt(120), rand.nextInt(40)));
        }
        String[] colors = {"#1677ff", "#0958d9", "#389e0d", "#cf1322", "#722ed1", "#d46b08"};
        for (int i = 0; i < code.length(); i++) {
            int x = 16 + i * 24;
            int y = 26 + rand.nextInt(6) - 3;
            int rotate = rand.nextInt(24) - 12;
            String color = colors[(i + rand.nextInt(3)) % colors.length];
            sb.append(String.format(
                    "<text x=\"%d\" y=\"%d\" font-family=\"'Courier New',monospace\" font-size=\"22\" font-weight=\"700\" fill=\"%s\" transform=\"rotate(%d,%d,%d)\">%c</text>",
                    x, y, color, rotate, x, y, code.charAt(i)));
        }
        sb.append("</svg>");
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(bytes);
    }

    public record CaptchaResult(String captchaId, String imageDataUrl) {}
}
