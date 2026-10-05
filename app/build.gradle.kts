import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseStoreFile = System.getenv("BP_RELEASE_STORE_FILE")
val releaseStorePassword = System.getenv("BP_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("BP_RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("BP_RELEASE_KEY_PASSWORD")
val releaseSigningAvailable = !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()
// Signing is required only when packaging a publishable release. Local compilation
// and the explicitly separate releaseCheck variant do not require credentials.
gradle.taskGraph.whenReady {
    val packagesRelease = allTasks.any {
        it.project.path == ":app" && it.name in setOf("packageRelease", "packageReleaseBundle")
    }
    if (packagesRelease && !releaseSigningAvailable) {
        throw GradleException("Publishable release requires all BP_RELEASE_* signing variables. " +
            "Use assembleReleaseCheck / bundleReleaseCheck for local or CI compile validation.")
    }
}
val appVersion = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}

android {
    namespace = "com.breakoutplus"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.breakoutplus"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersion.getProperty("versionCode").toInt()
        versionName = appVersion.getProperty("versionName")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
        create("releaseCheck") {
            initWith(getByName("release"))
            applicationIdSuffix = ".compilecheck"
            versionNameSuffix = "-compilecheck"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/LICENSE*",
            "META-INF/NOTICE*"
        )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.window:window:1.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    testImplementation("junit:junit:4.13.2")
}
