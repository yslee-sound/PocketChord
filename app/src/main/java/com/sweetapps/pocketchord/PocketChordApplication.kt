package com.sweetapps.pocketchord

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.sweetapps.pocketchord.ads.AppOpenAdManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PocketChord Application 클래스
 * - AdMob 초기화
 * - 앱 오프닝 광고 매니저 초기화
 */
class PocketChordApplication : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager

    // 앱 오프닝 광고 표시 상태
    private val _isShowingAppOpenAd = MutableStateFlow(false)
    val isShowingAppOpenAd: StateFlow<Boolean> = _isShowingAppOpenAd.asStateFlow()

    fun setAppOpenAdShowing(isShowing: Boolean) {
        _isShowingAppOpenAd.value = isShowing
    }

    override fun onCreate() {
        super.onCreate()

        // AdMob 초기화
        MobileAds.initialize(this) { initializationStatus ->
            android.util.Log.d("PocketChordApp", "AdMob 초기화 완료: ${initializationStatus.adapterStatusMap}")
        }

        // 앱 오프닝 광고 매니저 초기화
        appOpenAdManager = AppOpenAdManager(this)
    }
}

