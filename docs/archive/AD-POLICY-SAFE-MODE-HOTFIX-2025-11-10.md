# Ad Policy 안전 모드 핫픽스

**날짜**: 2025-11-10  
**버전**: v3.0 (핫픽스)  
**상태**: ✅ 완료

---

## 📋 개요

### 발견된 문제
`ad_policy` 테이블에서 `is_active = false`로 설정했지만 광고가 계속 표시되는 문제 발견

### 원인 분석

1. **RLS(Row Level Security) 정책**
   - Supabase의 RLS 정책이 `USING (is_active = true)`로 설정됨
   - `is_active = false`인 행은 SELECT 결과에서 필터링됨

2. **앱의 기본값 처리**
   - 정책이 없을 때(null) 기본값을 `true`로 설정
   - 결과적으로 "정책 없음" = "광고 활성화"로 동작

3. **문제 로그**
   ```
   AdPolicyRepo: Total rows fetched: 1
   AdPolicyRepo: ⚠️ 활성화된 광고 정책 없음 (기본값 사용)
   → 광고가 계속 표시됨
   ```

---

## 🔧 적용된 수정

### 수정 내용
정책이 없거나 조회 실패 시 **안전 모드(광고 비활성화)**로 동작하도록 변경

### 수정된 파일

#### 1. InterstitialAdManager.kt
```kotlin
// 변경 전
private suspend fun isInterstitialEnabledFromPolicy(): Boolean {
    return adPolicyRepository.getPolicy()
        .getOrNull()
        ?.adInterstitialEnabled
        ?: true  // 정책 조회 실패 시 기본값 true
}

// 변경 후
private suspend fun isInterstitialEnabledFromPolicy(): Boolean {
    return adPolicyRepository.getPolicy()
        .getOrNull()
        ?.adInterstitialEnabled
        ?: false  // 정책 조회 실패 시 안전 모드(광고 비활성화)
}
```

#### 2. AppOpenAdManager.kt
```kotlin
// 변경 전
private suspend fun isAppOpenEnabledFromPolicy(): Boolean {
    return adPolicyRepository.getPolicy()
        .getOrNull()
        ?.adAppOpenEnabled
        ?: true  // 정책 조회 실패 시 기본값 true
}

// 변경 후
private suspend fun isAppOpenEnabledFromPolicy(): Boolean {
    return adPolicyRepository.getPolicy()
        .getOrNull()
        ?.adAppOpenEnabled
        ?: false  // 정책 조회 실패 시 안전 모드(광고 비활성화)
}
```

#### 3. MainActivity.kt
```kotlin
// 변경 전
val newBannerEnabled = adPolicy?.adBannerEnabled ?: true

// 변경 후
val newBannerEnabled = adPolicy?.adBannerEnabled ?: false  // 정책 조회 실패 시 안전 모드(광고 비활성화)
```

---

## ✅ 검증

### 빌드 확인
```bash
gradlew assembleDebug
```
**결과**: ✅ BUILD SUCCESSFUL

### 설치 확인
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
**결과**: ✅ Success

---

## 🎯 현재 동작

### 케이스별 동작

| 상황 | 이전 동작 | 현재 동작 |
|------|-----------|-----------|
| `is_active = true` | ✅ 광고 표시 | ✅ 광고 표시 |
| `is_active = false` | ❌ 광고 표시됨 (버그) | ✅ 광고 비활성화 |
| 정책 없음 (null) | ❌ 광고 표시됨 | ✅ 광고 비활성화 (안전 모드) |
| 네트워크 오류 | ❌ 광고 표시됨 | ✅ 광고 비활성화 (안전 모드) |

### 긴급 광고 끄기

**Supabase SQL**:
```sql
-- 모든 광고 즉시 비활성화
UPDATE ad_policy
SET is_active = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

**예상 로그**:
```
AdPolicyRepo: Total rows fetched: 1
AdPolicyRepo: ⚠️ 활성화된 광고 정책 없음 (기본값 사용)
InterstitialAdManager: [정책] 전면 광고 비활성화됨
AppOpenAdManager: [정책] 앱 오픈 광고 비활성화됨
MainActivity: 🎯 배너 광고 정책: 비활성화
```

---

## 📌 장점

### 1. 안전성 향상
- ✅ 정책 조회 실패 시 안전 모드로 전환
- ✅ 긴급 상황에서 광고 즉시 비활성화 가능

### 2. 보안 개선
- ✅ RLS 정책 변경 없이 해결
- ✅ 앱 레벨에서 안전하게 처리

### 3. 운영 편의성
- ✅ Supabase에서 `is_active = false` 설정만으로 즉시 효과
- ✅ 캐시 만료 대기 불필요 (RLS가 필터링)

---

## 🚀 배포 상태

### Debug 빌드
- [x] ✅ 코드 수정 완료
- [x] ✅ 빌드 성공
- [x] ✅ APK 설치 완료
- [ ] 🔄 실제 동작 테스트 대기

### Release 빌드
- [ ] Release 빌드 예정
- [ ] 서명 확인 예정
- [ ] Play Store 업로드 예정

---

## 📝 관련 문서

- **[RELEASE-TEST-PHASE5-RELEASE.md](../release/RELEASE-TEST-PHASE5-RELEASE.md)** - Ad Policy 테스트 가이드
- **[RELEASE-TEST-CHECKLIST.md](../release/RELEASE-TEST-CHECKLIST.md)** - 전체 릴리즈 체크리스트

---

## 🔍 후속 조치

### 권장 사항
1. **테스트 완료 후 Release 빌드**
2. **문서 업데이트**
   - Phase 5 테스트 문서에 핫픽스 내용 반영 ✅ 완료
3. **모니터링**
   - Logcat에서 광고 정책 로그 확인
   - 실제 광고 표시 여부 검증

### 선택 사항 (장기)
1. **RLS 정책 개선**
   - `USING (app_id = current_setting('app.app_id')::text)` 같은 조건 추가
   - 역할(role) 기반 접근 제어

2. **모니터링 강화**
   - Supabase 대시보드에서 정책 변경 이력 추적
   - 앱에서 정책 로딩 실패율 모니터링

---

**작성자**: AI Assistant  
**검토자**: _____________  
**승인 일시**: _____________


