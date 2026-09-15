import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.properties.Properties
import java.util.zip.CRC32

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.protobuf)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.plugin.compose)
    alias(libs.plugins.kotlin.parcelize)
}

// 版本号全部从 git 推导：
//   main / master  →  versionName = <短hash>
//   其它分支       →  versionName = <短hash>-<分支名>[-dirty]
// versionCode 固定取提交数，保证单调递增（换分支构建也能正常覆盖安装）；
// 未提交改动只体现在 versionName 上，这样带改动构建出来的产物名不会互相覆盖。
fun git(vararg args: String) = providers.exec {
    commandLine("git", *args)
}.standardOutput.asText.map { it.trim() }

val gitCommitCount = git("rev-list", "HEAD", "--count").map { it.toInt() }.get()
val gitCommitHash = git("rev-parse", "--verify", "--short", "HEAD").get()
val gitBranch = git("rev-parse", "--abbrev-ref", "HEAD").get()

// 未提交改动的「内容指纹」，用来生成 rc 号：同一份改动得到同一个号，内容一变号就变，
// 于是能直接看出这次构建的产物有没有带上前一次没有的未提交改动。
// 输入取两样：
//   1) git diff HEAD —— 已跟踪文件的真实改动（含内容）。本仓库 git status 会报几百个
//      只有换行符差异的文件，但那些不进 diff，所以不用额外过滤。
//   2) 源码模块目录下未跟踪的新文件（连同内容），避免新增源文件不进指纹。
val workingTreeRc = run {
    val diffText = git("diff", "HEAD").get()
    val untrackedText = git(
        "ls-files", "--others", "--exclude-standard", "--",
        "app", "mojito", "SketchImageViewLoader", "media-support"
    ).get()
    if (diffText.isEmpty() && untrackedText.isEmpty()) {
        null
    } else {
        val crc = CRC32()
        crc.update(diffText.toByteArray())
        untrackedText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .sorted()
            .forEach { path ->
                crc.update(path.toByteArray())
                val f = rootProject.file(path)
                // 只吃小文件的内容，避免误改大文件把配置阶段拖慢
                if (f.isFile && f.length() < 2L * 1024 * 1024) crc.update(f.readBytes())
            }
        crc.value % 1_000_000
    }
}

val isMainBranch = gitBranch == "main" || gitBranch == "master"
// 分支名可能带 / 等字符，产物文件名里要清掉
val branchTag = gitBranch.replace(Regex("[^A-Za-z0-9._-]"), "-")
val buildVersionName = buildString {
    append(gitCommitHash)
    if (!isMainBranch) {
        append('-').append(branchTag)
        // 没有未提交改动时不带 rc 号
        workingTreeRc?.let { append(".rc").append(it.toString().padStart(6, '0')) }
    }
}

android {
    namespace = "com.example.c001apk.compose"
    compileSdk = 35
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.example.c001apk.compose"
        minSdk = 24
        targetSdk = 35
        versionCode = gitCommitCount
        versionName = buildVersionName
        // 开发日志开关：只在非 main / master 分支的构建里打开。
        // main 上 DevLog 的调用会被 R8 整段剪掉，不产生字符串拼接与 IO 开销。
        buildConfigField("boolean", "DEV_LOG", (!isMainBranch).toString())

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

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
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
    implementation("androidx.lifecycle:lifecycle-runtime:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.5")
    implementation("androidx.savedstate:savedstate:1.2.1")
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
    implementation(project(":media-support"))
    implementation("com.github.MikaelZero.mojito:coilimageloader:1.8.7") {
        exclude(group = "com.github.MikaelZero.mojito", module = "mojito")
    }
}
