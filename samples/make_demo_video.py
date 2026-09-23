#!/usr/bin/env python3
"""
Turns a raw portrait screen recording into a 1920x1080 demo for YouTube.

A phone capture is 9:19.5; posted as-is YouTube pillarboxes it into grey bars. This frames the
recording on the same background the Play assets use -- gradient, wordmark, throughput trace --
so the video matches the listing instead of looking like a screen grab someone forgot to crop.

The rounded corners are done by compositing an overlay that is the background with a rounded hole
punched in it: the video plays underneath and is only visible through the hole, which is cheaper
and sharper than masking the video stream.

Usage:  python3 make_demo_video.py <recording.mp4> [out.mp4]
(needs Pillow and ffmpeg; on this Mac, Xcode's python3 has Pillow)
"""

import os
import subprocess
import sys

from PIL import Image, ImageDraw

from make_play_screenshots import (ACCENT, SUBTITLE, TITLE, TRACE, _lerp,
                                   _smooth_curve, font, gradient_background)

CANVAS = (1920, 1080)
DEVICE_H = 1000          # how tall the phone plays; the rest follows from its aspect
MARGIN_RIGHT = 150
RADIUS = 26
# Same trim the listing screenshots use: the system navigation bar is the host device's, not the
# product's. The status bar stays -- the live speed indicator up there is the point of the app.
TRIM = 0.955


def _background(device_box):
    """The still frame behind the video: gradient, trace, wordmark, and the device's rim."""
    w, h = CANVAS
    bg = gradient_background(CANVAS).convert("RGBA")

    base_y, amp = h * 0.97, h * 0.17
    pts = [(x, base_y - v * amp) for x, v in _smooth_curve(TRACE, w)]

    fill = Image.new("RGBA", CANVAS, (0, 0, 0, 0))
    ImageDraw.Draw(fill).polygon(pts + [(w, h), (0, h)], fill=ACCENT + (40,))
    bg = Image.alpha_composite(bg, fill)

    line = Image.new("RGBA", CANVAS, (0, 0, 0, 0))
    ld = ImageDraw.Draw(line)
    for i in range(len(pts) - 1):
        ld.line([pts[i], pts[i + 1]], fill=_lerp(ACCENT, SUBTITLE, i / len(pts)) + (200,), width=3)
    bg = Image.alpha_composite(bg, line)

    draw = ImageDraw.Draw(bg)
    draw.text((150, 372), "NetSpeed", font=font(104, 800), fill=TITLE)
    draw.text((152, 500), "Data Usage Monitor", font=font(44, 600), fill=ACCENT)
    draw.rounded_rectangle([152, 576, 256, 582], 3, fill=ACCENT)
    draw.text((152, 616), "No ads  ·  No tracking  ·  All local", font=font(30, 500), fill=SUBTITLE)

    # Rim around where the video will appear, so the device reads as an object on the page.
    draw.rounded_rectangle(device_box, RADIUS, outline=(255, 255, 255, 46), width=2)
    return bg


def build(recording, out_path):
    probe = subprocess.run(
        ["ffprobe", "-v", "error", "-select_streams", "v:0",
         "-show_entries", "stream=width,height", "-of", "csv=p=0:s=x", recording],
        capture_output=True, text=True, check=True).stdout.strip().split("x")
    src_w, src_h = int(probe[0]), int(probe[1])

    src_h = int(src_h * TRIM)
    dev_w = round(DEVICE_H * src_w / src_h / 2) * 2      # even, h264 wants even dimensions
    x = CANVAS[0] - dev_w - MARGIN_RIGHT
    y = (CANVAS[1] - DEVICE_H) // 2
    box = [x, y, x + dev_w, y + DEVICE_H]

    bg = _background(box)

    # The overlay is the background with the device rectangle cut out.
    hole = Image.new("L", CANVAS, 255)
    ImageDraw.Draw(hole).rounded_rectangle(box, RADIUS, fill=0)
    overlay = bg.copy()
    overlay.putalpha(Image.composite(bg.getchannel("A"), Image.new("L", CANVAS, 0), hole))

    here = os.path.dirname(os.path.abspath(__file__))
    overlay_path = os.path.join(here, ".demo_overlay.png")
    overlay.save(overlay_path)

    subprocess.run([
        "ffmpeg", "-y", "-v", "error",
        "-i", recording,
        "-i", overlay_path,
        "-f", "lavfi", "-i", "anullsrc=channel_layout=stereo:sample_rate=48000",
        "-filter_complex",
        f"[0:v]crop=iw:ih*{TRIM}:0:0,scale={dev_w}:{DEVICE_H},setsar=1[dev];"
        f"color=c=black:s={CANVAS[0]}x{CANVAS[1]}:r=30[bg];"
        f"[bg][dev]overlay={x}:{y}:shortest=1[t];"
        f"[t][1:v]overlay=0:0,fade=t=in:st=0:d=0.6,"
        f"format=yuv420p[v]",
        "-map", "[v]", "-map", "2:a",
        "-c:v", "libx264", "-preset", "slow", "-crf", "18",
        "-profile:v", "high", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "128k", "-shortest",
        "-movflags", "+faststart",
        out_path,
    ], check=True)

    os.remove(overlay_path)
    return out_path


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    source = sys.argv[1]
    destination = sys.argv[2] if len(sys.argv) > 2 else "netspeed_demo_1080p.mp4"
    print("wrote", build(source, destination))
