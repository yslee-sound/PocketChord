package com.sweetapps.pocketchord.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 앱 버전 정보 모델
 *
 * Supabase 테이블 구조 (app_versions):
 * ```sql
 * CREATE TABLE app_versions (
 *     id BIGSERIAL PRIMARY KEY,
 *     version_code INTEGER NOT NULL UNIQUE,
 *     version_name TEXT NOT NULL,
 *     min_required_version INTEGER NOT NULL,
 *     release_notes TEXT NOT NULL,
 *     is_force_update BOOLEAN DEFAULT false,
 *     download_url TEXT,
 *     released_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
 * )
 * ```
 *
 * 용도:
 * - 앱 업데이트 알림
 * - 강제 업데이트 제어
 * - 릴리즈 노트 표시
 *
 * 사용 예시:
 * ```kotlin
 * // 최신 버전 확인
 * val latestVersion = supabase.from("app_versions")
 *     .select()
 *     .order("version_code", descending = true)
 *     .limit(1)
 *     .decodeSingleOrNull<AppVersion>()
 *
 * // 업데이트 필요 여부 확인
 * if (latestVersion?.isForceUpdate == true &&
 *     BuildConfig.VERSION_CODE < latestVersion.versionCode) {
 *     showForceUpdateDialog()
 * }
 * ```
 */
@Serializable
data class AppVersion(
    /**
     * 버전 정보 ID (자동 생성)
     */
    @SerialName("id")
    val id: Long? = null,

    /**
     * 앱 버전 코드 (build.gradle.kts의 versionCode)
     * 예: 1, 2, 3, ...
     * 숫자가 클수록 최신 버전
     */
    @SerialName("version_code")
    val versionCode: Int,

    /**
     * 앱 버전명 (사용자에게 표시)
     * 예: "1.0.0", "1.2.3", "2.0.0-beta"
     */
    @SerialName("version_name")
    val versionName: String,

    /**
     * 최소 요구 버전 코드
     * 이 버전보다 낮으면 업데이트 권장 메시지 표시
     */
    @SerialName("min_required_version")
    val minRequiredVersion: Int,

    /**
     * 릴리즈 노트 (업데이트 내용)
     * Markdown 형식 지원 가능
     * 예: "- 새로운 코드 추가\n- 버그 수정\n- 성능 개선"
     */
    @SerialName("release_notes")
    val releaseNotes: String,

    /**
     * 강제 업데이트 여부
     * - true: 앱 사용 불가, 반드시 업데이트 필요
     * - false: 선택적 업데이트 (나중에 하기 가능)
     */
    @SerialName("is_force_update")
    val isForceUpdate: Boolean = false,

    /**
     * 다운로드 URL (선택사항)
     * Google Play Store URL 또는 APK 직접 다운로드 링크
     * 예: "https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord"
     */
    @SerialName("download_url")
    val downloadUrl: String? = null,

    /**
     * 릴리즈 시간 (ISO 8601 형식)
     * 예: "2025-11-05T10:30:00Z"
     */
    @SerialName("released_at")
    val releasedAt: String? = null
)

