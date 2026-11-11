# NovaTV Mobil (telefon + tablet) Uygulama Planı

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aynı ürünü ikinci bir uygulama (`dev.anon.novatv.mobile`) olarak telefon ve tablete taşımak; TV uygulaması görünür olarak değişmeden.

**Architecture:** Ortak grafik/servis/rota kodu yeni `:app-common` kitaplığına taşınır; `:app` (TV) ve yeni `:mobile` onu kullanır. `:ui` modülüne kısa kenara bakan `NovaWindow` + `NovaScaffold` eklenir; TV her zaman `Expanded` olduğu için TV düzeni korunur. Her ekran kendi compact düzenini aynı Veri/ViewModel üzerinde çizer.

**Tech Stack:** Kotlin 2.x, Jetpack Compose for TV + Compose Material3, Navigation Compose, Room, Media3, Gradle çok modüllü.

**Spec:** `docs/superpowers/specs/2026-11-11-novatv-mobil-design.md`

## Global Constraints

- Paketler: TV `dev.anon.novatv`, mobil `dev.anon.novatv.mobile`; ikisi aynı cihazda yan yana kurulabilir.
- `minSdk 23`, `targetSdk 35`, `compileSdk 36`, Java/JVM 21, `versionName` tek kaynaktan (`0.1.0`).
- `feature → feature` bağımlılığı yasak; `:app-common` yalnızca core/feature/ui'ya bağlanır.
- Dokunma hedefi ≥ 48 dp; kenar boşlukları 16/24/32 dp tokenları.
- Mevcut `testTag`'ler silinmez: `open-playlists`, `settings-afr`, `settings-autostart`, `multiwall-2`, `multiwall-4`, `player-root`, `playback-error`, `playback-retry`, `error-channels`, `player-notice`, `player-zap`.
- Kimlik değişmez: mürekkep `#0C0C0D`, krem `#F6F1E7`, kırmızı `#E8322A`; odak krem dolgu + mürekkep yazı.
- Test kaynağı (`vavoo…`) ve kimlik bilgileri hiçbir dosyaya yazılmaz; akış URL'i Room'a yazılmaz.
- Her görev sonunda `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew … --max-workers=2` ile derleme yeşil olmalı.

## Review Focus

- Ayrı `applicationId` yüzünden mobil boş açılır: kaynak yokken kullanıcı ne yapacağını görmeli (yedek yönlendirmesi + Kaynak ekle), boş ekranla kalmamalı.
- Dokunuşla gelen olaylar odak dili tetiklememeli (krem dolgu yalnızca D-pad/klavye odağında), aksi hâlde telefonda her dokunuş "seçili" gibi görünür.
- Oynatıcı yatay kilidi çıkışta bırakılmazsa uygulamanın kalanı yatay kalır; `DisposableEffect` her iki yolda da (geri, hata, PiP) geri almalı.
- Aynı kanalın iki kez seçilmesi akışı yeniden yüklememeli (kanal listesinde geçerli kanala dokunma).
- Telefonda geri tuşu: alt sekmeden çıkışta uygulamadan çıkmalı, oynatıcıdan Canlı'ya dönmeli (yığın şişmemeli).

---

### Task 1: `:app-common` kitaplığını çıkar

**Files:**
- Create: `app-common/build.gradle.kts`, `app-common/src/main/AndroidManifest.xml`
- Move: `app/src/main/java/dev/anon/novatv/app/{NovaGraph,NovaApplication,BackupManager,ParentalGuard,RecordingService,AutoStartReceiver}.kt` → `app-common/src/main/java/dev/anon/novatv/app/common/`
- Create: `app-common/src/main/java/dev/anon/novatv/app/common/CommonRoutes.kt`
- Modify: `settings.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/dev/anon/novatv/app/{MainActivity,NovaNavHost}.kt`
- Move (manifest): `<service>`/`<receiver>`/`<provider>` ve izinler

**Interfaces:**
- Produces (paket `dev.anon.novatv.app.common`): `NovaGraph(context: Context)`, `NovaApplication` (`val graph: NovaGraph`), `Routes.LIVE`, `@Composable fun PlayerRouteContent(controller, viewModel, sleepMinutes, autoFrameRate, onBack, touch: Boolean = false)`, `subtitleMimeType(context, uri)`, `hasDocumentPicker(context)`.
- Consumes: mevcut core/feature API'leri değişmez.

