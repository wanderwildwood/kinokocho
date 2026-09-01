import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.wanderwildwood.kinokocho"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wanderwildwood.kinokocho"
        // The Kompakt runs Android 12 (API 31); nothing here needs anything newer.
        minSdk = 31
        targetSdk = 31
        versionCode = 5
        versionName = "0.2.3"

        /*
         * The iNaturalist application id, which is not in this repository.
         *
         * Supplied at build time as `-PinatClientId=...` or as a line in the gitignored
         * local.properties. A build without one produces an app with no iNaturalist
         * button at all, which is the intended state for anyone who is not the person
         * the application is registered to: a PKCE client id is not a secret, but it is
         * an identity, and a fork should be posting as itself. See INatConfig.
         */
        val inatClientId = (project.findProperty("inatClientId") as String?)
            ?: rootProject.file("local.properties").takeIf { it.isFile }?.let { file ->
                Properties().apply { file.inputStream().use(::load) }
                    .getProperty("inatClientId")
            }
        buildConfigField("String", "INAT_CLIENT_ID", "\"${inatClientId.orEmpty()}\"")
    }

    // A real keystore in signing/ signs every build type when it is present, so the
    // very first install is already release-signed and a later update can never hit
    // INSTALL_FAILED_UPDATE_INCOMPATIBLE. It is gitignored, and there is no fallback:
    // a fresh clone builds an unsigned release APK, which will not install anywhere.
    // A keystore committed to a public repo is not a signing key, it is a formality,
    // and a missing one should stop you rather than produce something installable.
    // (Debug builds still get the ordinary Android debug key from AGP.)
    val signingPropertiesFile = rootProject.file("signing/signing.properties")
    val realSigningConfig = if (signingPropertiesFile.isFile) {
        val signingProperties = Properties().apply {
            signingPropertiesFile.inputStream().use(::load)
        }
        signingConfigs.create("real") {
            storeFile = rootProject.file("signing/signing.keystore")
            storePassword = signingProperties.getProperty("STORE_PASSWORD")
            keyAlias = signingProperties.getProperty("KEY_ALIAS")
            keyPassword = signingProperties.getProperty("KEY_PASSWORD")
        }
    } else {
        null
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            realSigningConfig?.let { signingConfig = it }
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            realSigningConfig?.let { signingConfig = it }
        }
    }

    lint {
        // This app is sideloaded onto a Kompakt and is not going to Google Play, whose
        // API-33 floor this otherwise trips. Targeting the OS the device actually runs
        // is deliberate: see minSdk above.
        disable += "ExpiredTargetSdkVersion"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        // The About row has to be able to say which build it is.
        buildConfig = true
    }

    sourceSets {
        named("main") {
            kotlin.srcDir("src/main/kotlin")
        }
    }

    // Room writes the schema of every version to this directory and it is committed.
    // That is what makes a migration reviewable in a diff rather than discovered on a
    // phone holding four years of somebody's notes.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.mmd)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
}
