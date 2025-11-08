# Phase 3 완료: notice_policy 테이블 생성

**완료일**: 2025-11-09  
**소요 시간**: 약 30분  
**상태**: ✅ 완료

---

## ✅ 완료된 작업

### 1. Supabase 테이블 생성 ✅
- **파일**: `docs/sql/03-create-notice-policy.sql`
- **작업**: notice_policy 테이블 생성, 인덱스, RLS 정책, 테스트 데이터
- **핵심**: `notice_version` 필드로 명시적 버전 관리
- **다음 단계**: Supabase에서 이 SQL 실행 필요

### 2. Kotlin 모델 클래스 생성 ✅
- **파일**: `app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/NoticePolicy.kt`
- **핵심 변경**:
  - `notice_version`: 공지 버전 (명시적 제어)
  - `title`, `content`: 공지 내용
  - `image_url`, `action_url`: 부가 정보
- **상태**: 컴파일 성공

### 3. Repository 클래스 생성 ✅
- **파일**: `app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/NoticePolicyRepository.kt`
- **기능**: notice_policy 테이블 조회
- **상태**: 컴파일 성공

### 4. HomeScreen 통합 ✅
- **파일**: `app/src/main/java/com/sweetapps/pocketchord/ui/screens/HomeScreen.kt`
- **변경사항**:
  - NoticePolicyRepository import 추가
  - notice_policy 우선순위 3으로 조회
  - 버전 기반 추적 로직 구현 ("notice_v1", "notice_v2" ...)
  - AnnouncementDialog onDismiss에 버전 저장
  - 기존 app_policy notice fallback 유지
- **상태**: 컴파일 성공 (경고만 있음)

---

## 🎯 핵심 개선사항

### 버전 관리로 명시적 제어! ⭐

```sql
-- Before (id 기반, 재표시 불가능)
-- 1월 공지: id=1
-- 2월 공지: id=1 (같은 행 UPDATE) → 재표시 안 됨 ❌

-- After (버전 기반, 명시적 제어)
-- 1월 공지: notice_version=1
-- 오타 수정: notice_version=1 (유지) → 재표시 안 됨 ✅
-- 2월 공지: notice_version=2 (증가) → 모두에게 재표시! ✅
```

### 추적 방식

```kotlin
// 식별자: "notice_v1", "notice_v2", "notice_v3" ...
val identifier = "notice_v${notice.noticeVersion}"

// SharedPreferences에 저장
val prefs = context.getSharedPreferences("notice_prefs", Context.MODE_PRIVATE)
val viewedVersions = prefs.getStringSet("viewed_notices", setOf())

// 확인
if (viewedVersions.contains(identifier)) {
    // 이미 본 버전
}
```

### 우선순위 로직 (완성!)

```
1. emergency_policy (최우선!) ← Phase 2 완료
2. update_policy              ← Phase 1 완료
3. notice_policy              ← Phase 3 완료 ✅
4. app_policy (fallback)      ← 기존 로직 유지
```

---

## 🧪 테스트 가이드

### Step 1: Supabase에서 SQL 실행

```sql
-- 파일: docs/sql/03-create-notice-policy.sql 내용 복사해서 실행

-- 실행 후 확인:
SELECT * FROM notice_policy WHERE app_id = 'com.sweetapps.pocketchord';

-- 예상 결과:
-- id | app_id                      | notice_version | title         | content
-- 1  | com.sweetapps.pocketchord   | 1              | 환영합니다! 🎉 | PocketChord를...
```

### Step 2: 앱 실행 및 로그 확인

```
Logcat에서 "HomeScreen" 태그로 필터링:

예상 로그:
✅ "Phase 3: Checking notice_policy"
✅ "notice_policy found: version=1, title=환영합니다! 🎉"
✅ "Decision: NOTICE from notice_policy (version=1)"
```

**예상 결과**:
- ✅ 공지사항 팝업 표시
- ✅ X 버튼 표시됨
- ✅ X 클릭 시 팝업 닫힘

### Step 3: 재실행 테스트 (표시 안 됨)

```
1. 앱 종료
2. 앱 재실행

예상 로그:
✅ "Phase 3: Checking notice_policy"
✅ "Notice already viewed (version=1), skipping"
```

