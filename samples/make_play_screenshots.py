#!/usr/bin/env python3
"""
Builds Google Play listing artwork from raw device captures.

Three rules shape the layout, all of them the owner's:

  * No status bar. A listing image is about the app, and the clock, battery and carrier of
    whichever handset took the capture are nobody's business. The system navigation bar goes for
    the same reason -- the app's own bottom bar stays, it is part of the product.
  * Features are called out on the artwork rather than left to the caption. Someone swiping a
    listing reads two words per image, so the thing worth seeing gets a label pinned to it.
  * No device frame. The capture is shown as itself, scaled and shadowed, not pasted inside a
    drawing of a phone.

Play accepts portrait images between 16:9 and 9:16; 1080x1920 is the tallest allowed and is what
this targets.

Run from the samples/ directory:  python3 make_play_screenshots.py
(needs Pillow; on this Mac, Xcode's python3 has it)
"""

import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT_DIR = "play_store_screenshots"
CANVAS = (1080, 1920)

# Where the system furniture sits on the capture device, in source pixels. Taken from the view
# hierarchy rather than guessed: the content root starts below the status bar, and the navigation
# bar background declares its own top edge.
# Keyed by capture width, because the two devices report different insets: the phone's status bar
# is 95px tall with a 135px gesture bar, the tablet's 48px with 64px. Both come from the window
# manager's own decor insets rather than a guess.
SYSTEM_BARS = {
    1080: (95, 2205),    # phone, portrait
    1600: (48, 2496),    # tablet, portrait
}

# Pulled from the app's own dark palette (presentation/theme/Color.kt) so the artwork and the
# product agree: DarkBackground, DarkSurfaceVariant, DarkPrimary, DarkOnSurface, DarkOnSurfaceVariant.
BG_TOP = (13, 16, 23)
BG_BOTTOM = (26, 31, 43)
ACCENT = (157, 185, 255)
TITLE = (227, 230, 238)
SUBTITLE = (182, 189, 203)
# The mauve the app uses for upload; the dial sweeps between it and the cobalt primary.
TERTIARY = (240, 179, 214)
# The app's own unlit-tick grey, for the part of the dial a reading has not reached.
IDLE = (120, 128, 145)

FONT_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "..", "app", "src", "main", "res", "font", "manrope.ttf")


def font(size, weight):
    """Manrope at the given size and weight (200-800)."""
    f = ImageFont.truetype(FONT_FILE, int(size))
    f.set_variation_by_axes([weight])
    return f


# Each slide: capture, headline, sub-headline, and the callouts pinned onto the artwork.
# A callout is (x, y, label, detail, side) where x and y are fractions of the artwork rectangle
# and side says which way the card hangs off that anchor.
SLIDES = [
    ("raw_speed.png",
     "Real speed, measured",
     "Live throughput plus a built-in speed test",
     [((0.054, 0.645, 0.897, 0.0995), 0.92, 0.95)]),

    ("raw_usage_apps.png",
     "See which apps use data",
     "Per-app totals, split by mobile and Wi-Fi",
     [((0.021, 0.399, 0.954, 0.091), 0.92, 0.95)]),

    ("raw_usage_top.png",
     "Know before you go over",
     "Forecasts your cycle from the rate so far",
     [((0.055, 0.092, 0.625, 0.058), 0.90, 0.95)]),

    ("raw_history.png",
     "A month of history",
     "Every day, with warnings when you run hot",
     [((0.028, 0.443, 0.944, 0.170), 0.92, 0.95)]),

    ("raw_settings_top.png",
     "Yours to configure",
     "Units, alerts, overlay, themes",
     [((0.043, 0.227, 0.932, 0.139), 0.92, 0.95)]),
]


def gradient_background(size):
    """Vertical gradient with a soft accent glow behind the artwork."""
    w, h = size
    bg = Image.new("RGB", size, BG_TOP)
    draw = ImageDraw.Draw(bg)
    for y in range(h):
        t = y / h
        draw.line([(0, y), (w, y)],
                  fill=tuple(int(a + (b - a) * t) for a, b in zip(BG_TOP, BG_BOTTOM)))

    # A blurred ellipse reads as light falling on the artwork; a hard shape would look like a bug.
    glow = Image.new("RGB", size, (0, 0, 0))
    ImageDraw.Draw(glow).ellipse(
        [w * 0.10, h * 0.16, w * 0.90, h * 0.72],
        fill=(int(ACCENT[0] * 0.20), int(ACCENT[1] * 0.20), int(ACCENT[2] * 0.24)),
    )
    glow = glow.filter(ImageFilter.GaussianBlur(160))
    return Image.blend(bg, Image.blend(bg, glow, 0.55), 0.75)