- [ ] **Step 1:** `app-common/build.gradle.kts` yaz: `com.android.library`, `namespace = "dev.anon.novatv.app.common"`, `compileSdk 36`, `minSdk 23`, Compose açık, JVM 21. Bağımlılıklar `app/build.gradle.kts`'nin `implementation(project(...))` listesiyle aynı + `androidx.navigation.compose`, `androidx.work.runtime.ktx`, `androidx.datastore.preferences`.
- [ ] **Step 2:** Dosyaları `git mv` ile taşı, `package` satırlarını `dev.anon.novatv.app.common` yap; sınıf gövdelerinde değişiklik yok.
- [ ] **Step 3:** `NovaNavHost.kt`'den `Routes`, `subtitleMimeType`, `hasDocumentPicker` ve `PlayerRouteContent` bloğunu `CommonRoutes.kt`'ye taşı; `:app` tarafında `import dev.anon.novatv.app.common.*` ekle.
- [ ] **Step 4:** Manifest böl: izinler + `<service>/<receiver>/<provider>` `app-common/src/main/AndroidManifest.xml`'e, `android:name` tam paket adıyla (`.RecordingService` → `dev.anon.novatv.app.common.RecordingService`). `:app` manifestinde yalnız `application` (`.NovaApplication`, ikon, banner, tema) ve `MainActivity` kalır.
- [ ] **Step 5:** `settings.gradle.kts`'e `":app-common"` ekle; `app/build.gradle.kts`'e `implementation(project(":app-common"))`.
- [ ] **Step 6:** Doğrula: `./gradlew :app:assembleDebug :app:testDebugUnitTest` → BUILD SUCCESSFUL, testler aynı sayıda yeşil.
- [ ] **Step 7:** Cihaz: `adb install -r app/build/outputs/apk/release/app-release.apk` sonrası kanal listesi, kayıt başlat/durdur, yedek al ekranı açılıyor mu (kayıt servisi manifest birleşmesi çalışıyor mu). Hata varsa manifest birleşmesini düzelt.
- [ ] **Step 8:** Commit: `refactor: ortak uygulama kodunu :app-common'a taşı`

---

### Task 2: `NovaWindow` ve ölçü tokenları

**Files:**
- Create: `ui/src/main/java/dev/anon/novatv/ui/layout/NovaWindow.kt`
- Create: `ui/src/main/java/dev/anon/novatv/ui/layout/NovaSpacing.kt`
- Test: `ui/src/test/kotlin/dev/anon/novatv/ui/layout/NovaWindowTest.kt`
- Modify: `ui/build.gradle.kts` (JVM test bağımlılığı)

**Interfaces:**
- Produces: `enum class NovaWindow { Compact, Medium, Expanded }`, `fun windowClass(widthDp: Int): NovaWindow`, `fun libraryColumns(window: NovaWindow): Int`, `object NovaSpacing { val screen: Dp; val gap: Dp }` (compact 16/8, medium 24/12, expanded 32/16), `@Composable fun rememberNovaWindow(): NovaWindow`.

- [ ] **Step 1:** Testi yaz:

```kotlin
@Test fun `599 dp compact`() { assertEquals(NovaWindow.Compact, windowClass(599)) }
@Test fun `600 dp medium`() { assertEquals(NovaWindow.Medium, windowClass(600)) }
@Test fun `839 dp medium`() { assertEquals(NovaWindow.Medium, windowClass(839)) }
@Test fun `840 dp expanded`() { assertEquals(NovaWindow.Expanded, windowClass(840)) }
@Test fun `columns 2-3-5`() {
    assertEquals(2, libraryColumns(NovaWindow.Compact))
    assertEquals(3, libraryColumns(NovaWindow.Medium))
    assertEquals(5, libraryColumns(NovaWindow.Expanded))
}
```
- [ ] **Step 2:** `./gradlew :ui:testDebugUnitTest` → derleme hatası (fonksiyon yok). `ui` modülünde `testImplementation(libs.junit)` ekle.
- [ ] **Step 3:** `NovaWindow.kt` + `NovaSpacing.kt` yaz; `rememberNovaWindow` `LocalConfiguration.current.screenWidthDp` okur.
- [ ] **Step 4:** `./gradlew :ui:testDebugUnitTest` → PASS.
- [ ] **Step 5:** Commit: `feat(ui): pencere sınıfı ve ölçü tokenları`

