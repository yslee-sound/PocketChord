package com.sweetapps.pocketchord.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase 서버 관리 콘텐츠 모델
 *
 * PocketChord 앱에서 Supabase는 다음 용도로 사용됩니다:
 * - 공지사항 (Announcements)
 * - 앱 업데이트 정보
 * - 배너 광고 설정
 * - 기타 서버에서 관리하는 동적 콘텐츠
 *
 * 참고: 코드 데이터는 로컬 Room DB에 저장되며 Supabase를 사용하지 않습니다.
 */

/**
 * 공지사항 모델
 *
 * Supabase 테이블 구조 (announcements):
 * - id: bigint (primary key, auto-increment)
 * - title: text (공지사항 제목)
 * - content: text (공지사항 내용, Markdown 지원)
 * - type: text (공지 유형: "info", "warning", "update", "event" 등)
 * - priority: integer (우선순위, 높을수록 먼저 표시)
 * - is_active: boolean (활성화 여부)
 * - start_date: timestamp (표시 시작 시간)
 * - end_date: timestamp (표시 종료 시간, null이면 무제한)
 * - image_url: text (이미지 URL, 선택 사항)
 * - link_url: text (클릭 시 이동할 URL, 선택 사항)
 * - created_at: timestamp
 * - updated_at: timestamp
 */
@Serializable
data class Announcement(
    @SerialName("id")
    val id: Long? = null,

    @SerialName("title")
    val title: String,

    @SerialName("content")
    val content: String,

    @SerialName("type")
    val type: AnnouncementType = AnnouncementType.INFO,

    @SerialName("priority")
    val priority: Int = 0,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("start_date")
    val startDate: String? = null,  // ISO 8601 format

    @SerialName("end_date")
    val endDate: String? = null,  // null이면 무제한

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("link_url")
    val linkUrl: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * 공지사항 타입
 */
@Serializable
enum class AnnouncementType {
    @SerialName("info")
    INFO,           // 일반 정보

    @SerialName("warning")
    WARNING,        // 주의사항

    @SerialName("update")
    UPDATE,         // 앱 업데이트

    @SerialName("event")
    EVENT,          // 이벤트

    @SerialName("maintenance")
    MAINTENANCE     // 점검 안내
}

/**
 * 앱 버전 정보 모델
 *
 * Supabase 테이블 구조 (app_versions):
 * - id: bigint (primary key)
 * - version_code: integer (앱 버전 코드)
 * - version_name: text (앱 버전명, 예: "1.2.0")
 * - min_required_version: integer (최소 요구 버전 코드)
 * - release_notes: text (업데이트 내용)
 * - is_force_update: boolean (강제 업데이트 여부)
 * - download_url: text (다운로드 URL)
 * - released_at: timestamp
 */
@Serializable
data class AppVersion(
    @SerialName("id")
    val id: Long? = null,

    @SerialName("version_code")
    val versionCode: Int,

    @SerialName("version_name")
    val versionName: String,

    @SerialName("min_required_version")
    val minRequiredVersion: Int,

    @SerialName("release_notes")
    val releaseNotes: String,

    @SerialName("is_force_update")
    val isForceUpdate: Boolean = false,

    @SerialName("download_url")
    val downloadUrl: String? = null,

    @SerialName("released_at")
    val releasedAt: String? = null
)

/**
 * 배너 설정 모델
 *
 * Supabase 테이블 구조 (banner_config):
 * - id: bigint (primary key)
 * - location: text (배너 위치: "home", "chord_detail", "search" 등)
 * - ad_unit_id: text (AdMob 광고 단위 ID)
 * - is_enabled: boolean (활성화 여부)
 * - refresh_interval: integer (새로고침 간격, 초 단위)
 * - created_at: timestamp
 * - updated_at: timestamp
 */
@Serializable
data class BannerConfig(
    @SerialName("id")
    val id: Long? = null,

    @SerialName("location")
    val location: String,

    @SerialName("ad_unit_id")
    val adUnitId: String,

    @SerialName("is_enabled")
    val isEnabled: Boolean = true,

    @SerialName("refresh_interval")
    val refreshInterval: Int = 60,  // 기본 60초

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)


