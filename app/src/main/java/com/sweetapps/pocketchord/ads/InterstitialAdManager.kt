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

/**
 * 전면광고 관리 클래스
 * - 광고 로딩과 노출 빈도를 자동으로 관리
 * - 사용자 경험을 위해 일정 간격을 두고 노출
 * - 릴리즈 빌드에서는 항상 활성화됨
 */
class InterstitialAdManager(private val context: Context) {

    companion object {
        private const val TAG = "InterstitialAdManager"
        // AdMob 테스트 전면광고 ID
        private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

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

    init {
        // 앱 시작 시 광고 미리 로드
        loadAd()
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
            TEST_AD_UNIT_ID,
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
        if (!shouldShowAd()) {
            return false
        }

        interstitialAd?.show(activity)
        screenTransitionCount = 0 // 카운터 리셋
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

