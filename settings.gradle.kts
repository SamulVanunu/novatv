pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "NovaTV"
include(
    ":app",
    ":ui",
    ":core:model",
    ":core:database",
    ":core:settings",
    ":core:network",
    ":core:source",
    ":core:source-m3u",
    ":core:source-xstream",
    ":core:source-stalker",
    ":core:sync",
    ":core:epg",
    ":core:player",
    ":core:recording",
    ":feature:playlists",
    ":feature:live",
    ":feature:player",
    ":feature:epg",
    ":feature:library",
    ":feature:settings",
    ":feature:multiwall",
    ":feature:recordings",
)
