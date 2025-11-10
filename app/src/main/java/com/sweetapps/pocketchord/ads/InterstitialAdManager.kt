package com.sweetapps.pocketchord.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.sweetapps.pocketchord.BuildConfig
import com.sweetapps.pocketchord.PocketChordApplication
import com.sweetapps.pocketchord.data.supabase.repository.AdPolicyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 전면광고 관리 클래스
 * - 광고 로딩과 노출 빈도를 자동으로 관리
 * - 사용자 경험을 위해 일정 간격을 두고 노출
 * - Supabase AdPolicy로 실시간 ON/OFF 및 빈도 제어
 */
class InterstitialAdManager(private val context: Context) {

    companion object {
        private const val TAG = "InterstitialAdManager"

        // 광고 노출 간격 (초 단위)
        private const val AD_INTERVAL_SECONDS = 60 // 1분마다

        // 광고를 표시하기 전 최소 화면 전환 횟수
        private const val MIN_SCREEN_TRANSITIONS = 3
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastAdShowTime = 0L
    private var screenTransitionCount = 0

    private val sharedPreferences = context.getSharedPreferences("interstitial_ad_prefs", Context.MODE_PRIVATE)

    // Supabase 광고 정책 조회용 (AdPolicy로 변경)
    private val adPolicyRepository: AdPolicyRepository by lazy {
        val app = context.applicationContext as PocketChordApplication
        AdPolicyRepository(app.supabase)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        // 앱 시작 시 광고 미리 로드
        loadAd()
        // 마지막 광고 표시 시간 복원
        lastAdShowTime = sharedPreferences.getLong("last_ad_show_time", 0)
    }

    /**
     * Supabase 광고 정책에서 전면 광고 활성화 여부 확인
     */
    private suspend fun isInterstitialEnabledFromPolicy(): Boolean {
        val policy = adPolicyRepository.getPolicy().getOrNull()

        // 정책이 없으면 기본값 true (Supabase 장애 대응)
        if (policy == null) {
            Log.d(TAG, "[정책] 정책 없음 - 기본값(true) 사용")
            return true
        }

        // is_active가 false이면 모든 광고 비활성화
        if (!policy.isActive) {
            Log.d(TAG, "[정책] is_active = false - 모든 광고 비활성화")
            return false
        }

        // is_active = true일 때만 개별 플래그 확인
        val enabled = policy.adInterstitialEnabled
        Log.d(TAG, "[정책] 전면 광고 ${if (enabled) "활성화" else "비활성화"}")
        return enabled
    }

    /**
     * 빈도 제한 체크 (시간당/일일)
     */
    private suspend fun checkFrequencyLimit(): Boolean {
        val currentTime = System.currentTimeMillis()

        // 시간당 카운트 체크
        val hourlyCount = sharedPreferences.getInt("ad_count_hourly", 0)
        val lastHourReset = sharedPreferences.getLong("last_hour_reset", 0)

        // 1시간(3600초)이 지났으면 리셋
        if (currentTime - lastHourReset > 3600000) {
            sharedPreferences.edit {
                putInt("ad_count_hourly", 0)
                putLong("last_hour_reset", currentTime)
            }
            Log.d(TAG, "⏰ 시간당 카운트 리셋")
        }

        // 일일 카운트 체크
        val dailyCount = sharedPreferences.getInt("ad_count_daily", 0)
        val lastDayReset = sharedPreferences.getLong("last_day_reset", 0)

        // 24시간이 지났으면 리셋
        if (currentTime - lastDayReset > 86400000) {
            sharedPreferences.edit {
                putInt("ad_count_daily", 0)
                putLong("last_day_reset", currentTime)
            }
            Log.d(TAG, "📅 일일 카운트 리셋")
        }

        // 광고 정책에서 최대값 가져오기
        val adPolicy = adPolicyRepository.getPolicy().getOrNull()
        val maxPerHour = adPolicy?.adInterstitialMaxPerHour ?: 2  // 보수적 기본값
        val maxPerDay = adPolicy?.adInterstitialMaxPerDay ?: 15   // 보수적 기본값

        // 시간당 제한 체크
        if (sharedPreferences.getInt("ad_count_hourly", 0) >= maxPerHour) {
            Log.d(TAG, "⚠️ 시간당 빈도 제한 초과: ${hourlyCount}/${maxPerHour}")
            return false
        }

        // 일일 제한 체크
        if (sharedPreferences.getInt("ad_count_daily", 0) >= maxPerDay) {
            Log.d(TAG, "⚠️ 일일 빈도 제한 초과: ${dailyCount}/${maxPerDay}")
            return false
        }

        Log.d(TAG, "✅ 빈도 제한 통과: 시간당 ${hourlyCount}/${maxPerHour}, 일일 ${dailyCount}/${maxPerDay}")
        return true
    }

    /**
     * 빈도 카운트 증가
     */
    private fun incrementFrequencyCount() {
        val hourlyCount = sharedPreferences.getInt("ad_count_hourly", 0)
        val dailyCount = sharedPreferences.getInt("ad_count_daily", 0)
        sharedPreferences.edit {
            putInt("ad_count_hourly", hourlyCount + 1)
            putInt("ad_count_daily", dailyCount + 1)
        }
        Log.d(TAG, "📊 광고 카운트 증가: 시간당 ${hourlyCount + 1}, 일일 ${dailyCount + 1}")
    }

    /**
     * 전면광고를 로드합니다
     */
    private fun loadAd() {
        if (isLoading || interstitialAd != null) {
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            BuildConfig.INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "전면광고 로드 성공")
                    interstitialAd = ad
                    isLoading = false
                    setupAdCallbacks(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "전면광고 로드 실패: ${loadAdError.message}")
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * 광고 콜백 설정
     */
    private fun setupAdCallbacks(ad: InterstitialAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "전면광고 닫힘")
                interstitialAd = null
                // 광고가 닫힌 후 다음 광고를 미리 로드
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "전면광고 표시 실패: ${adError.message}")
                interstitialAd = null
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "전면광고 표시됨")
                lastAdShowTime = System.currentTimeMillis()
                saveLastAdShowTime()
            }
        }
    }

    /**
     * 화면 전환을 기록합니다
     */
    fun recordScreenTransition() {
        screenTransitionCount++
        Log.d(TAG, "화면 전환 카운트: $screenTransitionCount")
    }

    /**
     * 광고를 표시할지 여부를 결정합니다
     */
    private fun shouldShowAd(): Boolean {
        // 광고가 로드되지 않았으면 표시 불가
        if (interstitialAd == null) {
            Log.d(TAG, "광고가 로드되지 않음")
            return false
        }

        // 마지막 광고 표시로부터 충분한 시간이 지났는지 확인
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - lastAdShowTime) / 1000
        if (elapsedSeconds < AD_INTERVAL_SECONDS) {
            Log.d(TAG, "광고 간격 미달: ${elapsedSeconds}초/${AD_INTERVAL_SECONDS}초")
            return false
        }

        // 최소 화면 전환 횟수를 확인
        if (screenTransitionCount < MIN_SCREEN_TRANSITIONS) {
            Log.d(TAG, "화면 전환 횟수 미달: ${screenTransitionCount}/${MIN_SCREEN_TRANSITIONS}")
            return false
        }

        return true
    }

    /**
     * 전면광고를 표시합니다
     * @param activity 광고를 표시할 Activity
     * @return 광고가 표시되었는지 여부
     */
    fun showAd(activity: Activity): Boolean {
        // 기존 조건 체크
        if (!shouldShowAd()) {
            return false
        }

        // Supabase 정책 및 빈도 제한 체크 (블로킹)
        var shouldShow = false
        runBlocking {
            // 1. 정책 확인
            val enabled = isInterstitialEnabledFromPolicy()
            if (!enabled) {
                Log.d(TAG, "❌ Supabase 정책: 전면 광고 비활성화")
                return@runBlocking
            }

            // 2. 빈도 제한 확인
            if (!checkFrequencyLimit()) {
                Log.d(TAG, "⚠️ 빈도 제한: 광고 표시 안 함")
                return@runBlocking
            }

            shouldShow = true
        }

        if (!shouldShow) {
            return false
        }

        // 광고 표시
        interstitialAd?.show(activity)
        screenTransitionCount = 0 // 카운터 리셋
        incrementFrequencyCount() // 빈도 카운트 증가
        return true
    }

    /**
     * 특정 조건에서 광고를 시도합니다 (화면 전환 자동 기록 포함)
     */
    fun tryShowAd(activity: Activity) {
        recordScreenTransition()
        if (showAd(activity)) {
            Log.d(TAG, "전면광고 표시됨")
        } else {
            Log.d(TAG, "전면광고 표시 조건 미달")
        }
    }

    private fun saveLastAdShowTime() {
        sharedPreferences.edit {
            putLong("last_ad_show_time", lastAdShowTime)
        }
    }
}

