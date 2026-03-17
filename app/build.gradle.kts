plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.navigation.safeargs)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
}

android {
    namespace = "com.titanbiosync"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.titanbiosync"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // Modules
    implementation(project(":modules:domain-core"))
    implementation(project(":modules:data-local"))

    // Calendar (month view) - Kotlin 1.9.x compatible
    implementation("com.kizitonwose.calendar:view:2.5.0")

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // SplashScreen + UI extras
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.gridlayout:gridlayout:1.0.0")

    // Material Design
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Room (needed in app module for RoomDatabase + withTransaction extensions)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Media3
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // MPAndroidChart
    implementation(libs.mpandroidchart)

    // Google Play Services
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)

    // Firebase (BOM manages versions; google-services.json required at runtime)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    // Coroutine support for Firebase Tasks (.await())
    implementation(libs.kotlinx.coroutines.play.services)

    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Testing (unit tests w/ Robolectric)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Room in unit tests (optional, but ok to keep)
    testImplementation("androidx.room:room-runtime:2.6.1")
    testImplementation("androidx.room:room-ktx:2.6.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}