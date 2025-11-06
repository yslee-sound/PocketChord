package com.sweetapps.pocketchord.data.supabase.repository

import com.sweetapps.pocketchord.data.supabase.model.UpdateInfo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * 앱 버전 정보 Repository
 *
 * Supabase의 update_info 테이블과 통신하여
 * 앱 업데이트 관련 정보를 조회하는 역할을 담당합니다.
 */
class UpdateInfoRepository(
    private val client: SupabaseClient
) {
    /**
     * 최신 버전 정보 조회
     */
    suspend fun getLatestVersion(): Result<UpdateInfo?> = runCatching {
        client.from("update_info")
            .select()
            .decodeList<UpdateInfo>()
            .maxByOrNull { it.versionCode }
    }

    /**
     * 업데이트 필요 여부 확인
     */
    suspend fun checkUpdateRequired(currentVersionCode: Int): Result<UpdateInfo?> = runCatching {
        val list = client.from("update_info")
            .select()
            .decodeList<UpdateInfo>()

        val latest = list.maxByOrNull { it.versionCode }
        if (latest != null && latest.versionCode > currentVersionCode) latest else null
    }

    /**
     * 강제 업데이트 필요 여부 확인
     */
    suspend fun isForceUpdateRequired(currentVersionCode: Int): Result<Boolean> = runCatching {
        val latest = client.from("update_info")
            .select()
            .decodeList<UpdateInfo>()
            .maxByOrNull { it.versionCode }

        latest != null && latest.versionCode > currentVersionCode && latest.isForce
    }

    /**
     * 특정 버전 정보 조회
     */
    suspend fun getVersionByCode(versionCode: Int): Result<UpdateInfo?> = runCatching {
        client.from("update_info")
            .select()
            .decodeList<UpdateInfo>()
            .find { it.versionCode == versionCode }
    }

    /**
     * 모든 버전 목록 조회 (최신순)
     */
    suspend fun getVersionHistory(limit: Int = 10): Result<List<UpdateInfo>> = runCatching {
        client.from("update_info")
            .select()
            .decodeList<UpdateInfo>()
            .sortedByDescending { it.versionCode }
            .take(limit)
    }
}
