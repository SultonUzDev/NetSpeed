import java.util.Properties
import kotlin.apply
import kotlin.math.sign

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
}


val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}


fun signingValue(propKey: String, envKey: String): String? =
    System.getenv(envKey) ?: keystoreProperties.getProperty(propKey)

val hasSigningConfig = signingValue("storeFile", "KEYSTORE_PATH") != null






android {
    namespace = "com.sultonuzdev.netspeed"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sultonuzdev.netspeed"
        minSdk = 26
        targetSdk = 36
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 2
        versionName = System.getenv("VERSION_NAME") ?: "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }



    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = rootProject.file(signingValue("storeFile", "KEYSTORE_PATH")!!)
                storePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "KEY_PASSWORD")
            }
        }
    }


    buildTypes {
        release {
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            // R8: obfuscates + shrinks. Keep rules live in proguard-rules.pro; RootDetector and
            // the rest of our code are deliberately NOT kept, so integrity checks get renamed.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint { checkReleaseBuilds = false }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Dependency Injection - Koin
    implementation(libs.bundles.koin)

    // Database - Room
    implementation(libs.bundles.room)
    kapt(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.bundles.coroutines)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation("androidx.compose.material:material-icons-extended:1.7.8")

}