# NovaTV — TiviMate paritesinde Android TV IPTV oynatıcı

**Tarih:** 2026-11-07
**Durum:** Tasarım — uygulama onayı bekliyor
**Paket:** `dev.anon.novatv`
**Dizin:** `~/nova-tv`

---

## 1. Amaç

Android TV / Google TV / Fire TV için, TiviMate'ın kullanıcı deneyimini karşılayan
bir IPTV oynatıcı. Uygulama **boş doğar**: içerik barındırmaz, yalnızca kullanıcının
sağladığı kaynakları (Xtream Codes, M3U, Stalker Portal) okur ve oynatır.

Başarı, "kanal açılıyor" değil; TiviMate kullanan birinin cihazına kurup
"aradığım her şey burada" demesi.

### 1.1 Başarı kriterleri (ölçülebilir)

| Kriter | Hedef |
|---|---|
| Soğuk açılış (Fire TV Stick 4K) | < 2,0 s — son kanal listesine |
| 20.000 kanallı listede gezinme | 60 fps, takılma yok |
| EPG ızgarası (200 kanal × 72 saat) | akıcı kaydırma, bellek < 300 MB |
| Playlist yenileme (20.000 kanal) | < 60 s, iptal edilebilir, ilerleme gösterir |
| Kanal değiştirme (zapping) | < 1,5 s ilk kare |
| Kayıt | Planlanan programın %100'ü, çakışmada kullanıcıya sorar |
| Kumanda | Her ekran yalnızca D-pad ile tam kullanılabilir |

---

## 2. Kapsam

### 2.1 Hedef — TiviMate parite matrisi

Faz numaraları bölüm 9'daki teslim sırasıdır.

| Alan | Özellik | Faz |
|---|---|---|
| Kaynak | Xtream Codes (kullanıcı/şifre, API) | P1 |
| Kaynak | M3U/M3U8 URL + ayrı XMLTV URL | P1 |
| Kaynak | Stalker Portal (MAC) | P1 |
| Kaynak | Çoklu playlist, sıralama, aktif/pasif | P1 |
| Kaynak | Playlist başına User-Agent / Referer | P1 |
| Kaynak | Otomatik yenileme aralığı, elle yenileme | P1 |
| Canlı TV | Grup listesi, kanal listesi, logo, kanal numarası | P1 |
| Canlı TV | Favoriler, son izlenenler, gizleme, sıralama | P1 |
| Canlı TV | Liste/ızgara görünümü, kanal listesi içinde arama | P1 |
| Oynatıcı | HLS / TS / DASH, donanım+yazılım çözme | P2 |
| Oynatıcı | Ses parçası, altyazı (gömülü + harici SRT), ses passthrough | P2 |
| Oynatıcı | En-boy oranı modları, buffer ayarı | P2 |
| Oynatıcı | OSD: kanal bilgisi, EPG şeridi, ilerleme, kumanda kısayolları | P2 |
| Oynatıcı | PiP, son kanalı hatırla, açılışta son kanal | P2 |
| Oynatıcı | Otomatik kare hızı (AFR) — cihaz destekliyorsa | P8 |
| EPG | XMLTV ayrıştırma, çok günlük ızgara, şimdi/sıradaki | P3 |
| EPG | Program detayı, hatırlatıcı, kategori/reyting | P3 |
| EPG | Catch-up (arşiv) oynatma, kanal başına arşiv günü | P3 |
| EPG | EPG kaynağı atama, saat dilimi, artımlı senkron | P3 |
| Kütüphane | Film ızgarası, poster, künye, tür/yıl filtresi | P4 |
| Kütüphane | Dizi → sezon → bölüm, izleme ilerlemesi, devam et | P4 |
| Kayıt | Elle kayıt, programdan kayıt, planlı kayıt | P5 |
| Kayıt | Seri kuralı, bölüm saklama sayısı, erken/geç başlatma | P5 |
| Kayıt | Depolama klasörü (SAF), çakışma yönetimi, bildirim | P5 |
| Kayıt | Kayıt listesi, oynatma, silme | P5 |
| Çoklu ekran | 2 ve 4 kutu, ses odağı seçimi, kutu başına kanal | P6 |
| Profil | Çoklu profil, ayrı favori/geçmiş/kayıt, PIN | P7 |
| Ebeveyn | PIN, reyting sınırı, grup/kanal kilidi | P7 |
| Arama | Kanal + film + dizi + EPG üzerinde tek arama | P7 |
| Görünüm | Tema (koyu/açık/renkli), vurgu rengi, yoğunluk, punto | P8 |
| Görünüm | EPG satır yüksekliği/saat aralığı, liste stili, saat formatı | P8 |
| Sistem | Yedekleme/geri yükleme (JSON, isteğe bağlı şifreli) | P8 |
| Sistem | Uyku zamanlayıcısı, açılışta başlat | P8 |
| Sistem | Sideload APK, imzalı sürüm, güncelleme bildirimi | P8 |

