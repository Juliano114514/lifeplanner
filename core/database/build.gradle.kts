plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.example.lifeplanner.core.database"
  compileSdk {
    version = release(37)
  }

  defaultConfig {
    minSdk = 28
    consumerProguardFiles("consumer-rules.pro")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
  }

}

dependencies {
  implementation(project(":core:domain"))
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  implementation(libs.kotlinx.coroutines.core)
  ksp(libs.androidx.room.compiler)
}
