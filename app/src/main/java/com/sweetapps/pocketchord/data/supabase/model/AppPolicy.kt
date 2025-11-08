package com.sweetapps.pocketchord.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 앱 정책 테이블 모델 (하이브리드 방식)
 * - 운영 테이블과 히스토리 테이블로 분리된 구조
 * - RLS로 is_active = TRUE인 정책만 조회 가능
 *
 * 테이블: app_policy
 * 주요 컬럼:
 *  - app_id (TEXT UNIQUE): 앱 식별자
 *  - is_active (BOOLEAN): 팝업 활성화 여부
 *  - active_popup_type (popup_type ENUM): 팝업 타입
 *    - 'emergency': 긴급 공지 (X 버튼 없음)
 *    - 'force_update': 강제 업데이트 (뒤로가기 차단)
 *    - 'optional_update': 선택적 업데이트 (닫기 가능)
 *    - 'notice': 일반 공지
 *    - 'none': 팝업 없음
 *  - content (TEXT): 팝업 메시지
 *  - download_url (TEXT): 다운로드/이동 URL
 *  - min_supported_version (INT): 강제 업데이트 기준 버전 (force_update 전용)
 *  - latest_version_code (INT): 권장 업데이트 버전 (optional_update 전용)
 *
 *  ===== 광고 제어 필드 =====
 *  - ad_app_open_enabled (BOOLEAN): 앱 오픈 광고 활성화 여부 (기본값: true)
 *  - ad_interstitial_enabled (BOOLEAN): 전면 광고 활성화 여부 (기본값: true)
 *  - ad_banner_enabled (BOOLEAN): 배너 광고 활성화 여부 (기본값: true)
 *  - ad_interstitial_max_per_hour (INT): 전면 광고 시간당 최대 횟수 (기본값: 3)
 *  - ad_interstitial_max_per_day (INT): 전면 광고 하루 최대 횟수 (기본값: 20)
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
    val isActive: Boolean = false,

    @SerialName("active_popup_type")
    val activePopupType: String = "none",  // 'emergency', 'force_update', 'optional_update', 'notice', 'none'

    @SerialName("content")
    val content: String? = null,

    @SerialName("download_url")
    val downloadUrl: String? = null,

    @SerialName("min_supported_version")
    val minSupportedVersion: Int? = null,

    @SerialName("latest_version_code")
    val latestVersionCode: Int? = null,

    // ===== 광고 제어 필드 =====
    @SerialName("ad_app_open_enabled")
    val adAppOpenEnabled: Boolean = true,

    @SerialName("ad_interstitial_enabled")
    val adInterstitialEnabled: Boolean = true,

    @SerialName("ad_banner_enabled")
    val adBannerEnabled: Boolean = true,

    // ===== 광고 빈도 제어 필드 =====
    @SerialName("ad_interstitial_max_per_hour")
    val adInterstitialMaxPerHour: Int = 3,

    @SerialName("ad_interstitial_max_per_day")
    val adInterstitialMaxPerDay: Int = 20
) {
    /**
     * 강제 업데이트가 필요한지 확인
     * @param currentVersionCode 현재 앱 버전 코드
     * @return true: 강제 업데이트 필요, false: 업데이트 불필요
     */
    fun requiresForceUpdate(currentVersionCode: Int): Boolean {
        if (activePopupType != "force_update") return false
        val min = minSupportedVersion ?: return false
        return currentVersionCode < min
    }

    /**
     * 선택적 업데이트 권장 여부 확인
     * @param currentVersionCode 현재 앱 버전 코드
     * @return true: 업데이트 권장, false: 권장하지 않음
     */
    fun recommendsUpdate(currentVersionCode: Int): Boolean {
        if (activePopupType != "optional_update") return false
        val latest = latestVersionCode ?: return false
        return currentVersionCode < latest
    }
}