### 2.2 Kapsam dışı (YAGNI)

- Bulut senkronizasyonu, hesap sistemi, cihazlar arası paylaşım
- iOS / web / Tizen / webOS (mimari bunu engellemez, kod tabanı ayrı olur)
- Uygulama içi satın alma, reklam, abonelik
- İçerik barındırma, kendi kaynak servisi, transcoding
- DRM (Widevine) — kaynakların sağladığı düz HLS/TS ile sınırlı
- Telefon/tablet için özel arayüz (çalışır, optimize edilmez)
- Sunucu tarafı EPG önbelleği

### 2.3 Yasal sınır

Uygulama hiçbir kanal, film veya EPG verisi içermez. Kullanıcı kendi kaynağını girer.
Mağaza dışı (sideload) dağıtım bu yüzden seçildi: içerik sorumluluğu kullanıcıda,
uygulama bir oynatıcı.

---

## 3. Mimari

### 3.1 Temel ilke: yerel-öncelikli, sunucusuz

Ağ yalnızca senkron anında. Room tek doğru kaynak; UI yalnızca `Flow` dinler.
Liste açılışı ağ beklemez. Bu, 20.000 kanallı listelerde kumanda gecikmesini
sıfırlar ve çevrimdışı çalışmayı bedava getirir.

```
Kaynak (HTTP)  →  SyncWorker  →  Room  →  Repository  →  ViewModel  →  Compose
                                      ↘  PlayerManager (doğrudan stream URL)
```

### 3.2 Modüller ve bağımlılık yönü

Kural: `feature → core`, `core → core`. `feature` bir `feature`'ı import etmez.
`core:model` hiçbir şeye bağımlı değil (saf Kotlin).

| Modül | Bağımlı olduğu | İçerik |
|---|---|---|
| `:core:model` | — | Entity'ler, değer nesneleri, saf kurallar |
| `:core:database` | model | Room, DAO, migration, FTS |
| `:core:network` | model | OkHttp, ortak HTTP politikası, User-Agent |
| `:core:source` | model, network | `SourceAdapter` arayüzü, kayıt defteri |
| `:core:source-xstream` | source | Xtream API |
| `:core:source-m3u` | source | M3U + XMLTV URL |
| `:core:source-stalker` | source | Stalker Portal |
| `:core:sync` | model, database, source, settings | Playlist/Channel depoları, senkron motoru, WorkManager |
| `:core:epg` | model, database, network | XMLTV parser, senkron |
| `:core:player` | model | Media3 sarmalayıcı, parça/en-boy, PiP |
| `:core:recording` | model, database, network, player | Kayıt servisi, planlayıcı |
| `:core:settings` | model | DataStore, tema, profil, yedekleme |
| `:ui` | model | TV tasarım sistemi |
| `:feature:live` | hepsi (core) | Kanal listesi, gruplar |
| `:feature:epg` | core | EPG ızgarası |
| `:feature:vod` | core | Film/dizi |
| `:feature:player` | core | Tam ekran + OSD + çoklu ekran |
| `:feature:recordings` | core | Kayıt yönetimi |
| `:feature:search` | core | Birleşik arama |
| `:feature:settings` | core | Playlist, profil, görünüm |
| `:app` | tüm feature'lar | Nav, DI, ilk açılış |

