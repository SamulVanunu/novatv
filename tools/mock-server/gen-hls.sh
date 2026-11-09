#!/usr/bin/env bash
# 60 saniyelik test yayını: HLS (6 sn segment) + tek parça TS.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/fixtures/hls"
mkdir -p "$OUT"
rm -f "$OUT"/index.m3u8 "$OUT"/seg*.ts "$HERE/fixtures/test.ts"

ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i "testsrc=size=640x360:rate=25" \
  -f lavfi -i "sine=frequency=440:sample_rate=48000" \
  -t 60 -c:v libx264 -preset veryfast -pix_fmt yuv420p -g 50 -sc_threshold 0 \
  -c:a aac -b:a 96k \
  -f hls -hls_time 6 -hls_list_size 0 -hls_flags independent_segments \
  -hls_segment_filename "$OUT/seg%03d.ts" "$OUT/index.m3u8"

ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i "testsrc=size=640x360:rate=25" \
  -f lavfi -i "sine=frequency=440:sample_rate=48000" \
  -t 60 -c:v libx264 -preset veryfast -pix_fmt yuv420p -g 50 -sc_threshold 0 \
  -c:a aac -b:a 96k -f mpegts "$HERE/fixtures/test.ts"

echo "HLS: $OUT/index.m3u8"
ffprobe -hide_banner -loglevel error -show_entries format=duration -of default=nw=1:nk=1 "$OUT/index.m3u8"
