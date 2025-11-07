package com.sweetapps.pocketchord.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 중앙 정책 테이블 모델 (선택지 B)
 * - 앱 전역 팝업/업데이트 정책을 단일 레코드로 제어합니다.
 *
 * 예상 테이블: app_policy
 * 주요 컬럼:
 *  - app_id (TEXT)
 *  - is_active (BOOLEAN)
 *  - created_at (TIMESTAMPTZ)
 *  - min_supported_version (INT) : 현재 버전보다 작으면 강제X, 크면 강제 업데이트
 *  - latest_version_code (INT)   : 선택적 업데이트 비교 기준
 *  - update_is_active (BOOLEAN)  : 선택적 업데이트 토글
 *  - download_url (TEXT)         : 업데이트 버튼 이동 URL (선택)
 *  - emergency_is_active (BOOLEAN)
 *  - emergency_title (TEXT)
 *  - emergency_content (TEXT)
 *  - emergency_dismissible (BOOLEAN)
 *  - emergency_redirect_url (TEXT)
 *  - notice_is_active (BOOLEAN)  : (옵션) 공지를 정책에서 직접 노출하고 싶을 때
 *  - notice_title (TEXT)
 *  - notice_content (TEXT)
 */
@Serializable
data class AppPolicy(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("app_id")
    val appId: String,
    @SerialName("is_active")
    val isActive: Boolean = true,

    // 업데이트 관련
    @SerialName("min_supported_version")
    val minSupportedVersion: Int? = null,
    @SerialName("latest_version_code")
    val latestVersionCode: Int? = null,
    @SerialName("update_is_active")
    val updateIsActive: Boolean = false,
    @SerialName("download_url")
    val downloadUrl: String? = null,

    // 긴급 공지
    @SerialName("emergency_is_active")
    val emergencyIsActive: Boolean = false,
    @SerialName("emergency_title")
    val emergencyTitle: String? = null,
    @SerialName("emergency_content")
    val emergencyContent: String? = null,
    @SerialName("emergency_dismissible")
    val emergencyDismissible: Boolean = false,
    @SerialName("emergency_redirect_url")
    val emergencyRedirectUrl: String? = null,

    // (선택) 정책 기반 공지
    @SerialName("notice_is_active")
    val noticeIsActive: Boolean? = null,
    @SerialName("notice_title")
    val noticeTitle: String? = null,
    @SerialName("notice_content")
    val noticeContent: String? = null
) {
    fun requiresForceUpdate(currentVersionCode: Int): Boolean {
        val min = minSupportedVersion ?: return false
        return currentVersionCode < min
    }
}