### 3.3 Teknoloji tabanı

Kotlin, Jetpack Compose for TV, Media3/ExoPlayer, Room (KSP), Hilt, WorkManager,
DataStore, Paging3, Coil, kotlinx.serialization, OkHttp, XmlPullParser.
`minSdk 23`, `targetSdk 35`, `compileSdk 36`. Sürümler P0'da gerçek çözümlemeyle
kilitlenir; spec sürüm numarası iddia etmez.

**Neden native:** TiviMate ile aynı zemin — donanım çözme, tunneling, PiP,
kumanda odağı, gerçek kayıt. Çapraz platform çözümler bu dörtlüden en az ikisini
platform kanalıyla taklit eder.

### 3.4 Durum yönetimi

MVVM + tek yönlü akış. Her ekran `ViewModel` + `StateFlow<UiState>`.
Yan etkiler `suspend`; UI olayları `sealed interface` ile. Uzun listeler Paging3.

---

## 4. Veri modeli

```
Playlist(id, type, name, baseUrl, username, epgUrl, userAgent,
         isActive, sortOrder, lastSyncAt, lastSyncStatus, syncIntervalHours)
Credentials(playlistId, username, password)   // Keystore'lu ayrı depo, Room'da düz metin yok
Profile(id, name, pinHash, maxRating, isKids, createdAt)
Favorite(profileId, itemType, itemId)         // kanal / film / dizi
HiddenItem(profileId, itemType, key)          // grup veya kanal
WatchHistory(profileId, itemType, itemId, positionMs, durationMs, updatedAt)

Channel(id, playlistId, remoteId, number, name, groupName, logoUrl,
        tvgId, hasArchive, archiveDays, streamFormat)   // akış URL'i saklanmaz; StreamResolver üretir
EpgChannel(id, playlistId, tvgId, displayName, iconUrl)
EpgProgram(id, epgChannelId, channelId?, startAt, endAt, title, subtitle,
           description, category, rating, episodeNum, isNew)
Movie(id, playlistId, remoteId, name, year, rating, genres, plot, posterUrl,
      durationMin, containerExt, addedAt)
Series(id, playlistId, remoteId, name, year, rating, genres, plot, posterUrl,
       backdropUrl, episodeCount)
Season(id, seriesId, number, name)
Episode(id, seriesId, seasonId, remoteId, number, title, plot, durationMin,
        containerExt, stillUrl)

RecordingRule(id, profileId, channelId, titlePattern, seriesId?, keepEpisodes,
              startEarlyMin, endLateMin, folderUri, enabled)
Recording(id, ruleId?, channelId, title, startAt, endAt, status, fileUri,
          bytesWritten, error)
```

İndeksler: `Channel(playlistId, groupName)`, `Channel(name COLLATE NOCASE)`,
`EpgProgram(channelId, startAt)`, `Episode(seriesId, seasonId, number)`.
FTS4 sanal tabloları (`channel_fts`, `movie_fts`, `series_fts`, `epg_fts`) P7'de,
birleşik arama ile birlikte eklenir. P1'de kanal araması `nameNormalized` kolonu
üzerinde indeksli `LIKE` ile yapılır (20k satır için ölçülebilir maliyeti yok).

**Türkçe arama:** `İ/ı` katlaması SQLite'ın varsayılan `NOCASE`'i ile bozulur.
FTS tablolarına normalize edilmiş (lowercase + Türkçe eşleme) bir `search_title`
kolonu yazılır; sorgu aynı normalizasyondan geçer.

**Migration:** Şema sürümü her değişimde artar; `MigrationTestHelper` ile test.
Veri kaybı yalnızca playlist yeniden senkronuyla telafi edilebilir olduğu için
kaynak verisi türetilebilir; kullanıcı verisi (favori, geçmiş, kural, kayıt)
asla düşürülmez.

---

## 5. Kaynak katmanı

