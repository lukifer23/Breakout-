#!/usr/bin/env python3
"""
Generate a complete iOS AppIcon set for Breakout+ (no external deps).

Why this exists:
- Repo is CLI-first (no Xcode UI).
- Avoids image libraries by emitting a raw PPM and using macOS `sips` to convert/resize.
"""

from __future__ import annotations

import json
import math
import os
import subprocess
from dataclasses import dataclass


@dataclass(frozen=True)
class RGB:
    r: int
    g: int
    b: int


def clamp_u8(v: float) -> int:
    if v <= 0:
        return 0
    if v >= 255:
        return 255
    return int(v)


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def blend(dst: RGB, src: RGB, alpha: float) -> RGB:
    # alpha: 0..1
    a = max(0.0, min(1.0, alpha))
    return RGB(
        r=clamp_u8(dst.r * (1.0 - a) + src.r * a),
        g=clamp_u8(dst.g * (1.0 - a) + src.g * a),
        b=clamp_u8(dst.b * (1.0 - a) + src.b * a),
    )


def set_px(buf: bytearray, n: int, x: int, y: int, c: RGB) -> None:
    i = (y * n + x) * 3
    buf[i] = c.r
    buf[i + 1] = c.g
    buf[i + 2] = c.b


def get_px(buf: bytearray, n: int, x: int, y: int) -> RGB:
    i = (y * n + x) * 3
    return RGB(buf[i], buf[i + 1], buf[i + 2])


def fill_background(buf: bytearray, n: int) -> None:
    c0 = RGB(0x0B, 0x12, 0x20)  # bp_navy-ish
    c1 = RGB(0x06, 0x18, 0x22)  # deep teal
    glow_cyan = RGB(0x31, 0xE1, 0xF7)
    glow_mag = RGB(0xFF, 0x2E, 0xA6)

    cx = n * 0.52
    cy = n * 0.40
    maxd = math.hypot(n, n)

    for y in range(n):
        fy = y / (n - 1)
        for x in range(n):
            fx = x / (n - 1)
            t = (fx + fy) * 0.5
            base = RGB(
                r=clamp_u8(lerp(c0.r, c1.r, t)),
                g=clamp_u8(lerp(c0.g, c1.g, t)),
                b=clamp_u8(lerp(c0.b, c1.b, t)),
            )

            # Radial neon glows (cyan + magenta).
            d = math.hypot(x - cx, y - cy) / maxd
            a_c = math.exp(-(d * 10.0) ** 2) * 0.35
            a_m = math.exp(-(((d * 10.0) - 0.55) ** 2)) * 0.12
            c = blend(base, glow_cyan, a_c)
            c = blend(c, glow_mag, a_m)
            set_px(buf, n, x, y, c)


def draw_circle(buf: bytearray, n: int, cx: int, cy: int, r: int, fill: RGB, glow: RGB | None = None) -> None:
    x0 = max(0, cx - r - 6)
    x1 = min(n - 1, cx + r + 6)
    y0 = max(0, cy - r - 6)
    y1 = min(n - 1, cy + r + 6)

    rr = r * r
    for y in range(y0, y1 + 1):
        dy = y - cy
        for x in range(x0, x1 + 1):
            dx = x - cx
            d2 = dx * dx + dy * dy
            if d2 <= rr:
                dst = get_px(buf, n, x, y)
                set_px(buf, n, x, y, blend(dst, fill, 0.92))
            elif glow is not None:
                # Soft glow just outside the ball.
                dd = math.sqrt(d2) - r
                if 0.0 < dd < 10.0:
                    a = (1.0 - (dd / 10.0)) * 0.35
                    dst = get_px(buf, n, x, y)
                    set_px(buf, n, x, y, blend(dst, glow, a))


def draw_rounded_rect(
    buf: bytearray,
    n: int,
    x: int,
    y: int,
    w: int,
    h: int,
    r: int,
    fill: RGB,
    stroke: RGB,
    stroke_w: int = 6,
    glow: RGB | None = None,
) -> None:
    x0 = max(0, x - 10)
    y0 = max(0, y - 10)
    x1 = min(n - 1, x + w + 10)
    y1 = min(n - 1, y + h + 10)

    def inside(px: int, py: int, inset: int) -> bool:
        ix = px - x
        iy = py - y
        if ix < inset or iy < inset or ix >= w - inset or iy >= h - inset:
            return False
        # Rounded corners: check distance to nearest corner center.
        rx = min(ix - inset, (w - inset - 1) - ix)
        ry = min(iy - inset, (h - inset - 1) - iy)
        if rx >= r or ry >= r:
            return True
        return (rx - r) * (rx - r) + (ry - r) * (ry - r) <= 0

    for py in range(y0, y1 + 1):
        for px in range(x0, x1 + 1):
            # Glow halo
            if glow is not None:
                if inside(px, py, -2):
                    continue
                # Approx distance to rect bounds for glow
                dx = 0
                if px < x:
                    dx = x - px
                elif px >= x + w:
                    dx = px - (x + w - 1)
                dy = 0
                if py < y:
                    dy = y - py
                elif py >= y + h:
                    dy = py - (y + h - 1)
                dd = math.hypot(dx, dy)
                if dd < 9.0:
                    a = (1.0 - dd / 9.0) * 0.22
                    dst = get_px(buf, n, px, py)
                    set_px(buf, n, px, py, blend(dst, glow, a))

            if inside(px, py, 0):
                dst = get_px(buf, n, px, py)
                set_px(buf, n, px, py, blend(dst, fill, 0.88))
                continue

            if stroke_w > 0 and inside(px, py, stroke_w):
                # already filled; stroke is outside this inset, handled below
                continue

            if stroke_w > 0 and inside(px, py, -stroke_w):
                # Stroke band
                if not inside(px, py, 0):
                    dst = get_px(buf, n, px, py)
                    set_px(buf, n, px, py, blend(dst, stroke, 0.65))


