import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

apply(plugin = "kotlin-kapt")

android {
    namespace = "com.sweetapps.pocketchord"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sweetapps.pocketchord"
        minSdk = 26
        targetSdk = 36
        versionCode = 3  // 버그 수정: C#-Db 루트 매핑 문제 해결
        versionName = "1.0.3"  // 버그 수정 버전

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 모든 빌드 타입에 공통으로 적용되는 BuildConfig
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("int", "VERSION_CODE", "${versionCode}")

        // Supabase 접속 정보: 환경변수 → local.properties → gradle.properties 순으로 조회
        val props = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        val supabaseUrl = System.getenv("SUPABASE_URL")
            ?: props.getProperty("SUPABASE_URL")
            ?: (project.findProperty("SUPABASE_URL") as String?)
            ?: ""
        val supabaseAnonKey = System.getenv("SUPABASE_ANON_KEY")
            ?: props.getProperty("SUPABASE_ANON_KEY")
            ?: (project.findProperty("SUPABASE_ANON_KEY") as String?)
            ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseAnonKey}\"")
    }

    // 서명 설정 (환경변수 기반)
    signingConfigs {
        // 환경변수에서 서명 정보 읽기
        val keystorePath = System.getenv("KEYSTORE_PATH")
        val keystoreStorePw = System.getenv("KEYSTORE_STORE_PW")
        val keyAlias = System.getenv("KEY_ALIAS")
        val keyPw = System.getenv("KEY_PASSWORD")

        // 환경변수가 모두 있을 때만 release config 생성
        if (keystorePath != null && keystoreStorePw != null && keyAlias != null && keyPw != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystoreStorePw
                this.keyAlias = keyAlias
                keyPassword = keyPw
            }
        } else {
            // 환경변수 누락 시 경고
            println("⚠️ WARNING: Release signing config not available!")
            println("Required environment variables:")
            println("  - KEYSTORE_PATH")
            println("  - KEYSTORE_STORE_PW")
            println("  - KEY_ALIAS")
            println("  - KEY_PASSWORD")
            println("Debug builds will work, but release builds will use debug signing.")
        }
    }

    buildTypes {
        debug {
            // 디버그 빌드용 Supabase app_id
            buildConfigField(
                "String",
                "SUPABASE_APP_ID",
                "\"com.sweetapps.pocketchord.debug\""
            )

            // 빌드 타입 정보
            buildConfigField("String", "BUILD_TYPE", "\"debug\"")

            // 테스트 광고 ID (Google 제공)
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "APP_OPEN_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/9257395921\"")

            // 디버그 식별자 추가 (선택사항)
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }

        release {
            // 릴리즈 빌드용 Supabase app_id
            buildConfigField(
                "String",
                "SUPABASE_APP_ID",
                "\"com.sweetapps.pocketchord\""
            )

            // 빌드 타입 정보
            buildConfigField("String", "BUILD_TYPE", "\"release\"")

            // 실제 광고 ID (환경변수 또는 local.properties에서 설정)
            // ⚠️ 출시 전 반드시 실제 AdMob 광고 ID로 교체해야 함!
            val props = Properties().apply {
                val f = rootProject.file("local.properties")
                if (f.exists()) f.inputStream().use { load(it) }
            }
            val bannerAdId = System.getenv("BANNER_AD_UNIT_ID")
                ?: props.getProperty("BANNER_AD_UNIT_ID")
                ?: "ca-app-pub-3940256099942544/6300978111"  // 임시: 테스트 ID (교체 필요!)
            val interstitialAdId = System.getenv("INTERSTITIAL_AD_UNIT_ID")
                ?: props.getProperty("INTERSTITIAL_AD_UNIT_ID")
                ?: "ca-app-pub-3940256099942544/1033173712"  // 임시: 테스트 ID (교체 필요!)
            val appOpenAdId = System.getenv("APP_OPEN_AD_UNIT_ID")
                ?: props.getProperty("APP_OPEN_AD_UNIT_ID")
                ?: "ca-app-pub-3940256099942544/9257395921"  // 임시: 테스트 ID (교체 필요!)

            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"${bannerAdId}\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"${interstitialAdId}\"")
            buildConfigField("String", "APP_OPEN_AD_UNIT_ID", "\"${appOpenAdId}\"")

            // 서명 설정 적용 (환경변수가 있을 때만, 없으면 디버그 서명 사용)
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")

            // 코드 난독화 및 최적화
            isMinifyEnabled = true
            isShrinkResources = true

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Fragment 라이브러리 (최신 버전으로 명시적 선언)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.fragment.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // 아이콘: core + extended 모두 추가
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.6.0")
    // Room (explicit coordinates to ensure availability)
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    if (configurations.findByName("kapt") == null) configurations.create("kapt")
    configurations.getByName("kapt").dependencies.add(project.dependencies.create("androidx.room:room-compiler:2.8.3"))
    // Gson
    implementation("com.google.code.gson:gson:2.13.2")

    // Google Mobile Ads SDK (AdMob)
    implementation("com.google.android.gms:play-services-ads:22.6.0")

    // Lifecycle Process for App Open Ads
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")

    // Coil Compose for image loading in NoticeDialog
    implementation("io.coil-kt:coil-compose:2.6.0")

    // supabase 관련
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)

    // AndroidX SplashScreen: 시스템 스플래시 화면 지원 (Android 12+ 및 하위 호환)
    implementation("androidx.core:core-splashscreen:1.0.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}