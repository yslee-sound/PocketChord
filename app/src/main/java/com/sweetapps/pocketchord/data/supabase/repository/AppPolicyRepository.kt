package com.sweetapps.pocketchord.data.supabase.repository

import com.sweetapps.pocketchord.data.supabase.model.AppPolicy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * AppPolicy Repository: 단일 정책 레코드 조회
 */
class AppPolicyRepository(
    private val client: SupabaseClient,
    private val appId: String = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID
) {
    suspend fun getPolicy(): Result<AppPolicy?> = runCatching {
        val filtered = client.from("app_policy")
            .select {
                filter {
                    eq("app_id", appId)
                    // RLS가 is_active = TRUE 만 허용하므로 별도 boolean 필터는 생략
                }
                limit(1)
            }
            .decodeList<AppPolicy>()
        android.util.Log.d("AppPolicyRepo", "Filtered (by app_id only) count=${filtered.size} for appId='${appId}'")
        if (filtered.isNotEmpty()) return@runCatching filtered.first()

        val all = client.from("app_policy").select().decodeList<AppPolicy>()
        val ids = all.joinToString { p -> "${p.appId}(active=${p.isActive},len=${p.appId.length})" }
        android.util.Log.d("AppPolicyRepo", "Fallback scan total=${all.size} ids=[${ids}] target='${appId}' len=${appId.length}")
        val local = all.firstOrNull { it.appId == appId && it.isActive }
        if (local == null) {
            android.util.Log.d("AppPolicyRepo", "No matching row even after local filter. Check app_id exact text & hidden spaces.")
        } else {
            android.util.Log.d("AppPolicyRepo", "Local match found after fallback: appId='${local.appId}'")
        }
        local
    }
}
