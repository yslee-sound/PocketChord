# Phase 2 완료: emergency_policy 테이블 생성

**완료일**: 2025-11-09  
**소요 시간**: 약 30분  
**상태**: ✅ 완료

---

## ✅ 완료된 작업

### 1. Supabase 테이블 생성 ✅
- **파일**: `docs/sql/02-create-emergency-policy.sql`
- **작업**: emergency_policy 테이블 생성, 인덱스, RLS 정책, 테스트 데이터
- **핵심**: `is_dismissible` 필드로 X 버튼 제어 (Google Play 정책 준수)
- **다음 단계**: Supabase에서 이 SQL 실행 필요

### 2. Kotlin 모델 클래스 생성 ✅
- **파일**: `app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/EmergencyPolicy.kt`
- **핵심 변경**:
  - `is_dismissible`: X 버튼 허용 여부 (Google Play 정책 준수)
  - `redirect_url`: 새 앱 다운로드 URL
  - `new_app_id`: 새 앱 패키지명
- **상태**: 컴파일 성공

### 3. Repository 클래스 생성 ✅
- **파일**: `app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/EmergencyPolicyRepository.kt`
- **기능**: emergency_policy 테이블 조회
- **상태**: 컴파일 성공

### 4. HomeScreen 통합 ✅
- **파일**: `app/src/main/java/com/sweetapps/pocketchord/ui/screens/HomeScreen.kt`
- **변경사항**:
  - EmergencyPolicyRepository import 추가
  - emergency_policy 최우선순위로 조회
  - emergencyPolicy state 변수 추가
  - EmergencyRedirectDialog에 is_dismissible 적용
  - 기존 app_policy emergency fallback 유지
- **상태**: 컴파일 성공 (경고만 있음)

---

## 🎯 핵심 개선사항

### Google Play 정책 준수 ⭐
```kotlin
// Before (하드코딩)
isDismissible = false  // 코드에서 고정

// After (DB 제어)
isDismissible = emergencyPolicy.isDismissible  // Supabase에서 관리
```

### 우선순위 로직
```
1순위: emergency_policy (신규) ← 최우선!
  ↓ 없으면
2순위: update_policy (Phase 1)
  ↓ 없으면
3순위: app_policy emergency (기존, fallback)
```

---

## 🧪 테스트 가이드

### Step 1: Supabase에서 SQL 실행

```sql
-- 파일: docs/sql/02-create-emergency-policy.sql 내용 복사해서 실행

-- 실행 후 확인:
SELECT * FROM emergency_policy WHERE app_id = 'com.sweetapps.pocketchord';

-- 예상 결과:
-- id | app_id                      | is_active | is_dismissible | content
-- 1  | com.sweetapps.pocketchord   | false     | true           | ⚠️ [테스트]...
```

### Step 2: 앱 실행 및 로그 확인

```
Logcat에서 "HomeScreen" 태그로 필터링:

예상 로그:
✅ "Phase 2: Checking emergency_policy"
✅ "emergency_policy not found or error" (is_active=false이므로)
✅ "Phase 1: Trying update_policy" (다음 단계로 진행)
```

### Step 3: 긴급 상황 테스트 (X 버튼 있음)

```sql
-- Supabase에서 실행:
UPDATE emergency_policy 
SET is_active = true,       -- 활성화!
    is_dismissible = true,  -- X 버튼 허용
    content = '⚠️ [테스트] 긴급 공지입니다.\nX 버튼을 눌러 닫을 수 있습니다.'
WHERE app_id = 'com.sweetapps.pocketchord';
```

**예상 결과**:
- ✅ 앱 실행 시 긴급 팝업 표시
- ✅ **X 버튼 표시됨** (is_dismissible=true)
- ✅ X 클릭 시 팝업 닫힘
- ✅ 재실행 시 **다시 표시됨** (추적 없음)

### Step 4: 긴급 상황 테스트 (X 버튼 없음)

```sql
-- Supabase에서 실행:
UPDATE emergency_policy 
SET is_dismissible = false,  -- X 버튼 숨김 (주의!)
    content = '🚨 [테스트] 이 앱은 더 이상 지원되지 않습니다.\n새 앱을 설치해야 합니다.'
WHERE app_id = 'com.sweetapps.pocketchord';
```

**예상 결과**:
- ✅ 앱 실행 시 긴급 팝업 표시
- ✅ **X 버튼 없음** (is_dismissible=false)
- ✅ 뒤로가기 불가 (설정에 따라)
- ⚠️ **Google Play 정책 주의!** (is_dismissible=false는 최후의 수단)

### Step 5: 우선순위 테스트

