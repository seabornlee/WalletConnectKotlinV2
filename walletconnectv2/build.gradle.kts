import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("com.squareup.sqldelight")
    `maven-publish`
}

tasks.withType<Test> {
    useJUnitPlatform()
}

android {
    compileSdk = 30

    defaultConfig {
        minSdk = 23
        targetSdk = 30

        testInstrumentationRunner = "org.walletconnect.walletconnectv2.WCTestRunner"
        testInstrumentationRunnerArguments += mutableMapOf("runnerBuilder" to "de.mannodermaus.junit5.AndroidJUnit5Builder")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
    }

    kotlinOptions {
        jvmTarget = jvmVersion.toString()
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    packagingOptions {
        resources.excludes += setOf(
            "META-INF/LICENSE.md",
            "META-INF/LICENSE-notice.md",
            "META-INF/AL2.0",
            "META-INF/LGPL2.1"
        )
    }
}

kotlin {
    tasks.withType<KotlinCompile>() {
        kotlinOptions {
            sourceCompatibility = jvmVersion.toString()
            targetCompatibility = jvmVersion.toString()
            jvmTarget = jvmVersion.toString()
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.time.ExperimentalTime"
        }
    }
}

dependencies {
    okhttp()
    bouncyCastle()
    coroutines()
    moshi()
    scarlet()
    sqlDelight()
    security()

    jUnit5()
    robolectric()
    mockk()
    timber()
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create("release", MavenPublication::class) {
                // Applies the component for the release build variant.
                from(components.getByName("release"))
                // You can then customize attributes of the publication as shown below.
                groupId = "com.walletconnect"
                artifactId = "walletconnectv2"
                version = "1.0.0-alpha01"
            }
        }
    }
}