import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Telefon/tablet uygulaması. TV uygulamasıyla tüm grafiği ve rota gövdesini
// :app-common üzerinden paylaşır; :app modülüne bağımlılığı yoktur.
//
// Yan yükleme için release imzası: keystore.properties varsa kullanılır,
// yoksa release derlemesi imzasız kalır (ör. CI'da yalnızca derleme).
// :app ile aynı anahtar kullanılır.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystoreProperties.getProperty("storeFile").isNullOrBlank().not()

android {
    namespace = "dev.anon.novatv.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.anon.novatv.mobile"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":app-common"))
    // NavHost her özellik ekranını doğrudan bağlar (TV'deki NovaNavHost'un
    // aynısı); bu yüzden özellik modülleri tek tek listelemek yerine burada
    // açıkça duruyor.
    implementation(project(":ui"))
    implementation(project(":feature:live"))
    implementation(project(":feature:player"))
    implementation(project(":feature:epg"))
    implementation(project(":feature:library"))
    implementation(project(":feature:recordings"))
    implementation(project(":feature:multiwall"))
    implementation(project(":feature:playlists"))
    implementation(project(":feature:settings"))
    implementation(project(":core:model"))
    implementation(project(":core:player"))
    implementation(project(":core:epg"))
    implementation(project(":core:recording"))
    implementation(project(":core:settings"))
    implementation(project(":core:sync"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.tv.material)
    // NovaApplication, WorkManager.Configuration.Provider uygular; süper tipin
    // görünür olması için burada da gerekli.
    implementation(libs.androidx.work.runtime.ktx)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
}
