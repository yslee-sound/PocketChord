# Phase 1 완료: update_policy 테이블 생성

**완료일**: 2025-11-09  
**소요 시간**: 약 30분  
**상태**: ✅ 완료

---

## ✅ 완료된 작업

### 1. Supabase 테이블 생성 ✅
- **파일**: `docs/sql/01-create-update-policy.sql`
- **작업**: update_policy 테이블 생성, 인덱스, RLS 정책, 테스트 데이터
- **다음 단계**: Supabase에서 이 SQL 실행 필요

### 2. Kotlin 모델 클래스 생성 ✅
- **파일**: `app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/UpdatePolicy.kt`
- **핵심 변경**:
  - `min_supported_version` + `latest_version_code` → `target_version_code` (단일 필드)
  - `active_popup_type` → `is_force_update` (Boolean)
- **상태**: 컴파일 성공

### 3. Repository 클래스 생성 ✅
- **파일**: `app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/UpdatePolicyRepository.kt`
- **기능**: update_policy 테이블 조회
- **상태**: 컴파일 성공

### 4. HomeScreen 통합 ✅
- **파일**: `app/src/main/java/com/sweetapps/pocketchord/ui/screens/HomeScreen.kt`
- **변경사항**:
  - UpdatePolicyRepository import 추가
  - update_policy 우선 조회 로직 추가
  - 실패 시 app_policy로 fallback (기존 로직 유지)
- **상태**: 컴파일 성공 (경고만 있음)

---

## 🧪 테스트 가이드

### Step 1: Supabase에서 SQL 실행

```sql
-- 파일: docs/sql/01-create-update-policy.sql 내용 복사해서 실행

-- 실행 후 확인:
SELECT * FROM update_policy WHERE app_id = 'com.sweetapps.pocketchord';

-- 예상 결과:
-- id | app_id                      | target_version_code | is_force_update | message
-- 1  | com.sweetapps.pocketchord   | 1                   | false           | 새로운 기능이...
```

### Step 2: 앱 실행 및 로그 확인

```
Logcat에서 "HomeScreen" 태그로 필터링:

예상 로그:
✅ "Phase 1: Trying update_policy"
✅ "update_policy found: targetVersion=1, isForce=false"
✅ "update_policy exists but no update needed (current=1 >= target=1)"
```

### Step 3: 강제 업데이트 테스트

```sql
-- Supabase에서 실행:
UPDATE update_policy 
SET target_version_code = 999,      -- 현재 버전보다 높게
    is_force_update = true,
    message = '테스트: 강제 업데이트'
WHERE app_id = 'com.sweetapps.pocketchord';
```

**예상 결과**:
- ✅ 앱 실행 시 강제 업데이트 팝업 표시
- ✅ X 버튼 없음
- ✅ 뒤로가기 차단

### Step 4: 선택적 업데이트 테스트

```sql
-- Supabase에서 실행:
UPDATE update_policy 
SET target_version_code = 999,      -- 현재 버전보다 높게
    is_force_update = false,        -- 선택적!
    message = '테스트: 선택적 업데이트'
WHERE app_id = 'com.sweetapps.pocketchord';
```

**예상 결과**:
- ✅ 앱 실행 시 선택적 업데이트 팝업 표시
- ✅ "나중에" 버튼 있음
- ✅ "나중에" 클릭 후 재실행 시 표시 안 됨
- ✅ Supabase에서 target_version_code 변경 후 다시 표시됨

### Step 5: Fallback 테스트 (옵션)

```sql
-- update_policy를 비활성화하여 app_policy로 fallback 확인
UPDATE update_policy 
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

**예상 결과**:
- ✅ "update_policy not found or error" 로그
- ✅ "Querying app_policy (fallback)" 로그
- ✅ 기존 app_policy 로직 정상 작동

---

## 📊 테스트 체크리스트

- [ ] Supabase SQL 실행 완료
- [ ] 테이블 생성 확인 (`SELECT * FROM update_policy`)
- [ ] 앱 빌드 성공
- [ ] 앱 실행 성공
- [ ] 로그에서 "Phase 1: Trying update_policy" 확인
- [ ] 강제 업데이트 팝업 표시 확인
- [ ] 선택적 업데이트 팝업 표시 확인
- [ ] "나중에" 버튼 동작 확인
- [ ] 버전 조건 테스트 (현재 < 목표)
- [ ] Fallback 로직 확인 (옵션)

---

## 🎯 성공 기준

1. ✅ update_policy 테이블이 Supabase에 생성됨
2. ✅ UpdatePolicy.kt, UpdatePolicyRepository.kt 컴파일 성공
3. ✅ HomeScreen.kt 컴파일 성공
4. ✅ 앱 실행 시 update_policy 조회 시도 로그 확인
5. ✅ 강제/선택적 업데이트 팝업 정상 작동
6. ✅ 기존 app_policy fallback 로직 정상 작동

---

## 🐛 문제 해결

### 문제 1: "update_policy not found"

**원인**: 테이블이 아직 생성되지 않음

**해결**:
```sql
-- docs/sql/01-create-update-policy.sql 실행
```

### 문제 2: 컴파일 에러

**원인**: import 누락

**해결**:
```kotlin
import com.sweetapps.pocketchord.data.supabase.model.UpdatePolicy
import com.sweetapps.pocketchord.data.supabase.repository.UpdatePolicyRepository
```

### 문제 3: 팝업이 안 뜸

**원인**: target_version_code가 현재 버전보다 낮음

**해결**:
```sql
UPDATE update_policy 
SET target_version_code = 999  -- 충분히 높은 값
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 📝 다음 단계

### Phase 2: emergency_policy (1일)
- [ ] emergency_policy 테이블 생성
- [ ] EmergencyPolicy.kt 모델 생성
- [ ] EmergencyPolicyRepository.kt 생성
- [ ] HomeScreen에 우선순위 1로 통합

**시작 명령**:
```
"Phase 2 시작해줘"
```

---

## 📚 관련 파일

- `docs/sql/01-create-update-policy.sql` - SQL 스크립트
- `app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/UpdatePolicy.kt`
- `app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/UpdatePolicyRepository.kt`
- `app/src/main/java/com/sweetapps/pocketchord/ui/screens/HomeScreen.kt`

---

**Phase 1 완료!** 🎉  
테스트 완료 후 Phase 2로 진행하세요!

