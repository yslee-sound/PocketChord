package com.sweetapps.pocketchord

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.sweetapps.pocketchord.ads.AppOpenAdManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PocketChord Application 클래스
 * - AdMob 초기화
 * - 앱 오프닝 광고 매니저 초기화
 * - Supabase 클라이언트 전역 초기화/제공
 */
class PocketChordApplication : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager

    // 앱 오프닝 광고 표시 상태
    private val _isShowingAppOpenAd = MutableStateFlow(false)
    val isShowingAppOpenAd: StateFlow<Boolean> = _isShowingAppOpenAd.asStateFlow()

    fun setAppOpenAdShowing(isShowing: Boolean) {
        _isShowingAppOpenAd.value = isShowing
    }

    // Supabase 전역 클라이언트
    lateinit var supabase: SupabaseClient
        private set

    // Supabase 설정 여부 노출 (URL/KEY 모두 유효할 때만 true)
    var isSupabaseConfigured: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()

        // AdMob 초기화
        MobileAds.initialize(this) { initializationStatus ->
            android.util.Log.d("PocketChordApp", "AdMob 초기화 완료: ${initializationStatus.adapterStatusMap}")
        }

        // 앱 오프닝 광고 매니저 초기화
        appOpenAdManager = AppOpenAdManager(this)

        // Supabase 초기화
        // BuildConfig에 필드가 없을 경우에도 컴파일 가능하도록 리플렉션으로 안전하게 읽기
        val url = runCatching {
            BuildConfig::class.java.getField("SUPABASE_URL").get(null) as String
        }.getOrElse { "" }
        val anon = runCatching {
            BuildConfig::class.java.getField("SUPABASE_ANON_KEY").get(null) as String
        }.getOrElse { "" }

        isSupabaseConfigured = url.isNotBlank() && anon.isNotBlank()
        if (!isSupabaseConfigured) {
            android.util.Log.w("PocketChordApp", "Supabase 미설정: 환경변수 SUPABASE_URL / SUPABASE_ANON_KEY 를 확인하세요")
        } else {
            android.util.Log.i("PocketChordApp", "Supabase configured: url set (키는 로그에 출력하지 않습니다)")
        }

        supabase = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anon
        ) {
            install(Postgrest)
        }

        if (BuildConfig.DEBUG) {
            android.util.Log.d("PocketChordApp", "Supabase URL (debug)='${url}'")
        }
    }
}
