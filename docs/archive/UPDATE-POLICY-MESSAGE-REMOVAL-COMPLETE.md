# update_policy message 제거 및 download_url 필수화 완료!

**버전**: v1.0.0  
**작성일**: 2025-11-09  
**최종 업데이트**: 2025-11-09 07:00 KST  
**목적**: message 필드 제거, download_url NOT NULL 및 기본값 설정  
**상태**: ✅ 완료

---

## 📝 변경 이력

### v1.0.0 (2025-11-09 07:00)
- ✅ message 필드 제거 (release_notes로 통합)
- ✅ download_url NOT NULL 제약 조건 추가
- ✅ download_url 기본값: https://play.google.com/
- ✅ 모든 관련 코드 및 문서 업데이트 완료

---

## 🎯 변경 사항

### 1. UpdatePolicy 모델 ✅

**Before**:
```kotlin
@SerialName("message")
val message: String? = null  // 제거됨

@SerialName("download_url")
val downloadUrl: String? = null  // NULL 허용
```

**After**:
```kotlin
// message 필드 제거됨!

@SerialName("download_url")
val downloadUrl: String = "https://play.google.com/"  // NOT NULL, 기본값
```

---

### 2. SQL 스크립트 ✅

**01-create-update-policy.sql**:
```sql
-- Before
message TEXT,
download_url TEXT,

-- After
release_notes TEXT,
download_url TEXT NOT NULL DEFAULT 'https://play.google.com/',
```

**마이그레이션 SQL (10-update-policy-remove-message.sql)**:
```sql
-- 1. download_url에 기본값 설정
UPDATE public.update_policy 
SET download_url = CASE ...
WHERE download_url IS NULL;

-- 2. NOT NULL 제약 조건 추가
ALTER TABLE public.update_policy 
ALTER COLUMN download_url SET NOT NULL,
ALTER COLUMN download_url SET DEFAULT 'https://play.google.com/';

-- 3. message 컬럼 삭제
ALTER TABLE public.update_policy 
DROP COLUMN IF EXISTS message;
```

---

### 3. HomeScreen 코드 ✅

**Before**:
```kotlin
releaseNotes = up.releaseNotes ?: up.message ?: ""
```

**After**:
```kotlin
releaseNotes = up.releaseNotes ?: "새로운 업데이트가 있습니다."
```

---

## 📋 사용 예시

### 예시 1: 기본 업데이트
```sql
UPDATE update_policy 
SET target_version_code = 6,
    is_force_update = false,
    release_notes = '• 새로운 기능 추가\n• 버그 수정',
    download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord'
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 예시 2: 강제 업데이트
```sql
UPDATE update_policy 
SET target_version_code = 7,
    is_force_update = true,
    release_notes = '• 중요 보안 패치\n• 필수 업데이트',
    download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord'
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 예시 3: download_url 기본값 사용
```sql
UPDATE update_policy 
SET target_version_code = 8,
    release_notes = '• 성능 개선'
    -- download_url 생략 가능 (기본값 사용)
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 🔧 마이그레이션 방법

### 1. 기존 테이블 업데이트

**Supabase에서 실행**:
```bash
1. SQL Editor 열기
2. docs/sql/10-update-policy-remove-message.sql 복사
3. 실행
```

### 2. 결과 확인

```
✅ message 컬럼 삭제됨
✅ download_url NOT NULL 적용됨
✅ download_url 기본값: https://play.google.com/
```

---

## 💡 장점

### Before (message 필드 사용)
```kotlin
message = "필수 업데이트"
release_notes = "• 보안 패치"

// 코드에서:
releaseNotes = up.releaseNotes ?: up.message ?: ""
// → 복잡한 fallback 로직
```

**문제점**:
- ❌ message와 release_notes 중복 관리
- ❌ 어느 필드를 사용해야 하는지 혼란
- ❌ Fallback 로직 복잡

---

### After (release_notes만 사용)
```kotlin
release_notes = "• 필수 업데이트\n• 보안 패치"

// 코드에서:
releaseNotes = up.releaseNotes ?: "새로운 업데이트가 있습니다."
// → 단순명료!
```

**장점**:
- ✅ 단일 필드로 통합 (단순화)
- ✅ release_notes에 모든 메시지 포함
- ✅ 명확한 데이터 구조
- ✅ 유지보수 용이

---

## 📊 download_url NOT NULL 설정 이유

### Before (NULL 허용)
```sql
download_url TEXT
```

**문제점**:
- ❌ NULL일 때 처리 로직 필요
- ❌ 기본 스토어 링크를 코드에서 관리
- ❌ 앱마다 다른 링크 설정 시 혼란

---

### After (NOT NULL + 기본값)
```sql
download_url TEXT NOT NULL DEFAULT 'https://play.google.com/'
```

**장점**:
- ✅ 항상 값이 있음 (NULL 체크 불필요)
- ✅ 기본값으로 Play Store 메인 페이지
- ✅ 앱별 링크는 명시적으로 설정
- ✅ 데이터 무결성 보장

---

## ✅ 체크리스트

- [x] UpdatePolicy 모델에서 message 제거
- [x] download_url NOT NULL로 변경
- [x] HomeScreen에서 message 참조 제거
- [x] SQL 스크립트 업데이트 (01-create-update-policy.sql)
- [x] 마이그레이션 SQL 작성 (10-update-policy-remove-message.sql)
- [x] 디버그 테스트 데이터 업데이트
- [x] RELEASE-TEST-PHASE2 문서 업데이트
- [x] 컴파일 에러 확인 (없음 ✅)
- [ ] Supabase에서 마이그레이션 SQL 실행 (사용자가 실행)
- [ ] 앱 테스트 (사용자가 테스트)

---

## 🎉 완료!

- ✅ message 필드 제거됨
- ✅ release_notes로 통합됨
- ✅ download_url NOT NULL (기본값: https://play.google.com/)
- ✅ 코드 단순화 완료

**이제 Supabase에서 `10-update-policy-remove-message.sql`을 실행하세요!** 🚀

---

**문서 버전**: v1.0.0  
**마지막 수정**: 2025-11-09 07:00 KST

