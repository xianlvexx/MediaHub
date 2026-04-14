#!/usr/bin/env python3
"""
inpaint.py - AI 水印修复脚本
优先使用 LaMa AI 模型（效果最佳），未安装时自动降级到 OpenCV 算法。

安装 LaMa 依赖（一次性）:
    pip install simple-lama-inpainting torch torchvision
    首次运行会自动下载 LaMa 模型权重（约 200MB）

用法: python3 inpaint.py <input_path> <x> <y> <w> <h> <output_path>
退出码: 0=成功, 1=图片读取失败, 2=缺少必要依赖
"""
import sys


def main():
    if len(sys.argv) != 7:
        print("Usage: inpaint.py <input> <x> <y> <w> <h> <output>", file=sys.stderr)
        sys.exit(1)

    input_path  = sys.argv[1]
    x = int(sys.argv[2])
    y = int(sys.argv[3])
    w = int(sys.argv[4])
    h = int(sys.argv[5])
    output_path = sys.argv[6]

    # 优先尝试 LaMa AI 模型
    try:
        import simple_lama_inpainting  # noqa: F401
        _inpaint_lama(input_path, x, y, w, h, output_path)
        return
    except ImportError:
        print("simple-lama-inpainting 未安装，降级到 OpenCV 算法", file=sys.stderr)
        print("如需更好效果，请执行: pip install simple-lama-inpainting torch torchvision",
              file=sys.stderr)

    _inpaint_opencv(input_path, x, y, w, h, output_path)


# ─────────────────────────────────────────────────────────────────────────────
# LaMa AI 修复（首选）
# ─────────────────────────────────────────────────────────────────────────────

