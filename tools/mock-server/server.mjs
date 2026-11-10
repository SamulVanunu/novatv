#!/usr/bin/env node
// NovaTV geliştirme amaçlı sahte IPTV paneli.
// Xtream, M3U ve Stalker uçlarını; HLS/TS fixture'larını ve logoları servis eder.
// Kullanım: node tools/mock-server/server.mjs  (varsayılan port 8089)

import { createServer } from "node:http";
import { readFile, stat } from "node:fs/promises";
import { extname, join, normalize, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = resolve(fileURLToPath(new URL(".", import.meta.url)));
const FIXTURES = join(ROOT, "fixtures");
const PORT = Number(process.env.PORT ?? 8089);
const BASE = `http://127.0.0.1:${PORT}`;

/** İstemcinin gördüğü taban adres: host'ta 127.0.0.1, emülatörde 10.0.2.2. */
function originOf(req) {
  return req.headers.host ? `http://${req.headers.host}` : BASE;
}

/** Fixture'lardaki 127.0.0.1 bağlantılarını isteğin host'una çevirir. */
function rewriteHosts(body, req) {
  const host = req.headers.host;
  return host ? body.split(`127.0.0.1:${PORT}`).join(host) : body;
}
const USER = "demo";
const PASS = "demo";
const MAC = "00:1A:79:00:00:01";

const PNG_1X1 = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9ZJ8sAAAAASUVORK5CYII=",
  "base64",
);

const mime = {
  ".m3u8": "application/vnd.apple.mpegurl",
  ".ts": "video/mp2t",
  ".m3u": "audio/x-mpegurl",
  ".json": "application/json; charset=utf-8",
  ".png": "image/png",
};

function json(res, body, status = 200) {
  const payload = JSON.stringify(body);
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "content-length": Buffer.byteLength(payload),
    "access-control-allow-origin": "*",
  });
  res.end(payload);
}

function text(res, body, type = "text/plain; charset=utf-8", status = 200) {
  res.writeHead(status, {
    "content-type": type,
    "content-length": Buffer.byteLength(body),
    "access-control-allow-origin": "*",
  });
  res.end(body);
}

async function readJson(name, req) {
  const raw = await readFile(join(FIXTURES, "xtream", name), "utf8");
  return JSON.parse(req ? rewriteHosts(raw, req) : raw);
}

/**
 * XMLTV zamanı: `YYYYMMDDhhmmss +0300`. Rehber "şimdi"ye göre üretildiği için
 * kanal listesindeki programlar her zaman güncel saatle örtüşür.
 */
function xmltvTime(ms) {
  const date = new Date(ms + 3 * 60 * 60 * 1000);
  const pad = (value) => String(value).padStart(2, "0");
  return (
    date.getUTCFullYear() +
    pad(date.getUTCMonth() + 1) +
    pad(date.getUTCDate()) +
    pad(date.getUTCHours()) +
    pad(date.getUTCMinutes()) +
    pad(date.getUTCSeconds()) +
    " +0300"
  );
}

