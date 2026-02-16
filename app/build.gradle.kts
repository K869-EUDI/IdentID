import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.secrets)
    alias(libs.plugins.androidx.room)
    id("kotlin-parcelize")
}

fun getLocalProperty(key: String): String? {
    return try {
        val properties = Properties().apply {
            load(rootProject.file("local.properties").reader())
        }
        properties[key] as? String
    } catch (_: Exception) {
        null
    }
}

android {
    namespace = "com.k689.identid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.k689.identid"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables { useSupportLibrary = true }

        manifestPlaceholders["appName"] = "IdentID"
        manifestPlaceholders["appNameSuffix"] = ""

        // Deep link schemes
        val walletScheme = "eudi-wallet"
        val eudiOpenId4VpScheme = "eudi-openid4vp"
        val mdocOpenId4VpScheme = "mdoc-openid4vp"
        val openId4VpScheme = "openid4vp"
        val haipOpenId4VpScheme = "haip-vp"
        val credentialOfferScheme = "openid-credential-offer"
        val credentialOfferHaipScheme = "haip-vci"
        val openId4VciAuthorizationScheme = "eu.europa.ec.euidi"
        val openId4VciAuthorizationHost = "authorization"
        val rqesScheme = "rqes"
        val rqesHost = "oauth"
        val rqesPath = "/callback"
        val rqesDocRetrievalScheme = "eudi-rqes"

        buildConfigField("String", "DEEPLINK", "\"$walletScheme://\"")
        buildConfigField("String", "EUDI_OPENID4VP_SCHEME", "\"$eudiOpenId4VpScheme\"")
        buildConfigField("String", "MDOC_OPENID4VP_SCHEME", "\"$mdocOpenId4VpScheme\"")
        buildConfigField("String", "OPENID4VP_SCHEME", "\"$openId4VpScheme\"")
        buildConfigField("String", "HAIP_OPENID4VP_SCHEME", "\"$haipOpenId4VpScheme\"")
        buildConfigField("String", "CREDENTIAL_OFFER_SCHEME", "\"$credentialOfferScheme\"")
        buildConfigField("String", "CREDENTIAL_OFFER_HAIP_SCHEME", "\"$credentialOfferHaipScheme\"")
        buildConfigField("String", "ISSUE_AUTHORIZATION_SCHEME", "\"$openId4VciAuthorizationScheme\"")
        buildConfigField("String", "ISSUE_AUTHORIZATION_HOST", "\"$openId4VciAuthorizationHost\"")
        buildConfigField("String", "ISSUE_AUTHORIZATION_DEEPLINK", "\"$openId4VciAuthorizationScheme://$openId4VciAuthorizationHost\"")
        buildConfigField("String", "RQES_SCHEME", "\"$rqesScheme\"")
        buildConfigField("String", "RQES_HOST", "\"$rqesHost\"")
        buildConfigField("String", "RQES_DEEPLINK", "\"$rqesScheme://$rqesHost$rqesPath\"")
        buildConfigField("String", "RQES_DOC_RETRIEVAL_SCHEME", "\"$rqesDocRetrievalScheme\"")
        buildConfigField("String", "APP_VERSION", "\"1.0.0\"")

        manifestPlaceholders["deepLinkScheme"] = walletScheme
        manifestPlaceholders["deepLinkHost"] = "*"
        manifestPlaceholders["eudiOpenid4vpScheme"] = eudiOpenId4VpScheme
        manifestPlaceholders["eudiOpenid4vpHost"] = "*"
        manifestPlaceholders["mdocOpenid4vpScheme"] = mdocOpenId4VpScheme
        manifestPlaceholders["mdocOpenid4vpHost"] = "*"
        manifestPlaceholders["openid4vpScheme"] = openId4VpScheme
        manifestPlaceholders["openid4vpHost"] = "*"
        manifestPlaceholders["haipOpenid4vpScheme"] = haipOpenId4VpScheme
        manifestPlaceholders["haipOpenid4vpHost"] = "*"
        manifestPlaceholders["credentialOfferHost"] = "*"
        manifestPlaceholders["credentialOfferScheme"] = credentialOfferScheme
        manifestPlaceholders["credentialOfferHaipHost"] = "*"
        manifestPlaceholders["credentialOfferHaipScheme"] = credentialOfferHaipScheme
        manifestPlaceholders["openId4VciAuthorizationScheme"] = openId4VciAuthorizationScheme
        manifestPlaceholders["openId4VciAuthorizationHost"] = openId4VciAuthorizationHost
        manifestPlaceholders["rqesHost"] = rqesHost
        manifestPlaceholders["rqesScheme"] = rqesScheme
        manifestPlaceholders["rqesPath"] = rqesPath
        manifestPlaceholders["rqesDocRetrievalScheme"] = rqesDocRetrievalScheme
        manifestPlaceholders["rqesDocRetrievalHost"] = "*"
    }

    val keystoreFile = file("${rootProject.projectDir}/sign")
    if (keystoreFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = keystoreFile
                keyAlias = getLocalProperty("androidKeyAlias") ?: System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = getLocalProperty("androidKeyPassword") ?: System.getenv("ANDROID_KEY_PASSWORD")
                storePassword = getLocalProperty("androidKeyPassword") ?: System.getenv("ANDROID_KEY_PASSWORD")
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
        }
    }

    lint {
        disable += "RestrictedApi"
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        resources.excludes.add("/META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        jniLibs.pickFirsts.addAll(
            listOf(
                "lib/arm64-v8a/libc++_shared.so",
                "lib/armeabi-v7a/libc++_shared.so",
                "lib/x86/libc++_shared.so",
                "lib/x86_64/libc++_shared.so",
            )
        )
    }

    // KSP generated sources for Koin
    sourceSets.all {
        kotlin.directories.add("build/generated/ksp/$name/kotlin")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
    ignoreList.add("sdk.*")
}

dependencies {
    // Desugaring
    coreLibraryDesugaring(libs.android.desugarJdkLibs)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.constraintlayout.compose)

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Koin DI
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp)

    // Coil (image loading)
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
    implementation(libs.coil.kt.svg)
    implementation(libs.coil.kt.network.okhttp)

    // Coroutines
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Ktor (networking)
    implementation(libs.ktor.android)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Room (database)
    implementation(libs.androidx.room)
    ksp(libs.androidx.room.ksp)

    // EUDI Wallet Core
    implementation(libs.eudi.wallet.core)

    // RQES UI SDK
    implementation(libs.rqes.ui.sdk)

    // Camera
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.camera2)

    // Biometric
    implementation(libs.androidx.biometric)

    // Auth
    implementation(libs.androidx.appAuth)

    // Work Manager
    implementation(libs.androidx.work.ktx)

    // Accompanist
    implementation(libs.accompanist.permissions)

    // QR Code
    implementation(libs.zxing)

    // JSON
    implementation(libs.gson)

    // Phone number
    implementation(libs.google.phonenumber)

    // Logging
    implementation(libs.timber)
    implementation(libs.treessence)

    // SLF4J (for Ktor logging)
    implementation(libs.slf4j)

    // Blur effect
    implementation(libs.compose.cloudy)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
