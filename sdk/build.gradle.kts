plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "co.getkarla.sdk"
    compileSdk = 33

    defaultConfig {
        minSdk = 22

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
//            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.squareup:otto:1.3.8")
//    implementation("com.github.pro100svitlo:creditCardNfcReader:1.0.3")
    implementation("com.github.elvis-chuks.credit-Card-Nfc-Reader:library:parent-2.1.1")
    implementation("com.google.firebase:firebase-crashlytics-buildtools:2.9.4")

    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.google.code.gson:gson:2.8.8")
}

configure<PublishingExtension> {
    repositories {
//        maven {
//            url = uri("/users/macbook/Documents/Karla/maven-repo")
//        }
        maven {
            url = uri("https://jitpack.io")
        }
    }

    publications {
        create<MavenPublication>("Maven") {
            artifactId = "karla-sdk-andriod"
            groupId = "com.github.acumendigital"
            version = "0.3.0"
            afterEvaluate {
                artifact(tasks.getByName(
                    "bundleReleaseAar"
                ))
            }
        }

        withType<MavenPublication> {
            pom {
                packaging = "jar"
                name.set("sdk")
                description.set("Getkarla contactless sdk")
                url.set("getkarla.co")
                licenses {
                    license {
                        name.set("MIT license")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        name.set("Elvis Chuks")
                        email.set("celvischuks@gmail.com")
                    }
                }
            }
        }
    }
}