def draw_plus(buf: bytearray, n: int, cx: int, cy: int, size: int, thickness: int, color: RGB) -> None:
    x0 = cx - size
    x1 = cx + size
    y0 = cy - size
    y1 = cy + size
    t = thickness
    for y in range(max(0, y0), min(n, y1)):
        for x in range(max(0, x0), min(n, x1)):
            if abs(x - cx) <= t or abs(y - cy) <= t:
                dst = get_px(buf, n, x, y)
                set_px(buf, n, x, y, blend(dst, color, 0.85))


def render_icon_1024(out_ppm: str) -> None:
    n = 1024
    buf = bytearray(n * n * 3)
    fill_background(buf, n)

    cyan = RGB(0x31, 0xE1, 0xF7)
    mag = RGB(0xFF, 0x2E, 0xA6)
    gold = RGB(0xFF, 0xC7, 0x45)
    white = RGB(0xF5, 0xF7, 0xFF)

    # Brick row (reads at small sizes).
    bw, bh = 260, 140
    gap = 34
    start_x = (n - (bw * 3 + gap * 2)) // 2
    y = int(n * 0.20)
    for i in range(3):
        bx = start_x + i * (bw + gap)
        fill = mag if i == 1 else gold
        stroke = cyan
        draw_rounded_rect(buf, n, bx, y, bw, bh, r=34, fill=fill, stroke=stroke, stroke_w=7, glow=stroke)

    # Paddle-ish bar near bottom.
    draw_rounded_rect(
        buf, n,
        x=int(n * 0.20), y=int(n * 0.72),
        w=int(n * 0.60), h=80,
        r=36,
        fill=RGB(0x1A, 0x1F, 0x26),
        stroke=cyan,
        stroke_w=8,
        glow=cyan,
    )

    # Ball with glow.
    draw_circle(buf, n, cx=int(n * 0.66), cy=int(n * 0.50), r=92, fill=cyan, glow=cyan)

    # Signature plus.
    draw_plus(buf, n, cx=int(n * 0.40), cy=int(n * 0.50), size=115, thickness=28, color=white)

    with open(out_ppm, "wb") as f:
        f.write(f"P6\n{n} {n}\n255\n".encode("ascii"))
        f.write(buf)


def run(cmd: list[str]) -> None:
    subprocess.check_call(cmd)


def main() -> None:
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    appicon_dir = os.path.join(
        repo_root, "ios", "BreakoutPlus", "BreakoutPlus", "Resources", "Assets.xcassets", "AppIcon.appiconset"
    )
    os.makedirs(appicon_dir, exist_ok=True)

    ppm_path = os.path.join(appicon_dir, "_icon_1024.ppm")
    png_1024 = os.path.join(appicon_dir, "AppIcon-1024.png")

    render_icon_1024(ppm_path)
    run(["sips", "-s", "format", "png", ppm_path, "--out", png_1024])
    # Keep the asset catalog clean: the PPM is just an intermediate.
    if os.path.exists(ppm_path):
        try:
            os.remove(ppm_path)
        except OSError:
            pass

    specs = [
        # iPhone
        ("iphone", "notification", 20, "2x", 40),
        ("iphone", "notification", 20, "3x", 60),
        ("iphone", "settings", 29, "2x", 58),
        ("iphone", "settings", 29, "3x", 87),
        ("iphone", "spotlight", 40, "2x", 80),
        ("iphone", "spotlight", 40, "3x", 120),
        ("iphone", "app", 60, "2x", 120),
        ("iphone", "app", 60, "3x", 180),
        # iPad
        ("ipad", "notification", 20, "1x", 20),
        ("ipad", "notification", 20, "2x", 40),
        ("ipad", "settings", 29, "1x", 29),
        ("ipad", "settings", 29, "2x", 58),
        ("ipad", "spotlight", 40, "1x", 40),
        ("ipad", "spotlight", 40, "2x", 80),
        ("ipad", "app", 76, "1x", 76),
        ("ipad", "app", 76, "2x", 152),
        ("ipad", "app", 83.5, "2x", 167),
        # App Store
        ("ios-marketing", "app-store", 1024, "1x", 1024),
    ]

    images = []
    for idiom, role, pt, scale, px in specs:
        # Build filename
        pt_str = str(pt).rstrip("0").rstrip(".") if isinstance(pt, float) else str(pt)
        fn = f"AppIcon-{pt_str}x{pt_str}@{scale}.png" if idiom != "ios-marketing" else "AppIcon-1024.png"
        out_path = os.path.join(appicon_dir, fn)
        if os.path.basename(out_path) != "AppIcon-1024.png":
            run(["sips", "-z", str(px), str(px), png_1024, "--out", out_path])

        if idiom == "ios-marketing":
            images.append({"idiom": "ios-marketing", "size": "1024x1024", "scale": "1x", "filename": "AppIcon-1024.png"})
        else:
            images.append(
                {
                    "idiom": idiom,
                    "size": f"{pt_str}x{pt_str}",
                    "scale": scale,
                    "filename": os.path.basename(out_path),
                }
            )

    contents = {"images": images, "info": {"version": 1, "author": "xcode"}}
    with open(os.path.join(appicon_dir, "Contents.json"), "w", encoding="utf-8") as f:
        json.dump(contents, f, indent=2)
        f.write("\n")

    # Defensive cleanup in case a previous run left intermediates around.
    if os.path.exists(ppm_path):
        try:
            os.remove(ppm_path)
        except OSError:
            pass

    print(f"Generated iOS AppIcon set in: {appicon_dir}")


if __name__ == "__main__":
    main()
