# NovaTV Mobil — telefon ve tablet sürümü

**Tarih:** 2026-11-11
**Durum:** Tasarım onaylandı — uygulama planı bekliyor
**Paket (TV):** `dev.anon.novatv`
**Paket (mobil):** `dev.anon.novatv.mobile`
**Dizin:** `~/nova-tv`

---

## 1. Amaç

NovaTV bugün yalnızca TV için kurulmuş: sabit yan ray, 22 dp kenarlar, 680 dp
paneller, her yerde D-pad varsayımı. Telefonda bu düzen taşıyor, dokunmatik
oynatma denetimi yok ve dikey kullanım desteklenmiyor.

Bu iş, aynı ürünü **ikinci bir uygulama** olarak telefon ve tablete taşır:
"NovaTV Mobil". TV uygulaması görünür olarak değişmez; paylaşılan kod tek
yerde kalır.

### 1.1 Başarı kriterleri

| Kriter | Hedef |
|---|---|
| Telefonda (411 dp) her ekran | yatay kaydırma olmadan, tek elle kullanılır |
| Dokunma hedefi | hiçbir denetim < 48 dp değil |
| Oynatıcıda kanal değiştirme | dokunuşla ≤ 2 dokunuş, D-pad/CH tuşlarıyla da çalışır |
| TV uygulaması | görsel ve davranışsal olarak değişmez (regresyon turu) |
| Tablet (≥ 840 dp) | TV'ye yakın iki bölmeli düzen |
| APK | iki ayrı imzalı APK, aynı cihazda yan yana kurulabilir |

## 2. Kapsam

**İçinde:** Canlı, Rehber, Kütüphane (film/dizi detayı dahil), Arama, Kayıtlar,
Çoklu ekran, Kaynaklar, Ayarlar, oynatıcı (canlı + VOD), yedek/geri yükleme,
kumanda ve dokunmatik girdi.

**Dışında:** bulut senkron, uygulamalar arası veri paylaşımı, telefon için ayrı
özellik seti (indirme, dikey video, Chromecast), yeni görsel kimlik.

## 3. Mimari

### 3.1 Modüller

```
:app-common   (Android library)  — uygulama grafiği, servisler, ortak rotalar
:app          (TV uygulaması)    — MainActivity, TV kabuğu
:mobile       (yeni uygulama)    — MobileMainActivity, mobil kabuk
   ↓
:feature:*    :ui               — ekranlar, bileşenler, tema
   ↓
:core:*                          — veri, senkron, oynatma
```

- `:mobile` ve `:app` birbirine bağımlı değildir; ikisi de `:app-common`,
  `:feature:*`, `:ui` kullanır.
- `feature → feature` yasağı korunur; `:app-common` yalnızca core ve feature
  modüllerine bağlanır.

### 3.2 `:app-common`'a taşınanlar

| Dosya | Neden |
|---|---|
| `NovaGraph.kt`, `NovaApplication.kt` | iki uygulama aynı grafiği kurar |
| `BackupManager.kt` | mobilde de yedek/geri yükleme var |
| `ParentalGuard.kt` | PIN kapısı iki uygulamada aynı |
| `RecordingService/Receiver/BootReceiver`, `AutoStartReceiver` | manifest birleşmesiyle iki uygulamada da kayıtlı |
| `Routes`, `PlayerRouteContent`, `subtitleMimeType`, `hasDocumentPicker` | oynatıcı ve belge seçici davranışı tek yerde |
| `network_security_config`, izinler, servis/alıcı bildirimleri | manifest birleşmesi |