def rounded(im, radius):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, im.size[0], im.size[1]], radius, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def centered(draw, text, fnt, y, fill, width):
    w = draw.textbbox((0, 0), text, font=fnt)[2]
    draw.text(((width - w) // 2, y), text, font=fnt, fill=fill)


def strip_system_bars(shot):
    """Drops the status bar and the system navigation bar, keeping the app's own bottom bar."""
    top, nav_top = SYSTEM_BARS.get(shot.width, (0, shot.height))
    return shot.crop((0, top, shot.width, min(nav_top, shot.height)))


def draw_zoom_inset(canvas, full_shot, rect, width_frac, bottom_frac, k):
    """
    A magnified crop of the app's own interface, floated over the artwork.

    Cropped from the full-resolution capture rather than from the copy already scaled down for the
    canvas, so the enlargement actually resolves more detail instead of just interpolating what is
    behind it. The crop is a real part of the product -- nothing is redrawn or relabelled.

    `rect` is (x, y, w, h) as fractions of the stripped capture; the panel is centred horizontally
    and hung from `bottom_frac` of the canvas height, which keeps it clear of the headline and of
    whatever the screenshot is showing off in its upper half.
    """
    cw, ch = canvas.size
    fx, fy, fw, fh = rect
    box = (int(fx * full_shot.width), int(fy * full_shot.height),
           int((fx + fw) * full_shot.width), int((fy + fh) * full_shot.height))
    crop = full_shot.crop(box)

    target_w = int(width_frac * cw)
    target_h = max(1, int(crop.height * target_w / crop.width))
    crop = crop.resize((target_w, target_h), Image.LANCZOS)

    x = (cw - target_w) // 2
    y = int(bottom_frac * ch) - target_h
    radius = int(26 * k)

    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [x, y + int(14 * k), x + target_w, y + target_h + int(14 * k)], radius, fill=(0, 0, 0, 200))
    canvas = Image.alpha_composite(canvas, shadow.filter(ImageFilter.GaussianBlur(int(26 * k))))

    panel = rounded(crop, radius)
    canvas.paste(panel, (x, y), panel)

    # A hairline in the accent, so the panel reads as laid over the screenshot rather than as a
    # part of it that happens to be larger.
    ImageDraw.Draw(canvas).rounded_rectangle(
        [x, y, x + target_w, y + target_h], radius,
        outline=ACCENT + (170,), width=max(2, int(3 * k)))
    return canvas


def build(source, title, subtitle, index, callouts=(), canvas_size=CANVAS, out_dir=OUT_DIR):
    """One listing image. Every measurement scales off a 1080-wide reference, so the same layout
    serves a phone slide and a 10-inch tablet slide without a second set of numbers."""
    cw, ch = canvas_size
    k = cw / 1080

    canvas = gradient_background(canvas_size).convert("RGBA")
    draw = ImageDraw.Draw(canvas)

    centered(draw, title, font(64 * k, 800), int(92 * k), TITLE, cw)
    centered(draw, subtitle, font(34 * k, 500), int(180 * k), SUBTITLE, cw)
    draw.rounded_rectangle(
        [cw // 2 - 46 * k, 248 * k, cw // 2 + 46 * k, 254 * k], 3, fill=ACCENT)

    full = strip_system_bars(Image.open(source).convert("RGB"))
    shot = full

    top = int(300 * k)
    available_h = ch - top - int(44 * k)
    scale = min(available_h / shot.height, (cw - int(180 * k)) / shot.width)
    shot = shot.resize((int(shot.width * scale), int(shot.height * scale)), Image.LANCZOS)
    radius = int(30 * k)
    shot_rounded = rounded(shot, radius)

    x = (cw - shot.width) // 2

    shadow = Image.new("RGBA", canvas_size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [x + 4, top + 16, x + shot.width + 4, top + shot.height + 16], radius, fill=(0, 0, 0, 165))
    canvas = Image.alpha_composite(canvas, shadow.filter(ImageFilter.GaussianBlur(int(30 * k))))

    canvas.paste(shot_rounded, (x, top), shot_rounded)

    for rect, width_frac, bottom_frac in callouts:
        canvas = draw_zoom_inset(canvas, full, rect, width_frac, bottom_frac, k)

    os.makedirs(out_dir, exist_ok=True)
    stem = os.path.splitext(source)[0].replace("raw_", "")
    out = os.path.join(out_dir, f"{index:02d}_{stem}.png")
    canvas.convert("RGB").save(out, "PNG", optimize=True)
    return out


# ---------------------------------------------------------------------------------------------
# Feature graphic
# ---------------------------------------------------------------------------------------------

# A minute of throughput, as the in-app sparkline would trace it: quiet, a burst, a settle.
TRACE = [0.06, 0.05, 0.08, 0.06, 0.10, 0.34, 0.72, 0.95, 0.83, 0.61,
         0.66, 0.52, 0.38, 0.44, 0.30, 0.22, 0.26, 0.17, 0.12, 0.14,
         0.09, 0.28, 0.55, 0.47, 0.33, 0.19, 0.13, 0.10, 0.08, 0.07]

# The dial's own geometry, mirroring presentation/components/SpeedCircle.kt so the banner shows
# the product's actual signature rather than a ring that merely resembles it.
GAUGE_START, GAUGE_SWEEP, TICK_COUNT, MESH_STEP = 135.0, 270.0, 52, 3.0

HERO_MBPS = 58.4


def _lerp(a, b, t):
    return tuple(int(x + (y - x) * t) for x, y in zip(a, b))


def _smooth_curve(values, width, samples_per_step=24):
    dense = []
    for i in range(len(values) - 1):
        for j in range(samples_per_step):
            t = j / samples_per_step
            e = (1 - math.cos(t * math.pi)) / 2
            dense.append(values[i] + (values[i + 1] - values[i]) * e)
    dense.append(values[-1])
    return [(x * width / (len(dense) - 1), v) for x, v in enumerate(dense)]


def _polar(cx, cy, angle_deg, r):
    a = math.radians(angle_deg)
    return cx + math.cos(a) * r, cy + math.sin(a) * r


def _band(draw, cx, cy, r_centre, thickness, a0, a1, fill):
    """An arc of given thickness centred on r_centre. PIL grows an arc's width inwards."""
    outer = r_centre + thickness / 2
    draw.arc([cx - outer, cy - outer, cx + outer, cy + outer], a0, a1,
             fill=fill, width=max(1, int(thickness)))


def _dial_progress(mbps):
    """The app's own log scale: a decade per quarter turn."""
    return min(1.0, max(0.0, (math.log10(mbps) + 1.0) / 4.0))


def build_feature_graphic(out_name="00_feature_graphic.png"):
    """
    The 1024x500 banner at the top of the Play listing.

    Drawn rather than cropped from a capture: at 1024 wide a downscaled 1080px screenshot is soft,
    and a slice of a phone shows a fragment of every element instead of the one that matters. The
    dial is the product's signature, so it is the hero, rendered at 3x and downsampled, using the
    same tick scale, mesh band and log-scaled fill the app draws.

    Play may overlay a play button over the centre when a promo video is attached, and crops the
    edges on some surfaces, so the text holds to the left, the dial to the right, and the middle
    band carries nothing but background.
    """
    scale = 3
    w, h = 1024 * scale, 500 * scale
    canvas = gradient_background((w, h)).convert("RGBA")

    # --- throughput trace, along the lower band ------------------------------------------------
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

    # --- the dial ------------------------------------------------------------------------------
    cx, cy, radius = int(w * 0.775), int(h * 0.47), int(h * 0.36)
    progress = _dial_progress(HERO_MBPS)
    lit = GAUGE_SWEEP * progress

    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse(
        [cx - radius * 1.6, cy - radius * 1.6, cx + radius * 1.6, cy + radius * 1.6],
        fill=ACCENT + (26,))
    canvas = Image.alpha_composite(canvas, glow.filter(ImageFilter.GaussianBlur(70 * scale // 3)))

    ring = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    rd = ImageDraw.Draw(ring)

    mesh_in, mesh_out = radius * 0.58, radius * 0.84
    tick_in = radius * 0.90
    track_r = (mesh_in + mesh_out) / 2

    # Rim scale. Lit ticks carry the gradient; the rest state the range that is left.
    for i in range(TICK_COUNT):
        t = i / (TICK_COUNT - 1)
        a = GAUGE_START + GAUGE_SWEEP * t
        colour = _lerp(ACCENT, TERTIARY, t) + (255,) if t <= progress else IDLE + (110,)
        rd.line([_polar(cx, cy, a, tick_in), _polar(cx, cy, a, radius)],
                fill=colour, width=int(3 * scale))

    # Mesh band: a wash, four concentric arcs over it, then the spokes.
    step = 1.0
    i = 0.0
    while i < lit:
        t = i / GAUGE_SWEEP
        colour = _lerp(ACCENT, TERTIARY, t)
        a0 = GAUGE_START + i
        _band(rd, cx, cy, track_r, mesh_out - mesh_in, a0, a0 + step + 0.6, colour + (58,))
        for n in range(1, 5):
            r = mesh_in + (mesh_out - mesh_in) * n / 5
            _band(rd, cx, cy, r, 2 * scale, a0, a0 + step + 0.6, colour + (135,))
        i += step

    a = 0.0
    while a <= lit:
        t = a / GAUGE_SWEEP
        ang = GAUGE_START + a
        rd.line([_polar(cx, cy, ang, mesh_in), _polar(cx, cy, ang, mesh_out)],
                fill=_lerp(ACCENT, TERTIARY, t) + (150,), width=int(1.6 * scale))
        a += MESH_STEP

    # Hairline track, only where the mesh has not reached.
    _band(rd, cx, cy, track_r, 2 * scale, GAUGE_START + lit, GAUGE_START + GAUGE_SWEEP,
          IDLE + (130,))
    # The band's inner boundary, which gives the sweep a defined edge against the core.
    _band(rd, cx, cy, mesh_in, 3 * scale, GAUGE_START, GAUGE_START + lit, ACCENT + (235,))
    # Leading edge.
    edge = _lerp(ACCENT, TERTIARY, progress)
    rd.line([_polar(cx, cy, GAUGE_START + lit, mesh_in),
             _polar(cx, cy, GAUGE_START + lit, radius)],
            fill=edge + (255,), width=int(5 * scale))
    canvas = Image.alpha_composite(canvas, ring)

    # Core, so the trace does not run through the figure.
    disc = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    dd = ImageDraw.Draw(disc)
    inner = radius * 0.45
    dd.ellipse([cx - inner, cy - inner, cx + inner, cy + inner], fill=BG_TOP + (255,),
               outline=ACCENT + (140,), width=int(2 * scale))
    canvas = Image.alpha_composite(canvas, disc)

    draw = ImageDraw.Draw(canvas)
    draw.text((cx, cy - radius * 0.10), f"{HERO_MBPS:.1f}", font=font(58 * scale, 700),
              fill=TITLE, anchor="mm")
    draw.text((cx, cy + radius * 0.26), "Mbps", font=font(28 * scale, 600),
              fill=SUBTITLE, anchor="mm")

    # --- wordmark, left -------------------------------------------------------------------------
    x = 64 * scale
    draw.text((x, 128 * scale), "NetSpeed", font=font(92 * scale, 800), fill=TITLE)
    draw.text((x + 2, 242 * scale), "Data Usage Monitor", font=font(40 * scale, 600), fill=ACCENT)
    draw.rounded_rectangle([x + 2, 310 * scale, x + 94 * scale, 316 * scale], 3 * scale, fill=ACCENT)
    draw.text((x + 2, 344 * scale), "No ads  ·  No tracking  ·  All local",
              font=font(27 * scale, 500), fill=SUBTITLE)

    os.makedirs(OUT_DIR, exist_ok=True)
    out = os.path.join(OUT_DIR, out_name)
    # Play rejects alpha in the feature graphic, so it is flattened to RGB.
    canvas.resize((1024, 500), Image.LANCZOS).convert("RGB").save(out, "PNG", optimize=True)
    return out


# Play asks for 7-inch and 10-inch tablet screenshots separately; both accept the same artwork at
# different pixel sizes, so one set of captures feeds both. No callouts: the tablet captures were
# taken on a different device whose bars sit elsewhere, so the anchors would not line up.
TABLET_SLIDES = [
    ("tab_speed.png", "Built for the big screen", "One readable column, whatever the display"),
    ("tab_usage.png", "See which apps use data", "Per-app totals, split by mobile and Wi-Fi"),
    ("tab_history.png", "A month of history", "Every day, with warnings when you run hot"),
    ("tab_settings.png", "Yours to configure", "Units, alerts, overlay, themes"),
]

TABLET_SIZES = {"tablet_7": (1200, 1920), "tablet_10": (1600, 2560)}


def build_tablets():
    written = []
    for folder, size in TABLET_SIZES.items():
        out_dir = os.path.join(OUT_DIR, folder)
        for i, (src, title, sub) in enumerate(TABLET_SLIDES, start=1):
            if not os.path.exists(src):
                print(f"skipped (missing): {src}")
                continue
            written.append(build(src, title, sub, i, (), canvas_size=size, out_dir=out_dir))
    return written


if __name__ == "__main__":
    for i, (src, title, sub, callouts) in enumerate(SLIDES, start=1):
        if not os.path.exists(src):
            print(f"skipped (missing): {src}")
            continue
        print("wrote", build(src, title, sub, i, callouts))
    for path in build_tablets():
        print("wrote", path)
    print("wrote", build_feature_graphic())
