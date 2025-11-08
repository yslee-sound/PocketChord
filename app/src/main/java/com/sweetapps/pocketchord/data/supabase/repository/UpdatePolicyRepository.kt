package com.sweetapps.pocketchord.data.supabase.repository

import com.sweetapps.pocketchord.data.supabase.model.UpdatePolicy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * 업데이트 정책 Repository
 *
 * Supabase의 update_policy 테이블과 통신하여
 * 업데이트 정책 데이터를 조회하는 역할을 담당합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val repository = UpdatePolicyRepository(supabase, "com.sweetapps.pocketchord")
 * val policy = repository.getPolicy().getOrNull()
 *
 * policy?.let {
 *     when {
 *         it.requiresForceUpdate(currentVersion) -> {
 *             // 강제 업데이트 팝업 표시
 *         }
 *         it.recommendsOptionalUpdate(currentVersion) -> {
 *             // 선택적 업데이트 팝업 표시
 *         }
 *     }
 * }
 * ```
 */
class UpdatePolicyRepository(
    private val client: SupabaseClient,
    private val appId: String = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID
) {
    /**
     * 활성 업데이트 정책 조회
     *
     * @return 활성화된 업데이트 정책, 없으면 null
     */
    suspend fun getPolicy(): Result<UpdatePolicy?> = runCatching {
        client.from("update_policy")
            .select()
            .decodeList<UpdatePolicy>()
            .find { it.appId == appId && it.isActive }
    }

    /**
     * 특정 ID의 업데이트 정책 조회
     *
     * @param id 정책 ID
     * @return 해당 정책, 없으면 null
     */
    suspend fun getPolicyById(id: Long): Result<UpdatePolicy?> = runCatching {
        client.from("update_policy")
            .select()
            .decodeList<UpdatePolicy>()
            .find { it.id == id && it.appId == appId }
    }
}