Her uygulama kendi `MainActivity`, ikon, etiket, tema ve (TV'de) banner'ını
bildirir. `:app-common` kendi `applicationId`'sini taşımaz.

### 3.3 Mobil uygulama

- `dev.anon.novatv.mobile`, etiket **"NovaTV Mobil"**, aynı imza anahtarı
  (`~/.android-keys/novatv.jks`), min 23 / target 35.
- Aynı "Yayın" kimliği: kırmızı zemin + krem N, adaptive ikon; splash mürekkep.
- Açılış hedefi TV ile aynı kural: son izlenen kanal varsa oynatıcı, yoksa Canlı.

### 3.4 Veri

Ayrı `applicationId` ayrı veri dizini demek: kaynaklar, rehber, kayıtlar ve
profiller taşınmaz. Geçiş yolu mevcut yedek/geri yükleme:

- TV'de **Ayarlar → Yedek al**, dosyayı telefona taşı, mobilde **Geri yükle**.
- Mobil ilk açılışta kaynak yoksa tek satırlık yönlendirme gösterir:
  "Kaynak ekle ya da TV yedeğini geri yükle."
- Parola/akış adresi politikası değişmez: Keystore + `redactUrl()`.

## 4. Uyarlanabilir katman (`ui`)

- `NovaWindow`: pencere genişliğine göre `Compact (<600 dp)`, `Medium (600–840)`,
  `Expanded (≥840)` — Android/Material pencere sınıfı kuralı; döndürülmüş
  telefonda genişlik büyüdüğü için sınıf da büyür. Tek yerden
  `LocalConfiguration.current.screenWidthDp`.
- `NovaScaffold`: `Expanded`/TV → bugünkü yan ray; `Compact`/`Medium` → alt
  gezinme çubuğu. TV her zaman `Expanded` olduğu için TV düzeni korunur.
- Tokenlar: kenar boşluğu 16/24/32, dokunma hedefi ≥48 dp, satır yüksekliği
  56 dp (compact), sütun sayısı `when (window) { Compact -> 2; Medium -> 3;
  Expanded -> 5 }`.
- Tipografi ölçeği mevcut `NovaTheme(typeScale)` ile; compact'ta varsayılan 1.0.
- Mevcut `NovaPill`, `NovaPanel`, `NovaBadge` dokunmada da çalışır; odak dili
  yalnızca D-pad/klavye odağında görünür, dokunuşta dolgu kullanılmaz.

## 5. Ekranlar

| Ekran | Compact / Medium (telefon, küçük tablet) | Expanded (tablet, TV) |
|---|---|---|
| Canlı | yatay grup çipleri, tam genişlik satırlar, yıldız dokunmada | bugünkü grup rayı + satırlar |
| Rehber | kanal başına "şimdi / sıradaki" listesi, programa dokun → detay sayfası | bugünkü ızgara |
| Kütüphane | 2–3 sütun; detay tam sayfa | 5 sütun; detay iki bölme |
| Arama | tek sütun, klavye açık | bugünkü |
| Kayıtlar | tek sütun kart | bugünkü |
| Çoklu ekran | 2'li dikey; 4'lü seçilirse "telefonda küçük" uyarısı | bugünkü |
| Kaynaklar / Ayarlar | tek sütun, kaydırılabilir, bölüm başlıkları | bugünkü |
| Menü | alt çubukta "Daha fazla" sayfası | başlıktaki Menü düğmesi |

Telefonda EPG ızgarası yerine "şimdi/sıradaki" listesi bilinçli tek sapma:
94 dp hücre 411 dp genişlikte okunmuyor.

## 6. Oynatıcı

- Dokun → OSD aç/kapa; 4 sn sonra kendiliğinden kapanır (mevcut süre).
- OSD: ‹ Kanal / Kanal › düğmeleri, **kanal listesi** sayfası (dokunmatik
  zapping), duraklat, ses, altyazı, oran, tampon; VOD'da ilerleme çubuğu ve
  10 sn geri/ileri.
- Kanal değiştirme mantığı TV ile aynı: `PlayerViewModel.stepChannel`,
  `ChannelRepository.zappingOrder`, 2,5 sn kanal bandı, uçlarda sarma.
- D-pad ve CH+/CH- aynen çalışır (tablet + harici kumanda).
- Oynatıcı rotası aktifken ekran yatay (`sensorLandscape`), çıkışta serbest.
- PiP mobilde de açık (API 31+ otomatik, altında `onUserLeaveHint`).

## 7. Kimlik

- Aynı N işareti ve palet; mobil etiket "NovaTV Mobil".
- Adaptive ikon + monochrome aynı kaynaktan (`tools/brand/make_icons.py`);
  mobil için ek PNG üretilmez, aynı `mipmap` kullanılır.
- TV banner yalnızca TV uygulamasında.

## 8. Test ve doğrulama

- Birim: `windowClass(widthDp)`, sütun sayısı, oynatıcı tuş eşlemesi
  (dokunma düğmeleri dahil), mobil başlangıç hedefi.
- Bağlı (instrumented): mobil kabukta sekme geçişi, oynatıcı OSD düğmeleri;
  mevcut `testTag`'ler korunur (`open-playlists`, `settings-afr`,
  `settings-autostart`, `multiwall-2`, `multiwall-4`, `player-root`).
- Cihaz: `vakit-tel` (1080×2400, 411 dp) telefon; aynı AVD 800×1280 dp'ye
  çevrilerek tablet; `tv36` TV regresyon turu (Canlı, Rehber, Ayarlar,
  oynatıcı, kanal değiştirme).
- Çıktı: `NovaTV-<sürüm>-release.apk` ve `NovaTV-Mobil-<sürüm>-release.apk`,
  SHA-256 ile.

## 9. Riskler

| Risk | Karşılık |
|---|---|
| `:app-common` taşıması TV'de regresyon | taşıma sonrası ilk iş TV turu; davranış değişmez |
| EPG'nin iki düzeni bakım yükü | ortak veri katmanı; yalnızca görünüm ayrışır |
| Ayrı veri kullanıcıyı şaşırtır | ilk açılışta yedek yönlendirmesi |
| Dokunmatik + D-pad çakışması | odak dili yalnızca tuş girdisinde; dokunuş dolgu kullanmaz |
| İki APK'nın sürüm kayması | tek `versionName` kaynağı, aynı commit'ten iki `assemble` |

## 10. Teslim sırası

1. `:app-common` taşıması; TV APK'sı aynı davranışla derlenir ve doğrulanır.
2. `NovaWindow` + `NovaScaffold` + tokenlar; TV regresyonu.
3. `:mobile` iskeleti: grafik, kabuk, alt gezinme, Canlı + Ayarlar.
4. Kalan ekranlar: Rehber, Kütüphane, Arama, Kayıtlar, Çoklu ekran, Kaynaklar.
5. Dokunmatik oynatıcı + kanal listesi + yatay kilit.
6. Telefon/tablet cihaz turu, testler, iki imzalı APK.

## 11. Bitti sayılır

- TV APK'sı önceki turla aynı görünür davranışı verir; `tv36` turu temiz.
- Telefonda 411 dp'de tüm ekranlar kaydırmasız kullanılır, hiçbir hedef <48 dp değil.
- Tablette ≥840 dp'de iki bölmeli düzen açılır.
- Oynatıcıda dokunarak kanal değiştirme, kanal listesi ve D-pad/CH tuşları çalışır.
- Yedek al/geri yükle mobilde çalışır; ayrı veri davranışı ilk açılışta anlatılır.
- İki APK imzalı, aynı cihazda yan yana kurulabilir, hash'leri raporlanır.
