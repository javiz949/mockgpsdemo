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

        // Xposed legacy API used by the LSPatch-compatible hook module.
        // compileOnly is used in :hook, so the API is NOT bundled into the module APK.
        maven {
            url = uri("https://api.xposed.info/")
        }
    }
}

rootProject.name = "MockGpsDemo"
include(":app")
include(":hook")
