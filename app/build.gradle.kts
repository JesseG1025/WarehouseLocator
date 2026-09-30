import java.util.Properties

// 1. Plugins MUST be immediately after imports
plugins {
    alias(libs.plugins.android.application)
}

// 2. Property logic goes here
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()){
    localProperties.load(localPropertiesFile.inputStream())
}
val serverIp = localProperties.getProperty("SERVER_IP") ?: "127.0.0.1"

// 3. Android configuration
android {
    namespace = "com.example.warehouselocationscanner"

    // Fixed SDK syntax
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.warehouselocationscanner"
        minSdk = 26
        targetSdk = 37 // Matched to compileSdk for stability
        versionCode = 1
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Secure variable injection
        buildConfigField("String", "SERVER_IP", "\"$serverIp\"")
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
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.material)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}