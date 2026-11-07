plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

subprojects {
    tasks.withType<Test>().configureEach {
        // tr_TR varsayılan locale'i Conscrypt/Robolectric native adını bozuyor ("lınux").
        systemProperty("user.language", "en")
        systemProperty("user.country", "US")
    }
}