```kotlin
interface SourceAdapter {
    suspend fun authenticate(): SourceAccount
    suspend fun live(): List<Channel>
    suspend fun vod(): List<Movie>
    suspend fun series(): List<Series>
    suspend fun epg(days: Int): EpgSource
    fun streamUrl(channel: Channel, format: StreamFormat): String
    fun catchupUrl(channel: Channel, at: Instant, minutes: Int): String?
}
```

| Adaptör | Kimlik | Notlar |
|---|---|---|
| Xtream | `player_api.php` | `get_live_streams`, `get_vod_streams`, `get_series`, `get_series_info`, `xmltv.php`, `get_short_epg`. Arşiv: `tv_archive` + `tv_archive_duration` |
| M3U | URL | `#EXTINF` (süre, `tvg-id`, `tvg-logo`, `group-title`), `#EXTGRP`, `#EXTVLCOPT` (UA/referer). EPG: `x-tvg-url` başlığı veya elle URL |
| Stalker | MAC | El sıkışma + `get_all_channels`, `get_ordered_list` (vod/series). Protokol kırılgan; hata durumunda playlist "kısmi" işaretlenir |

**Ortak davranış:**
- Senkron artımlı ve iptal edilebilir; ilerleme `SyncProgress` ile yayınlanır.
- Tek seferde tek senkron (Mutex); kullanıcı yenile derse önceki iptal edilir.
- HTTP: 15 s bağlanma, 30 s okuma; 3 deneme, üstel geri çekilme; 401/403'te
  yeniden deneme yok, kullanıcıya "kimlik reddedildi".
- Büyük yanıtlar streaming ayrıştırılır (M3U satır satır, XML pull).
- Kaynak silinince bağlı kanal/EPG/kayıt kayıtları tek transaction'da temizlenir.

---

## 6. EPG

- XMLTV streaming parser; `YYYYMMDDhhmmss +ZZZZ` ve offsetsiz biçimler,
  `xmltv_ns` bölüm numaralandırması, ikon/kategori/reyting.
- Xtream: `xmltv.php` (gzip) ve gerekirse kanal başına `get_short_epg`.
- Pencere: varsayılan 3 gün, ayarlanabilir 1–7. Eski programlar günlük temizlenir.
- Senkron: açılışta (son senkron > 6 saat ise) + gecelik WorkManager.
- Izgara sanallaştırılmış: yalnızca görünen saat dilimi ve kanal aralığı compose edilir.
- Yakalama (catch-up): yalnızca `hasArchive` kanallarda; URL adaptörden gelir,
  oynatıcı normal akış gibi davranır (ileri/geri sarılabilir).
- Saat dilimi: cihaz saati; kaynak offset'i korunur.

---

## 7. Oynatıcı

