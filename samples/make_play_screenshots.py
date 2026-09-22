#!/usr/bin/env python3
"""
Builds Google Play listing screenshots from raw device captures.

Play accepts portrait images between 16:9 and 9:16, so 1080x1920 is the tallest allowed and is
what this targets. A raw 1080x2340 capture is taller than that ratio, so it is scaled to fit the
space left under the caption rather than cropped -- the status bar stays visible because the live
speed indicator sitting up there is the product's signature.

Run from the samples/ directory:  python3 make_play_screenshots.py
(needs Pillow; on this Mac, Xcode's python3 has it)
"""

import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT_DIR = "play_store_screenshots"
CANVAS = (1080, 1920)

# Pulled from the app's own dark palette (presentation/theme/Color.kt) so the frames and the
# product agree: DarkBackground, DarkSurfaceVariant, DarkPrimary, DarkOnSurface, DarkOnSurfaceVariant.
BG_TOP = (13, 16, 23)
BG_BOTTOM = (26, 31, 43)
ACCENT = (157, 185, 255)
TITLE = (227, 230, 238)
SUBTITLE = (182, 189, 203)

# The app's own typeface, straight from its resources, so captions match what is on screen.
# It is a variable font; weight is picked per use below instead of from separate files.
FONT_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "..", "app", "src", "main", "res", "font", "manrope.ttf")


def font(size, weight):
    """Manrope at the given size and weight (200-800)."""
    f = ImageFont.truetype(FONT_FILE, size)
    f.set_variation_by_axes([weight])
    return f

# Ordered as a pitch: what it does, what makes it different, then the detail.
SLIDES = [
    ("img.png",
     "Real speed, measured",
     "Live throughput plus a built-in speed test"),
    ("img_1.png",
     "See which apps use data",
     "Per-app totals, split by mobile and Wi-Fi"),
    ("img_2.png",
     "Know before you go over",
     "Forecasts your cycle from the rate so far"),
    ("img_3.png",
     "A month of history",
     "Every day, with warnings when you run hot"),
    ("img_4.png",
     "Yours to configure",
     "Units, alerts, overlay, themes"),
]


def gradient_background(size):
    """Vertical gradient with a soft accent glow behind the phone."""
    w, h = size
    bg = Image.new("RGB", size, BG_TOP)
    draw = ImageDraw.Draw(bg)
    for y in range(h):
        t = y / h
        draw.line(
            [(0, y), (w, y)],
            fill=tuple(int(a + (b - a) * t) for a, b in zip(BG_TOP, BG_BOTTOM)),
        )

    # A blurred ellipse reads as light falling on the frame; a hard shape would look like a bug.
    glow = Image.new("RGB", size, (0, 0, 0))
    ImageDraw.Draw(glow).ellipse(
        [w * 0.10, h * 0.16, w * 0.90, h * 0.72],
        fill=(int(ACCENT[0] * 0.20), int(ACCENT[1] * 0.20), int(ACCENT[2] * 0.24)),
    )
    glow = glow.filter(ImageFilter.GaussianBlur(160))
    return Image.blend(bg, Image.blend(bg, glow, 0.55), 0.75)