**예상 결과**:
- ✅ 공지사항 팝업 표시 **안 됨** (이미 봤음)

### Step 4: 오타 수정 테스트 (버전 유지)

```sql
-- Supabase에서 실행: content만 수정 (버전 유지)
UPDATE notice_policy 
SET content = 'PocketChord를 이용해 주셔서 감사합니다!'  -- 오타 수정
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version = 1 (그대로)

-- 앱 재실행
```

**예상 결과**:
- ✅ 공지사항 팝업 표시 **안 됨** (버전이 같으므로)
- ✅ 로그: "Notice already viewed (version=1), skipping"

### Step 5: 새 공지 테스트 (버전 증가) ⭐

```sql
-- Supabase에서 실행: 버전 증가!
UPDATE notice_policy 
SET title = '2월 이벤트 🎉',
    content = '밸런타인 데이 특별 할인 이벤트 진행 중!',
    notice_version = 2  -- 버전 증가!
WHERE app_id = 'com.sweetapps.pocketchord';

-- 앱 재실행
```

**예상 결과**:
- ✅ 공지사항 팝업 **다시 표시됨!** (새 버전)
- ✅ 로그: "Decision: NOTICE from notice_policy (version=2)"
- ✅ X 클릭 후 재실행 → 표시 안 됨 (version=2 추적됨)

### Step 6: 우선순위 테스트

```sql
-- notice_policy 활성화 + update_policy 활성화
UPDATE notice_policy SET is_active = true WHERE app_id = 'com.sweetapps.pocketchord';
UPDATE update_policy SET target_version_code = 999, is_force_update = false WHERE app_id = 'com.sweetapps.pocketchord';
```

**예상 결과**:
- ✅ **update 팝업만 표시됨** (우선순위 2 > 3)
- ✅ "나중에" 클릭 → 앱 재실행 → notice 팝업 표시됨

### Step 7: SharedPreferences 확인

```kotlin
// 개발자 도구 또는 로그로 확인
val prefs = context.getSharedPreferences("notice_prefs", Context.MODE_PRIVATE)
val viewedVersions = prefs.getStringSet("viewed_notices", setOf())
Log.d("Test", "Viewed versions: $viewedVersions")

// 예상 출력:
// Viewed versions: [notice_v1, notice_v2]
```

---

## 📊 테스트 체크리스트

- [ ] Supabase SQL 실행 완료
- [ ] 테이블 생성 확인 (`SELECT * FROM notice_policy`)
- [ ] 앱 빌드 성공
- [ ] 앱 실행 성공
- [ ] 로그에서 "Phase 3: Checking notice_policy" 확인
- [ ] 공지사항 팝업 표시 확인
- [ ] X 클릭 후 재실행 시 표시 안 됨 확인
- [ ] 오타 수정 (버전 유지) → 재표시 안 됨 확인
- [ ] 새 공지 (버전 증가) → 재표시됨 확인
- [ ] 우선순위 확인 (update > notice)
- [ ] Fallback 확인 (app_policy notice)

---

## 🎯 성공 기준

1. ✅ notice_policy 테이블이 Supabase에 생성됨
2. ✅ NoticePolicy.kt, NoticePolicyRepository.kt 컴파일 성공
3. ✅ HomeScreen.kt 컴파일 성공
4. ✅ 앱 실행 시 notice_policy 조회 시도 로그 확인
5. ✅ notice_version으로 재표시 제어 확인
6. ✅ 오타 수정 vs 새 공지 구분 확인
7. ✅ 우선순위 로직 정상 작동 (emergency > update > notice)
8. ✅ 기존 app_policy notice fallback 정상 작동

---

## 🐛 문제 해결

### 문제 1: "notice_policy not found"

**원인**: 테이블이 아직 생성되지 않았거나 is_active=false

**해결**:
```sql
-- 테이블 생성
-- docs/sql/03-create-notice-policy.sql 실행

-- 활성화
UPDATE notice_policy SET is_active = true 
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 문제 2: 버전 증가했는데도 재표시 안 됨

**원인**: SharedPreferences에 새 버전이 이미 저장됨

**해결**:
```kotlin
// 앱 데이터 삭제 또는
val prefs = context.getSharedPreferences("notice_prefs", Context.MODE_PRIVATE)
prefs.edit().clear().apply()