---

### Task 3: `NovaScaffold` — yan ray / alt gezinme

**Files:**
- Create: `ui/src/main/java/dev/anon/novatv/ui/layout/NovaScaffold.kt`
- Test: `ui/src/test/kotlin/dev/anon/novatv/ui/layout/NovaScaffoldTest.kt`

**Interfaces:**
- Produces: `data class NovaTab(val key: String, val label: String, val testTag: String)`, `fun navigationMode(window: NovaWindow): NovaNavMode` (`Rail` / `Bottom`; `Rail` yalnız `Expanded`), `@Composable fun NovaScaffold(window, tabs, selected, onSelect, content: @Composable (PaddingValues) -> Unit)`.
- Consumes: Task 2 tipleri.

- [ ] **Step 1:** Test: `assertEquals(NovaNavMode.Rail, navigationMode(NovaWindow.Expanded))`, `Bottom` for Compact/Medium.
- [ ] **Step 2:** `./gradlew :ui:testDebugUnitTest` → FAIL (tip yok).
- [ ] **Step 3:** `NovaScaffold` yaz: `Bottom` modda `NavigationBar` benzeri 5 eşit dokunma hedefi (≥56 dp), `Rail` modda mevcut `GroupRail` görünümüne yakın ince ray. Seçili sekme krem yazı + 3 dp kırmızı gösterge; odak halkası yalnız D-pad.
- [ ] **Step 4:** `./gradlew :ui:testDebugUnitTest` → PASS.
- [ ] **Step 5:** Commit: `feat(ui): uyarlanabilir iskelet (ray/alt çubuk)`

---

### Task 4: `:mobile` iskeleti

**Files:**
- Create: `mobile/build.gradle.kts`, `mobile/src/main/AndroidManifest.xml`, `mobile/src/main/java/dev/anon/novatv/mobile/{MobileApplication,MobileMainActivity,MobileNavHost}.kt`, `mobile/src/main/res/values/strings.xml`, `mobile/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (paylaşılan drawable'lar `:app-common` üzerinden)
- Modify: `settings.gradle.kts`, `app-common/src/main/res/values/strings.xml` (uygulama adı kaynağı)

**Interfaces:**
- Produces: `@Composable fun MobileNavHost(graph: NovaGraph, window: NovaWindow)`; rotalar `Routes.LIVE`, `Routes.GUIDE` … `:app-common`'a taşınmış rota sabitleri.
- Consumes: Task 1 (`NovaGraph`, `PlayerRouteContent`), Task 3 (`NovaScaffold`).

- [ ] **Step 1:** `mobile/build.gradle.kts`: `com.android.application`, `applicationId = "dev.anon.novatv.mobile"`, `versionCode/versionName` `:app` ile aynı, imza bloğu `:app`'ten kopyalanır (aynı `keystore.properties`), `implementation(project(":app-common"))` + feature/ui bağımlılıkları.
- [ ] **Step 2:** Manifest: `.MobileApplication`, `.MobileMainActivity` (`exported`, `configChanges` aynı, `LEANBACK_LAUNCHER` **yok**), etiket `@string/mobile_app_name` = "NovaTV Mobil", adaptive ikon.
- [ ] **Step 3:** `MobileMainActivity` `NovaTheme` + `MobileNavHost`; `MobileApplication` `NovaApplication`'ı genişletir (grafik paylaşılır).
- [ ] **Step 4:** `MobileNavHost` iskeleti: `NovaScaffold` + 5 sekme (Canlı, Rehber, Kütüphane, Kayıtlar, Daha fazla); yalnız Canlı ve Ayarlar bağlanır, diğerleri geçici boş sayfa; oynatıcı rotası `PlayerRouteContent` ile.
- [ ] **Step 5:** `./gradlew :mobile:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 6:** `vakit-tel` AVD'yi başlat, kur, aç: alt çubuk görünür, Canlı listesi açılır; TV APK'sı hâlâ çalışıyor (`tv36`).
- [ ] **Step 7:** Commit: `feat(mobil): ayrı mobil uygulama iskeleti`

