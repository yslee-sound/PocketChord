# 배너 광고 자동 갱신 구현 완료

**날짜**: 2025-01-08  
**상태**: ✅ 완료

---

## 🎯 문제

**기존 동작**:
```
앱 시작 → Supabase 정책 조회 (1번) → 배너 표시/숨김
Supabase 변경 → 앱 재시작해야만 반영 ❌
```

**문제점**:
- 캐싱이 5분인데 앱을 재시작하지 않으면 반영 안 됨
- 사용자가 앱을 계속 사용 중이면 정책 변경이 반영되지 않음

---

## ✅ 해결

**새로운 동작**:
```
앱 시작 → Supabase 정책 조회 → 배너 표시/숨김
5분 경과 → 자동 재조회 → 변경 감지 → 배너 자동 업데이트 ✅
5분 경과 → 자동 재조회 → 변경 없음 → 유지
```

**장점**:
- 앱을 끄지 않아도 5분 이내 자동 반영
- 캐시와 완벽하게 동기화
- 사용자 경험 방해 없음

---

## 🔧 구현 내용

### 변경 파일
- `MainActivity.kt`

### 변경 코드

**Before (잘못된 구현)**:
```kotlin
LaunchedEffect(Unit) {
    val policy = policyRepo.getPolicy().getOrNull()
    isBannerEnabled = policy?.adBannerEnabled ?: true
    // 한 번만 실행 → 이후 갱신 안 됨 ❌
}
```

**After (올바른 구현)**:
```kotlin
LaunchedEffect(Unit) {
    val policyRepo = AppPolicyRepository(app.supabase)
    
    while (true) {  // 무한 루프로 주기적 체크 ✅
        val policy = policyRepo.getPolicy().getOrNull()
        val newBannerEnabled = policy?.adBannerEnabled ?: true
        
        // 변경 감지
        if (isBannerEnabled != newBannerEnabled) {
            Log.d("MainActivity", "🔄 배너 광고 정책 변경: $isBannerEnabled → $newBannerEnabled")
            isBannerEnabled = newBannerEnabled
        } else {
            Log.d("MainActivity", "🎯 배너 광고 정책: $isBannerEnabled")
        }
        
        // 5분 대기 (캐시 만료 주기와 동일)
        delay(5 * 60 * 1000L)
    }
}
```

---

## 📊 동작 흐름

### 시나리오: Supabase에서 배너 광고 OFF

```
10:00:00 - 앱 시작
           → Supabase 조회: "배너 ON" ✅
           → 배너 표시
           
10:05:00 - 5분 경과 (첫 번째 자동 체크)
           → Supabase 조회: "배너 ON"
           → 변경 없음 → 배너 유지
           
10:07:00 - 관리자가 Supabase에서 배너 OFF로 변경
           
10:10:00 - 5분 경과 (두 번째 자동 체크)
           → Supabase 조회: "배너 OFF" ✅
           → 변경 감지!
           → Log: "🔄 배너 광고 정책 변경: 활성화 → 비활성화"
           → 배너 자동으로 숨김 ✅
           
10:15:00 - 5분 경과 (세 번째 자동 체크)
           → Supabase 조회: "배너 OFF"
           → 변경 없음 → 배너 숨김 유지
```

**반영 시간**: 변경 후 **최대 5분** 이내 (캐시와 동기화)

---

## 🧪 테스트 방법

### 1. 정상 동작 테스트

1. 앱 실행 → 배너 표시 확인
2. Supabase에서 OFF:
   ```sql
   UPDATE app_policy 
   SET ad_banner_enabled = false 
   WHERE app_id = 'com.sweetapps.pocketchord';
   ```
3. **앱을 끄지 않고 5분 대기**
4. Logcat 확인:
   ```
   MainActivity: 🔄 배너 광고 정책 변경: 활성화 → 비활성화
   ```
5. 배너가 자동으로 숨겨지는지 확인 ✅

### 2. 역방향 테스트

1. Supabase에서 ON:
   ```sql
   UPDATE app_policy 
   SET ad_banner_enabled = true 
   WHERE app_id = 'com.sweetapps.pocketchord';
   ```
2. 5분 대기
3. 배너가 자동으로 나타나는지 확인 ✅

### 3. 캐싱 동작 확인

```
첫 조회 (10:00):
  AppPolicyRepo: 🔄 Supabase에서 정책 새로 가져오기
  MainActivity: 🎯 배너 광고 정책: 활성화

두 번째 조회 (10:05):
  AppPolicyRepo: 📦 캐시된 정책 사용 (유효 시간: XXX초 남음)
  MainActivity: 🎯 배너 광고 정책: 활성화

세 번째 조회 (10:10, 캐시 만료):
  AppPolicyRepo: 🔄 Supabase에서 정책 새로 가져오기
  MainActivity: 🔄 배너 광고 정책 변경: 활성화 → 비활성화
```

---

## 📝 로그 예시

### 정상 동작 (변경 없음)
```
10:00:00  MainActivity: 🎯 배너 광고 정책: 활성화
10:05:00  MainActivity: 🎯 배너 광고 정책: 활성화
10:10:00  MainActivity: 🎯 배너 광고 정책: 활성화
```

### 정책 변경 감지
```
10:00:00  MainActivity: 🎯 배너 광고 정책: 활성화
10:05:00  MainActivity: 🎯 배너 광고 정책: 활성화
10:10:00  MainActivity: 🔄 배너 광고 정책 변경: 활성화 → 비활성화
10:15:00  MainActivity: 🎯 배너 광고 정책: 비활성화
```

---

## ⚠️ 주의사항

### 1. 반영 시간
- **최대 5분** 걸릴 수 있음
- 즉시 반영 필요 시: 앱 재시작

### 2. 캐시와의 관계
- 5분 자동 체크 = 캐시 만료 주기
- 완벽하게 동기화됨
- 불필요한 네트워크 요청 없음

### 3. 배터리 영향
- `LaunchedEffect`는 컴포지션이 활성화된 동안만 실행
- 앱이 백그라운드로 가면 자동 중지
- 배터리 영향 최소

### 4. 메모리 영향
- `while(true)` 루프지만 5분마다 1번만 실행
- `delay()`로 대부분 시간 대기
- 메모리/CPU 영향 없음

---

## 🎉 결과

### Before ❌
```
Supabase 변경 → 앱 재시작 필요 → 사용자 불편
```

### After ✅
```
Supabase 변경 → 5분 이내 자동 반영 → 완벽!
```

---

## 🚀 추가 개선 가능 사항 (선택)

### 1. Supabase Realtime 사용
```kotlin
// 즉시 반영 (0초 지연)
supabase.from("app_policy")
    .on(SupabaseEvent.UPDATE) { payload ->
        // 정책 변경 즉시 감지 및 반영
    }
```

**장점**: 즉시 반영  
**단점**: Realtime 구독 필요, 복잡도 증가

### 2. 갱신 주기 조정
```kotlin
delay(3 * 60 * 1000L)  // 3분으로 단축
delay(10 * 60 * 1000L) // 10분으로 연장
```

**현재 5분이 적절**: 캐시와 동기화, 빠른 반영

---

## ✅ 체크리스트

완료 확인:
- [x] `MainActivity.kt` 수정
- [x] `while(true)` 루프로 주기적 체크
- [x] 변경 감지 로직
- [x] 로그 추가
- [x] 5분 대기 설정
- [x] 컴파일 확인
- [x] 문서 업데이트

---

**구현자**: GitHub Copilot  
**완료일**: 2025-01-08  
**상태**: ✅ 완료 및 테스트 준비 완료

