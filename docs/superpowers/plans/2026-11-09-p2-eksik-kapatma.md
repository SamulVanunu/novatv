# P2 Eksik Kapatma Planı

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** P0–P2 kapsamındaki yarım kalan özellikleri tamamlamak: akış başlıkları (UA/Referer), M3U kaynak URL'inin şifreli saklanması, son izlenenler, senkron hata nedeni, PiP, harici altyazı, donanım çözme bildirimi.

**Architecture:** Kaynak başlıkları kanal modeline ve oynatıcıya taşınır; `StreamResolver` artık URL + başlık döndürür. M3U kaynak URL'i Room'da redakte edilip Keystore'lu depoda tutulur. Son izlenenler mevcut `watch_history` tablosundan sayfalanır. Oynatıcı PiP ve harici altyazı ile genişler.

**Tech Stack:** Kotlin, Compose for TV, Media3 1.5.1, Room 2.7.2, Paging 3, WorkManager, EncryptedSharedPreferences.

**Spec:** `docs/superpowers/specs/2026-11-07-novatv-tvimate-parity-design.md`

## Global Constraints

- Java 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`), testler `user.language=en`, `user.country=US`.
- Kimlik bilgisi, token, gerçek kaynak URL'i koda/teste/git'e girmez.
- D-pad ile her ekran erişilebilir kalır; mevcut `testTag`'ler silinmez.
- HTTP kaynaklara izin verilir; başlıklar yalnızca akış isteğine eklenir.
- Room şema sürümü 2; migration testi zorunlu.

## Review Focus

- `#EXTVLCOPT` başlığı olan listede kanal açılmıyorsa başlık taşınmıyor demektir.
- Şifre içeren M3U linki kaydedilip yeniden açıldığında adres eksiksiz dönmeli; listede kırpılmış görünmeli.
- `watch_history` boşken "Son izlenenler" rayı görünmemeli; doluysa en yeni üstte olmalı.
- Senkron hatası mesajı URL/şifre sızdırmamalı.
- PiP'te OSD görünmemeli; PiP'ten dönünce oynatma sürmeli.

---

### Task 1: Şema v2 — başlık ve hata alanları

**Files:** `core/model/{Channel,Playlist}.kt`, `core/database/entity/{ChannelEntity,PlaylistEntity}.kt`, `core/database/mapper/Mappers.kt`, `core/database/NovaDatabase.kt`, `core/database/migrations/Migrations.kt` (yeni), `core/database/build.gradle.kts`, `core/database/src/androidTest/.../MigrationTest.kt` (yeni).

**Interfaces:**
- `Channel.streamUserAgent: String? = null`, `Channel.streamReferer: String? = null`
- `Playlist.referer: String? = null`, `Playlist.lastError: String? = null`
- `MIGRATION_1_2: Migration` — dört `ALTER TABLE ... ADD COLUMN`.

- [ ] Migration testi yaz (v1 → v2, kolonlar null).
- [ ] Kırmızıyı doğrula.
- [ ] Migration + entity + mapper + `Room.databaseBuilder(...).addMigrations(MIGRATION_1_2)`.
- [ ] `./gradlew :core:database:test :core:database:connectedDebugAndroidTest` yeşil.

### Task 2: Akış başlıkları uçtan uca

**Files:** `core/source-m3u/M3uParser.kt`, `core/sync/StreamResolver.kt`, `feature/player/PlayerViewModel.kt`, `app/NovaNavHost.kt`, form ekranı/viewmodel, adaptörler (`M3u/Xtream/Stalker`).

**Interfaces:**
- `data class ResolvedStream(val url: String, val headers: Map<String, String> = emptyMap())`
- `StreamResolver.resolve(channel, format): ResolvedStream` — başlık önceliği: kanal UA/Referer > playlist UA/Referer.
- `ChannelStreamResolver.resolve(channel): ResolvedStream`; `PlaybackController.prepare(url, headers)`.
- M3U `PendingEntry.toChannel(playlistId, url, userAgent, referrer)`.

- [ ] `M3uParserTest`: `#EXTVLCOPT` başlıkları kanala yazılıyor.
- [ ] `StreamResolverTest`: kanal başlığı playlist başlığını eziyor.
- [ ] Uygula; `:core:source-m3u:test :core:sync:test :feature:player:test` yeşil.
- [ ] Forma "Referer (isteğe bağlı)" alanı; adaptör fetch başlıklarına `Referer`.

