#!/bin/sh
# Renders the foreground layer, the legacy launcher PNG and docs/mockups/icon-preview.png
# (needs python3, rsvg-convert, ImageMagick).
set -e
cd "$(dirname "$0")"
python3 gen_icon.py
RES=../../../smarttubetv/src/oculo/res
T=$(mktemp -d)
mkdir -p "$RES/drawable-nodpi"
rm -f "$RES/drawable/oculo_icon_foreground.xml"
# Foreground layer: 108dp at 4 px/dp (xxxhdpi), transparent outside the mark and its glow.
rsvg-convert -w 432 -h 432 oculo-icon-foreground.svg -o "$RES/drawable-nodpi/oculo_icon_foreground.png"
rsvg-convert -w 512 -h 512 oculo-icon-legacy.svg -o "$T/legacy.png"
magick "$T/legacy.png" -alpha off "$RES/mipmap-nodpi/app_icon.png"
rsvg-convert -w 1024 -h 1024 oculo-icon-full.svg -o "$T/full.png"
# Masks applied to the 108dp canvas: circle of 72dp diameter (round launchers, Google TV) and squircle.
magick -size 1024x1024 xc:black -fill white -draw "circle 512,512 512,171" "$T/circle.png"
magick -size 1024x1024 xc:black -fill white -draw "roundrectangle 171,171 853,853 150,150" "$T/squircle.png"
for m in circle squircle; do
  magick "$T/full.png" "$T/$m.png" -alpha off -compose CopyOpacity -composite -crop 684x684+170+170 +repage "$T/$m-icon.png"
done
# Banner crop (the TV mark of the Android TV banner) for comparison, same height as the large icons.
magick ../logos-v3/oculo-banner@2x.png -crop 230x230+40+65 +repage -resize 300x300 "$T/banner.png"
# Large (300px) and small (64px and 40px, shown 1:1) on the Google TV dark background, banner crop at the end.
magick -size 1400x420 xc:'#1F1F1F' \
  \( "$T/circle-icon.png" -resize 300x300 \) -geometry +40+40 -composite \
  \( "$T/squircle-icon.png" -resize 300x300 \) -geometry +380+40 -composite \
  \( "$T/circle-icon.png" -resize 64x64 \) -geometry +740+80 -composite \
  \( "$T/squircle-icon.png" -resize 64x64 \) -geometry +840+80 -composite \
  \( "$T/circle-icon.png" -resize 40x40 \) -geometry +752+200 -composite \
  \( "$T/squircle-icon.png" -resize 40x40 \) -geometry +852+200 -composite \
  \( "$RES/mipmap-nodpi/app_icon.png" -resize 120x120 \) -geometry +740+270 -composite \
  "$T/banner.png" -geometry +1060+40 -composite \
  -fill '#9A9A9A' -font /System/Library/Fonts/Supplemental/Arial.ttf -pointsize 18 \
  -annotate +40+375 'Round (Google TV)' -annotate +380+375 'Squircle' -annotate +740+60 'Small' \
  -annotate +870+390 'Legacy PNG' -annotate +1060+375 'Banner (2x crop)' \
  ../icon-preview.png
rm -rf "$T"
