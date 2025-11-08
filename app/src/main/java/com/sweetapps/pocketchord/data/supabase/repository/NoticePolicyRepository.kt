package com.sweetapps.pocketchord.data.supabase.repository

import com.sweetapps.pocketchord.data.supabase.model.NoticePolicy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * 공지사항 정책 Repository
 *
 * Supabase의 notice_policy 테이블과 통신하여
 * 공지사항 데이터를 조회하는 역할을 담당합니다.
 *
 * 특징:
 * - 우선순위 3: emergency, update 다음
 * - 버전 관리: notice_version으로 재표시 제어
 * - 명시적 제어: 오타 수정 vs 새 공지 구분 가능
 *
 * 사용 예시:
 * ```kotlin
 * val repository = NoticePolicyRepository(supabase, "com.sweetapps.pocketchord")
 * val notice = repository.getActiveNotice().getOrNull()
 *
 * notice?.let {
 *     // 버전 기반 추적
 *     val identifier = "notice_v${it.noticeVersion}"
 *     val prefs = context.getSharedPreferences("notice_prefs", Context.MODE_PRIVATE)
 *     val viewedVersions = prefs.getStringSet("viewed_notices", setOf()) ?: setOf()
 *
 *     if (!viewedVersions.contains(identifier)) {
 *         // 새 버전 → 공지 표시
 *         showNoticeDialog(it)
 *
 *         // X 클릭 시 저장
 *         viewedVersions.add(identifier)
 *         prefs.edit { putStringSet("viewed_notices", viewedVersions) }
 *     }
 * }
 * ```
 */
class NoticePolicyRepository(
    private val client: SupabaseClient,
    private val appId: String = com.sweetapps.pocketchord.BuildConfig.SUPABASE_APP_ID
) {
    /**
     * 활성 공지 정책 조회
     *
     * @return 활성화된 공지 정책, 없으면 null
     */
    suspend fun getActiveNotice(): Result<NoticePolicy?> = runCatching {
        client.from("notice_policy")
            .select()
            .decodeList<NoticePolicy>()
            .find { it.appId == appId && it.isActive }
    }

    /**
     * 특정 ID의 공지 정책 조회
     *
     * @param id 정책 ID
     * @return 해당 정책, 없으면 null
     */
    suspend fun getNoticeById(id: Long): Result<NoticePolicy?> = runCatching {
        client.from("notice_policy")
            .select()
            .decodeList<NoticePolicy>()
            .find { it.id == id && it.appId == appId }
    }
}

