# NovaTV / Yayın

Android TV (ve telefon/tablet) için yerel-öncelikli IPTV oynatıcı. Uygulama
içerik barındırmaz; kaynak kullanıcı tarafından girilir:

- **Xtream Codes** (sunucu, kullanıcı adı, şifre)
- **M3U / M3U8** bağlantısı
- **Stalker Portal** (MAC)

Aynı gövdeden iki uygulama çıkar, yan yana kurulur:

| Uygulama | Paket | Ad | Arayüz |
|---|---|---|---|
| TV | `dev.anon.novatv` | Yayın | Kumanda (D-pad), Leanback |
| Telefon/tablet | `dev.anon.novatv.mobile` | NovaTV Mobil | Dokunma, ≥48 dp hedefler |

Sürüm `0.1.0`, `minSdk 23`, `targetSdk 35`, `compileSdk 36`. Dağıtım sideload
APK iledir; mağaza yayını yoktur.

## Özellikler

- Canlı TV, gruplar, favoriler, son izlenenler; kumandayla kanal geçişi
  (`CH+`/`CH-`, OSD kapalıyken D-pad ↑/↓) ve 2,5 sn kanal bandı.
- Rehber (EPG): TV'de ızgara, telefonda "şimdi/sıradaki" listesi + detay.
- Kütüphane (film/dizi), arama, profiller, ebeveyn kilidi.
- Kayıt (Media3 + WorkManager), çoklu ekran (TV 4, telefon 2 kutu).
- Görünüm: tema, yazı boyutu, saat biçimi, AFR, açılışta başlatma.
- Yedek al / geri yükle — yalnız görünüm ve profil adları; kaynaklar taşınmaz.

## Derleme

```bash
# Java 21 gerekir
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew \
  :app:assembleRelease :mobile:assembleRelease
```

Çıktılar:

- `app/build/outputs/apk/release/app-release.apk`
- `mobile/build/outputs/apk/release/mobile-release.apk`

Release imzası `keystore.properties` varsa uygulanır (bkz.
`keystore.properties.example`, anahtar üretimi `tools/make-keystore.sh`).
Dosya yoksa APK imzasız derlenir.

## Test

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :mobile:lintVitalRelease
```

`0.1.0` durumu: 401 birim testi, 0 hata. Cihaz turları Android 15 TV
emülatöründe (D-pad) ve Pixel 6 profilinde (dokunma) yapıldı.

## Modüller

| Modül | İçerik |
|---|---|
| `app` | TV kabuğu: `MainActivity`, `NovaNavHost`, Leanback manifesti |
| `mobile` | Telefon kabuğu: `MobileMainActivity`, `MobileNavHost` |
| `app-common` | Ortak rotalar, Room, senkron, oynatma, kayıt, yedek, grafik |
| `core/*` | Veri, kaynak istemcileri (Xtream/M3U/Stalker), ayarlar, EPG |
| `feature/*` | Canlı, EPG, kütüphane, oynatıcı, kayıt, çoklu ekran, ayarlar |
| `ui` | `NovaWindow` (Compact/Medium/Expanded), `NovaScaffold`, tema, bileşenler |

Pencere sınıfı genişliğe göredir. Mobil kabuk en fazla `Medium`'da kalır:
TV gövdeleri tv-material `ClickableSurface` kullanır ve yalnız kumanda
Enter'ını işler. `Expanded` televizyonun düzenidir.

## Gizlilik

Kimlik bilgileri ve şifre taşıyan liste adresleri cihazda şifreli depoda
tutulur; günlüklere ve yedeğe yazılmaz. Depoda gerçek kaynak, kullanıcı adı
veya parola yoktur; testlerde yalnız Demo/Mock değerler kullanılır.

## Belgeler

- Spec (TV): `docs/superpowers/specs/2026-11-07-novatv-tvimate-parity-design.md`
- Spec (mobil): `docs/superpowers/specs/2026-11-11-novatv-mobil-design.md`
- Planlar: `docs/superpowers/plans/`

## Bilinen sınırlar

- Tablette 1066 dp (3 sütun, tam sayfa detay) düzeni henüz cihazda turlanmadı.
- Mobil kabuk için instrumented test yok; rota/geri davranışı birim testleri
  ve cihaz turuyla korunuyor.
- Film oynatıcısından dönüş kütüphaneye iner (detay ekran durumunda).
- Kayıt silme onaysızdır (TV paritesi).

## Lisans

Lisans dosyası yok; tüm haklar saklıdır.
