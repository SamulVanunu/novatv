plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// TV ve telefon uygulamalarının paylaştığı grafik, servis, rota ve kimlik
// kaynakları. Yalnızca core/feature/ui modüllerine bağlanır.
android {
    namespace = "dev.anon.novatv.app.common"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":core:database"))
    implementation(project(":core:sync"))
    implementation(project(":core:settings"))
    implementation(project(":core:source-xstream"))
    implementation(project(":core:source-m3u"))
    implementation(project(":core:source-stalker"))
    implementation(project(":feature:playlists"))
    implementation(project(":feature:live"))
    implementation(project(":feature:player"))
    implementation(project(":feature:epg"))
    implementation(project(":feature:library"))
    implementation(project(":feature:recordings"))
    implementation(project(":feature:multiwall"))
    implementation(project(":feature:settings"))
    implementation(project(":core:player"))
    implementation(project(":core:epg"))
    implementation(project(":core:recording"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
}
