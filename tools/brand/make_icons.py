"""NovaTV launcher ikonu ve TV banner'ını üretir.

Tasarım: sinyal kırmızısı zemin, krem "N". N, üç parçanın birleşimi:
sol gövde, sağ gövde ve iki ucu yatay kesilmiş diyagonal. Geometri
108x108'lik adaptive icon tuvalinde güvenli daireye (r=33) sığar.
"""
from PIL import Image, ImageDraw, ImageFont

RED = (232, 50, 42, 255)
CREAM = (246, 241, 231, 255)
FONT = "/home/samuel/nova-tv/ui/src/main/res/font/plex_sans_condensed_semibold.ttf"
OUT = "/home/samuel/nova-tv/app/src/main/res"

# 108 birimlik tuvalde N geometrisi (güvenli daire içinde).
X0, X1 = 32.0, 76.0
Y0, Y1 = 30.0, 78.0
STEM = 8.0
DIAG = 10.0


def n_shapes(x0=X0, x1=X1, y0=Y0, y1=Y1, stem=STEM, diag=DIAG):
    """N'i üç poligon olarak döndürür."""
    left = [(x0, y0), (x0 + stem, y0), (x0 + stem, y1), (x0, y1)]
    right = [(x1 - stem, y0), (x1, y0), (x1, y1), (x1 - stem, y1)]
    top_l = x0 + stem
    bot_r = x1 - stem
    diagonal = [
        (top_l, y0),
        (top_l + diag, y0),
        (bot_r, y1),
        (bot_r - diag, y1),
    ]
    return [left, right, diagonal]


def draw_n(draw, scale, ox, oy, color, origin=(0.0, 0.0)):
    """N'i çizer. origin, tasarım koordinatında (ox, oy)'ye oturan noktadır."""
    for poly in n_shapes():
        draw.polygon(
            [(ox + (x - origin[0]) * scale, oy + (y - origin[1]) * scale) for x, y in poly],
            fill=color,
        )


def rounded_tile(size, radius_ratio=0.235, ss=4):
    w = size * ss
    img = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, w - 1, w - 1], radius=int(w * radius_ratio), fill=RED)
    draw_n(d, scale=w / 108.0, ox=0, oy=0, color=CREAM)
    return img.resize((size, size), Image.LANCZOS)


def banner(width, height, ss=4):
    w, h = width * ss, height * ss
    img = Image.new("RGBA", (w, h), RED)
    d = ImageDraw.Draw(img)

    # N işareti solda, kilit dilimin dikey ortasında.
    mark_h = h * 0.28
    scale = mark_h / (Y1 - Y0)
    mark_w = (X1 - X0) * scale
    # Kelime markası: optik olarak işaretin ortasına hizalı, geniş aralıklı.
    text = "NOVATV"
    tracking = w * 0.013
    font = ImageFont.truetype(FONT, int(h * 0.205))
    bbox = d.textbbox((0, 0), text, font=font)
    cap_h = bbox[3] - bbox[1]
    widths = [d.textlength(ch, font=font) for ch in text]
    total = sum(widths) + tracking * (len(text) - 1)

    # Kilit dilimi yatayda ortala.
    gap = w * 0.052
    ox = (w - (mark_w + gap + total)) / 2
    oy = (h - mark_h) / 2
    draw_n(d, scale=scale, ox=ox, oy=oy, color=CREAM, origin=(X0, Y0))
    x = ox + mark_w + gap
    y = (h - cap_h) / 2 - bbox[1]
    for ch, cw in zip(text, widths):
        d.text((x, y), ch, font=font, fill=CREAM)
        x += cw + tracking

    return img.resize((width, height), Image.LANCZOS)


def main():
    for folder, size in [
        ("mipmap-mdpi", 48),
        ("mipmap-hdpi", 72),
        ("mipmap-xhdpi", 96),
        ("mipmap-xxhdpi", 144),
        ("mipmap-xxxhdpi", 192),
    ]:
        img = rounded_tile(size)
        img.save(f"{OUT}/{folder}/ic_launcher.png")
        print(folder, size, "ok")

    for folder, dims in [
        ("drawable-mdpi", (320, 180)),
        ("drawable-xhdpi", (640, 360)),
        ("drawable-xxhdpi", (960, 540)),
    ]:
        img = banner(*dims)
        img.save(f"{OUT}/{folder}/banner.png")
        print(folder, dims, "ok")


if __name__ == "__main__":
    main()
