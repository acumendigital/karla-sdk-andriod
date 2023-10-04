pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        jcenter()
//        maven {
//            url = uri("https://jitpack.io")
//        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        jcenter()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            credentials { username = "jp_mu5a5evnsugudvpvn4k91so78g" }
        }
    }
}

rootProject.name = "Karla"
include(":app")
include(":sdk")