function xmlEscape(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

const XMLTV_CHANNELS = [
  { id: "trt1", name: "TRT 1", icon: "/logo/trt1.png" },
  { id: "trtspor", name: "TRT Spor", icon: "/logo/trtspor.png" },
  { id: "belgesel", name: "Doğa TV", icon: null },
];

const XMLTV_SLOTS = {
  trt1: [
    ["Sabah Haberleri", "Günün ilk haber bülteni."],
    ["Gündüz Kuşağı", "Sohbet ve yaşam programı."],
    ["Öğle Haberleri", "Öğle saatlerinin gelişmeleri."],
    ["Dizi Kuşağı", "Günün dizisi."],
    ["Ana Haber", "Akşam haberlerinin tamamı."],
    ["Prime Time", "Akşamın öne çıkan yapımı."],
    ["Gece Yayını", "Gece boyunca seçkin yapımlar."],
  ],
  trtspor: [
    ["Spor Bülteni", "Günün spor gelişmeleri."],
    ["Canlı Maç", "Stüdyodan canlı karşılaşma."],
    ["Spor Gündemi", "Yorumcularla günün değerlendirmesi."],
    ["Özetler", "Günün maç özetleri."],
  ],
  belgesel: [
    ["Vahşi Yaşam", "Doğanın saklı köşelerinden görüntüler."],
    ["Okyanuslar", "Derin sulardaki yaşam."],
    ["Anadolu", "Anadolu'nun dört mevsimi."],
    ["Gökyüzü", "Gece gökyüzünün izinde."],
  ],
};

/** "Şimdi"ye çıpalanmış 30 dakikalık programlarla XMLTV üretir. */
function buildXmltv(origin) {
  const now = Date.now();
  const slot = 30 * 60 * 1000;
  const currentSlot = Math.floor(now / slot);
  const from = currentSlot - 8; // 4 saat geriye: geçmiş program + catch-up
  const to = currentSlot + 144; // 3 gün ileri: yarın/öbür gün dolu

  const parts = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<tv generator-info-name="NovaTV mock">',
  ];

  for (const channel of XMLTV_CHANNELS) {
    parts.push(`  <channel id="${xmlEscape(channel.id)}">`);
    parts.push(`    <display-name>${xmlEscape(channel.name)}</display-name>`);
    if (channel.icon) parts.push(`    <icon src="${xmlEscape(origin + channel.icon)}" />`);
    parts.push("  </channel>");
  }

  const channels = Object.keys(XMLTV_SLOTS);
  for (let index = from; index < to; index++) {
    for (const channelId of channels) {
      const variants = XMLTV_SLOTS[channelId];
      const [title, description] = variants[index % variants.length];
      const startAt = index * slot;
      const endAt = startAt + slot;
      // Ana haber kuşağında yaş sınırı etiketi: ebeveyn kilidi testleri için.
      const rated = title === "Prime Time";
      parts.push(`  <programme start="${xmltvTime(startAt)}" stop="${xmltvTime(endAt)}" channel="${xmlEscape(channelId)}">`);
      parts.push(`    <title lang="tr">${xmlEscape(title)}</title>`);
      parts.push(`    <desc lang="tr">${xmlEscape(description)}</desc>`);
      parts.push(`    <category lang="tr">${xmlEscape(channelId === "trtspor" ? "Spor" : "Genel")}</category>`);
      if (rated) parts.push('    <rating system="MPAA"><value>18</value></rating>');
      if (index % 6 === 0) parts.push('    <new />');
      parts.push("  </programme>");
    }
  }

  parts.push("</tv>");
  return parts.join("\n");
}

