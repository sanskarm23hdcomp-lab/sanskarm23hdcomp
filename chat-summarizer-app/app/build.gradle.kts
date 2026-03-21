plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Uncomment the next line after adding google-services.json (production mode only):
    // id("com.google.gms.google-services")
}

android {
    namespace  = "com.example.groupchat"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.groupchat"
        minSdk        = 24
        targetSdk     = 34
        versionCode   = 1
        versionName   = "1.0"

        // Expose the Gemini API key via BuildConfig.
        // Add  GEMINI_API_KEY=AIza...  to local.properties to enable production mode.
        // Leave it blank to run in Demo Mode (no key required).
        val geminiKey = project.findProperty("GEMINI_API_KEY")?.toString() ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildFeatures {
        buildConfig  = true
        viewBinding  = true
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
    // Firebase (only needed in production mode – comment out when running demo only)
    // implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    // implementation("com.google.firebase:firebase-firestore-ktx")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // AndroidX / Material
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("com.google.android.material:material:1.12.0")
}
