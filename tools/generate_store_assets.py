#!/usr/bin/env python3
import math
import os
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFilter, ImageFont
except Exception as exc:  # pragma: no cover - runtime dependency
    raise SystemExit("Pillow is required. Install with: python3 -m pip install --user pillow") from exc

ROOT = Path(__file__).resolve().parent.parent
OUTPUT_ICON = ROOT / "store_assets" / "icon" / "BreakoutPlus-icon-512.png"
OUTPUT_FEATURE = ROOT / "store_assets" / "feature_graphic" / "BreakoutPlus-feature-1024x500.png"


def blend(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3)) + (255,)


def radial_gradient(size, inner, outer):
    img = Image.new("RGBA", (size, size), outer + (255,))
    draw = ImageDraw.Draw(img)
    cx = cy = size // 2
    max_r = size // 2
    for r in range(max_r, 0, -1):
        t = 1 - (r / max_r)
        color = blend(inner, outer, t)
        bbox = (cx - r, cy - r, cx + r, cy + r)
        draw.ellipse(bbox, fill=color)
    return img


def add_glow(base, shape_fn, color, blur=18, alpha=180):
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    shape_fn(draw, color + (alpha,))
    glow = layer.filter(ImageFilter.GaussianBlur(radius=blur))
    base.alpha_composite(glow)


def draw_icon(size=512):
    inner = (10, 28, 60)
    outer = (4, 6, 16)
    img = radial_gradient(size, inner, outer)

    # Diagonal glow
    overlay = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_overlay = ImageDraw.Draw(overlay)
    draw_overlay.polygon([(0, size * 0.15), (size, 0), (size, size * 0.45), (0, size * 0.6)],
                         fill=(40, 120, 255, 35))
    draw_overlay.polygon([(0, size * 0.6), (size, size * 0.35), (size, size * 0.7), (0, size * 0.95)],
                         fill=(255, 60, 120, 30))
    img.alpha_composite(overlay)

    draw = ImageDraw.Draw(img)

    # Orbit ring
    ring_outer = size * 0.82
    ring_inner = size * 0.72
    ring_bbox = (
        (size - ring_outer) / 2,
        (size - ring_outer) / 2,
        (size + ring_outer) / 2,
        (size + ring_outer) / 2,
    )

    def ring_shape(d, rgba):
        d.ellipse(ring_bbox, outline=rgba, width=int(size * 0.03))

    add_glow(img, ring_shape, (90, 200, 255), blur=22, alpha=160)
    ring_shape(draw, (120, 220, 255, 220))

    # Plus sign
    plus_size = size * 0.22
    plus_thickness = size * 0.05
    cx = cy = size / 2

    def plus_shape(d, rgba):
        d.rounded_rectangle(
            (cx - plus_thickness / 2, cy - plus_size / 2, cx + plus_thickness / 2, cy + plus_size / 2),
            radius=plus_thickness / 2,
            fill=rgba,
        )
        d.rounded_rectangle(
            (cx - plus_size / 2, cy - plus_thickness / 2, cx + plus_size / 2, cy + plus_thickness / 2),
            radius=plus_thickness / 2,
            fill=rgba,
        )

    add_glow(img, plus_shape, (255, 255, 255), blur=16, alpha=180)
    plus_shape(draw, (245, 245, 255, 230))

    # Paddle
    paddle_w = size * 0.42
    paddle_h = size * 0.055
    paddle_y = size * 0.72
    paddle_bbox = (
        (size - paddle_w) / 2,
        paddle_y,
        (size + paddle_w) / 2,
        paddle_y + paddle_h,
    )
    add_glow(img, lambda d, rgba: d.rounded_rectangle(paddle_bbox, radius=paddle_h / 2, fill=rgba),
             (255, 80, 160), blur=14, alpha=160)
    draw.rounded_rectangle(paddle_bbox, radius=paddle_h / 2, fill=(255, 90, 170, 230))

    # Ball
    ball_r = size * 0.05
    ball_cx = size * 0.68
    ball_cy = size * 0.32
    ball_bbox = (ball_cx - ball_r, ball_cy - ball_r, ball_cx + ball_r, ball_cy + ball_r)
    add_glow(img, lambda d, rgba: d.ellipse(ball_bbox, fill=rgba), (255, 220, 120), blur=12, alpha=180)
    draw.ellipse(ball_bbox, fill=(255, 230, 150, 230))

    return img


def load_font(size):
    candidates = [
        "/System/Library/Fonts/SFNSDisplay.ttf",
        "/System/Library/Fonts/SFNS.ttf",
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
        "/Library/Fonts/Arial Bold.ttf",
        "/System/Library/Fonts/Supplemental/Helvetica Neue Bold.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def draw_feature_graphic():
    width, height = 1024, 500
    base = Image.new("RGBA", (width, height), (4, 6, 16, 255))

    # Background gradient
    grad = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(grad)
    for y in range(height):
        t = y / height
        color = blend((9, 24, 52), (4, 6, 16), t)
        draw.line([(0, y), (width, y)], fill=color)
    base.alpha_composite(grad)

    # Accent glow bands
    glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.rectangle([0, height * 0.55, width, height * 0.85], fill=(40, 140, 255, 30))
    glow_draw.rectangle([0, height * 0.2, width, height * 0.45], fill=(255, 60, 120, 24))
    base.alpha_composite(glow)

    # Icon
    icon = draw_icon(380)
    base.alpha_composite(icon, (70, 60))

    # Text
    text_layer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    text_draw = ImageDraw.Draw(text_layer)
    title_font = load_font(86)
    subtitle_font = load_font(34)
    title = "Breakout+"
    subtitle = "Neon brick-breaker"

    tx = 470
    ty = 170
    shadow = (0, 0, 0, 140)
    text_draw.text((tx + 3, ty + 3), title, font=title_font, fill=shadow)
    text_draw.text((tx, ty), title, font=title_font, fill=(245, 245, 255, 230))

    text_draw.text((tx + 2, ty + 95), subtitle, font=subtitle_font, fill=(160, 210, 255, 210))

    base.alpha_composite(text_layer)

    return base


def main():
    OUTPUT_ICON.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_FEATURE.parent.mkdir(parents=True, exist_ok=True)

    icon = draw_icon()
    icon.save(OUTPUT_ICON, format="PNG", optimize=True)

    feature = draw_feature_graphic()
    feature.save(OUTPUT_FEATURE, format="PNG", optimize=True)

    print(f"Wrote {OUTPUT_ICON}")
    print(f"Wrote {OUTPUT_FEATURE}")


if __name__ == "__main__":
    main()