async function serveFile(res, path) {
  try {
    const info = await stat(path);
    if (!info.isFile()) return text(res, "bulunamadı", "text/plain", 404);
    const body = await readFile(path);
    res.writeHead(200, {
      "content-type": mime[extname(path)] ?? "application/octet-stream",
      "content-length": body.length,
      "access-control-allow-origin": "*",
    });
    res.end(body);
  } catch {
    text(res, "bulunamadı", "text/plain", 404);
  }
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url ?? "/", BASE);
  const path = url.pathname;
  console.log(`${new Date().toISOString()} ${req.method} ${req.url}`);

  if (req.method === "OPTIONS") {
    res.writeHead(204, {
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "GET, POST, OPTIONS",
      "access-control-allow-headers": "*",
    });
    res.end();
    return;
  }

  // --- Sağlık ---
  if (path === "/" || path === "/health") {
    const origin = originOf(req);
    json(res, { ok: true, user: USER, hls: `${origin}/hls/index.m3u8`, ts: `${origin}/fixtures/test.ts` });
    return;
  }

  // --- Xtream Codes ---
  if (path === "/player_api.php") {
    const username = url.searchParams.get("username");
    const password = url.searchParams.get("password");
    const action = url.searchParams.get("action");

    if (username !== USER || password !== PASS) {
      json(res, {
        user_info: { auth: 0, status: "Invalid", message: "Kullanıcı adı veya şifre hatalı" },
      });
      return;
    }

    if (!action) {
      json(res, {
        user_info: {
          username: USER,
          password: PASS,
          message: "NovaTV mock panel",
          auth: 1,
          status: "Active",
          exp_date: "4102444800",
          is_trial: "0",
          active_cons: "1",
          created_at: "1700000000",
          max_connections: "4",
          allowed_output_formats: ["m3u8", "ts"],
        },
        server_info: {
          url: new URL(originOf(req)).hostname,
          port: String(PORT),
          server_protocol: "http",
          timezone: "Europe/Istanbul",
        },
      });
      return;
    }

    if (action === "get_live_categories") {
      json(res, await readJson("get_live_categories.json"));
      return;
    }
    if (action === "get_live_streams") {
      json(res, await readJson("get_live_streams.json", req));
      return;
    }
    if (action === "get_vod_categories") {
      json(res, await readJson("get_vod_categories.json"));
      return;
    }
    if (action === "get_vod_streams") {
      json(res, await readJson("get_vod_streams.json", req));
      return;
    }
    if (action === "get_series_categories") {
      json(res, await readJson("get_series_categories.json"));
      return;
    }
    if (action === "get_series") {
      json(res, await readJson("get_series.json", req));
      return;
    }
    if (action === "get_series_info") {
      json(res, await readJson("get_series_info.json", req));
      return;
    }
    if (action === "get_short_epg") {
      json(res, { epg_listings: [] });
      return;
    }
    json(res, []);
    return;
  }

  if (path === "/get.php") {
    const type = url.searchParams.get("type") ?? "m3u_plus";
    if (url.searchParams.get("username") !== USER || url.searchParams.get("password") !== PASS) {
      text(res, "kimlik reddedildi", "text/plain", 401);
      return;
    }
    const playlist = await readFile(join(FIXTURES, "live.m3u"), "utf8");
    text(res, rewriteHosts(playlist, req), type.includes("m3u") ? mime[".m3u"] : "text/plain");
    return;
  }

  if (path === "/xmltv.php") {
    text(res, buildXmltv(originOf(req)), "application/xml; charset=utf-8");
    return;
  }

  // --- Yayın yönlendirmeleri ---
  if (/^\/live\/demo\/demo\/[^/]+\.m3u8$/.test(path)) {
    res.writeHead(302, { location: "/hls/index.m3u8" });
    res.end();
    return;
  }
  if (/^\/live\/demo\/demo\/[^/]+\.ts$/.test(path)) {
    res.writeHead(302, { location: "/fixtures/test.ts" });
    res.end();
    return;
  }
  if (/^\/movie\/demo\/demo\/[^/]+$/.test(path)) {
    res.writeHead(302, { location: "/fixtures/test.ts" });
    res.end();
    return;
  }
  if (/^\/series\/demo\/demo\/[^/]+$/.test(path)) {
    res.writeHead(302, { location: "/fixtures/test.ts" });
    res.end();
    return;
  }
  // Arşiv (catch-up): /timeshift/demo/demo/<dakika>/<yyyy-MM-dd:HH-mm>/<id>.ts
  if (/^\/timeshift\/demo\/demo\/[^/]+\/[^/]+\/[^/]+$/.test(path)) {
    res.writeHead(302, { location: "/fixtures/test.ts" });
    res.end();
    return;
  }

  // --- Stalker Portal ---
  if (path.endsWith("/portal.php")) {
    const action = url.searchParams.get("action");
    if (action === "handshake") {
      json(res, { js: { token: "mock-stalker-token", random: "1234567890" } });
      return;
    }
    if (action === "get_profile") {
      json(res, {
        js: {
          id: "1",
          name: "NovaTV mock",
          status: "1",
          expire_billing_date: "2099-12-31 23:59:59",
          max_connections: "2",
          mac: MAC,
        },
      });
      return;
    }
    if (action === "get_genres") {
      json(res, {
        js: [
          { id: "1", title: "Ulusal" },
          { id: "2", title: "Spor" },
        ],
      });
      return;
    }
    if (action === "get_all_channels") {
      json(res, {
        js: {
          data: [
            { id: "1", name: "Portal TRT 1", tv_genre_id: "1", logo: `${originOf(req)}/logo/trt1.png`, xmltv_id: "trt1", tv_archive: "0", tv_archive_duration: "0" },
            { id: "2", name: "Portal Spor", tv_genre_id: "2", logo: "", xmltv_id: "trtspor", tv_archive: "0", tv_archive_duration: "0" },
          ],
        },
      });
      return;
    }
    json(res, { js: { data: [] } });
    return;
  }

  if (path.endsWith("/play/live.php")) {
    // Portal istenen uzantıya göre yönlendirir: m3u8 → HLS, ts → MPEG-TS.
    const ext = url.searchParams.get("extension") ?? "ts";
    const target = ext.includes("m3u8") ? "/hls/index.m3u8" : "/fixtures/test.ts";
    res.writeHead(302, { location: target });
    res.end();
    return;
  }

  // --- Logolar ---
  if (path.startsWith("/logo/")) {
    res.writeHead(200, { "content-type": "image/png", "content-length": PNG_1X1.length });
    res.end(PNG_1X1);
    return;
  }

  // --- Statik fixture'lar ---
  if (path.startsWith("/hls/")) {
    const file = normalize(join(FIXTURES, "hls", path.slice("/hls/".length)));
    if (!file.startsWith(join(FIXTURES, "hls"))) return text(res, "yasak", "text/plain", 403);
    return serveFile(res, file);
  }
  if (path === "/fixtures/test.ts") {
    return serveFile(res, join(FIXTURES, "test.ts"));
  }

  text(res, "bulunamadı", "text/plain", 404);
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`NovaTV mock panel: ${BASE}`);
  console.log(`  Xtream : ${BASE}/player_api.php?username=demo&password=demo`);
  console.log(`  M3U    : ${BASE}/get.php?username=demo&password=demo&type=m3u_plus`);
  console.log(`  HLS    : ${BASE}/hls/index.m3u8`);
});
