import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// A versão pode vir da tag do GitHub (ex.: v0.2 -> "0.2"). Fallback: 0.1
val appVersionName: String = (project.findProperty("appVersionName") as String?)
    ?.removePrefix("v")
    ?.takeIf { it.isNotBlank() }
    ?: "0.1"
val appVersionCode: Int = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.hikins.tunelfai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hikins.tunelfai"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
        vectorDrawables { useSupportLibrary = true }
    }

    // Assinatura de release: usa a keystore dos GitHub Secrets se existir; senão, a chave fixa
    // versionada em app/assinatura. Assim todo APK sai com a MESMA assinatura e instala por cima.
    val keystoreFile = rootProject.file("release.keystore")
    val keystoreFixa = file("assinatura/tunelfai.jks")
    signingConfigs {
        create("release") {
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            } else {
                storeFile = keystoreFixa
                storePassword = "tunelfai2026"
                keyAlias = "tunelfai"
                keyPassword = "tunelfai2026"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Leitor de código de barras do chassi com área de leitura (CameraX + ML Kit)
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
}
