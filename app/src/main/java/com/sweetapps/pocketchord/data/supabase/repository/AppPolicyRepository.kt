package com.sweetapps.pocketchord.data.supabase.repository

import com.sweetapps.pocketchord.data.supabase.model.AppPolicy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * AppPolicy Repository: 앱 정책 조회
 * - RLS 정책에 의해 is_active = TRUE인 정책만 조회 가능
 * - 5분 캐싱으로 네트워크 요청 최소화
 */
class AppPolicyRepository(
    private val client: SupabaseClient,
    private val appId: String = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID
) {
    companion object {
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5분
    }

    private var cachedPolicy: AppPolicy? = null
    private var cacheTimestamp: Long = 0

    /**
     * 현재 활성화된 앱 정책 조회 (5분 캐싱)
     * @return 정책이 있으면 AppPolicy, 없으면 null
     */
    suspend fun getPolicy(): Result<AppPolicy?> = runCatching {
        val currentTime = System.currentTimeMillis()

        // 캐시가 유효하면 캐시 사용
        if (cachedPolicy != null && currentTime - cacheTimestamp < CACHE_DURATION_MS) {
            val remainingSeconds = (CACHE_DURATION_MS - (currentTime - cacheTimestamp)) / 1000
            android.util.Log.d("AppPolicyRepo", "📦 캐시된 정책 사용 (유효 시간: ${remainingSeconds}초 남음)")
            return@runCatching cachedPolicy
        }

        // 캐시 만료 또는 없음 → Supabase에서 새로 가져오기
        android.util.Log.d("AppPolicyRepo", "===== Policy Fetch Started =====")
        android.util.Log.d("AppPolicyRepo", "🔄 Supabase에서 정책 새로 가져오기")
        android.util.Log.d("AppPolicyRepo", "Target app_id: $appId")
        
        // Supabase filter가 작동하지 않는 문제로 인해 전체 조회 후 클라이언트에서 필터링
        val allPolicies = client.from("app_policy")
            .select()
            .decodeList<AppPolicy>()

        android.util.Log.d("AppPolicyRepo", "Total rows fetched: ${allPolicies.size}")

        // 클라이언트에서 필터링
        val policy = allPolicies.firstOrNull { it.appId == appId && it.isActive }

        if (policy != null) {
            android.util.Log.d("AppPolicyRepo", "✅ Policy found:")
            android.util.Log.d("AppPolicyRepo", "  - id: ${policy.id}")
            android.util.Log.d("AppPolicyRepo", "  - app_id: ${policy.appId}")
            android.util.Log.d("AppPolicyRepo", "  - is_active: ${policy.isActive}")
            android.util.Log.d("AppPolicyRepo", "  - active_popup_type: ${policy.activePopupType}")
            android.util.Log.d("AppPolicyRepo", "  - content: ${policy.content?.take(50)}...")
            android.util.Log.d("AppPolicyRepo", "  - download_url: ${policy.downloadUrl}")
            android.util.Log.d("AppPolicyRepo", "🔍 광고 정책:")
            android.util.Log.d("AppPolicyRepo", "  - App Open: ${policy.adAppOpenEnabled}")
            android.util.Log.d("AppPolicyRepo", "  - Interstitial: ${policy.adInterstitialEnabled}")
            android.util.Log.d("AppPolicyRepo", "  - Banner: ${policy.adBannerEnabled}")
            android.util.Log.d("AppPolicyRepo", "  - Interstitial 시간당 최대: ${policy.adInterstitialMaxPerHour}회")
            android.util.Log.d("AppPolicyRepo", "  - Interstitial 하루 최대: ${policy.adInterstitialMaxPerDay}회")

            // 캐시 저장
            cachedPolicy = policy
            cacheTimestamp = currentTime
        } else {
            android.util.Log.w("AppPolicyRepo", "❌ No policy found!")
            android.util.Log.w("AppPolicyRepo", "All app_ids in database:")
            allPolicies.forEach {
                android.util.Log.w("AppPolicyRepo", "  - '${it.appId}' (active=${it.isActive})")
            }
            android.util.Log.w("AppPolicyRepo", "Looking for: '$appId'")
        }
        
        android.util.Log.d("AppPolicyRepo", "===== Policy Fetch Completed =====")
        policy
    }

    /**
     * 캐시 강제 초기화 (필요 시 호출)
     */
    fun clearCache() {
        cachedPolicy = null
        cacheTimestamp = 0
        android.util.Log.d("AppPolicyRepo", "🗑️ 정책 캐시 초기화")
    }
}