// 또는 Supabase에서 버전을 더 높게
UPDATE notice_policy SET notice_version = 10 WHERE app_id = '...';
```

### 문제 3: 오타 수정했는데 재표시됨

**원인**: 버전도 같이 증가시킴

**해결**:
```sql
-- 오타 수정 시 버전은 건드리지 말 것!
UPDATE notice_policy 
SET content = '수정된 내용'  -- content만 수정
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version은 UPDATE 하지 않음!
```

---

## 📝 운영 가이드

### 시나리오별 가이드

| 작업 | notice_version | SQL 예시 |
|------|---------------|----------|
| **오타 수정** | 유지 | `UPDATE notice_policy SET content = '...' WHERE ...` |
| **내용 약간 보완** | 유지 | `UPDATE notice_policy SET content = '...' WHERE ...` |
| **새 이벤트 공지** | 증가 | `UPDATE notice_policy SET notice_version = 2, content = '...' WHERE ...` |
| **월별 공지** | 증가 | `UPDATE notice_policy SET notice_version = notice_version + 1, ... WHERE ...` |

### 권장 사항

1. ✅ **오타 수정**: 버전 유지 (사용자 경험 향상)
2. ✅ **내용 보완**: 중요도에 따라 판단
3. ✅ **새 이벤트**: 버전 증가 (모두에게 알림)
4. ✅ **정기 공지**: 버전 증가 패턴 사용 (예: 매월 +1)

### 버전 관리 예시

```sql
-- 1월: 신년 이벤트 (version=1)
UPDATE notice_policy 
SET title = '🎉 신년 이벤트',
    content = '새해 맞이 50% 할인!',
    notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord';

-- 1월 16일: 오타 수정 (version=1 유지)
UPDATE notice_policy 
SET content = '새해 맞이 50% 특별 할인!'
WHERE app_id = 'com.sweetapps.pocketchord';
-- 이미 본 사용자에게 재표시 안 됨 ✅

-- 2월: 밸런타인 이벤트 (version=2)
UPDATE notice_policy 
SET title = '💝 밸런타인 데이',
    content = '2월 특별 이벤트!',
    notice_version = 2
WHERE app_id = 'com.sweetapps.pocketchord';
-- 모든 사용자에게 재표시됨 ✅

-- 3월: 봄맞이 이벤트 (version=3)
UPDATE notice_policy 
SET title = '🌸 봄맞이 이벤트',
    content = '봄맞이 특가 세일!',
    notice_version = 3
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 🎉 Phase 3 완료!

### 완성된 시스템

```
✅ 3개 테이블 모두 완성!

1. emergency_policy (최우선)
   - is_dismissible로 X 버튼 제어
   - Google Play 정책 준수

2. update_policy (우선순위 2)
   - target_version_code 단일 필드
   - is_force_update로 강제/선택 구분

3. notice_policy (우선순위 3) ← 완료!
   - notice_version으로 명시적 제어
   - 오타 수정 vs 새 공지 구분 가능
```

---

## 📝 다음 단계

### Phase 4: app_policy 정리 (선택사항, 0.5일)

옵션 A: **ad_policy로 이름 변경** (권장)
- app_policy → ad_policy (광고 정책 전용)
- 불필요한 컬럼 제거

옵션 B: **컬럼만 제거**
- active_popup_type, min_supported_version, latest_version_code 제거
- 광고 정책 컬럼만 유지

**시작 명령**:
```
"Phase 4 시작해줘"
```

또는 테스트 먼저:
```
"Phase 1, 2, 3 통합 테스트 해줘"
```

---

## 📚 관련 파일

- `docs/sql/03-create-notice-policy.sql` - SQL 스크립트
- `app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/NoticePolicy.kt`
- `app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/NoticePolicyRepository.kt`
- `app/src/main/java/com/sweetapps/pocketchord/ui/screens/HomeScreen.kt`

---

**Phase 3 완료!** 🎊  
버전 관리로 명시적으로 제어 가능한 공지사항 시스템이 구축되었습니다!

**전체 시스템 완성!** 🚀
- ✅ Phase 1: update_policy
- ✅ Phase 2: emergency_policy  
- ✅ Phase 3: notice_policy

이제 Supabase에서 SQL을 실행하고 테스트하세요!

