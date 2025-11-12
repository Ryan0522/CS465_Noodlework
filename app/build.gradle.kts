plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.roomies"
    compileSdk = 34   // Use a stable version you have installed (e.g., 34)

    defaultConfig {
        applicationId = "com.example.roomies"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Core Android & UI
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // RecyclerView (for chore list)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Lifecycle components (still useful even in Java)
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.3")

    // Room Database (for storing chores/reminders)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation(libs.activity)
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // WorkManager (for background reminders)
    implementation("androidx.work:work-runtime:2.9.0")

    // Testing (optional)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
