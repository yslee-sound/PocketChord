package com.sweetapps.pocketchord.data.supabase.repository

import com.sweetapps.pocketchord.data.supabase.model.AppVersion
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * 앱 버전 정보 Repository
 *
 * Supabase의 app_versions 테이블과 통신하여
 * 앱 업데이트 관련 정보를 조회하는 역할을 담당합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val repository = UpdateInfoRepository(supabase)
 * val updateNeeded = repository.checkUpdateRequired(BuildConfig.VERSION_CODE)
 * ```
 */
class UpdateInfoRepository(
    private val client: SupabaseClient
) {
    /**
     * 최신 버전 정보 조회
     *
     * @return 가장 최신 버전 정보, 없으면 null
     */
    suspend fun getLatestVersion(): Result<AppVersion?> = runCatching {
        client.from("app_versions")
            .select()
            .decodeList<AppVersion>()
            .maxByOrNull { it.versionCode }
    }

    /**
     * 업데이트 필요 여부 확인
     *
     * @param currentVersionCode 현재 앱의 버전 코드
     * @return 업데이트가 필요하면 새 버전 정보, 아니면 null
     */
    suspend fun checkUpdateRequired(currentVersionCode: Int): Result<AppVersion?> = runCatching {
        val latest = client.from("app_versions")
            .select()
            .decodeList<AppVersion>()
            .maxByOrNull { it.versionCode }

        // 최신 버전이 현재 버전보다 높으면 반환
        if (latest != null && latest.versionCode > currentVersionCode) {
            latest
        } else {
            null
        }
    }

    /**
     * 강제 업데이트 필요 여부 확인
     *
     * @param currentVersionCode 현재 앱의 버전 코드
     * @return 강제 업데이트가 필요하면 true
     */
    suspend fun isForceUpdateRequired(currentVersionCode: Int): Result<Boolean> = runCatching {
        val latest = client.from("app_versions")
            .select()
            .decodeList<AppVersion>()
            .maxByOrNull { it.versionCode }

        latest != null &&
                latest.versionCode > currentVersionCode &&
                latest.isForceUpdate
    }

    /**
     * 특정 버전 정보 조회
     *
     * @param versionCode 조회할 버전 코드
     * @return 해당 버전 정보, 없으면 null
     */
    suspend fun getVersionByCode(versionCode: Int): Result<AppVersion?> = runCatching {
        client.from("app_versions")
            .select()
            .decodeList<AppVersion>()
            .find { it.versionCode == versionCode }
    }

    /**
     * 모든 버전 목록 조회 (최신순)
     *
     * @param limit 조회할 개수 (기본 10개)
     * @return 버전 정보 리스트
     */
    suspend fun getVersionHistory(limit: Int = 10): Result<List<AppVersion>> = runCatching {
        client.from("app_versions")
            .select()
            .decodeList<AppVersion>()
            .sortedByDescending { it.versionCode }
            .take(limit)
    }
}


