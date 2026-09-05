// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later

import java.util.Properties
import com.android.build.api.variant.FilterConfiguration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // Note: androidx.baselineprofile plugin is NOT applied here because it is
    // incompatible with AGP 9.x. AGP 8.3+ includes native baseline profile
    // support via com.android.application; the baselineProfile() dep below and
    // the baseline-prof.txt file are sufficient.
    id("kotlin-parcelize")
//    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.cluno1.sonorus"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.cluno1.sonorus"
        minSdk = 26
        targetSdk = 37
        
        val overrideVersionCode = project.findProperty("versionCodeOverride")?.toString()?.toIntOrNull()
        val overrideVersionName = project.findProperty("versionNameOverride")?.toString()
        versionCode = overrideVersionCode ?: 1000000
        versionName = overrideVersionName ?: "1.0.0"

        val overrideReleaseDate = project.findProperty("releaseDateOverride")?.toString()
        buildConfigField("String", "RELEASE_DATE", "\"${overrideReleaseDate ?: "2026-09-03"}\"")

        val isNightly = project.findProperty("nightly")?.toString() == "true"
        buildConfigField("boolean", "IS_NIGHTLY", isNightly.toString())

        // This fork ships against the first-party Catalog API and its trusted COS assets only.
        buildConfigField("boolean", "CATALOG_ONLY", "true")
        buildConfigField("boolean", "FIRST_PARTY_UPDATES", "true")
        buildConfigField("String", "GITHUB_OWNER", "\"Cluno1\"")
        buildConfigField("String", "GITHUB_REPO", "\"Sonorus\"")
        buildConfigField("String", "SOURCE_URL", "\"https://github.com/Cluno1/Sonorus\"")
        buildConfigField("String", "RELEASES_URL", "\"https://github.com/Cluno1/Sonorus/releases\"")
        buildConfigField("String", "ISSUES_URL", "\"https://github.com/Cluno1/Sonorus/issues\"")
        buildConfigField("String", "UPSTREAM_SOURCE_URL", "\"https://github.com/cromaguy/Rhythm\"")

        // The Catalog-only client never enables Apple Music; do not embed provider credentials.
        buildConfigField("String", "APPLE_MUSIC_FALLBACK_TOKEN", "\"\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    // Product flavors for different distribution channels
    flavorDimensions += "distribution"
    
    productFlavors {
        create("fdroid") {
            dimension = "distribution"
            applicationId = "io.github.cluno1.sonorus"
            
            // F-Droid build: Enable all features (FOSS ethos)
            buildConfigField("boolean", "ENABLE_YOUTUBE_MUSIC", "false")
            buildConfigField("boolean", "ENABLE_LYRICALLY_API", "false")
            buildConfigField("boolean", "ENABLE_DEEZER", "false")
            buildConfigField("boolean", "ENABLE_LRCLIB", "false")
            buildConfigField("boolean", "ENABLE_BETTERLYRICS", "false")
            buildConfigField("boolean", "ENABLE_SPOTIFY_SEARCH", "false")
            buildConfigField("boolean", "ENABLE_WIKIPEDIA", "false")
            buildConfigField("String", "FLAVOR", "\"fdroid\"")
            buildConfigField("boolean", "FIRST_PARTY_UPDATES", "false")
            
            versionNameSuffix = "-fdroid"
        }
        
        create("github") {
            dimension = "distribution"
            applicationId = "io.github.cluno1.sonorus"
            
            // GitHub releases: Enable all features (same as F-Droid)
            buildConfigField("boolean", "ENABLE_YOUTUBE_MUSIC", "false")
            buildConfigField("boolean", "ENABLE_LYRICALLY_API", "false")
            buildConfigField("boolean", "ENABLE_DEEZER", "false")
            buildConfigField("boolean", "ENABLE_LRCLIB", "false")
            buildConfigField("boolean", "ENABLE_BETTERLYRICS", "false")
            buildConfigField("boolean", "ENABLE_SPOTIFY_SEARCH", "false")
            buildConfigField("boolean", "ENABLE_WIKIPEDIA", "false")
            buildConfigField("String", "FLAVOR", "\"github\"")
            buildConfigField("boolean", "FIRST_PARTY_UPDATES", "true")
            
            versionNameSuffix = "-gh"
        }
    }

    val signingProperties = getProperties(".config/keystore.properties")
    val releaseSigning =
        if (signingProperties != null) {
            signingConfigs.create("release") {
                keyAlias = signingProperties.property("key_alias")
                keyPassword = signingProperties.property("key_password")
                storePassword = signingProperties.property("store_password")
                storeFile = rootProject.file(signingProperties.property("store_file"))
            }
        } else if (providers.gradleProperty("allowDebugReleaseSigning").orNull == "true") {
            // Explicit local dry-run only. CI releases always provide the fixed Sonorus key.
            signingConfigs.getByName("debug")
        } else {
            null
        }

    defaultConfig {
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = releaseSigning
//            ndk {
//                debugSymbolLevel = "SYMBOL_TABLE"
//            }
            // Reproducible builds: disable build timestamp
            if (System.getenv("CI") == "true" || System.getenv("BUILD_REPRODUCIBLE") == "true") {
                // Use a fixed timestamp for reproducible builds  
                tasks.configureEach {
                    // Disable timestamps in bundle reports for reproducible builds
                    if (name.contains("BundleReport", ignoreCase = true)) {
                        enabled = false
                    }
                }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            //isMinifyEnabled = false
            //isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
        // Required by the macrobenchmark module for baseline profile generation.
        // Mirrors release (fully minified + signed) so the profile reflects production.
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = releaseSigning
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles
        includeInBundle = false
    }

    packaging {
        resources {
            merges += "/META-INF/INDEX.LIST"
            merges += "**/io.netty.versions.properties"
        }
    }

    // ABI splits: create smaller per-architecture APKs (reduces size by ~5–10 MB each)
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = true // also keep a universal APK for IzzyOnDroid/F-Droid
        }
    }

    lint {
        abortOnError = true
        disable.addAll(
            listOf(
                "MissingTranslation",
                "UnsafeOptInUsageError",
                "NonObservableLocale",
                "StateFlowValueCalledInComposition",
                "LocalContextGetResourceValueCall",
                "UnusedMaterial3ScaffoldPaddingParameter"
            )
        )
    }

}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abiSuffix = output.filters
                .find { it.filterType == FilterConfiguration.FilterType.ABI }
                ?.identifier
                ?.let { "-$it" }
                ?: ""

            output.outputFileName.set(
                "Sonorus-${android.defaultConfig.versionName}-${variant.name}${abiSuffix}.apk"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.alphatab)
    // Desugaring library
    coreLibraryDesugaring(libs.androidx.desugar.jdk.libs)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    
    // Compose dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    
    // Material 3 dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.window)
    implementation(libs.com.google.android.material)

    // Media3 dependencies
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.org.jellyfin.media3.ffmpeg.decoder)
    
    // Icons - Material Symbols variable font (res/font/material_symbols_outlined.ttf)
    // Replaces the deprecated material-icons-extended library for faster build times
    implementation(libs.androidx.palette.ktx)
    
    // Glance for modern widgets
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    
    // Physics-based animations
    implementation(libs.androidx.compose.animation)
    //noinspection GradleDependency
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.androidx.compose.animation.core)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Permissions
    implementation(libs.com.google.accompanist.accompanist.permissions)
    
    // Fragment
    implementation(libs.androidx.fragment.ktx)
    
    // MediaRouter for Android media output switching
    implementation(libs.androidx.mediarouter)
    
    // Coil for image loading
    implementation(libs.io.coil.kt.coil.compose)
    
    // Audio metadata editing
    implementation(libs.net.jthink.jaudiotagger)
    implementation(libs.taglib)
    
    // Network
    implementation(libs.com.squareup.retrofit2.retrofit)
    implementation(libs.com.squareup.retrofit2.converter.gson)
    implementation(libs.com.squareup.okhttp3.okhttp)
    implementation(libs.com.squareup.okhttp3.logging.interceptor)
    implementation(libs.com.google.code.gson.gson)
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
//    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Coroutines for async operations
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
    implementation(libs.androidx.foundation.layout)
    
    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)
    
    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Jetpack Paging 3
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    // Baseline Profile – installer ensures the .prof asset is loaded into ART on first launch.
    // AGP 9.x natively picks up app/src/main/baseline-prof/baseline-prof.txt without any
    // additional plugin wiring; the baselineProfile() configuration is not available without
    // the standalone plugin (which is AGP 8.x only).
    implementation(libs.androidx.profileinstaller)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.com.squareup.okhttp3.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // LeakCanary for memory leak detection (debug builds only)
    debugImplementation(libs.com.squareup.leakcanary.leakcanary.android)
}

composeCompiler {
    includeComposeMappingFile.set(false)
}

fun getProperties(fileName: String): Properties? {
    val file = rootProject.file(fileName)
    return if (file.exists()) {
        Properties().also { properties ->
            file.inputStream().use { properties.load(it) }
        }
    } else null
}

fun Properties.property(key: String) =
    this.getProperty(key) ?: "$key missing"
