# Kalan fazlar (P3–P8) — uygulama planı

Sıra değil, kapsam önemli: her faz sonunda yeşil build + kurulabilir APK.
Ayrıntı spec: `docs/superpowers/specs/2026-11-07-novatv-tvimate-parity-design.md`.

## P3 — EPG
- `core/epg`: XMLTV SAX ayrıştırıcı (offset/offset'siz zaman, `xmltv_ns`, kategori/reyting,
  gzip). Birim testleri: üç zaman biçimi, bölüm numarası, eksik `stop`.
- `core/sync`: `EpgRepository` — tvgId eşleşen kanallara program yazar, pencereyi
  (varsayılan 3 gün) tazeler, eskiyi siler; `SyncEngine` canlı senkrondan sonra çağırır.
- DAO: gün penceresi sorgusu, kanal başına şimdi/sırada, eski temizliği.
- `feature/epg`: 3 gün sekmesi, kanal × saat ızgarası (tek yatay kaydırma, dikey
  sanallaştırma), program detayı, hatırlatıcı (AlarmManager + bildirim), catch-up
  düğmesi (`hasArchive` + `catchupUrl`).
- Oynatıcı: `PlayerRoutes` arşiv argümanları; `StreamResolver.resolveCatchup`.
- Live başlığına "Rehber" girişi.

## P4 — Kütüphane
- Xtream VOD/dizi senkronu (`get_vod_streams`, `get_series`, `get_series_info`),
  `movie/series/season/episode` tablolarına yazım; M3U/Stalker boş.
- `feature/library`: poster ızgarası, tür/yıl filtresi, künye, dizi → sezon → bölüm,
  `watch_history` ile "devam et" (konum + süre).
- VOD akış çözücü (kimlik bellekten, URL üretilir; DB'ye yazılmaz).

## P5 — Kayıt
- `core/recording`: TS bayt kopyalayıcı + HLS segment birleştirici (yeniden bağlanma,
  `#EXT-X-DISCONTINUITY`), foreground servis, bildirim, yarım dosya kurtarma.
- Planlama: AlarmManager exact + BOOT_COMPLETED yeniden kurma; seri kuralı
  (kanal + normalize başlık, `keepEpisodes`), çakışma soru politikası (1 eşzamanlı).
- Depolama: SAF ağaç URI, yoksa uygulama dış dizini; boş alan kontrolü.
- `feature/recordings`: liste, oynat, sil; EPG detayından "Kaydet".

## P6 — Çoklu ekran
- `feature/multiwall`: 2/4 kutu, kutu başına kanal seçici, odaklanan kutunun sesi,
  düşük tampon; D-pad ile kutu gezinme ve geri.

## P7 — Profil / ebeveyn / arama
- Profil CRUD + geçiş (favori/geçmiş zaten profil bazlı), PIN (SHA-256 + tuz).
- Ebeveyn: reyting sınırı (EPG kategori/reyting) + gizli kanal/grup; kilitli içerikte PIN.
- Tek arama: kanal + film + dizi + EPG başlığı, `nameNormalized` indeksleriyle.

## P8 — Görünüm / sistem
- Tema (koyu/açık), vurgu, punto/yoğunluk, saat biçimi, EPG satır yüksekliği.
- Yedekleme/geri yükleme: JSON (kimlik bilgisi yok), isteğe bağlı AES.
- Uyku zamanlayıcısı, açılışta başlat, AFR (destekli cihazda), imzalı APK (var).

## Doğrulama
Her faz: birim testleri + `:app:assembleDebug`; faz sonunda bağlı testler ve
emülatörde D-pad ile elle tur. Gerçek playlist yalnızca cihazda.