def rounded(im, radius):
    """Rounds the corners of the capture so it reads as a device, not a pasted rectangle."""
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, im.size[0], im.size[1]], radius, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def centered(draw, text, font, y, fill):
    w = draw.textbbox((0, 0), text, font=font)[2]
    draw.text(((CANVAS[0] - w) // 2, y), text, font=font, fill=fill)


def build(source, title, subtitle, index):
    canvas = gradient_background(CANVAS)
    draw = ImageDraw.Draw(canvas)

    title_font = font(64, 800)
    sub_font = font(34, 500)

    centered(draw, title, title_font, 96, TITLE)
    centered(draw, subtitle, sub_font, 184, SUBTITLE)

    # Accent rule between the caption and the phone.
    draw.rounded_rectangle(
        [CANVAS[0] // 2 - 46, 252, CANVAS[0] // 2 + 46, 258], 3, fill=ACCENT
    )

    shot = Image.open(source).convert("RGB")
    # Trim the system navigation bar; the status bar stays, it shows the speed indicator.
    shot = shot.crop((0, 0, shot.width, int(shot.height * 0.955)))

    top = 310
    available_h = CANVAS[1] - top - 70
    scale = available_h / shot.height
    shot = shot.resize((int(shot.width * scale), int(shot.height * scale)), Image.LANCZOS)
    shot = rounded(shot, 34)

    x = (CANVAS[0] - shot.width) // 2

    # Drop shadow, so the frame sits above the background rather than on it.
    shadow = Image.new("RGBA", CANVAS, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [x + 6, top + 14, x + shot.width + 6, top + shot.height + 14], 34, fill=(0, 0, 0, 150)
    )
    canvas = Image.alpha_composite(
        canvas.convert("RGBA"), shadow.filter(ImageFilter.GaussianBlur(26))
    )

    canvas.paste(shot, (x, top), shot)

    # Hairline rim, the same trick the in-app overlay uses to define a translucent edge.
    ImageDraw.Draw(canvas).rounded_rectangle(
        [x, top, x + shot.width, top + shot.height], 34,
        outline=(255, 255, 255, 40), width=2,
    )

    os.makedirs(OUT_DIR, exist_ok=True)
    out = os.path.join(OUT_DIR, f"{index:02d}_{os.path.splitext(source)[0]}.png")
    canvas.convert("RGB").save(out, "PNG", optimize=True)
    return out


# The mauve the app uses for upload; the ring sweeps between it and the cobalt primary.
TERTIARY = (240, 179, 214)

# A minute of throughput, as the in-app sparkline would trace it: quiet, a burst, a settle.
TRACE = [0.06, 0.05, 0.08, 0.06, 0.10, 0.34, 0.72, 0.95, 0.83, 0.61,
         0.66, 0.52, 0.38, 0.44, 0.30, 0.22, 0.26, 0.17, 0.12, 0.14,
         0.09, 0.28, 0.55, 0.47, 0.33, 0.19, 0.13, 0.10, 0.08, 0.07]


def _lerp(a, b, t):
    return tuple(int(x + (y - x) * t) for x, y in zip(a, b))


def _smooth_curve(values, width, samples_per_step=24):
    """Resamples the trace into a dense polyline with rounded shoulders."""
    dense = []
    for i in range(len(values) - 1):
        for j in range(samples_per_step):
            t = j / samples_per_step
            # Cosine easing between samples: a straight polyline reads as a chart axis,
            # this reads as a signal.
            e = (1 - math.cos(t * math.pi)) / 2
            dense.append(values[i] + (values[i + 1] - values[i]) * e)
    dense.append(values[-1])
    return [(x * width / (len(dense) - 1), v) for x, v in enumerate(dense)]


def build_feature_graphic(out_name="00_feature_graphic.png"):
    """
    The 1024x500 banner at the top of the Play listing.

    Drawn rather than cropped from a capture: at 1024 wide a downscaled 1080px screenshot is soft,
    and a slice of a phone shows a fragment of every element instead of the one that matters. The
    dial is the product's signature, so it is the hero, rendered at 3x and downsampled.

    Play may overlay a play button over the centre when a promo video is attached, and crops the
    edges on some surfaces, so the text holds to the left, the dial to the right, and the middle
    band carries nothing but background.
    """
    scale = 3
    w, h = 1024 * scale, 500 * scale
    canvas = gradient_background((w, h)).convert("RGBA")

    # --- throughput trace, along the lower band -------------------------------------------------
    base_y, amp = h * 0.97, h * 0.17
    pts = [(x, base_y - v * amp) for x, v in _smooth_curve(TRACE, w)]

    fill = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(fill).polygon(pts + [(w, h), (0, h)], fill=ACCENT + (46,))
    canvas = Image.alpha_composite(canvas, fill)

    line = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ld = ImageDraw.Draw(line)
    for i in range(len(pts) - 1):
        ld.line([pts[i], pts[i + 1]], fill=_lerp(ACCENT, TERTIARY, i / len(pts)) + (235,),
                width=4 * scale)
    canvas = Image.alpha_composite(canvas, line)

    # --- the dial -------------------------------------------------------------------------------
    cx, cy, radius, stroke = int(w * 0.775), int(h * 0.47), int(h * 0.33), int(h * 0.028)

    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse(
        [cx - radius * 1.7, cy - radius * 1.7, cx + radius * 1.7, cy + radius * 1.7],
        fill=ACCENT + (26,),
    )
    canvas = Image.alpha_composite(canvas, glow.filter(ImageFilter.GaussianBlur(70 * scale // 3)))

    ring = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    rd = ImageDraw.Draw(ring)
    box = [cx - radius, cy - radius, cx + radius, cy + radius]
    steps = 720
    for i in range(steps):
        a = i * 360 / steps
        # Cobalt at the top, mauve at the bottom, mirrored back -- the same two colours the app
        # sweeps between while a test runs, held still here.
        t = abs((i / steps) * 2 - 1)
        rd.arc(box, a - 91, a - 89, fill=_lerp(TERTIARY, ACCENT, t), width=stroke)
    canvas = Image.alpha_composite(canvas, ring)

    # Disc inside the ring, so the trace does not run through the figure.
    disc = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    inner = radius - stroke
    ImageDraw.Draw(disc).ellipse([cx - inner, cy - inner, cx + inner, cy + inner],
                                 fill=BG_TOP + (255,))
    canvas = Image.alpha_composite(canvas, disc)

    draw = ImageDraw.Draw(canvas)
    draw.text((cx, cy - radius * 0.14), "58.4", font=font(56 * scale, 700),
              fill=ACCENT, anchor="mm")
    draw.text((cx, cy + radius * 0.34), "Mbps", font=font(30 * scale, 600),
              fill=SUBTITLE, anchor="mm")

    # --- wordmark, left ------------------------------------------------------------------------
    x = 64 * scale
    draw.text((x, 128 * scale), "NetSpeed", font=font(92 * scale, 800), fill=TITLE)
    draw.text((x + 2, 242 * scale), "Data Usage Monitor", font=font(40 * scale, 600), fill=ACCENT)
    draw.rounded_rectangle(
        [x + 2, 310 * scale, x + 94 * scale, 316 * scale], 3 * scale, fill=ACCENT
    )
    draw.text((x + 2, 344 * scale), "No ads  \u00b7  No tracking  \u00b7  All local",
              font=font(27 * scale, 500), fill=SUBTITLE)

    os.makedirs(OUT_DIR, exist_ok=True)
    out = os.path.join(OUT_DIR, out_name)
    # Play rejects alpha in the feature graphic, so it is flattened to RGB.
    canvas.resize((1024, 500), Image.LANCZOS).convert("RGB").save(out, "PNG", optimize=True)
    return out


if __name__ == "__main__":
    for i, (src, title, sub) in enumerate(SLIDES, start=1):
        if not os.path.exists(src):
            print(f"skipped (missing): {src}")
            continue
        print("wrote", build(src, title, sub, i))
    print("wrote", build_feature_graphic())
