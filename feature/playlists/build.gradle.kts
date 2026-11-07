plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.anon.novatv.feature.playlists"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":core:model"))
    implementation(project(":core:sync"))
    implementation(project(":core:settings"))
    implementation(project(":core:source"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(project(":core:database"))
    androidTestImplementation(project(":core:network"))
    androidTestImplementation(project(":core:source-m3u"))
    androidTestImplementation(project(":core:source-xstream"))
    androidTestImplementation(project(":core:source-stalker"))
    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(libs.androidx.paging.testing)
    androidTestImplementation(libs.androidx.datastore.preferences)
    androidTestImplementation(libs.mockwebserver)
}