---

### Task 4b: Dokunmatik etkileşim temeli (plan dışı, ruling F15)

Gerçek: tv-material'ın tıklanabilir yüzeyi (`tvClickable`) yalnız D-pad/ENTER
işliyor, işaretçi (dokunma) olayı yok. Bu yüzden telefonda sekme çubuğu ve
`NovaPill` düğmeleri dokunuşa ölü. Task 4'te `:mobile` içine geçici bir
koordinat yakalayıcı (`touchTabBar`) kondu; kalıcı çözüm ortak ilkel.

**Files:**
- Create: `ui/src/main/java/dev/anon/novatv/ui/component/NovaTapSurface.kt`
- Modify: `ui/src/main/java/dev/anon/novatv/ui/component/NovaComponents.kt` (`NovaPill` içi)
- Modify: `ui/src/main/java/dev/anon/novatv/ui/layout/NovaScaffold.kt` (sekme yüzeyi)
- Modify: `mobile/src/main/java/dev/anon/novatv/mobile/MobileNavHost.kt` (`touchTabBar` kaldır)

**Interfaces:**
- Produces: `@Composable fun NovaTapSurface(onClick, modifier, shape, containerColor, contentColor, border, enabled, scaleOnPress, interactionSource, content)` — hem işaretçi `clickable` hem D-pad `focusable`/ENTER; odak dolgusu ve ölçek `interactionSource`'tan.
- Tüketir: `NovaColors.Focus/FocusInk`.

- [ ] **Step 1:** `NovaTapSurface` yaz: `Modifier.clickable(interactionSource, indication = null, enabled, onClick)` + odak ölçeği/dolgusu; ripple yok (kimlik dili).
- [ ] **Step 2:** `NovaPill` gövdesini `NovaTapSurface`'a geçir; genel API ve görünüm aynı kalır.
- [ ] **Step 3:** `NovaScaffold` sekme yüzeyini `NovaTapSurface`'a geçir; `:mobile`'daki `touchTabBar` ve kopya 64 dp sabitini **sil**.
- [ ] **Step 4:** `./gradlew :ui:testDebugUnitTest :app:assembleDebug :mobile:assembleDebug` → yeşil.
- [ ] **Step 5:** TV regresyonu (`tv36`): odak halkası/dolgusu aynı, D-pad ENTER çalışıyor, ripple yok, kanal listesi ve ayarlar açılıyor.
- [ ] **Step 6:** Telefon (`nova-tel`): dokunuşla sekme değişimi, Ayarlar satırı/pill düğmesi, oynatıcı OSD düğmesi.
- [ ] **Step 7:** Commit: `feat(ui): dokunma ve D-pad'i birlikte işleyen ortak yüzey`

---

### Task 5: Canlı ekranı compact düzeni

**Files:**
- Modify: `feature/live/src/main/kotlin/dev/anon/novatv/feature/live/LiveScreen.kt`
- Create: `feature/live/src/main/kotlin/dev/anon/novatv/feature/live/CompactLive.kt`
- Modify: `app/src/main/java/dev/anon/novatv/app/NovaNavHost.kt` (TV çağrısı `window = NovaWindow.Expanded`)

**Interfaces:**
- Produces: `@Composable fun LiveScreen(..., window: NovaWindow = NovaWindow.Expanded)`; compact yol `CompactLive(state, channels, callbacks…)`.
- Consumes: Task 2/3 tokenları; `GroupRail` API'si değişmez.

- [ ] **Step 1:** `LiveScreen` imzasına `window` ekle; `window == Compact || Medium` iken `CompactLive`e devret, aksi hâlde mevcut gövde.
- [ ] **Step 2:** `CompactLive`: yatay kaydırılan grup çipleri (`NovaPill`, seçili krem dolgu), 56 dp satırlar (`ChannelRow` yerine dokunmatik satır: logo 44 dp, ad, grup, yıldız), ızgara düğmesi yerine 2 sütunlu kart görünümü.
- [ ] **Step 3:** `./gradlew :feature:live:testDebugUnitTest :app:assembleDebug` → yeşil.
- [ ] **Step 4:** Cihaz: `vakit-tel` — grup çipi değişimi, kanala dokun → oynatıcı, yıldız dokunuşu; `tv36` — TV düzeni değişmemiş (ekran görüntüsü karşılaştır).
- [ ] **Step 5:** Commit: `feat(canlı): telefonda çipli ve dokunmatik liste`

