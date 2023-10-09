pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        jcenter()
    }
//    resolutionStrategy {
//        eachPlugin {
//            if (requested.id.id == "kotlinx-serialization") {
//                useModule("org.jetbrains.kotlinx:kotlinx-serialization:1.5.1")
//            }
//        }
//    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        jcenter()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "Karla"
include(":app")
include(":sdk")
