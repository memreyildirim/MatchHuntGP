plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

/// local.properties dosyasından API key'i oku
val localPropertiesFile = rootProject.file("local.properties")
val mapsApiKey = if (localPropertiesFile.exists()) {
    localPropertiesFile.readLines()
        .find { it.startsWith("MAPS_API_KEY") }
        ?.substringAfter("=")
        ?.trim()
        ?.removeSurrounding("\"") ?: ""
} else ""

// Release imzalama ortam degiskenlerinden okunur. CI/local build sirasinda set edilmemisse
// release imzalanmaz ve `assembleRelease` standart debug-fallback davranisina dusmek
// yerine imzasiz cikti uretir; Play yuklemeden once env set edilmelidir.
val releaseStoreFile: String? = System.getenv("MATCHHUNT_RELEASE_STORE_FILE")
val releaseStorePassword: String? = System.getenv("MATCHHUNT_RELEASE_STORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("MATCHHUNT_RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("MATCHHUNT_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseStoreFile != null &&
        releaseStorePassword != null &&
        releaseKeyAlias != null &&
        releaseKeyPassword != null

android {
    namespace = "com.emreyildirim.matchhuntv1"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.emreyildirim.matchhuntv1"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API key'i manifest placeholder olarak ekle
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
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
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    testOptions{
        unitTests{
            isIncludeAndroidResources = true
            all {
                if (it is Test){
                    it.useTestNG()
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.play.services.auth)
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.3")
    implementation("androidx.compose.material:material-icons-core:1.6.3")
    implementation("androidx.compose.foundation:foundation:1.6.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")
    implementation ("com.google.accompanist:accompanist-swiperefresh:0.32.0")
    implementation(libs.androidx.compose.ui.text)
    //testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //firebase cloud notifications (push notification)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    //testNg test implenmentationlar
    testImplementation("org.testng:testng:7.10.2")


    //Appium Java Client için testImplementations
    testImplementation("io.appium:java-client:9.2.2")

    //extent report için implementation
    testImplementation("com.aventstack:extentreports:5.0.9")

    // ViewModel + repository unit testleri icin
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation(kotlin("test"))
}