#!/usr/bin/env python3
"""
Builds Google Play listing screenshots from raw device captures.

Play accepts portrait images between 16:9 and 9:16, so 1080x1920 is the tallest allowed and is
what this targets. A raw 1080x2340 capture is taller than that ratio, so it is scaled to fit the
space left under the caption rather than cropped -- the status bar stays visible because the live
speed indicator sitting up there is the product's signature.

Run from the samples/ directory:  python3 make_play_screenshots.py
"""

from PIL import Image, ImageDraw, ImageFilter, ImageFont
import os

OUT_DIR = "play_store_screenshots"
CANVAS = (1080, 1920)

# Pulled from the app's own dark palette so the frames and the product agree.
BG_TOP = (10, 14, 20)
BG_BOTTOM = (19, 27, 38)
ACCENT = (79, 195, 247)
TITLE = (240, 245, 250)
SUBTITLE = (150, 163, 178)

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

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

    title_font = ImageFont.truetype(FONT_BOLD, 64)
    sub_font = ImageFont.truetype(FONT_REG, 34)

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


def build_feature_graphic(source="img.png", out_name="00_feature_graphic.png"):
    """
    The 1024x500 banner at the top of the Play listing.

    Play may overlay a play button in the centre when a promo video is attached, and crops the
    edges on some surfaces, so the text is held to the left third and nothing important goes near
    a border.
    """
    size = (1024, 500)
    canvas = gradient_background(size)

    # A self-contained slice -- the live speed card, the dial with a finished result, and the
    # button -- rather than an arbitrary window onto a phone. An arbitrary crop lands mid-label at
    # the top and bottom, which reads as a mistake rather than a bleed.
    shot = Image.open(source).convert("RGB").crop((0, 100, 1080, 1360))
    target_h = 460
    scale = target_h / shot.height
    shot = shot.resize((int(shot.width * scale), target_h), Image.LANCZOS)
    shot = rounded(shot, 26)

    x, y = 1024 - shot.width - 52, 20
    shadow = Image.new("RGBA", size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [x + 8, y + 16, x + shot.width + 8, y + shot.height + 16], 26, fill=(0, 0, 0, 170)
    )
    canvas = Image.alpha_composite(
        canvas.convert("RGBA"), shadow.filter(ImageFilter.GaussianBlur(30))
    )
    canvas.paste(shot, (x, y), shot)
    ImageDraw.Draw(canvas).rounded_rectangle(
        [x, y, x + shot.width, y + shot.height], 26, outline=(255, 255, 255, 45), width=2
    )

    draw = ImageDraw.Draw(canvas)
    draw.text((64, 138), "NetSpeed", font=ImageFont.truetype(FONT_BOLD, 88), fill=TITLE)
    draw.text((66, 246), "Data Usage Monitor",
              font=ImageFont.truetype(FONT_REG, 40), fill=ACCENT)
    draw.rounded_rectangle([66, 310, 158, 316], 3, fill=ACCENT)
    draw.text((66, 344), "No ads  ·  No tracking  ·  All local",
              font=ImageFont.truetype(FONT_REG, 27), fill=SUBTITLE)

    os.makedirs(OUT_DIR, exist_ok=True)
    out = os.path.join(OUT_DIR, out_name)
    # Play rejects alpha in the feature graphic, so it is flattened to RGB.
    canvas.convert("RGB").save(out, "PNG", optimize=True)
    return out


if __name__ == "__main__":
    for i, (src, title, sub) in enumerate(SLIDES, start=1):
        if not os.path.exists(src):
            print(f"skipped (missing): {src}")
            continue
        print("wrote", build(src, title, sub, i))
    print("wrote", build_feature_graphic())