- `PlayerManager`: tek `ExoPlayer` örneği, `MediaSessionService` (TV'de medya tuşları).
- `DataSource.Factory`: playlist başına User-Agent/Referer; HLS ve TS için ayrı
  `MediaSource` seçimi; `DefaultLoadControl` buffer'ı ayardan.
- Renderer: donanım önce, başarısızsa yazılım (kullanıcıya "donanım çözme kapalı" bildirimi).
- Tunneling: API 29+ TV'de açık; sorun çıkaran cihazda ayarla kapatılabilir.
- Ses: parça seçimi, passthrough (AC3/EAC3/DTS) desteği cihaz yeteneğine göre.
- Altyazı: canlıda gömülü parçalar; yerel dosyalarda (kayıt/VOD) yan dosya (`.srt`, `.vtt`)
  aynı dizinden; boyut/renk/konum ayarı.
- En-boy: fit / fill / zoom / 16:9 / 4:3 / orijinal.
- OSD: alt bilgi şeridi (kanal, şimdi/sıradaki, ilerleme), `INFO` ile açılır,
  4 s sonra söner; kanal listesi OSD içinden açılır.
- Hata: 3 ardışık hata → "Yayın açılamadı" + kaynak/format bilgisi + yeniden dene.
- PiP: API 26+; TV'de sistem PiP davranışı cihaza göre değişir, güvenli geri dönüş var.

---

## 8. Kayıt ve timeshift — risk bölümü

**Gerçek:** Media3 canlı yayını kaydetmez. Bunu "ExoPlayer ile kayıt" diye paketleyen
her tasarım yanlıştır. Çözüm ayrı bir akış kopyalayıcı:

| Akış | Yöntem | Çıktı |
|---|---|---|
| Ham MPEG-TS | OkHttp ile bayt kopyası (yeniden bağlanma + 206/range yok sayma) | `.ts` |
| HLS | m3u8 yoklama, segment sıralı indirme, birleştirme; `#EXT-X-DISCONTINUITY` korunur | `.ts` |
| DASH | v1 kapsam dışı (kaynak sağlayıcılarda nadir) | — |

- Servis: `RecordingService` (foreground, `dataSync`), her kayıt için bildirim.
- Planlama: `AlarmManager.setExactAndAllowWhileIdle` + `BOOT_COMPLETED` yeniden kurma;
  WorkManager yedek tetikleyici (alarm izni yoksa).
- Depolama: SAF ağaç URI (kullanıcı klasör seçer), kalıcı izin; yoksa uygulama
  dış depolaması. Boş alan kontrolü: süre × tahmini bit hızı + %20 marj.
- Çakışma: aynı anda 1 kayıt (ayarlanabilir 1–3); çakışmada kullanıcıya seçim.
- Seri kuralı: kanal + normalize başlık eşleşmesi; tekrar bölümler `episodeNum`
  ile ayıklanır; `keepEpisodes` aşılırsa en eski silinir.
- Kayıt sırasında oynatma: aynı anda canlı izleme ayrı bağlantı kullanır.
- Kurtarma: uygulama ölürse yarım dosya `status=INCOMPLETE` işaretlenir, oynatılabilir.
- Oynatma: kayıt `.ts` olarak ExoPlayer ile oynar; ileride isteğe bağlı MP4 remux (P8).

**Timeshift:** sağlayıcı arşivi varsa `catchupUrl`; yerel timeshift (duraklatılmış
canlı yayını kaydetme) P5'te arşivli kanallarda çalışır, arşivsiz kanallarda
yalnızca buffer içinde geri sarma (P2'de 60 s buffer).

---

## 9. Teslim fazları

Her faz sonunda **kurulabilir, çalışan APK** ve doğrulama listesi.

| Faz | İçerik | DoD (kabul) |
|---|---|---|
| P0 | Modüller, tema, nav, Room iskeleti, CI betiği, mock sunucu | Boş ekranlar arası gezinme, build yeşil, 3 birim testi |
| P1 | Kaynak adaptörleri, playlist ekleme/silme/yenileme, kanal listesi, gruplar, favoriler, arama | Xtream + M3U + Stalker eklenir; 20k kanal kaydırılır; favori kalıcı |
| P2 | Oynatıcı, OSD, parça/en-boy, PiP, son kanal | HLS/TS oynar; ses/altyazı seçilir; D-pad ile tüm OSD |
| P3 | EPG senkron, ızgara, detay, hatırlatıcı, catch-up | 3 günlük ızgara; program detayı; arşiv oynar |
| P4 | Film/dizi kütüphanesi, sezon/bölüm, ilerleme | Poster ızgarası; dizi bölümü açılır; devam et çalışır |
| P5 | Kayıt servisi, planlama, seri kuralı, liste | Planlı kayıt dosyaya yazılır ve oynar; çakışma sorulur |
| P6 | Çoklu ekran 2/4 | 4 kutu aynı anda oynar; ses odağı değişir |
| P7 | Profiller, PIN, ebeveyn, birleşik arama | Profil geçişi favorileri ayırır; kilitli kanal PIN ister |
| P8 | Görünüm özelleştirme, yedekleme, uyku, AFR, imzalı APK | Tema değişir; yedek geri yüklenir; imzalı APK kurulur |

**Tahmini ağırlık:** P0–P2 toplam işin ~%45'i; kayıt (P5) tek başına ~%20'si.

---

## 10. Test stratejisi

| Katman | Yöntem |
|---|---|
| Ayrıştırıcılar | Altın fixture'lar: M3U (eski/yeni), Xtream JSON dökümleri, XMLTV 3 varyant, bozuk satırlar |
| URL üretimi | Catch-up/stream URL tablo testleri |
| Veritabanı | `MigrationTestHelper`, DAO, FTS Türkçe katlama |
| Kayıt | Sahte HTTP sunucusu; TS kopyası ve HLS segment birleştirme bayt karşılaştırması |
| Oynatıcı | Media3 test-utils + yerel HLS fixture; durum ve parça listesi doğrulaması |
| UI | Compose TV odak testleri: odak sırası, geri davranışı, D-pad ile erişilebilirlik |
| Performans | Macrobenchmark: açılış, liste kaydırma, EPG ızgara |
| Elle | Fire TV Stick 4K, Android TV emülatörü (1080p), NVIDIA Shield (varsa) |

**Geliştirme içeriği:** `~/nova-tv/tools/mock-server` — Xtream API + M3U + XMLTV +
HLS fixture sunar; gerçek sağlayıcı kimliği olmadan uçtan uca test. Ayrıca
`~/inattv_work`, `~/rectv_work` altındaki mevcut M3U/JSON çıktıları gerçek veri
olarak kullanılabilir (kimlik bilgileri koda girmez, yalnızca yerel test).

---

## 11. Güvenlik

- Sağlayıcı şifreleri `EncryptedSharedPreferences` (Keystore) içinde; Room'da düz metin yok.
- Stream URL'leri loglanmaz (URL'de kullanıcı/şifre taşıyan Xtream biçimi yaygın).
- Ağ: yalnızca HTTPS tercih; HTTP'ye izin verilir (IPTV gerçeği) ama kullanıcı uyarılır.
- Yedek dosyası varsayılan olarak şifresiz uyarısı verir; isteğe bağlı AES.
- Dışa açılan servis yok; `exported=false`, tek `MainActivity` (leanback intent).
- Yetkiler: `INTERNET`, `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`, `RECORD_AUDIO` yok,
  `READ_MEDIA_*` yok (SAF).

---

## 12. Riskler

| Risk | Etki | Azaltma |
|---|---|---|
| Kayıt akışı sağlayıcıya göre bozuk (redirect, token süresi) | Yüksek | P5 başında 3 farklı sağlayıcıyla saha testi; yeniden bağlanma + segment kurtarma |
| Stalker protokolü kırılgan | Orta | Adaptör izole; hata "kısmi playlist" olarak görünür, diğer kaynaklar etkilenmez |
| Fire TV Stick'te bellek/CPU | Orta | Paging + sanal ızgara + görsel önbellek; Macrobenchmark eşiği |
| HLS canlı kenarı / düşük gecikme | Orta | `DefaultLoadControl` ve `liveConfiguration` ayarı; kullanıcıya buffer seçeneği |
| Türkçe arama bozulması | Düşük | Normalize FTS kolonu + test |
| Sideload imza/anahtar kaybı | Düşük | Anahtar `~/.android-keys/novatv.jks` dışında, yedekli; repoya girmez |

---

## 13. Karar kayıtları

1. **Native Kotlin + Compose for TV** — TiviMate paritesi için tek gerçekçi zemin.
2. **Sunucusuz, yerel-öncelikli** — gecikme ve çevrimdışı; hesap/bulut yok.
3. **Çok modüllü Gradle** — 7 özellik alanı tek modülde çürür.
4. **Üç kaynak adaptörü tek arayüzde** — üst katman kaynağı bilmez.
5. **Kayıt = ayrı akış kopyalayıcı** — Media3 yanılsamasına girilmez.
6. **Sideload APK** — mağaza süreci v1'e yük değil.
7. **TiviMate adı/markası kullanılmaz** — uygulama adı NovaTV.

---

## 14. Açık uçlar

Yok. Uygulama sırasında ortaya çıkan her karar bu dosyaya işlenir.