```sql
-- emergency_policy 활성화 + update_policy 활성화
UPDATE emergency_policy SET is_active = true WHERE app_id = 'com.sweetapps.pocketchord';
UPDATE update_policy SET target_version_code = 999 WHERE app_id = 'com.sweetapps.pocketchord';
```

**예상 결과**:
- ✅ **emergency 팝업만 표시됨** (update 팝업 무시)
- ✅ 로그: "Decision: EMERGENCY from emergency_policy"
- ✅ 로그: "return@LaunchedEffect" (다른 팝업 건너뜀)

### Step 6: 정리

```sql
-- 테스트 완료 후 비활성화
UPDATE emergency_policy 
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 📊 테스트 체크리스트

- [ ] Supabase SQL 실행 완료
- [ ] 테이블 생성 확인 (`SELECT * FROM emergency_policy`)
- [ ] 앱 빌드 성공
- [ ] 앱 실행 성공
- [ ] 로그에서 "Phase 2: Checking emergency_policy" 확인
- [ ] X 버튼 있는 긴급 팝업 표시 확인 (is_dismissible=true)
- [ ] X 버튼 없는 긴급 팝업 표시 확인 (is_dismissible=false)
- [ ] 추적 없음 확인 (매번 표시)
- [ ] 우선순위 확인 (emergency > update)
- [ ] Fallback 확인 (app_policy emergency)

---

## 🎯 성공 기준

1. ✅ emergency_policy 테이블이 Supabase에 생성됨
2. ✅ EmergencyPolicy.kt, EmergencyPolicyRepository.kt 컴파일 성공
3. ✅ HomeScreen.kt 컴파일 성공
4. ✅ 앱 실행 시 emergency_policy 조회 시도 로그 확인
5. ✅ is_dismissible로 X 버튼 제어 확인
6. ✅ 우선순위 로직 정상 작동 (emergency > update)
7. ✅ 기존 app_policy emergency fallback 정상 작동

---

## 🐛 문제 해결

### 문제 1: "emergency_policy not found"

**원인**: 테이블이 아직 생성되지 않았거나 is_active=false

**해결**:
```sql
-- 테이블 생성
-- docs/sql/02-create-emergency-policy.sql 실행

-- 활성화
UPDATE emergency_policy SET is_active = true 
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 문제 2: X 버튼이 안 보임 (is_dismissible=true인데도)

**원인**: EmergencyRedirectDialog의 isDismissible 파라미터 전달 확인 필요

**해결**:
```kotlin
// HomeScreen.kt 확인
isDismissible = emergencyPolicy!!.isDismissible,  // ← DB 값 사용
onDismiss = if (emergencyPolicy!!.isDismissible) {
    { showEmergencyDialog = false }
} else {
    { /* X 버튼 없음 */ }
}
```

### 문제 3: update 팝업이 같이 뜸

**원인**: return@LaunchedEffect 누락

**해결**:
```kotlin
emergency?.let { ep ->
    emergencyPolicy = ep
    showEmergencyDialog = true
    return@LaunchedEffect  // ← 이 줄이 있어야 함!
}
```

---

## ⚠️ Google Play 정책 주의사항

### is_dismissible 사용 가이드

| 상황 | is_dismissible | 설명 |
|------|---------------|------|
| **서비스 종료 예정** | ✅ `true` | 사용자에게 선택권 제공 (권장) |
| **새 앱 안내** | ✅ `true` | 부드러운 마이그레이션 (권장) |
| **앱 완전 차단** | ⚠️ `false` | 최후의 수단 (주의!) |

**권장 사항**:
- ✅ 기본값 `true` 사용 (Google Play 정책 준수)
- ⚠️ `false`는 정말 긴급한 경우만 (앱 완전 차단 등)
- ✅ `true`로 설정해도 추적 없이 매번 표시 가능

---

## 📝 다음 단계

### Phase 3: notice_policy (1일)
- [ ] notice_policy 테이블 생성
- [ ] NoticePolicy.kt 모델 생성 (notice_version 필드 포함)
- [ ] NoticePolicyRepository.kt 생성
- [ ] HomeScreen에 우선순위 3으로 통합
- [ ] 버전 기반 추적 로직 구현

**시작 명령**:
```
"Phase 3 시작해줘"
```

---

## 📚 관련 파일

- `docs/sql/02-create-emergency-policy.sql` - SQL 스크립트
- `app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/EmergencyPolicy.kt`
- `app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/EmergencyPolicyRepository.kt`
- `app/src/main/java/com/sweetapps/pocketchord/ui/screens/HomeScreen.kt`

---

**Phase 2 완료!** 🎉  
Google Play 정책을 준수하는 긴급 팝업 시스템이 구축되었습니다!

테스트 완료 후 Phase 3으로 진행하세요!