---

### Task 6: Rehber compact düzeni (şimdi / sıradaki)

**Files:**
- Modify: `feature/epg/src/main/kotlin/dev/anon/novatv/feature/epg/EpgScreen.kt`
- Create: `feature/epg/src/main/kotlin/dev/anon/novatv/feature/epg/CompactEpg.kt`
- Test: `feature/epg/src/test/kotlin/dev/anon/novatv/feature/epg/CompactEpgTest.kt`

**Interfaces:**
- Produces: `@Composable fun EpgScreen(..., window: NovaWindow = NovaWindow.Expanded)`; `internal fun nowNextOf(row: GuideRow, now: Long): Pair<EpgProgram?, EpgProgram?>`.
- Consumes: `GuideRow(channel, programs)` (core/sync), `EpgProgram`.

- [ ] **Step 1:** Test: şimdiki program (`startAt <= now < endAt`) ve sıradaki (`startAt > now`) seçimi; program yoksa `(null, null)`; geçmiş programlar sıradaki sayılmaz.
- [ ] **Step 2:** `./gradlew :feature:epg:testDebugUnitTest` → FAIL.
- [ ] **Step 3:** `nowNextOf` yaz, testi geçir.
- [ ] **Step 4:** `CompactEpg`: gün sekmeleri, kanal satırı = logo + ad + "ŞİMDİ 19:00 · program" + "SIRADAKİ 20:00 · program"; dokununca tam ekran program sayfası (mevcut `selected` detay paneliyle aynı eylemler: izle, arşiv, hatırlatıcı, kaydet).
- [ ] **Step 5:** `EpgScreen` içinde `window != Expanded` iken `CompactEpg`; mevcut ızgara yolu değişmez.
- [ ] **Step 6:** `./gradlew :feature:epg:testDebugUnitTest :app:assembleDebug` → PASS; `tv36` rehberi değişmemiş.
- [ ] **Step 7:** Commit: `feat(rehber): telefonda şimdi/sıradaki listesi`

---

### Task 7: Kütüphane ve arama compact düzeni

