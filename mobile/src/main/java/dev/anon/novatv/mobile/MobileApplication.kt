package dev.anon.novatv.mobile

import dev.anon.novatv.app.common.NovaApplication

/**
 * Telefon uygulamasının Application'ı.
 *
 * Ortak `NovaApplication` tek başına yeterlidir: grafik, kayıt kurtarma ve
 * WorkManager yapılandırması orada kurulur. `nova-tv-mobil` tasarımındaki
 * "mobil uygulama `NovaApplication`'ı genişletir" adımı yerine bir tür
 * (`typealias`) kullanılıyor; davranış birebir aynı, iki uygulama da tam
 * nitelikli `dev.anon.novatv.app.common.NovaApplication` sınıfını manifestte
 * bildirir. Ortak sınıfa ekleme yapılmadığı için TV uygulaması değişmez.
 *
 * Telefona özel bir kurulum gerekirse (ör. ayarları mobilde gizleme) önce
 * `NovaApplication` `open` yapılmalı ve bu tür gerçek bir alt sınıfa dönmeli.
 */
typealias MobileApplication = NovaApplication
