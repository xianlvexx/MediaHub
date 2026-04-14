package cn.xlvexx.mediahub.controller;

import cn.xlvexx.mediahub.service.InpaintService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 图像水印修复控制器
 *
 * @author 林风自在
 * @date 2026-03-31
 */
@RestController
@RequestMapping("/api")
public class InpaintController {

    @Resource
    private InpaintService inpaintService;

    @PostMapping(value = "/inpaint", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> inpaint(@RequestPart("image") MultipartFile image, @RequestParam int x, @RequestParam int y, @RequestParam int w, @RequestParam int h) throws IOException {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("图片不能为空");
        }
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("选区尺寸无效");
        }
        byte[] result = inpaintService.inpaint(image.getBytes(), x, y, w, h);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(result);
    }
}
