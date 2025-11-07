package com.sweetapps.pocketchord.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * rpc_get_app_popup 반환 JSON 매핑 모델
 */
@Serializable
data class PopupDecision(
    @SerialName("type") val type: String? = null, // emergency | force_update | optional_update | notice

    // 공지 계열
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("dismissible") val dismissible: Boolean? = null,
    @SerialName("redirect_url") val redirectUrl: String? = null,

    // 업데이트 계열
    @SerialName("is_force") val isForce: Boolean? = null,
    @SerialName("version_code") val versionCode: Int? = null,
    @SerialName("download_url") val downloadUrl: String? = null
) {
    val kind: String get() = type?.lowercase()?.trim() ?: ""
}

