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
import java.util.Date

/**
 * 앱 오프닝 광고 관리 클래스
 * - 앱 시작 시 또는 백그라운드에서 돌아올 때 광고 표시
 * - 콜드 스타트와 웜 스타트 모두 지원
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

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // 앱 시작 시 광고 미리 로드 (스위치 ON/OFF 무관하게)
        loadAd(force = true)
    }

    private fun isAppOpenEnabled(): Boolean {
        val adPrefs = application.getSharedPreferences("ads_prefs", android.content.Context.MODE_PRIVATE)
        return adPrefs.getBoolean("app_open_test_mode", false)
    }

    /**
     * 앱 오프닝 광고를 로드합니다
     * @param force true이면 스위치 상태와 무관하게 로드 시도
     */
    private fun loadAd(force: Boolean = false) {
        // 스위치가 꺼져 있으면 로드하지 않음 (단, 강제 로드시에는 예외)
        if (!force && !isAppOpenEnabled()) {
            Log.d(TAG, "앱 오프닝 광고 비활성화됨: 로드하지 않음")
            return
        }

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
     */
    fun showAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit = {}) {
        // 스위치가 꺼져 있으면 표시하지 않음 (미리 로드된 광고는 보존)
        if (!isAppOpenEnabled()) {
            Log.d(TAG, "앱 오프닝 광고 비활성화됨: 표시하지 않음")
            onAdDismissed()
            return
        }

        // 테스트 모드 확인 (ON일 때 정책 무시)
        val adPrefs = application.getSharedPreferences("ads_prefs", android.content.Context.MODE_PRIVATE)
        val isTestMode = adPrefs.getBoolean("app_open_test_mode", false)

        // 이미 광고를 표시 중이면 무시
        if (isShowingAd) {
            Log.d(TAG, "이미 광고를 표시 중입니다")
            return
        }

        // 테스트 모드일 때는 정책 무시하고 광고 강제 표시
        if (isTestMode) {
            Log.d(TAG, "🧪 테스트 모드: 정책 무시하고 광고 강제 표시")
            if (appOpenAd != null) {
                showAdNow(activity, onAdDismissed)
            } else {
                Log.d(TAG, "광고가 로드되지 않았습니다. 로드를 시도합니다")
                loadAd()
                onAdDismissed()
            }
            return
        }

        // 일반 모드: 정책에 맞춰 표시
        if (!isAdAvailable()) {
            Log.d(TAG, "광고를 사용할 수 없습니다. 로드를 시도합니다")
            loadAd()
            onAdDismissed()
            return
        }

        showAdNow(activity, onAdDismissed)
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

        // 스위치가 꺼져 있으면 아무 것도 하지 않음 (미리 로드된 광고는 보존)
        if (!isAppOpenEnabled()) {
            Log.d(TAG, "앱 오프닝 광고 비활성화됨: onStart에서 작업 없음")
            isFirstLaunch = false
            return
        }

        // 테스트 모드 확인
        val adPrefs = application.getSharedPreferences("ads_prefs", android.content.Context.MODE_PRIVATE)
        val isTestMode = adPrefs.getBoolean("app_open_test_mode", false)

        // 테스트 모드가 아닐 때만 첫 실행 체크
        if (!isTestMode && isFirstLaunch) {
            Log.d(TAG, "첫 실행이므로 광고를 표시하지 않습니다")
            isFirstLaunch = false
            // 필요 시 다음을 위한 로드 (스위치가 ON일 때만)
            loadAd()
            return
        }

        // 테스트 모드일 때는 첫 실행도 무시
        if (isTestMode && isFirstLaunch) {
            Log.d(TAG, "🧪 테스트 모드: 첫 실행이지만 광고를 시도합니다")
            isFirstLaunch = false
        }

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