### Task 3: M3U kaynak URL'i şifreli

**Files:** `core/settings/{CredentialStore,EncryptedCredentialStore}.kt`, `core/model/Urls.kt` (yeni), `core/sync/PlaylistRepository.kt`, form.

**Interfaces:**
- `CredentialStore.putSourceUrl(id, url)`, `getSourceUrl(id)`, `removeSourceUrl(id)`.
- `fun redactUrl(url: String): String` — query ve userinfo atılır, `scheme://host[:port]/path` kalır.
- `RoomPlaylistRepository`: M3U add/update → URL secret'a, Room'a redakte; `load` → secret varsa tam URL; `remove` → secret silinir.

- [ ] `redactUrl` testi; repository testi (kaydet → listede kırpık, `load` → tam).
- [ ] Uygula; testler yeşil.

### Task 4: Son izlenenler

**Files:** `ChannelDao`, `ChannelRepository`, `LiveViewModel` (sabit), `LiveScreen` (boş durum metni).

**Interfaces:**
- `ChannelRepository.RECENT_KEY = "__recent"`; `groups()` `Tümü`den sonra `GroupItem(RECENT_KEY, "Son izlenenler", n)` (n>0).
- `ChannelDao.recentPaging(playlistIds, q, favoritesOnly, profileId)` — `watch_history.updatedAt DESC`, gizli kanal/grup filtreleri aynı.

- [ ] DAO testi: iki kanal izlenir, en yeni önce; gizli kanal düşer.
- [ ] `channels(filter)` `RECENT_KEY` için `recentPaging` kullanır.
- [ ] Rail'de seçilince başlık "Son izlenenler" olur.

### Task 5: Senkron hata nedeni

**Files:** `SyncEngine`, `PlaylistDao`, `PlaylistsScreen`.

**Interfaces:**
- `finish(...)` `lastError` yazar; başarıda `null`.
- `fun publicSyncMessage(t: Throwable): String?` — `https?://\S+`, `username=…`, `password=…` temizlenir, 160 karakter.

- [ ] `SyncEngineTest`: 401 → `syncState=FAILED`, `lastError` dolu ve URL içermiyor.
- [ ] Kartta `lastError` danger renkte, en fazla 2 satır.

### Task 6: Oynatıcı ve küçük düzeltmeler

**Files:** `PlayerScreen`, `PlayerOsd`, `PlayerViewModel`, `MainActivity`, `feature/player/Pip.kt` (yeni), `ChannelRow`.

- [ ] PiP: `PlayerPip.enter` + `MainActivity.onUserLeaveHint`; API 31+ `setAutoEnterEnabled(true)`; PiP'te OSD kapanır.
- [ ] Arşiv rozeti kaldırılır (P3'e kadar).
- [ ] Donanım→yazılım bildirimi OSD üstünde 6 sn görünür.
- [ ] `−60 sn` düğmesi (`canSeekBack` iken).
- [ ] Emülatörde HOME → PiP penceresi; dönüşte oynatma sürüyor.

### Task 7: Harici altyazı

**Files:** `PlaybackController`, `PlayerViewModel`, `PlayerOsd`, `PlayerScreen`, app rotası.

**Interfaces:**
- `data class SubtitleSpec(val uri: Uri, val mimeType: String, val language: String = "und")`
- `prepare(url, headers, startPositionMs, subtitle: SubtitleSpec? = null)`; `MediaItem.Builder().setSubtitleConfigurations(...)`.
- `PlayerViewModel.setExternalSubtitle(spec)` — akışı yeniden hazırlar.
- OSD altyazı menüsünde "Dosyadan seç…" → `ActivityResultContracts.OpenDocument`.

- [ ] `mediaItemFor` testi: altyazı yapılandırması ekleniyor.
- [ ] Emülatörde SRT seçilip altyazı parçası listelenir.

### Task 8: Doğrulama

- [ ] `./gradlew test`, `:app:connectedDebugAndroidTest`, `:core:player:connectedDebugAndroidTest -PhlsUrl=...`.
- [ ] Gerçek M3U kaynağı (kullanıcının verdiği liste) ile: kaynak ekle → senkron → kanal aç → oynat.
- [ ] `:app:assembleRelease` imzalı.
