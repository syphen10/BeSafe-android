import java.util.Properties

// Google's public AdMob test IDs (always serve test ads).
val TEST_ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.besafe.app"
    compileSdk = 36

    defaultConfig {
        // Store identity (permanent once on Play). "com.besafe.app" was already registered on Android
        // by someone else, so the Play listing uses this unique ID; the code namespace is unchanged.
        applicationId = "com.syphen.besafe"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.2.0"
        vectorDrawables.useSupportLibrary = true

        // AdMob IDs come from gradle.properties; defaults are Google's public test IDs.
        val admobAppId = (project.findProperty("besafe.admobAppId") as String?) ?: TEST_ADMOB_APP_ID
        val bannerUnit = (project.findProperty("besafe.bannerUnitId") as String?) ?: TEST_BANNER_UNIT_ID
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "BANNER_UNIT_ID", "\"$bannerUnit\"")
    }

    // Upload key for Google Play. keystore.properties + the .jks are gitignored in the public repo
    // and backed up only in the private repo; without them, release builds are simply unsigned.
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val hasUploadKey = keystoreProps.containsKey("storeFile")

    signingConfigs {
        create("release") {
            if (hasUploadKey) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasUploadKey) signingConfig = signingConfigs.getByName("release")
        }
        // The build the user installs for testing. Debuggable apps run without ART's ahead-of-time
        // compilation or baseline profiles and start 2-4x slower, so it's optimised like release
        // and only differs in being signed with the debug key.
        debug {
            // Test ads only: tapping your own live ads can get the AdMob account suspended.
            buildConfigField("String", "BANNER_UNIT_ID", "\"$TEST_BANNER_UNIT_ID\"")
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // `gradlew testDebugUnitTest -PstoreShots=<dir> --tests "*StoreScreenshots*"` renders store
        // screenshots into <dir>; without the property those tests are skipped.
        unitTests.all {
            it.systemProperty("storeShotsDir", (project.findProperty("storeShots") as String?) ?: "")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Installs the bundled Compose baseline profiles on sideloaded APKs too, not just Play installs.
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    // Google Play In-App Review: lets users rate without leaving the app.
    implementation("com.google.android.play:review-ktx:2.0.2")
    // Banner ads + Google's consent form for EEA/UK/Swiss users (same versions as Vivichi).
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    testImplementation("junit:junit:4.13.2")
    // Store screenshots rendered from the real Compose screens on the JVM (no emulator needed).
    testImplementation(composeBom)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
