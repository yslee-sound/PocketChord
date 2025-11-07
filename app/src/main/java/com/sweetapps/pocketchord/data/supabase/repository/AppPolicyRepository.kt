package com.sweetapps.pocketchord.data.supabase.repository

import com.sweetapps.pocketchord.data.supabase.model.AppPolicy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * AppPolicy Repository: 앱 정책 조회
 * - RLS 정책에 의해 is_active = TRUE인 정책만 조회 가능
 */
class AppPolicyRepository(
    private val client: SupabaseClient,
    private val appId: String = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID
) {
    /**
     * 현재 활성화된 앱 정책 조회
     * @return 정책이 있으면 AppPolicy, 없으면 null
     */
    suspend fun getPolicy(): Result<AppPolicy?> = runCatching {
        android.util.Log.d("AppPolicyRepo", "===== Policy Fetch Started =====")
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
}
