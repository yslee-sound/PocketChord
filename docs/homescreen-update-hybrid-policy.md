# HomeScreen.kt 업데이트 - 하이브리드 AppPolicy 구조 적용

## 문제
`HomeScreen.kt`가 기존 `AppPolicy` 모델 구조를 사용하고 있어 컴파일 에러 발생:
```
Unresolved reference 'emergencyIsActive'
```

## 해결
`HomeScreen.kt`를 새로운 하이브리드 방식의 `AppPolicy` 구조에 맞게 업데이트

---

## 변경 사항

### Before (기존 구조)
```kotlin
// 여러 개의 boolean 필드로 팝업 타입 관리
if (p.emergencyIsActive && ...) { }
if (p.requiresForceUpdate(...)) { }
if (p.updateIsActive && ...) { }
if (p.noticeIsActive && ...) { }
```

### After (하이브리드 구조)
```kotlin
// 단일 activePopupType 필드로 팝업 타입 관리
when (p.activePopupType) {
    "emergency" -> { }
    "force_update" -> { }
    "optional_update" -> { }
    "notice" -> { }
    "none" -> { }
}
```

---

## 핵심 변경 내용

### 1. 정책 조회 로직 단순화
```kotlin
// Before: 복잡한 조건 분기
if (p.emergencyIsActive && !p.emergencyTitle.isNullOrBlank() && !p.emergencyContent.isNullOrBlank()) { }

// After: 간단한 타입 체크
when (p.activePopupType) {
    "emergency" -> { }
}
```

### 2. 필드 매핑 변경

| 기존 필드 | 새 필드 | 비고 |
|----------|--------|------|
| `emergencyIsActive` | `activePopupType == "emergency"` | 긴급 공지 |
| `emergencyTitle` | `content` | 통합 필드 |
| `emergencyContent` | `content` | 통합 필드 |
| `updateIsActive` | `activePopupType == "optional_update"` | 선택적 업데이트 |
| `noticeIsActive` | `activePopupType == "notice"` | 일반 공지 |
| `noticeTitle` | (제거) | content만 사용 |
| `noticeContent` | `content` | 통합 필드 |

### 3. 업데이트 로직 개선

#### Force Update
```kotlin
// Before
if (p.requiresForceUpdate(currentVersion)) { }

// After (동일하게 유지)
if (p.requiresForceUpdate(currentVersion)) { }
```

#### Optional Update
```kotlin
// Before
val optionalAllowed = p.updateIsActive && (p.latestVersionCode ?: 0) > currentVersion

// After
if (p.recommendsUpdate(currentVersion) && ...) { }
```

---

## 팝업 우선순위 (변경 없음)

1. **Emergency** (긴급 공지)
2. **Force Update** (강제 업데이트)
3. **Optional Update** (선택적 업데이트)
4. **Notice** (일반 공지)

---

## 테스트 시나리오

### 1. 긴급 공지
```sql
UPDATE app_policy SET
  is_active = TRUE,
  active_popup_type = 'emergency',
  content = '긴급 점검 안내',
  download_url = 'https://example.com'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```
**예상**: 긴급 공지 팝업 표시

### 2. 강제 업데이트
```sql
UPDATE app_policy SET
  is_active = TRUE,
  active_popup_type = 'force_update',
  min_supported_version = 100,
  download_url = 'market://...'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```
**예상**: 강제 업데이트 팝업 표시 (현재 버전 < 100)

### 3. 선택적 업데이트
```sql
UPDATE app_policy SET
  is_active = TRUE,
  active_popup_type = 'optional_update',
  latest_version_code = 100,
  download_url = 'market://...'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```
**예상**: 선택적 업데이트 팝업 표시 (현재 버전 < 100)

### 4. 일반 공지
```sql
UPDATE app_policy SET
  is_active = TRUE,
  active_popup_type = 'notice',
  content = '새로운 기능이 추가되었습니다'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```
**예상**: 일반 공지 팝업 표시

### 5. 팝업 없음
```sql
UPDATE app_policy SET
  is_active = FALSE,
  active_popup_type = 'none'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```
**예상**: 팝업 표시 안 됨

---

## 로그 출력 예시

### 정책 조회 성공
```
D/HomeScreen: Policy fetch success: id=1 appId=com.sweetapps.pocketchord.debug active=true type=force_update minSupported=5 latest=null
D/HomeScreen: Decision: FORCE UPDATE popup (minSupported=5)
```

### 정책 없음
```
W/HomeScreen: No active policy row for app_id='com.sweetapps.pocketchord.debug'. Check: (1) app_policy.app_id 값, (2) is_active=true, (3) RLS policy allowing read, (4) anon key valid.
```

### 팝업 타입별
```
D/HomeScreen: Decision: EMERGENCY popup will show
D/HomeScreen: Decision: FORCE UPDATE popup (minSupported=5)
D/HomeScreen: Decision: OPTIONAL UPDATE popup (latest=6)
D/HomeScreen: Decision: NOTICE popup
D/HomeScreen: Decision: No popup (type=none)
```

---

## 주의사항

### 1. 기존 데이터 마이그레이션
기존 `app_policy` 테이블을 사용 중이었다면 데이터 마이그레이션 필요:
```sql
-- 기존 테이블 백업
CREATE TABLE app_policy_backup AS SELECT * FROM app_policy;

-- 테이블 재생성 (docs/supabase-app-policy-hybrid.md 참고)
DROP TABLE app_policy;
-- ... SQL 실행 ...

-- 초기 데이터 생성
INSERT INTO app_policy (app_id, is_active, active_popup_type)
VALUES ('com.sweetapps.pocketchord.debug', FALSE, 'none');
```

### 2. RLS 정책 확인
```sql
-- RLS가 활성화되어 있는지 확인
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE tablename = 'app_policy';
-- rowsecurity = true 여야 함
```

### 3. 오프라인 캐싱
강제 업데이트는 오프라인에서도 작동하도록 SharedPreferences에 캐싱됨:
- `force_required_version`: 강제 업데이트 버전
- `force_update_info`: 강제 업데이트 정보 JSON

---

## 컴파일 상태

✅ **컴파일 에러 없음**
- 기존 에러: `Unresolved reference 'emergencyIsActive'` → **해결됨**
- 남은 경고: 11개 (컴파일에 영향 없음)

---

## 관련 파일

1. **모델**: `app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/AppPolicy.kt`
2. **Repository**: `app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/AppPolicyRepository.kt`
3. **UI**: `app/src/main/java/com/sweetapps/pocketchord/ui/screens/HomeScreen.kt`
4. **다이얼로그**: `app/src/main/java/com/sweetapps/pocketchord/ui/dialog/AppPolicyDialogs.kt`

---

## 다음 단계

1. ✅ HomeScreen.kt 업데이트 완료
2. ✅ 컴파일 에러 해결
3. 🔜 앱 테스트 (Supabase 설정 후)
4. 🔜 각 팝업 타입별 동작 확인

---

**작성일**: 2025-11-08  
**상태**: ✅ 완료

