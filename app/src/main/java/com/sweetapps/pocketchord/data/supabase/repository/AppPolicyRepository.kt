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
        client.from("app_policy")
            .select()
            .decodeList<AppPolicy>()
            .firstOrNull { it.appId == appId && it.isActive }
    }
}