**Files:**
- Modify: `feature/library/src/main/kotlin/dev/anon/novatv/feature/library/{LibraryScreen,SeriesDetailScreen}.kt`
- Create: `feature/library/src/main/kotlin/dev/anon/novatv/feature/library/CompactLibrary.kt`
- Test: `ui/src/test/.../NovaWindowTest.kt` (sütun testi Task 2'de)

**Interfaces:**
- Produces: `@Composable fun LibraryScreen(..., window: NovaWindow = NovaWindow.Expanded)`, `SeriesDetailScreen(..., window: NovaWindow = NovaWindow.Expanded)`.

- [ ] **Step 1:** `LibraryScreen` sütun sayısını `libraryColumns(window)`'dan al; kart genişliği sabit `width` yerine `GridCells.Fixed`.
- [ ] **Step 2:** Compact: filtreler tek satır yatay kaydırma; film detayı tam sayfa; dizi detayında sezon bölümleri tek sütun.
- [ ] **Step 3:** Arama sonuçları (varsa ortak liste) aynı sütun kuralını kullanır; klavye açılınca liste küçülür (`imePadding`).
- [ ] **Step 4:** `./gradlew :feature:library:testDebugUnitTest :app:assembleDebug` → yeşil.
- [ ] **Step 5:** Cihaz: `vakit-tel` film/dizi aç, geri dön; `tv36` kütüphane değişmemiş.
- [ ] **Step 6:** Commit: `feat(kütüphane): telefonda sütunlu ızgara ve tam sayfa detay`

---

### Task 8: Kayıtlar, çoklu ekran, kaynaklar, ayarlar compact

**Files:**
- Modify: `feature/recordings/src/main/kotlin/dev/anon/novatv/feature/recordings/RecordingsScreen.kt`
- Modify: `feature/multiwall/src/main/kotlin/dev/anon/novatv/feature/multiwall/MultiWallScreen.kt`
- Modify: `feature/playlists/src/main/kotlin/dev/anon/novatv/feature/playlists/PlaylistsScreen.kt`
- Modify: `feature/settings/src/main/kotlin/dev/anon/novatv/feature/settings/SettingsScreen.kt`

**Interfaces:**
- Produces: her ekrana `window: NovaWindow = NovaWindow.Expanded` parametresi; `MultiWallScreen` compact'ta 2 kutu dikey, 4 seçilirse `NovaBadge("Telefonda küçük", Warning)`.

- [ ] **Step 1:** Dört ekranda sabit yatay paddingleri `NovaSpacing` tokenına çevir; içerik `widthIn(max = 720.dp)` ile ortala.
- [ ] **Step 2:** `MultiWallScreen`: `window != Expanded` iken 2 pane dikey; `multiwall-2`/`multiwall-4` testTag'leri korunur.
- [ ] **Step 3:** Kaynaklar: satırlar 64 dp, düzenle/sil düğmeleri dokunmada görünür (odakta değil).
- [ ] **Step 4:** `./gradlew :feature:recordings:testDebugUnitTest :feature:settings:testDebugUnitTest :app:assembleDebug` → yeşil.
- [ ] **Step 5:** Cihaz: telefonda dört ekranı gez; `tv36` çoklu ekran 4'lü ve ayarlar değişmemiş.
- [ ] **Step 6:** Commit: `feat(mobil): kalan ekranlarda compact düzen`

---

### Task 9: Dokunmatik oynatıcı

**Files:**
- Create: `feature/player/src/main/kotlin/dev/anon/novatv/feature/player/TouchPlayerScreen.kt`
- Create: `feature/player/src/main/kotlin/dev/anon/novatv/feature/player/ChannelPickerSheet.kt`
- Modify: `feature/player/src/main/kotlin/dev/anon/novatv/feature/player/PlayerViewModel.kt`
- Modify: `feature/player/src/main/kotlin/dev/anon/novatv/feature/player/PlayerScreen.kt` (`buildPlayerView`, `PlaybackError`, `ChannelBanner` → `internal`)
- Test: `feature/player/src/test/kotlin/dev/anon/novatv/feature/player/ChannelOrderTest.kt`

**Interfaces:**
- Produces: `fun PlayerViewModel.selectChannel(channel: Channel)` (aynı kanalda no-op), `val channelOrder: StateFlow<List<Channel>>`, `@Composable fun TouchPlayerScreen(state, controller, channelOrder, onSelectChannel, …)`.
- Consumes: `channelStep`, `zappingOrder()`, `PlayerUiState.zap`.

- [ ] **Step 1:** Test: aynı kanal seçimi yeniden yüklemez (`selectChannel(current)` → `prepare` çağrısı yok), farklı kanal `prepare` çağırır ve `catchup` temizlenir (fake controller ile).
- [ ] **Step 2:** `./gradlew :feature:player:testDebugUnitTest` → FAIL.
- [ ] **Step 3:** `selectChannel` + `channelOrder` yaz; `stepChannel` bunları kullanacak şekilde sadeleşir.
- [ ] **Step 4:** `TouchPlayerScreen`: video tam ekran, dokun → OSD (4 sn), alt panelde ‹ Kanal / Duraklat / Kanal ›, ikinci satır Ses · Altyazı · Oran · Tampon · Liste; VOD'da ilerleme çubuğu + 10 sn geri/ileri; kanal bandı üstte.
- [ ] **Step 5:** `ChannelPickerSheet`: `channelOrder` listesi, arama alanı, geçerli kanal işaretli; dokun → `onSelectChannel` + kapat.
- [ ] **Step 6:** `:mobile` NavHost oynatıcı rotasında `TouchPlayerScreen`; rota açıkken `activity.requestedOrientation = SCREEN_ORIENTATION_SENSOR_LANDSCAPE`, `onDispose`'ta `UNSPECIFIED`.
- [ ] **Step 7:** `./gradlew :feature:player:testDebugUnitTest :mobile:assembleDebug` → PASS.
- [ ] **Step 8:** Cihaz: telefonda OSD aç/kapa, kanal listesinden geçiş, yatay kilit ve çıkışta dikey dönüş; `tv36` D-pad ve CH+/- hâlâ çalışıyor.
- [ ] **Step 9:** Commit: `feat(oynatıcı): dokunmatik OSD ve kanal listesi`

---

### Task 10: İlk açılış yönlendirmesi ve "Daha fazla" menüsü

**Files:**
- Modify: `mobile/src/main/java/dev/anon/novatv/mobile/MobileNavHost.kt`
- Create: `mobile/src/main/java/dev/anon/novatv/mobile/MobileMoreScreen.kt`
- Test: `mobile/src/test/kotlin/dev/anon/novatv/mobile/StartDestinationTest.kt`

**Interfaces:**
- Produces: `internal fun startDestination(hasSources: Boolean, lastChannel: String?): String`; `MobileMoreScreen(onSearch, onMultiwall, onSources, onSettings, onBackup)`.

- [ ] **Step 1:** Test: kaynak yoksa `Routes.LIVE` + yönlendirme bayrağı; kaynak varsa son kanal; son kanal yoksa Canlı.
- [ ] **Step 2:** `./gradlew :mobile:testDebugUnitTest` → FAIL.
- [ ] **Step 3:** `startDestination` yaz, testi geçir; Canlı ekranında kaynak yoksa `NovaPanel` içinde "Kaynak ekle" + "TV yedeğini geri yükle" satırı.
- [ ] **Step 4:** `MobileMoreScreen`: Ara, Çoklu ekran, Kaynaklar, Ayarlar, Yedek al / Geri yükle (mevcut `BackupManager` akışı, `CreateDocument`/`OpenDocument`).
- [ ] **Step 5:** `./gradlew :mobile:testDebugUnitTest :mobile:assembleDebug` → PASS.
- [ ] **Step 6:** Cihaz: kaynaksız mobil açılış yönlendirmesi; yedek al → geri yükle ile TV verisini taşıma (mock kaynakla).
- [ ] **Step 7:** Commit: `feat(mobil): boş açılış yönlendirmesi ve daha fazla menüsü`

---

### Task 11: Cihaz turu, iki APK

**Files:**
- Modify: `.memory/{CURRENT,HISTORY,DECISIONS,SUMMARY}.md`
- Create: `~/Downloads/novatv/NovaTV-Mobil-0.1.0-release.apk` (çıktı kopyası)

- [ ] **Step 1:** `./gradlew test --max-workers=2` → tüm birim testleri yeşil; sayıyı kaydet.
- [ ] **Step 2:** `./gradlew :app:assembleRelease :mobile:assembleRelease` → iki imzalı APK.
- [ ] **Step 3:** `vakit-tel` (411 dp) turu: Canlı, Rehber, Kütüphane, Kayıtlar, Çoklu ekran, Kaynaklar, Ayarlar, oynatıcı (dokunmatik + kanal listesi), yedek/geri yükleme. Ekran görüntüleri al.
- [ ] **Step 4:** Aynı AVD `hw.lcd.width/height = 1600×2560 @320 dpi` → ~800 dp tablet; iki bölmeli düzen ve 3 sütun doğrulanır.
- [ ] **Step 5:** `tv36` regresyon: Canlı, Rehber ızgarası, Ayarlar (AFR, açılışta başlatma), çoklu ekran 4'lü, oynatıcıda D-pad ve CH+/- kanal değiştirme.
- [ ] **Step 6:** `.memory` güncelle: mobil uygulama, ayrı veri, iki APK, doğrulanan/doğrulanmayan.
- [ ] **Step 7:** Commit + iki APK'yı `~/Downloads/novatv/`'e kopyala, `SHA256SUMS.txt` yaz.

## Self-Review Notları

- Spec §3.2 taşıma listesi Task 1'de; §4 kırılımlar Task 2–3'te; §5 ekran tablosu Task 5–8'de; §6 oynatıcı Task 9'da; §7 kimlik Task 4'te; §3.4 veri Task 10'da; §8 doğrulama Task 11'de.
- Spec'te "hepsi" kapsamı: Arama, Rehber, Kütüphane, Kayıtlar, Çoklu ekran, Kaynaklar, Ayarlar, oynatıcı — hepsi bir göreve bağlı.
- `PlayerUiState.zap`, `ChannelBanner` ve `channelStep` Task 9'da yeniden kullanılıyor; adlar mevcut kodla aynı.
- Task 6'daki `nowNextOf` yalnız compact görünüm için; ızgara yolu değişmiyor.
