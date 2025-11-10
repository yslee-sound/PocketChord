package com.sweetapps.pocketchord.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.sweetapps.pocketchord.BuildConfig
import com.sweetapps.pocketchord.PocketChordApplication
import com.sweetapps.pocketchord.data.supabase.repository.AdPolicyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 앱 오프닝 광고 관리 클래스
 * - 앱 시작 시 또는 백그라운드에서 돌아올 때 광고 표시
 * - 콜드 스타트와 웜 스타트 모두 지원
 * - Supabase AdPolicy로 실시간 ON/OFF 제어
 */
class AppOpenAdManager(
    private val application: Application
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    companion object {
        private const val TAG = "AppOpenAdManager"

        // 광고 표시 간격 (밀리초)
        private const val AD_TIMEOUT_MS = 4 * 60 * 60 * 1000L // 4시간
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    private var isFirstLaunch = true // 첫 실행 여부

    // Supabase 광고 정책 조회용 (AdPolicy로 변경)
    private val adPolicyRepository: AdPolicyRepository by lazy {
        AdPolicyRepository((application as PocketChordApplication).supabase)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // 앱 시작 시 광고 미리 로드
        loadAd()
    }

    /**
     * Supabase 광고 정책에서 앱 오픈 광고 활성화 여부 확인
     */
    private suspend fun isAppOpenEnabledFromPolicy(): Boolean {
        val policy = adPolicyRepository.getPolicy().getOrNull()

        // 정책이 없으면 기본값 true (Supabase 장애 대응)
        if (policy == null) {
            android.util.Log.d(TAG, "[정책] 정책 없음 - 기본값(true) 사용")
            return true
        }

        // is_active가 false이면 모든 광고 비활성화
        if (!policy.isActive) {
            android.util.Log.d(TAG, "[정책] is_active = false - 모든 광고 비활성화")
            return false
        }

        // is_active = true일 때만 개별 플래그 확인
        val enabled = policy.adAppOpenEnabled
        android.util.Log.d(TAG, "[정책] 앱 오픈 광고 ${if (enabled) "활성화" else "비활성화"}")
        return enabled
    }

    /**
     * 앱 오프닝 광고를 로드합니다
     */
    private fun loadAd() {

        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            application,
            BuildConfig.APP_OPEN_AD_UNIT_ID,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "앱 오프닝 광고 로드 성공")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = java.util.Date().time
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "앱 오프닝 광고 로드 실패: ${loadAdError.message}")
                    isLoadingAd = false
                }
            }
        )
    }

    /**
     * 광고가 사용 가능한지 확인
     */
    private fun isAdAvailable(): Boolean {
        // 광고가 로드된 후 4시간이 지났으면 만료된 것으로 간주
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }

    /**
     * 광고 로드 시간이 4시간 이내인지 확인
     */
    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference = java.util.Date().time - loadTime
        val numMilliSecondsPerHour = 3600000L
        return dateDifference < numMilliSecondsPerHour * 4
    }

    /**
     * 광고를 표시합니다
     * - Supabase 정책 확인 후 표시 여부 결정
     */
    fun showAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit = {}) {
        // 이미 광고를 표시 중이면 무시
        if (isShowingAd) {
            Log.d(TAG, "이미 광고를 표시 중입니다")
            return
        }

        // Supabase 정책 확인
        scope.launch {
            try {
                val isEnabledFromPolicy = isAppOpenEnabledFromPolicy()

                Log.d(TAG, "🔍 앱 오픈 광고 정책 확인:")
                Log.d(TAG, "  - Supabase 정책: ${if (isEnabledFromPolicy) "활성화" else "비활성화"}")

                // Supabase 정책에서 비활성화되어 있으면 표시하지 않음
                if (!isEnabledFromPolicy) {
                    Log.d(TAG, "❌ Supabase 정책: 앱 오픈 광고 비활성화")
                    onAdDismissed()
                    return@launch
                }

                // 정책에서 활성화되어 있으면 광고 표시
                if (!isAdAvailable()) {
                    Log.d(TAG, "광고를 사용할 수 없습니다. 로드를 시도합니다")
                    loadAd()
                    onAdDismissed()
                    return@launch
                }

                showAdNow(activity, onAdDismissed)
            } catch (e: Exception) {
                Log.e(TAG, "광고 정책 확인 중 오류: ${e.message}")
                // 오류 발생 시 기본값(활성화)으로 동작
                if (isAdAvailable()) {
                    showAdNow(activity, onAdDismissed)
                } else {
                    onAdDismissed()
                }
            }
        }
    }

    /**
     * 광고를 즉시 표시합니다
     */
    private fun showAdNow(activity: Activity, onAdDismissed: () -> Unit) {
        Log.d(TAG, "앱 오프닝 광고를 표시합니다")

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "광고가 닫혔습니다")
                appOpenAd = null
                isShowingAd = false
                // Application에 광고가 닫혔음을 알림
                (application as? PocketChordApplication)?.setAppOpenAdShowing(false)
                onAdDismissed()
                loadAd() // 다음 광고 미리 로드 (스위치 상태에 따라 로드)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "광고 표시 실패: ${adError.message}")
                appOpenAd = null
                isShowingAd = false
                // Application에 광고가 닫혔음을 알림
                (application as? PocketChordApplication)?.setAppOpenAdShowing(false)
                onAdDismissed()
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "광고가 표시되었습니다")
                isShowingAd = true
                // Application에 광고가 표시되었음을 알림
                (application as? PocketChordApplication)?.setAppOpenAdShowing(true)
            }
        }

        appOpenAd?.show(activity)
    }

    // LifecycleObserver - 앱이 포그라운드로 올 때
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        // 첫 실행 체크 (콜드 스타트 시 광고 표시하지 않음)
        if (isFirstLaunch) {
            Log.d(TAG, "첫 실행이므로 광고를 표시하지 않습니다")
            isFirstLaunch = false
            loadAd() // 다음을 위한 로드
            return
        }

        // 백그라운드에서 복귀 시 광고 표시
        currentActivity?.let { activity ->
            Log.d(TAG, "앱이 포그라운드로 왔습니다 (백그라운드에서 복귀)")
            showAdIfAvailable(activity)
        }
    }

    // ActivityLifecycleCallbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // 스플래시 화면에서는 광고를 표시하지 않음
        if (!isShowingAd && !activity.javaClass.simpleName.contains("Splash")) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }
}
