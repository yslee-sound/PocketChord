package com.sweetapps.pocketchord.data.supabase.repository

import com.sweetapps.pocketchord.data.supabase.model.EmergencyPolicy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * 긴급 상황 정책 Repository
 *
 * Supabase의 emergency_policy 테이블과 통신하여
 * 긴급 상황 데이터를 조회하는 역할을 담당합니다.
 *
 * 특징:
 * - 최우선순위: 다른 모든 팝업보다 먼저 확인
 * - 추적 없음: 매번 표시 (사용자가 조치 취할 때까지)
 * - Google Play 준수: is_dismissible로 X 버튼 제어
 *
 * 사용 예시:
 * ```kotlin
 * val repository = EmergencyPolicyRepository(supabase, "com.sweetapps.pocketchord")
 * val emergency = repository.getActiveEmergency().getOrNull()
 *
 * emergency?.let {
 *     // 긴급 상황 발생!
 *     showEmergencyDialog = true
 *     return  // 다른 팝업 무시
 * }
 * ```
 */
class EmergencyPolicyRepository(
    private val client: SupabaseClient,
    private val appId: String = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID
) {
    /**
     * 활성 긴급 정책 조회
     *
     * @return 활성화된 긴급 정책, 없으면 null
     */
    suspend fun getActiveEmergency(): Result<EmergencyPolicy?> = runCatching {
        client.from("emergency_policy")
            .select()
            .decodeList<EmergencyPolicy>()
            .find { it.appId == appId && it.isActive }
    }

    /**
     * 특정 ID의 긴급 정책 조회
     *
     * @param id 정책 ID
     * @return 해당 정책, 없으면 null
     */
    suspend fun getEmergencyById(id: Long): Result<EmergencyPolicy?> = runCatching {
        client.from("emergency_policy")
            .select()
            .decodeList<EmergencyPolicy>()
            .find { it.id == id && it.appId == appId }
    }
}

