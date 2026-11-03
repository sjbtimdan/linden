#!/usr/bin/env python3
"""Overlay the Play Store caption strip on raw 1080x1920 Linden screenshots.

Capture the raw screens with `adb exec-out screencap -p`, name them as listed in
CAPTIONS below, put them in one directory, then run:

    python3 scripts/store-captions.py <raw-dir> [<out-dir>]

The output defaults to docs/store-assets/screenshots. Requires Pillow.
"""
import argparse
import os
import sys

from PIL import Image, ImageDraw, ImageFont

CAPTIONS = {
    "screen-1-ledger-entries.png": "All your money, one timeline",
    "screen-2-payment-capture.png": "Automatic payment capture",
    "screen-3-entry-calculator.png": "Add entries in seconds",
    "screen-4-ledger-accounts.png": "Track accounts in any currency",
    "screen-5-ledger-categories.png": "Know where it goes",
    "screen-6-insights.png": "12 months at a glance",
}

WIDTH, HEIGHT = 1080, 1920
LEFT, TOP, BOX_WIDTH, BOX_HEIGHT, RADIUS = 48, 30, 984, 120, 26
TEXT_SIZE = 57
SAMPLE_SCALE = 4
GRADIENT_START = (12, 64, 39)
GRADIENT_END = (29, 120, 72)


def load_font(size):
    candidates = [
        ("/System/Library/Fonts/SFNS.ttf", None, "Semibold"),
        ("/System/Library/Fonts/HelveticaNeue.ttc", 1, None),
        ("/System/Library/Fonts/Helvetica.ttc", 1, None),
        ("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", None, None),
        ("/System/Library/Fonts/Supplemental/Arial Bold.ttf", None, None),
    ]
    for path, index, variation in candidates:
        if not os.path.exists(path):
            continue
        font = ImageFont.truetype(path, size, index=index or 0)
        if variation:
            try:
                font.set_variation_by_name(variation)
            except OSError:
                pass
        return font
    return ImageFont.load_default(size)


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def build_gradient():
    gradient = Image.new("RGB", (BOX_WIDTH, BOX_HEIGHT))
    pixels = gradient.load()
    for y in range(BOX_HEIGHT):
        for x in range(BOX_WIDTH):
            t = (x / (BOX_WIDTH - 1) + y / (BOX_HEIGHT - 1)) / 2
            pixels[x, y] = lerp(GRADIENT_START, GRADIENT_END, t)
    return gradient


def build_mask():
    mask = Image.new("L", (BOX_WIDTH * SAMPLE_SCALE, BOX_HEIGHT * SAMPLE_SCALE), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, BOX_WIDTH * SAMPLE_SCALE - 1, BOX_HEIGHT * SAMPLE_SCALE - 1),
        radius=RADIUS * SAMPLE_SCALE,
        fill=255,
    )
    return mask.resize((BOX_WIDTH, BOX_HEIGHT), Image.LANCZOS)


def copy_background_over_status_icons(image):
    """The caption covers the status bar; clean up any pixel peeking out beside it."""
    pixels = image.load()
    for y in range(TOP, TOP + BOX_HEIGHT):
        for x in list(range(LEFT)) + list(range(LEFT + BOX_WIDTH, WIDTH)):
            red, green, blue = pixels[x, y]
            if red > 120 and green > 120 and blue > 120:
                source = LEFT - 1 if x < LEFT else LEFT + BOX_WIDTH
                pixels[x, y] = pixels[source, y]


def overlay(raw_path, out_path, font, gradient, mask):
    image = Image.open(raw_path).convert("RGB")
    if image.size != (WIDTH, HEIGHT):
        raise SystemExit(f"{raw_path}: expected {WIDTH}x{HEIGHT}, got {image.size[0]}x{image.size[1]}")

    copy_background_over_status_icons(image)
    image.paste(gradient, (LEFT, TOP), mask)

    draw = ImageDraw.Draw(image)
    caption = CAPTIONS[os.path.basename(raw_path)]
    bbox = draw.textbbox((0, 0), caption, font=font)
    if bbox[2] - bbox[0] > BOX_WIDTH - 56:
        raise SystemExit(f"{caption!r} is too wide for the caption strip")
    x = LEFT + BOX_WIDTH / 2 - (bbox[0] + bbox[2]) / 2
    y = TOP + BOX_HEIGHT / 2 - (bbox[1] + bbox[3]) / 2 + 1
    draw.text((x, y), caption, font=font, fill=(255, 255, 255))

    image.save(out_path, format="PNG", optimize=True)
    print(f"{out_path} ({image.size[0]}x{image.size[1]}, RGB)")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("raw_dir", help="directory containing the raw screen captures")
    parser.add_argument("out_dir", nargs="?", default="docs/store-assets/screenshots",
                        help="output directory (default: docs/store-assets/screenshots)")
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    font = load_font(TEXT_SIZE)
    gradient = build_gradient()
    mask = build_mask()

    missing = 0
    for name in CAPTIONS:
        raw_path = os.path.join(args.raw_dir, name)
        if not os.path.exists(raw_path):
            print(f"missing: {raw_path}", file=sys.stderr)
            missing += 1
            continue
        overlay(raw_path, os.path.join(args.out_dir, name), font, gradient, mask)
    if missing:
        raise SystemExit(f"{missing} raw screenshot(s) missing")


if __name__ == "__main__":
    main()
