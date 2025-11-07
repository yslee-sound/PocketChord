package com.sweetapps.pocketchord.debug

import android.util.Log
import com.sweetapps.pocketchord.BuildConfig
import com.sweetapps.pocketchord.data.supabase.repository.AppPolicyRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Supabase 연결 테스트 유틸리티
 *
 * 사용법:
 * 1. MainActivity onCreate에서 호출:
 *    SupabaseDebugTest.testConnection(this)
 *
 * 2. 로그 확인:
 *    adb logcat -s SupabaseTest:*
 */
object SupabaseDebugTest {

    fun testConnection(context: android.content.Context) {
        Log.d("SupabaseTest", "========================================")
        Log.d("SupabaseTest", "Supabase Connection Test Started")
        Log.d("SupabaseTest", "========================================")

        // BuildConfig 확인
        Log.d("SupabaseTest", "BuildConfig.SUPABASE_URL: ${BuildConfig.SUPABASE_URL}")
        Log.d("SupabaseTest", "BuildConfig.SUPABASE_ANON_KEY: ${BuildConfig.SUPABASE_ANON_KEY.take(20)}...")
        Log.d("SupabaseTest", "BuildConfig.SUPABASE_APP_ID: ${BuildConfig.SUPABASE_APP_ID}")
        Log.d("SupabaseTest", "BuildConfig.VERSION_CODE: ${BuildConfig.VERSION_CODE}")

        // Supabase 클라이언트 생성
        val client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
        }

        Log.d("SupabaseTest", "✅ Supabase client created")

        // 비동기 테스트
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. AppPolicy로 직접 쿼리 테스트
                Log.d("SupabaseTest", "----------------------------------------")
                Log.d("SupabaseTest", "Test 1: Direct Query with AppPolicy")
                Log.d("SupabaseTest", "----------------------------------------")

                val allPolicies = client.from("app_policy")
                    .select()
                    .decodeList<com.sweetapps.pocketchord.data.supabase.model.AppPolicy>()

                Log.d("SupabaseTest", "Total rows in app_policy: ${allPolicies.size}")
                allPolicies.forEachIndexed { index, policy ->
                    Log.d("SupabaseTest", "Row $index:")
                    Log.d("SupabaseTest", "  - id: ${policy.id}")
                    Log.d("SupabaseTest", "  - app_id: '${policy.appId}'")
                    Log.d("SupabaseTest", "  - is_active: ${policy.isActive}")
                    Log.d("SupabaseTest", "  - active_popup_type: ${policy.activePopupType}")
                    Log.d("SupabaseTest", "  - content: ${policy.content?.take(50)}...")
                }

                // 2. 필터링 쿼리 테스트
                Log.d("SupabaseTest", "----------------------------------------")
                Log.d("SupabaseTest", "Test 2: Filtered Query (app_id)")
                Log.d("SupabaseTest", "----------------------------------------")

                val targetAppId = BuildConfig.SUPABASE_APP_ID
                Log.d("SupabaseTest", "Looking for app_id: '$targetAppId'")

                val filtered = client.from("app_policy")
                    .select {
                        filter {
                            eq("app_id", targetAppId)
                        }
                    }
                    .decodeList<com.sweetapps.pocketchord.data.supabase.model.AppPolicy>()

                Log.d("SupabaseTest", "Filtered rows: ${filtered.size}")
                if (filtered.isEmpty()) {
                    Log.w("SupabaseTest", "❌ No rows found for app_id='$targetAppId'")
                    Log.w("SupabaseTest", "Possible reasons:")
                    Log.w("SupabaseTest", "  1. Wrong app_id in database")
                    Log.w("SupabaseTest", "  2. Network error")
                    Log.w("SupabaseTest", "  3. Supabase connection issue")
                } else {
                    filtered.forEach { policy ->
                        Log.d("SupabaseTest", "✅ Found:")
                        Log.d("SupabaseTest", "  - id: ${policy.id}")
                        Log.d("SupabaseTest", "  - app_id: '${policy.appId}'")
                        Log.d("SupabaseTest", "  - is_active: ${policy.isActive}")
                        Log.d("SupabaseTest", "  - active_popup_type: ${policy.activePopupType}")
                    }
                }

                // 3. Repository 테스트
                Log.d("SupabaseTest", "----------------------------------------")
                Log.d("SupabaseTest", "Test 3: AppPolicyRepository")
                Log.d("SupabaseTest", "----------------------------------------")

                val repository = AppPolicyRepository(client, targetAppId)
                repository.getPolicy().onSuccess { policy ->
                    if (policy != null) {
                        Log.d("SupabaseTest", "✅ Repository returned policy:")
                        Log.d("SupabaseTest", "  - id: ${policy.id}")
                        Log.d("SupabaseTest", "  - app_id: ${policy.appId}")
                        Log.d("SupabaseTest", "  - is_active: ${policy.isActive}")
                        Log.d("SupabaseTest", "  - active_popup_type: ${policy.activePopupType}")
                    } else {
                        Log.w("SupabaseTest", "❌ Repository returned null")
                    }
                }.onFailure { e ->
                    Log.e("SupabaseTest", "❌ Repository error: ${e.message}", e)
                }

            } catch (e: Exception) {
                Log.e("SupabaseTest", "❌ Test failed: ${e.message}", e)
                e.printStackTrace()
            }

            Log.d("SupabaseTest", "========================================")
            Log.d("SupabaseTest", "Supabase Connection Test Completed")
            Log.d("SupabaseTest", "========================================")
        }
    }
}