def _inpaint_lama(input_path, x, y, w, h, output_path):
    try:
        from simple_lama_inpainting import SimpleLama
        from PIL import Image
        import numpy as np
        import cv2
    except ImportError as e:
        print(f"缺少依赖: {e}", file=sys.stderr)
        sys.exit(2)

    # 读取图片，保留 alpha 通道
    try:
        img_full = Image.open(input_path)
    except Exception as e:
        print(f"无法读取图片: {e}", file=sys.stderr)
        sys.exit(1)

    has_alpha = img_full.mode == "RGBA"
    alpha_ch  = img_full.split()[3] if has_alpha else None
    img_rgb   = img_full.convert("RGB")
    img_arr   = np.array(img_rgb)
    ih, iw    = img_arr.shape[:2]

    # 边界校正
    x = max(0, min(x, iw - 1))
    y = max(0, min(y, ih - 1))
    w = max(1, min(w, iw - x))
    h = max(1, min(h, ih - y))

    # ── 前置检测：采样选区紧邻 1px 边缘，判断背景是否为纯色/渐变 ──────────────
    # 纯色/渐变底（低方差）→ 直接边缘填色，快速且无幻觉；复杂纹理 → 走 LaMa
    border_pixels = []
    if y > 0:       border_pixels.append(img_arr[y - 1, x:x + w].reshape(-1, 3))
    if y + h < ih:  border_pixels.append(img_arr[y + h, x:x + w].reshape(-1, 3))
    if x > 0:       border_pixels.append(img_arr[y:y + h, x - 1].reshape(-1, 3))
    if x + w < iw:  border_pixels.append(img_arr[y:y + h, x + w].reshape(-1, 3))
    if border_pixels:
        bp = np.concatenate(border_pixels, axis=0).astype(np.float32)
        # 过滤高亮文字像素（亮度 > 200 视为文字/高光，不纳入背景均匀性分析）
        brightness = bp.mean(axis=1)
        bp_bg = bp[brightness < 200]
        if len(bp_bg) < 4:
            bp_bg = bp  # 滤后像素太少则退回原始采样
        # 按通道分别求方差再均值——避免颜色通道数值差（如 R=220 vs B=30）虚高方差
        var = float(np.mean([np.var(bp_bg[:, c]) for c in range(3)]))
    else:
        var = 9999.0

    if var < 400.0:
        print(f"背景通道方差低({var:.1f}<400)，使用列背景采样重建（跳过 LaMa）", file=sys.stderr)
        result_arr = img_arr.copy()

        def bg_row_estimate(rows):
            """从多行像素中过滤文字像素（亮度>=200），返回每列背景色估计 (w,3)"""
            if len(rows) == 0:
                return None
            f      = rows.astype(np.float32)         # (n, w, 3)
            is_bg  = (f.mean(axis=2) < 200).astype(np.float32)  # (n, w)
            s      = (f * is_bg[:, :, np.newaxis]).sum(axis=0)  # (w, 3)
            cnt    = is_bg.sum(axis=0)[:, np.newaxis]            # (w, 1)
            return np.where(cnt > 0, s / np.maximum(cnt, 1), f.mean(axis=0))

        # 上下各取最多 15 行，过滤文字像素后取背景色
        top_rows = img_arr[max(0, y - 15):y,          x:x + w]
        bot_rows = img_arr[y + h:min(ih, y + h + 15), x:x + w]

        row_top = bg_row_estimate(top_rows)
        row_bot = bg_row_estimate(bot_rows)

        if row_top is None and row_bot is None:
            result_arr[y:y + h, x:x + w] = _boundary_fill(img_arr, x, y, w, h, iw, ih)
        else:
            if row_top is None: row_top = row_bot
            if row_bot is None: row_bot = row_top
            ts   = np.linspace(0, 1, h, dtype=np.float32).reshape(h, 1, 1)
            fill = ((1 - ts) * row_top[np.newaxis] + ts * row_bot[np.newaxis]).clip(0, 255).astype(np.uint8)
            result_arr[y:y + h, x:x + w] = fill

        result = Image.fromarray(result_arr)
        if has_alpha:
            result = result.convert("RGBA")
            result.putalpha(alpha_ch)
        result.save(output_path, format="PNG")
        print("列背景采样修复完成", file=sys.stderr)
        return

    # ── 复杂纹理背景：调用 LaMa ──────────────────────────────────────────────

    # mask 膨胀 4~8px，覆盖 logo 抗锯齿半透明边缘
    dilate_px = max(4, min(8, (min(w, h) * 8) // 100))
    kernel    = cv2.getStructuringElement(
        cv2.MORPH_ELLIPSE, (dilate_px * 2 + 1, dilate_px * 2 + 1)
    )
    mask_arr = np.zeros((ih, iw), dtype=np.uint8)
    mask_arr[y:y + h, x:x + w] = 255
    mask_dilated = cv2.dilate(mask_arr, kernel)

    # pad 上限 50px：足够感知背景纹理，避免拉入周边文字干扰重建
    pad = max(20, min(max(w, h), 50))
    cx1 = max(0, x - pad)
    cy1 = max(0, y - pad)
    cx2 = min(iw, x + w + pad)
    cy2 = min(ih, y + h + pad)

    crop_img  = Image.fromarray(img_arr[cy1:cy2, cx1:cx2])
    crop_mask = Image.fromarray(mask_dilated[cy1:cy2, cx1:cx2], mode="L")

    # 限制送入 LaMa 的 crop 尺寸，避免大图 CPU 推理超时
    MAX_LAMA_DIM = 512
    crop_w_orig, crop_h_orig = crop_img.size  # PIL: (width, height)
    scale = min(MAX_LAMA_DIM / crop_w_orig, MAX_LAMA_DIM / crop_h_orig)
    if scale < 1.0:
        lama_w = int(crop_w_orig * scale)
        lama_h = int(crop_h_orig * scale)
        crop_img  = crop_img.resize((lama_w, lama_h), Image.LANCZOS)
        crop_mask = crop_mask.resize((lama_w, lama_h), Image.NEAREST)
        print(
            f"LaMa 修复: x={x} y={y} w={w} h={h} dilate={dilate_px}px "
            f"crop=({cx1},{cy1})-({cx2},{cy2}) 缩放至 {lama_w}x{lama_h} (CPU 模式，请耐心等待…)",
            file=sys.stderr
        )
    else:
        print(
            f"LaMa 修复: x={x} y={y} w={w} h={h} dilate={dilate_px}px "
            f"crop=({cx1},{cy1})-({cx2},{cy2}) (CPU 模式，请耐心等待…)",
            file=sys.stderr
        )

    # 强制 CPU 加载：patch torch.jit.load，避免无 GPU 环境下的 RuntimeError
    import torch
    _orig_jit_load = torch.jit.load
    torch.jit.load = lambda *a, **kw: _orig_jit_load(*a, **{**kw, "map_location": torch.device("cpu")})
    try:
        model = SimpleLama()
    finally:
        torch.jit.load = _orig_jit_load
    result_crop = np.array(model(crop_img, crop_mask))

    # LaMa 内部会将图片 resize 到 8 的倍数，输出尺寸可能与输入不同，需还原
    crop_h, crop_w = cy2 - cy1, cx2 - cx1
    if result_crop.shape[0] != crop_h or result_crop.shape[1] != crop_w:
        result_crop = cv2.resize(result_crop, (crop_w, crop_h), interpolation=cv2.INTER_LINEAR)

    # 羽化混合回原图，消除修复区域与原图的硬拼接边缘
    feather_r  = dilate_px * 4 + 1          # 保证为奇数
    blend_mask = mask_dilated[cy1:cy2, cx1:cx2].astype(np.float32) / 255.0
    blend_mask = cv2.GaussianBlur(blend_mask, (feather_r, feather_r), 0)
    blend_mask = blend_mask[:, :, np.newaxis]  # H,W → H,W,1 便于广播

    orig_crop = img_arr[cy1:cy2, cx1:cx2].astype(np.float32)
    blended   = (orig_crop * (1.0 - blend_mask)
                 + result_crop.astype(np.float32) * blend_mask)

    result_arr = img_arr.copy()
    result_arr[cy1:cy2, cx1:cx2] = blended.clip(0, 255).astype(np.uint8)

    result = Image.fromarray(result_arr)

    # 恢复 alpha 通道
    if has_alpha:
        result = result.convert("RGBA")
        result.putalpha(alpha_ch)

    result.save(output_path, format="PNG")
    print("LaMa 修复完成", file=sys.stderr)


# ─────────────────────────────────────────────────────────────────────────────
# OpenCV 降级算法（LaMa 不可用时兜底）
# ─────────────────────────────────────────────────────────────────────────────

def _inpaint_opencv(input_path, x, y, w, h, output_path):
    try:
        import cv2
        import numpy as np
    except ImportError as e:
        print(f"缺少依赖: {e}。请执行: pip install opencv-python", file=sys.stderr)
        sys.exit(2)

    img = cv2.imread(input_path, cv2.IMREAD_UNCHANGED)
    if img is None:
        print(f"无法读取图片: {input_path}", file=sys.stderr)
        sys.exit(1)

    if len(img.shape) == 3 and img.shape[2] == 4:
        bgr, alpha = img[:, :, :3], img[:, :, 3]
    else:
        bgr   = img if len(img.shape) == 3 else cv2.cvtColor(img, cv2.COLOR_GRAY2BGR)
        alpha = None

    ih, iw = bgr.shape[:2]
    x = max(0, min(x, iw - 1))
    y = max(0, min(y, ih - 1))
    w = max(1, min(w, iw - x))
    h = max(1, min(h, ih - y))

    # 分析周边方差，选择算法
    sb = max(6, min(30, min(w, h) // 2))
    sx, sy = max(0, x - sb), max(0, y - sb)
    ex, ey = min(iw, x + w + sb), min(ih, y + h + sb)
    region = bgr[sy:ey, sx:ex].astype(np.float32)
    outer  = np.ones(region.shape[:2], dtype=bool)
    outer[y - sy:y - sy + h, x - sx:x - sx + w] = False
    border_px = region[outer]
    variance  = float(np.var(border_px)) if len(border_px) > 0 else 9999.0
    is_smooth = variance < 400.0

    print(f"OpenCV 降级修复: smooth={is_smooth} var={variance:.1f}", file=sys.stderr)

    if is_smooth:
        result = bgr.copy()
        result[y:y + h, x:x + w] = _boundary_fill(bgr, x, y, w, h, iw, ih)
    else:
        mask   = np.zeros((ih, iw), dtype=np.uint8)
        mask[y:y + h, x:x + w] = 255
        radius = max(3, min(7, min(w, h) // 6))
        result = cv2.inpaint(bgr, mask, radius, cv2.INPAINT_TELEA)

    if alpha is not None:
        result = cv2.merge([result, alpha])

    cv2.imwrite(output_path, result)


def _boundary_fill(bgr, x, y, w, h, iw, ih):
    import numpy as np

    row_idx = np.arange(h, dtype=np.float64).reshape(h, 1)
    col_idx = np.arange(w, dtype=np.float64).reshape(1, w)
    acc  = np.zeros((h, w, 3), dtype=np.float64)
    wsum = np.zeros((h, w),    dtype=np.float64)

    if y > 0:
        d = row_idx + 1
        row_t = bgr[y - 1, x:x + w].astype(np.float64)
        acc  += row_t[np.newaxis, :, :] * (1.0 / d)[:, :, np.newaxis]
        wsum += (1.0 / d).squeeze(1)[:, np.newaxis] * np.ones((1, w))

    if y + h < ih:
        d = h - row_idx
        row_b = bgr[y + h, x:x + w].astype(np.float64)
        acc  += row_b[np.newaxis, :, :] * (1.0 / d)[:, :, np.newaxis]
        wsum += (1.0 / d).squeeze(1)[:, np.newaxis] * np.ones((1, w))

    if x > 0:
        d = col_idx + 1                                   # shape (1, w)
        col_l = bgr[y:y + h, x - 1].astype(np.float64)  # shape (h, 3)
        acc  += col_l[:, np.newaxis, :] * (1.0 / d)[:, :, np.newaxis]  # (h,1,3)*(1,w,1)->(h,w,3)
        wsum += np.ones((h, 1)) * (1.0 / d)              # (h,1)*(1,w)->(h,w)

    if x + w < iw:
        d = w - col_idx                                   # shape (1, w)
        col_r = bgr[y:y + h, x + w].astype(np.float64)  # shape (h, 3)
        acc  += col_r[:, np.newaxis, :] * (1.0 / d)[:, :, np.newaxis]  # (h,1,3)*(1,w,1)->(h,w,3)
        wsum += np.ones((h, 1)) * (1.0 / d)              # (h,1)*(1,w)->(h,w)

    zero = wsum == 0
    if zero.any():
        fb = np.median(bgr[y:y + h, x:x + w].reshape(-1, 3), axis=0).astype(np.float64)
        acc[zero], wsum[zero] = fb, 1.0

    return (acc / wsum[:, :, np.newaxis]).clip(0, 255).astype(np.uint8)


if __name__ == '__main__':
    main()
