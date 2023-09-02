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
            isMinifyEnabled = false
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

//    implementation("com.romellfudi.fudinfc:fudi-nfc:android-12-1.1.0")
}

configure<PublishingExtension> {
    repositories {
        maven {
            url = uri("/users/macbook/Documents/Karla/maven-repo")
        }
    }

    publications {
        create<MavenPublication>("Maven") {
            artifactId = "karla"
            groupId = "co.getkarla.sdk"
            version = "0.0.6"
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