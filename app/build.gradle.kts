plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.example.lifeplanner"
  compileSdk {
    version = release(37)
  }

  defaultConfig {
    applicationId = "com.example.lifeplanner"
    minSdk = 28
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(project(":core:domain"))
  implementation(project(":core:database"))
  implementation(project(":libui"))
  implementation(project(":feature:todo"))
  implementation(project(":feature:schedule"))
  implementation(project(":feature:dishes"))
  implementation(project(":feature:inventory"))

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.koin.android)
  implementation(libs.kotlinx.serialization.json)
}
