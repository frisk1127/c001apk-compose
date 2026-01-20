import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.protobuf)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.plugin.compose)
    alias(libs.plugins.kotlin.parcelize)
}

val gitCommitCount = providers.exec {
    commandLine("git", "rev-list", "HEAD", "--count")
}.standardOutput.asText.map { it.trim().toInt() }.get()
val gitCommitHash = providers.exec {
    commandLine("git", "rev-parse", "--verify", "--short", "HEAD")
}.standardOutput.asText.map { it.trim() }.get()

android {
    namespace = "com.example.c001apk.compose"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.c001apk.compose"
        minSdk = 24
        targetSdk = 35
        versionCode = gitCommitCount
        versionName = gitCommitHash

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val localProperties = Properties().also {
        val properties = rootProject.file("local.properties")
        if (properties.exists())
            it.load(properties.inputStream())
    }

    val config = localProperties.getProperty("KEYSTORE_PATH")?.let {
        signingConfigs.create("release") {
            storeFile = file(it)
            storePassword = localProperties.getProperty("KEYSTORE_PASSWORD")
            keyAlias = localProperties.getProperty("KEY_ALIAS")
            keyPassword = localProperties.getProperty("KEY_PASSWORD")
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    val disableR8 = (project.findProperty("disableR8") as? String) == "true"

    buildTypes {
        all {
            signingConfig = config ?: signingConfigs["debug"]
        }
        release {
            isMinifyEnabled = !disableR8
            isShrinkResources = !disableR8
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packagingOptions.resources.excludes += setOf(
        "META-INF/**",
        "okhttp3/**",
        "kotlin/**",
        "org/**",
        "**.properties",
        "**.bin",
        "**/*.proto"
    )

    dependenciesInfo.includeInApk = false

    applicationVariants.configureEach {
        outputs.configureEach {
            if (baseName == "release")
                (this as? ApkVariantOutputImpl)?.outputFileName =
                    "c001apk-compose_$versionName($versionCode).apk"
        }
    }
}

ksp {
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}

protobuf {
    protoc {
        artifact = libs.google.protobuf.protoc.get().toString()
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                register("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)
    testImplementation(libs.junit)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewModel.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size.android)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.material3.adaptive.navigation.suite)

    implementation(libs.google.accompanist.drawablepainter)
    implementation(libs.google.android.material)
    implementation(libs.google.dagger.hilt.android)
    ksp(libs.google.dagger.hilt.android.compiler)
    implementation(libs.google.protobuf.kotlin.lite)

    implementation(libs.squareup.okhttp3.logging.interceptor)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.gson)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.jp.wasabeef.transformers.coil)
    implementation(libs.me.zhanghai.android.appiconloader.coil)

    implementation(libs.jbcrypt)
    implementation(libs.jsoup)
    implementation(libs.toolbar.compose)
    implementation(libs.oss.android.sdk)
    implementation(libs.material.kolor)

    implementation(project(":mojito"))
    implementation(project(":SketchImageViewLoader"))
    implementation("com.github.MikaelZero.mojito:coilimageloader:1.8.7") {
        exclude(group = "com.github.MikaelZero.mojito", module = "mojito")
    }
}
