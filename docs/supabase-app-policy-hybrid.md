# Supabase App Policy (하이브리드 방식) - 운영 테이블 + 히스토리 관리

본 문서는 PocketChord의 앱 정책을 **운영 테이블**과 **히스토리 테이블**로 분리하여 관리하는 하이브리드 방식을 설명합니다.

## 📚 하이브리드 방식이란?

**운영 테이블**(`app_policy`)과 **히스토리 테이블**(`app_policy_history`)을 분리해서 사용하는 방법입니다.

### 테이블 역할

| 테이블 | 용도 | 행 개수 | 변경 방법 |
|--------|------|---------|----------|
| **`app_policy`** | 현재 활성 정책 (앱이 읽음) | 앱당 1개 | `UPDATE` |
| **`app_policy_history`** | 과거 변경 기록 보관 | 변경시마다 누적 | 자동 (트리거) |

### 비유로 이해하기

- **운영 테이블** = 현재 입고 있는 옷
- **히스토리 테이블** = 옷장 속 옛날 옷들 (입지 않지만 버리지 않음)

---

## 🎛️ is_active 필드를 사용하는 이유

### 왜 `active_popup_type`만으로는 부족한가?

**문제 상황**:
```sql
-- 오전 9시: 강제 업데이트 설정을 미리 준비
UPDATE app_policy SET
  active_popup_type = 'force_update',
  min_supported_version = 5,
  download_url = '...';
-- ❌ 바로 모든 사용자에게 팝업이 뜸 (의도하지 않음)
```

### `is_active` 필드의 장점

1. **예약 설정 가능**
   - 팝업 내용을 미리 작성해두고, 원하는 시점에 `is_active = true`로 활성화
   - 긴급 상황 대비: 미리 긴급 공지를 준비해두고 필요할 때 즉시 켜기

2. **안전한 테스트**
   - 설정을 검토하고 확인한 후 활성화
   - 실수로 잘못된 설정이 즉시 반영되는 것을 방지

3. **일시 중단 가능**
   - 팝업을 잠시 끄고 싶을 때 `is_active = false`만 변경
   - 설정 내용은 그대로 유지되어 나중에 다시 켜기 편함

4. **팀 협업**
   - 담당자 A가 설정 준비 → 담당자 B가 검토 후 활성화

### 사용 예시

```sql
-- 1) 오전 9시: 강제 업데이트 설정 준비 (기본값으로 비활성)
UPDATE app_policy SET
  active_popup_type = 'force_update',
  -- is_active = false,  -- 기본값이므로 생략 가능
  content = DEFAULT,
  download_url = 'https://play.google.com/...',
  min_supported_version = 5
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- ✅ 설정은 저장되지만 is_active = false이므로 사용자에게는 팝업이 표시되지 않음

-- 2) 오전 10시: 검토 완료 후 활성화
UPDATE app_policy SET
  is_active = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- ✅ 이제 팝업이 사용자에게 표시됨

-- 3) 오후 3시: 일시적으로 중단
UPDATE app_policy SET
  is_active = false
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- ✅ 팝업 사라짐 (설정은 그대로 유지)

-- 4) 오후 4시: 다시 활성화
UPDATE app_policy SET
  is_active = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- ✅ 설정 변경 없이 바로 재활성화
```

### 기본값이 FALSE인 이유

- **안전한 운영**: 새 설정이 즉시 반영되지 않아 실수 방지
- **예약 설정**: 미리 준비해두고 원하는 시점에 활성화
- **명시적 활성화**: 담당자가 직접 확인 후 `is_active = true` 설정

### 요약

| 필드 | 역할 | 기본값 |
|------|------|--------|
| `is_active` | **언제** 팝업을 보여줄지 제어 | **FALSE** (비활성) |
| `active_popup_type` | **어떤** 팝업을 보여줄지 결정 | `'none'` |

**결론**: 두 필드를 함께 사용하면 **설정 준비 → 검토 → 활성화**의 안전한 운영 흐름이 가능합니다.

---

## 🚀 1단계: 테이블 생성 (처음 1번만 실행)

Supabase 대시보드 → **SQL Editor**에서 아래 전체 코드를 실행하세요.

```sql
-- ==========================================
-- 1. 기존 테이블 정리 (재실행 시 필요)
-- ==========================================
DROP TABLE IF EXISTS public.app_policy_history CASCADE;
DROP TABLE IF EXISTS public.app_policy CASCADE;
DROP TYPE IF EXISTS popup_type CASCADE;

-- ==========================================
-- 2. ENUM 타입 생성 (5가지 팝업 타입)
-- ==========================================
CREATE TYPE popup_type AS ENUM (
  'emergency',         -- 1순위: 긴급 공지
  'force_update',      -- 2순위: 강제 업데이트
  'optional_update',   -- 3순위: 선택적 업데이트
  'notice',            -- 4순위: 일반 공지
  'none'               -- 팝업 없음
);

-- ==========================================
-- 3. 운영 테이블 생성 (앱이 읽는 곳)
-- ==========================================
CREATE TABLE public.app_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  app_id TEXT NOT NULL UNIQUE,  -- 앱마다 1개만 존재
  is_active BOOLEAN NOT NULL DEFAULT FALSE,  -- 팝업 활성화 여부 (기본: 비활성)
  active_popup_type popup_type NOT NULL DEFAULT 'none',  -- 어떤 팝업을 보여줄지

  -- 공통 메시지 (기본값 설정)
  content TEXT DEFAULT '더 안정적이고 개선된 환경을 위해 최신 버전으로 업데이트해 주세요. 지금 업데이트하시면 앱을 계속 이용하실 수 있습니다.',
  download_url TEXT,

  -- 버전 관련
  min_supported_version INTEGER,
  latest_version_code INTEGER
);

-- ==========================================
-- 4. 히스토리 테이블 생성 (과거 기록 보관)
-- ==========================================
CREATE TABLE public.app_policy_history (
  id BIGSERIAL PRIMARY KEY,
  archived_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),  -- 저장된 시간
  
  -- 운영 테이블과 동일한 구조
  app_id TEXT NOT NULL,
  is_active BOOLEAN,
  active_popup_type popup_type,
  content TEXT,
  download_url TEXT,
  min_supported_version INTEGER,
  latest_version_code INTEGER,
  
  -- 추가 정보
  change_reason TEXT  -- 왜 바뀌었는지 메모
);

-- ==========================================
-- 5. 인덱스 (빠른 검색)
-- ==========================================
-- 히스토리에서 앱별로 최신순 조회 시 빠르게
CREATE INDEX idx_policy_history_app_archived
ON app_policy_history (app_id, archived_at DESC);

-- 운영 테이블 팝업 타입별 조회
CREATE INDEX idx_app_policy_popup_type 
ON app_policy (active_popup_type);

-- ==========================================
-- 6. RLS (Row Level Security) - 보안 필수 🔒
-- ==========================================
-- RLS를 활성화하면 is_active = TRUE인 행만 클라이언트에서 조회 가능
-- is_active = FALSE인 준비 중 설정은 데이터베이스 레벨에서 차단됨
ALTER TABLE public.app_policy ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "allow_read_policy" ON public.app_policy;
CREATE POLICY "allow_read_policy"
ON public.app_policy
FOR SELECT
USING (is_active = TRUE);  -- 활성화된 정책만 읽기 가능

-- 참고: 클라이언트는 읽기만 가능 (INSERT/UPDATE/DELETE 차단)
-- 정책 수정은 Supabase 대시보드 또는 Service Role 키로만 가능

-- ==========================================
-- 7. 제약조건 (데이터 무결성)
-- ==========================================
-- content: none 제외하고 모두 필수
ALTER TABLE public.app_policy ADD CONSTRAINT check_content_with_type
CHECK (
  (active_popup_type = 'none')  -- 팝업 없음
  OR
  (active_popup_type IN ('emergency', 'force_update', 'optional_update', 'notice')
   AND content IS NOT NULL)  -- 팝업 있으면 content 필수
);

-- download_url: emergency/force/optional은 필수, notice/none은 선택
ALTER TABLE public.app_policy ADD CONSTRAINT check_download_url_with_type
CHECK (
  (active_popup_type IN ('emergency', 'force_update', 'optional_update')
   AND download_url IS NOT NULL)  -- 3가지 팝업은 URL 필수
  OR
  (active_popup_type IN ('notice', 'none'))  -- notice와 none은 URL 선택
);

-- min_supported_version: force_update일 때 필수
ALTER TABLE public.app_policy ADD CONSTRAINT check_min_version_with_force_update
CHECK (
  (active_popup_type != 'force_update')  -- force_update가 아닌 경우
  OR
  (active_popup_type = 'force_update' AND min_supported_version IS NOT NULL)  -- force_update는 필수
);

-- latest_version_code: optional_update일 때 필수
ALTER TABLE public.app_policy ADD CONSTRAINT check_latest_version_with_optional_update
CHECK (
  (active_popup_type != 'optional_update')  -- optional_update가 아닌 경우
  OR
  (active_popup_type = 'optional_update' AND latest_version_code IS NOT NULL)  -- optional_update는 필수
);

-- ==========================================
-- 8. 자동 백업 트리거 (마법 ✨)
-- ==========================================
-- 정책을 UPDATE할 때마다 자동으로 히스토리에 저장
CREATE OR REPLACE FUNCTION backup_policy_to_history()
RETURNS TRIGGER AS $$
BEGIN
  -- 기존 값을 히스토리 테이블에 복사
  INSERT INTO app_policy_history (
    app_id,
    is_active,
    active_popup_type,
    content,
    download_url,
    min_supported_version,
    latest_version_code,
    change_reason
  ) VALUES (
    OLD.app_id,
    OLD.is_active,
    OLD.active_popup_type,
    OLD.content,
    OLD.download_url,
    OLD.min_supported_version,
    OLD.latest_version_code,
    '자동 백업'
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_backup_policy ON app_policy;
CREATE TRIGGER trigger_backup_policy
BEFORE UPDATE ON app_policy
FOR EACH ROW
EXECUTE FUNCTION backup_policy_to_history();

-- ==========================================
-- 9. 초기 데이터
-- ==========================================
INSERT INTO public.app_policy (app_id, is_active, active_popup_type)
VALUES
  ('com.sweetapps.pocketchord.debug', FALSE, 'none'),
  ('com.sweetapps.pocketchord', FALSE, 'none')
ON CONFLICT (app_id) DO NOTHING;  -- 이미 있으면 건너뛰기
```

---

## ✅ SQL 실행 후 확인

### 1. RLS 활성화 확인

```sql
-- RLS가 제대로 활성화되었는지 확인
SELECT schemaname, tablename, rowsecurity
FROM pg_tables
WHERE tablename = 'app_policy';

-- 예상 결과: rowsecurity = true
```

### 2. RLS 정책 확인

```sql
-- 생성된 정책 목록 확인
SELECT policyname, cmd, qual
FROM pg_policies
WHERE tablename = 'app_policy';

-- 예상 결과:
-- policyname: allow_read_policy
-- cmd: SELECT
-- qual: (is_active = true)
```

### 3. 클라이언트 관점 테스트

```sql
-- anon 키로 조회 (앱에서 보는 것과 동일)
-- is_active = TRUE인 행만 보임
SELECT * FROM app_policy;

-- 결과: is_active = FALSE인 행은 보이지 않음 ✅
```

### 4. 관리자 관점 테스트 (SQL Editor)

```sql
-- SQL Editor는 RLS를 우회하므로 모든 행 조회 가능
SELECT 
  app_id, 
  is_active, 
  active_popup_type,
  CASE 
    WHEN is_active THEN '✅ 활성 (클라이언트에 노출)'
    ELSE '🔒 비활성 (클라이언트에서 숨김)'
  END as status
FROM app_policy;
```

---

## 🔐 RLS 보안 설명

### RLS가 보호하는 것

| 상황 | RLS 없음 | RLS 있음 |
|------|---------|---------|
| 준비 중 설정 조회 | ❌ 노출됨 | ✅ 차단됨 |
| 악의적 조회 시도 | ❌ 성공함 | ✅ 차단됨 |
| 앱 코드 실수 | ❌ 잘못된 데이터 조회 | ✅ DB가 자동 보호 |
| 프로덕션 배포 | ❌ 보안 위험 | ✅ 안전함 |

### RLS 작동 원리

```
클라이언트 (anon 키 사용)
    ↓
SELECT * FROM app_policy WHERE app_id = '...'
    ↓
Supabase RLS 엔진
    ↓
자동으로 필터 추가: AND is_active = TRUE
    ↓
결과: is_active = TRUE인 행만 반환 ✅
```

### Supabase 대시보드에서 모든 데이터 보는 방법

1. **Table Editor** → 우측 상단 **"RLS Bypass"** 토글 ON
2. **SQL Editor** → 직접 쿼리 (RLS 자동 우회)
3. **Service Role 키** 사용 (관리자 전용)

---

## 🎯 2단계: 정책 변경 방법 (일상 운영)

### 📌 기본 규칙

- **운영 테이블만 UPDATE** 하면 됩니다
- **히스토리는 자동으로 저장**됩니다 (트리거가 알아서 처리)
- 직접 히스토리 테이블에 INSERT하지 마세요

---

### 📝 예시 1: 긴급 공지 켜기

**상황**: 서버 긴급 점검 안내를 띄워야 함

```sql
UPDATE app_policy SET
  is_active = TRUE,  -- 활성화
  active_popup_type = 'emergency',
  content = '서버 긴급 점검 중입니다. 15:00까지 완료 예정입니다.',
  download_url = 'https://status.example.com'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과**:
- ✅ 앱에서 긴급 공지 팝업 표시 (X 버튼 없음)
- ✅ 이전 설정이 자동으로 `app_policy_history`에 저장됨
- ✅ 버튼 클릭 시 status.example.com으로 이동

---

### 📝 예시 2: 강제 업데이트 설정

**상황**: 버전 5 미만 사용자는 앱을 사용할 수 없도록 강제 업데이트

```sql
UPDATE app_policy SET
  is_active = TRUE,  -- 활성화
  active_popup_type = 'force_update',
  content = DEFAULT,  -- 기본 메시지 사용
  download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord',
  min_supported_version = 5  -- 강제 업데이트 기준 버전
  -- latest_version_code는 건드리지 않음 (기존 값 유지 또는 NULL)
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과**:
- ✅ 버전 5 미만 사용자에게 강제 업데이트 팝업 표시
- ✅ 뒤로가기 버튼 차단 (앱 사용 불가)
- ✅ 이전 '긴급 공지' 설정이 히스토리에 저장됨

**참고**: 
- `force_update`는 **`min_supported_version`만 확인**합니다
- `latest_version_code`는 앱 로직에서 사용하지 않으므로 **업데이트할 필요 없음**
- Supabase UI에서 업데이트할 때도 `min_supported_version`만 입력하면 됩니다

---

### 📝 예시 3: 선택적 업데이트로 전환

**상황**: 버전 6이 출시되어 사용자에게 업데이트 권장

```sql
UPDATE app_policy SET
  is_active = TRUE,  -- 활성화
  active_popup_type = 'optional_update',
  content = '새로운 기능이 추가되었습니다. 업데이트를 권장합니다.',
  download_url = 'market://details?id=com.sweetapps.pocketchord',
  latest_version_code = 6  -- 권장 업데이트 버전
  -- min_supported_version은 건드리지 않음 (기존 값 유지 또는 NULL)
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과**:
- ✅ 버전 6 미만 사용자에게 선택적 업데이트 팝업 표시
- ✅ 닫기 버튼 있음 (사용자가 나중에 업데이트 가능)
- ✅ 이전 '강제 업데이트' 설정이 히스토리에 저장됨

**참고**: 
- `optional_update`는 **`latest_version_code`만 확인**합니다
- `min_supported_version`은 앱 로직에서 사용하지 않으므로 **업데이트할 필요 없음**
- Supabase UI에서 업데이트할 때도 `latest_version_code`만 입력하면 됩니다

---

### 📝 예시 4: 일반 공지

**상황**: 이벤트 안내나 새 기능 소개

```sql
UPDATE app_policy SET
  is_active = TRUE,  -- 활성화
  active_popup_type = 'notice',
  content = '🎉 코드 편집 기능이 추가되었습니다! 설정 메뉴에서 확인하세요.',
  download_url = NULL  -- 선택사항: 없으면 확인 버튼만, 있으면 클라이언트 구현에 따라 활용 가능
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과**:
- ✅ 일반 공지 팝업 표시 (닫기 가능)
- ✅ 이전 '선택적 업데이트' 설정이 히스토리에 저장됨

**참고**: 
- `download_url`은 **선택사항**입니다 (NULL 또는 값 모두 허용)
- 값을 넣으면 클라이언트 구현에 따라 "자세히 보기" 버튼 등으로 활용 가능
- 기본 구현에서는 무시됨

---

### 📝 예시 5: 모든 팝업 끄기

**상황**: 정상 운영 모드로 복귀

```sql
UPDATE app_policy SET
  is_active = FALSE,  -- 비활성화
  active_popup_type = 'none',
  content = NULL,
  download_url = NULL,
  min_supported_version = NULL,
  latest_version_code = NULL
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과**:
- ✅ 모든 팝업 사라짐
- ✅ 이전 '일반 공지' 설정이 히스토리에 저장됨

---

### 📝 예시 6: 설정은 유지하고 일시 중단만

**상황**: 팝업 설정은 그대로 두고 잠시만 끄고 싶음

```sql
-- 팝업만 끄기 (설정은 유지)
UPDATE app_policy SET
  is_active = FALSE
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 나중에 다시 켜기
UPDATE app_policy SET
  is_active = TRUE
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과**:
- ✅ `active_popup_type`, `content` 등 모든 설정이 그대로 유지됨
- ✅ `is_active`만 토글하여 즉시 ON/OFF 가능

---

## 📖 3단계: 히스토리 조회 방법

### 🔍 방법 1: 최근 변경 기록 10개 보기

```sql
SELECT 
  archived_at AS "저장시간",
  active_popup_type AS "팝업타입",
  content AS "메시지",
  min_supported_version AS "최소버전",
  latest_version_code AS "최신버전",
  change_reason AS "변경사유"
FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
ORDER BY archived_at DESC
LIMIT 10;
```

**결과 예시**:
```
저장시간              | 팝업타입          | 메시지              | 최소버전 | 최신버전
---------------------|------------------|---------------------|---------|----------
2025-01-20 15:30:00 | optional_update  | 새로운 기능이...    | NULL    | 6
2025-01-20 14:00:00 | force_update     | 더 안정적이고...    | 5       | NULL
2025-01-20 10:00:00 | emergency        | 서버 긴급 점검...   | NULL    | NULL
2025-01-19 18:00:00 | none             | NULL                | NULL    | NULL
```

---

### 🔍 방법 2: 오늘 변경된 기록만 보기

```sql
SELECT 
  archived_at AS "저장시간",
  active_popup_type AS "팝업타입",
  content AS "메시지"
FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
  AND archived_at::DATE = CURRENT_DATE
ORDER BY archived_at DESC;
```

---

### 🔍 방법 3: 가장 최근 변경 1개만 보기

```sql
SELECT * FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
ORDER BY archived_at DESC
LIMIT 1;
```

---

### 🔍 방법 4: 특정 기간 변경 횟수 확인

```sql
-- 최근 7일간 변경 횟수
SELECT COUNT(*) AS "변경횟수"
FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
  AND archived_at > NOW() - INTERVAL '7 days';
```

---

### 🔍 방법 5: 팝업 타입별 사용 빈도

```sql
-- 어떤 팝업을 가장 많이 사용했는지 확인
SELECT 
  active_popup_type AS "팝업타입",
  COUNT(*) AS "사용횟수"
FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
GROUP BY active_popup_type
ORDER BY COUNT(*) DESC;
```

---

## ⏪ 4단계: 이전 설정으로 되돌리기 (롤백)

### 📝 예시 1: 바로 이전 설정으로 복구

**상황**: 방금 변경한 설정이 문제가 있어서 되돌리고 싶음

```sql
-- 1) 바로 이전 설정 확인
SELECT * FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
ORDER BY archived_at DESC
LIMIT 1;

-- 2) 복구 실행
UPDATE app_policy
SET
  active_popup_type = h.active_popup_type,
  content = h.content,
  download_url = h.download_url,
  min_supported_version = h.min_supported_version,
  latest_version_code = h.latest_version_code
FROM (
  SELECT * FROM app_policy_history
  WHERE app_id = 'com.sweetapps.pocketchord.debug'
  ORDER BY archived_at DESC
  LIMIT 1
) h
WHERE app_policy.app_id = 'com.sweetapps.pocketchord.debug';
```

---

### 📝 예시 2: 2번째 이전 설정으로 복구

```sql
-- LIMIT 1 OFFSET 1 = 2번째 최근 기록
UPDATE app_policy
SET
  active_popup_type = h.active_popup_type,
  content = h.content,
  download_url = h.download_url,
  min_supported_version = h.min_supported_version,
  latest_version_code = h.latest_version_code
FROM (
  SELECT * FROM app_policy_history
  WHERE app_id = 'com.sweetapps.pocketchord.debug'
  ORDER BY archived_at DESC
  LIMIT 1 OFFSET 1  -- 0=가장 최근, 1=2번째, 2=3번째...
) h
WHERE app_policy.app_id = 'com.sweetapps.pocketchord.debug';
```

---

### 📝 예시 3: 특정 시간대 설정으로 복구

```sql
-- 1시간 전 설정으로 복구
UPDATE app_policy
SET
  active_popup_type = h.active_popup_type,
  content = h.content,
  download_url = h.download_url,
  min_supported_version = h.min_supported_version,
  latest_version_code = h.latest_version_code
FROM (
  SELECT * FROM app_policy_history
  WHERE app_id = 'com.sweetapps.pocketchord.debug'
    AND archived_at > NOW() - INTERVAL '1 hour'
  ORDER BY archived_at DESC
  LIMIT 1
) h
WHERE app_policy.app_id = 'com.sweetapps.pocketchord.debug';
```

---

## 🧹 5단계: 히스토리 정리 (선택)

### 오래된 기록 삭제

히스토리가 너무 많이 쌓이면 정리할 수 있습니다.

```sql
-- 90일 이전 기록 삭제
DELETE FROM app_policy_history
WHERE archived_at < NOW() - INTERVAL '90 days';

-- 180일 이전 기록 삭제
DELETE FROM app_policy_history
WHERE archived_at < NOW() - INTERVAL '180 days';
```

---

## 📊 자주 사용하는 쿼리 모음

### 1️⃣ 현재 활성 정책 확인

```sql
-- 지금 앱이 읽는 설정
SELECT 
  active_popup_type AS "현재팝업타입",
  content AS "메시지",
  download_url AS "URL",
  min_supported_version AS "최소버전",
  latest_version_code AS "최신버전"
FROM app_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

---

### 2️⃣ 변경 이력 요약

```sql
-- 최근 5개 변경 요약
SELECT 
  TO_CHAR(archived_at, 'MM-DD HH24:MI') AS "시간",
  active_popup_type AS "타입",
  CASE 
    WHEN min_supported_version IS NOT NULL THEN '강제 v' || min_supported_version
    WHEN latest_version_code IS NOT NULL THEN '선택 v' || latest_version_code
    WHEN active_popup_type = 'emergency' THEN '긴급 공지'
    WHEN active_popup_type = 'notice' THEN '일반 공지'
    ELSE '팝업 없음'
  END AS "설명"
FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
ORDER BY archived_at DESC
LIMIT 5;
```

---

### 3️⃣ 최근 24시간 변경 타임라인

```sql
SELECT 
  TO_CHAR(archived_at, 'HH24:MI') AS "시간",
  active_popup_type AS "팝업타입",
  SUBSTRING(content, 1, 30) || '...' AS "메시지미리보기"
FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
  AND archived_at > NOW() - INTERVAL '24 hours'
ORDER BY archived_at ASC;
```

---

## ⚠️ 주의사항 및 규칙

### ✅ 올바른 사용 방법

```sql
-- ✅ 운영 테이블만 UPDATE
UPDATE app_policy SET
  active_popup_type = 'force_update',
  content = DEFAULT,
  download_url = 'https://play.google.com/...',
  min_supported_version = 5
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- → 트리거가 자동으로 히스토리에 저장
```

---

### ❌ 잘못된 사용 방법

```sql
-- ❌ 히스토리 테이블에 직접 INSERT (절대 금지)
INSERT INTO app_policy_history (...) VALUES (...);

-- ❌ 운영 테이블을 DELETE (복구 어려움)
DELETE FROM app_policy WHERE app_id = '...';

-- ❌ 히스토리 테이블 전체 삭제 (절대 금지)
DROP TABLE app_policy_history;
```

---

### 🔧 실수 복구 방법

#### 1. 운영 테이블을 실수로 삭제한 경우

```sql
-- 히스토리가 있으면 복구 가능
INSERT INTO app_policy (
  app_id,
  active_popup_type,
  content,
  download_url,
  min_supported_version,
  latest_version_code
)
SELECT 
  app_id,
  active_popup_type,
  content,
  download_url,
  min_supported_version,
  latest_version_code
FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
ORDER BY archived_at DESC
LIMIT 1;
```

---

#### 2. 잘못된 설정을 UPDATE한 경우

```sql
-- 바로 이전 설정으로 롤백 (4단계 참고)
UPDATE app_policy
SET
  active_popup_type = h.active_popup_type,
  content = h.content,
  download_url = h.download_url,
  min_supported_version = h.min_supported_version,
  latest_version_code = h.latest_version_code
FROM (
  SELECT * FROM app_policy_history
  WHERE app_id = 'com.sweetapps.pocketchord.debug'
  ORDER BY archived_at DESC
  LIMIT 1
) h
WHERE app_policy.app_id = 'com.sweetapps.pocketchord.debug';
```

---

## 🎓 타입별 필수 필드 정리

| `active_popup_type` | `content` | `download_url` | `min_supported_version` | `latest_version_code` | 동작 |
|---------------------|-----------|----------------|------------------------|-----------------------|------|
| `emergency` | **필수** | **필수** | 사용 안 함 | 사용 안 함 | X 버튼 없음, URL 이동 |
| `force_update` | **필수** | **필수** | **필수** | 사용 안 함 | 뒤로가기 차단 |
| `optional_update` | **필수** | **필수** | 사용 안 함 | **필수** | 닫기 가능 |
| `notice` | **필수** | **선택** (NULL 가능) | 사용 안 함 | 사용 안 함 | 단순 공지 |
| `none` | NULL 가능 | NULL 가능 | 사용 안 함 | 사용 안 함 | 모든 팝업 끄기 |

### 필드 역할

- **`min_supported_version`**: 강제 업데이트 기준 (이 버전 **미만**은 앱 사용 불가)
- **`latest_version_code`**: 선택적 업데이트 권장 버전 (이 버전 **미만**에게 업데이트 권장)
- **`download_url` (notice 타입)**: 선택사항. 값이 있으면 클라이언트 구현에 따라 활용 가능 (예: "자세히 보기" 버튼)

### 📱 실전 예시: 버전별 설정 방법

#### 상황: 현재 릴리즈 버전이 2인 경우

**사용자 분포**:
- 버전 1 사용자: 30%
- 버전 2 사용자: 70%

**시나리오 1: 버전 1 사용자를 강제 업데이트**
```sql
UPDATE app_policy SET
  active_popup_type = 'force_update',
  min_supported_version = 2,  -- "2 미만(즉, 1)은 강제 업데이트"
  download_url = 'https://play.google.com/...'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 결과:
-- ✅ 버전 1 사용자 (30%) → 강제 업데이트 팝업 (앱 사용 불가)
-- ✅ 버전 2 사용자 (70%) → 팝업 없음 (정상 사용)
```

**시나리오 2: 버전 1 사용자에게 권장 (강제 아님)**
```sql
UPDATE app_policy SET
  active_popup_type = 'optional_update',
  latest_version_code = 2,  -- "2 미만(즉, 1)에게 업데이트 권장"
  download_url = 'https://play.google.com/...'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 결과:
-- ✅ 버전 1 사용자 (30%) → 선택적 업데이트 팝업 (닫기 가능)
-- ✅ 버전 2 사용자 (70%) → 팝업 없음 (정상 사용)
```

**시나리오 3: 모든 사용자 정상 사용 (업데이트 없음)**
```sql
UPDATE app_policy SET
  active_popup_type = 'none'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 결과:
-- ✅ 모든 사용자 → 팝업 없음 (정상 사용)
```

#### 상황: 버전 3을 출시한 후

**사용자 분포** (출시 직후):
- 버전 1: 10%
- 버전 2: 80%
- 버전 3: 10%

**시나리오 4: 버전 1, 2를 모두 강제 업데이트**
```sql
UPDATE app_policy SET
  active_popup_type = 'force_update',
  min_supported_version = 3,  -- "3 미만(1, 2)은 강제 업데이트"
  download_url = 'https://play.google.com/...'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 결과:
-- ✅ 버전 1, 2 사용자 (90%) → 강제 업데이트 팝업
-- ✅ 버전 3 사용자 (10%) → 팝업 없음
```

**시나리오 5: 버전 1만 강제, 버전 2는 권장**

먼저 강제 업데이트로 버전 1을 차단:
```sql
UPDATE app_policy SET
  active_popup_type = 'force_update',
  min_supported_version = 2,  -- 버전 1 차단
  download_url = 'https://play.google.com/...'
WHERE app_id = 'com.sweetapps.pocketchord';
```

일주일 후 버전 1이 사라지면 선택적 업데이트로 전환:
```sql
UPDATE app_policy SET
  active_popup_type = 'optional_update',
  latest_version_code = 3,  -- 버전 2에게 권장
  download_url = 'https://play.google.com/...'
WHERE app_id = 'com.sweetapps.pocketchord';

-- 결과:
-- ✅ 버전 2 사용자 → 선택적 업데이트 팝업 (닫기 가능)
-- ✅ 버전 3 사용자 → 팝업 없음
```

### 🎯 버전 설정 공식

| 목적 | 설정 값 | 예시 (현재 릴리즈: 2) |
|------|--------|---------------------|
| **특정 버전 이하를 강제 업데이트** | `min_supported_version = 차단할_버전 + 1` | 버전 1 차단 → `2` 입력 |
| **특정 버전 이하에게 권장** | `latest_version_code = 권장할_버전` | 버전 1에게 권장 → `2` 입력 |
| **모든 사용자 허용** | `active_popup_type = 'none'` | 팝업 없음 |

### ⚠️ 주의사항

1. **`min_supported_version`은 "이 버전 미만" 차단**
   - `min_supported_version = 2` → 버전 **1** 차단, 버전 2는 OK
   - `min_supported_version = 3` → 버전 **1, 2** 차단, 버전 3은 OK

2. **`latest_version_code`는 "이 버전 미만"에게 권장**
   - `latest_version_code = 2` → 버전 **1**에게 권장, 버전 2는 팝업 없음
   - `latest_version_code = 3` → 버전 **1, 2**에게 권장, 버전 3은 팝업 없음

3. **점진적 업데이트 전략 권장**
   ```
   1단계: force_update (오래된 버전 강제 차단)
   2단계: optional_update (중간 버전에게 권장)
   3단계: none (충분히 업데이트되면 팝업 끄기)
   ```

### 🔑 중요: 업데이트 시 주의사항

**Q: 팝업 타입을 바꿀 때 다른 필드를 NULL로 설정해야 하나?**  
**A: 아니요, 필요 없습니다!**

앱 로직은 `active_popup_type`에 따라 **해당 필드만 확인**합니다:

```kotlin
when (policy.activePopupType) {
    "force_update" -> {
        // min_supported_version만 확인
        // latest_version_code는 보지도 않음
        if (currentVersion < policy.minSupportedVersion) {
            showForceUpdate()
        }
    }
    "optional_update" -> {
        // latest_version_code만 확인
        // min_supported_version은 보지도 않음
        if (currentVersion < policy.latestVersionCode) {
            showOptionalUpdate()
        }
    }
}
```

**결론**: Supabase UI에서 팝업을 변경할 때:
- ✅ **사용하는 필드만 업데이트**하면 됩니다
- ✅ 다른 필드는 **무시**되므로 건드릴 필요 없음
- ✅ NULL로 초기화하는 것은 **선택사항** (깔끔하게 관리하려면 권장)
- ✅ `notice` 타입의 `download_url`은 선택사항 (있어도 되고 없어도 됨)

#### 🛡️ 안전성 보장

**Q1: 실수로 `force_update`에서 `latest_version_code`에 값을 넣으면?**  
**A: 문제 없습니다!** 앱이 완전히 무시합니다.

```sql
-- 예: 실수로 두 필드에 모두 값 입력
UPDATE app_policy SET
  active_popup_type = 'force_update',
  min_supported_version = 5,
  latest_version_code = 99;  -- 실수

-- 앱 동작:
-- ✅ min_supported_version(5)만 확인
-- ✅ latest_version_code(99)는 아예 참조 안 함
-- ✅ 사용자에게 영향 없음
```

**Q2: `notice` 타입에서 `download_url`에 값을 넣으면?**  
**A: 문제 없습니다!** 제약조건에서 허용됩니다.

```sql
-- notice에 download_url 설정
UPDATE app_policy SET
  active_popup_type = 'notice',
  content = '이벤트 참여하세요',
  download_url = 'https://event.example.com';  -- OK

-- 앱 동작:
-- ✅ 기본 구현: download_url 무시
-- ✅ 선택적 구현: "자세히 보기" 버튼으로 활용 가능
```

**이유**: 앱 로직이 `active_popup_type`에 따라 **사용할 필드만 선택적으로 읽기** 때문입니다.

---

## 📐 선택사항 필드 설계 철학

### 왜 "엄격한 제약" 대신 "선택적 허용"을 선택했는가?

본 시스템은 **운영 효율성**을 최우선으로 고려하여 설계되었습니다.

#### 선택사항으로 설계된 필드들:

| 팝업 타입 | 선택사항 필드 | 설명 |
|----------|-------------|------|
| `force_update` | `latest_version_code` | 사용 안 함 (optional_update 전용) |
| `optional_update` | `min_supported_version` | 사용 안 함 (force_update 전용) |
| `notice` | `download_url` | 확장 가능 (향후 활용 여지) |

---

### 🎯 설계 원칙: "제약보다 유연성"

#### 대안 A: 엄격한 제약조건 (채택 안 함 ❌)
```sql
-- 예: force_update일 때 latest_version_code를 NULL로 강제
ALTER TABLE app_policy ADD CONSTRAINT ...
CHECK (
  (active_popup_type = 'force_update' AND latest_version_code IS NULL)
  ...
);
```

**문제점**:
- ❌ 운영자가 타입 전환 시 3~4개 필드를 모두 관리해야 함
- ❌ 실수 시 에러 발생 → 긴급 상황에서 스트레스
- ❌ 제약조건 복잡도 증가
- ❌ 히스토리에서 컨텍스트 손실

---

#### 대안 B: 선택적 허용 (현재 설계 ✅)
```sql
-- 필수 필드만 체크, 나머지는 자유
CHECK (
  (active_popup_type = 'force_update' AND min_supported_version IS NOT NULL)
  OR
  (active_popup_type = 'optional_update' AND latest_version_code IS NOT NULL)
  ...
);
```

**장점**:
- ✅ **운영 속도**: 사용할 필드만 입력하면 됨
- ✅ **안전성**: 실수로 값을 넣어도 앱이 무시 → 사용자 영향 없음
- ✅ **긴급 대응**: 빠른 정책 변경 가능
- ✅ **히스토리 보존**: 모든 컨텍스트가 보존되어 나중에 분석 가능
- ✅ **확장성**: 향후 기능 추가 시 스키마 변경 불필요

---

### 📊 실전 비교: 타입 전환 시나리오

#### 상황: force_update → optional_update 전환

**엄격한 제약 (A안):**
```sql
UPDATE app_policy SET
  active_popup_type = 'optional_update',
  min_supported_version = NULL,      -- ⚠️ 반드시 NULL로 지워야 함
  latest_version_code = 6;           -- 새 값 입력
-- 2개 필드를 동시에 관리 (번거로움)
```

**선택적 허용 (B안 - 현재):**
```sql
UPDATE app_policy SET
  active_popup_type = 'optional_update',
  latest_version_code = 6;           -- 사용할 필드만 입력
-- min_supported_version은 그냥 놔둬도 됨 (앱이 무시)
```

→ **50% 적은 작업량, 100% 안전**

---

### 🔥 긴급 상황 대응

**상황**: 치명적 버그 발견, 즉시 강제 업데이트 필요 (새벽 3시)

**엄격한 제약:**
```sql
UPDATE app_policy SET
  active_popup_type = 'force_update',
  min_supported_version = 7,
  latest_version_code = NULL;  -- ⚠️ 까먹으면 에러!
-- ERROR: check constraint violated
-- 문서 찾아보고, 다시 실행... (시간 낭비)
```

**선택적 허용 (현재):**
```sql
UPDATE app_policy SET
  active_popup_type = 'force_update',
  min_supported_version = 7;
-- ✅ 즉시 완료!
```

→ **스트레스 상황에서 실수 방지**

---

### 💡 추가 이점: 히스토리 컨텍스트 보존

**엄격한 제약:**
```sql
-- 히스토리에 NULL만 저장됨
archived_at | min_supported | latest_version
------------|---------------|---------------
10:00       | 5             | NULL
14:00       | NULL          | 6
```
→ 이전 컨텍스트 손실

**선택적 허용 (현재):**
```sql
-- 모든 값이 보존됨
archived_at | min_supported | latest_version
------------|---------------|---------------
10:00       | 5             | 6 (이전 값)
14:00       | 5 (이전 값)  | 6
```
→ 완전한 이력 추적 가능

---

### 🎓 설계 교훈

**"완벽한 데이터"보다 "운영 가능한 시스템"**

1. **개발자 관점**: 깔끔한 데이터 선호
2. **운영자 관점**: 빠르고 안전한 작업 선호
3. **비즈니스 관점**: 긴급 대응 능력 중요

→ **운영자와 비즈니스 우선** ✅

---

### 📝 결론

본 시스템은 다음을 우선순위로 설계되었습니다:

1. **운영 효율성** (빠른 정책 변경)
2. **안전성** (실수해도 사용자 영향 없음)
3. **긴급 대응** (스트레스 상황 대응)
4. **확장성** (향후 기능 추가 용이)
5. **이력 추적** (완전한 컨텍스트 보존)

**트레이드오프**: 데이터 완벽함 < 운영 편의성

**검증**: 실제 운영 경험으로 검증된 접근 방식

---

## 🚀 실전 시나리오 예시

### 시나리오: 강제 업데이트 → 선택적 업데이트 → 정상 운영

```sql
-- 1) 오전 10시: 강제 업데이트 설정
UPDATE app_policy SET
  is_active = TRUE,  -- 활성화
  active_popup_type = 'force_update',
  content = DEFAULT,
  download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord',
  min_supported_version = 5  -- force_update는 이 필드만 사용
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- ✅ 히스토리에 이전 설정('none') 자동 저장됨

-- 2) 오후 2시: 문의가 많아서 선택적 업데이트로 완화
UPDATE app_policy SET
  active_popup_type = 'optional_update',
  latest_version_code = 5  -- optional_update는 이 필드만 사용
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- ✅ 히스토리에 오전 설정('force_update') 자동 저장됨
-- ✅ min_supported_version은 5로 남아있지만, optional_update는 이 값을 무시함

-- 3) 오후 5시: 충분히 업데이트되어 팝업 끄기
UPDATE app_policy SET
  is_active = FALSE,  -- 비활성화
  active_popup_type = 'none',
  content = NULL,
  download_url = NULL,
  latest_version_code = NULL
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- ✅ 히스토리에 오후 설정('optional_update') 자동 저장됨

-- 4) 히스토리 확인
SELECT 
  TO_CHAR(archived_at, 'HH24:MI') AS "시간",
  active_popup_type AS "타입",
  min_supported_version AS "최소버전",
  latest_version_code AS "최신버전"
FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
  AND archived_at::DATE = CURRENT_DATE
ORDER BY archived_at ASC;

-- 결과:
-- 시간  | 타입              | 최소버전 | 최신버전
-- -----|------------------|---------|----------
-- 10:00 | none             | NULL    | NULL
-- 14:00 | force_update     | 5       | NULL
-- 17:00 | optional_update  | NULL    | 5
```

---

## 🔗 클라이언트 연동 (Kotlin)

### 앱에서 정책 조회

```kotlin
// 운영 테이블에서 현재 정책 1건만 조회
// RLS 정책으로 is_active = TRUE인 행만 자동 필터링됨
val policy = supabase.postgrest
    .from("app_policy")
    .select {
        filter { eq("app_id", BuildConfig.SUPABASE_APP_ID) }
        limit(1)
    }
    .decodeList<AppPolicy>()
    .firstOrNull()

// 팝업 분기 처리 (타입별로 필요한 필드만 확인)
when (policy?.activePopupType) {
    "emergency" -> showEmergencyDialog(
        content = policy.content!!,
        downloadUrl = policy.downloadUrl!!
        // min_supported_version, latest_version_code는 참조 안 함
    )
    "force_update" -> {
        val currentVersion = BuildConfig.VERSION_CODE
        if (currentVersion < (policy.minSupportedVersion ?: 0)) {
            showForceUpdateDialog(
                content = policy.content!!,
                downloadUrl = policy.downloadUrl!!
            )
            // latest_version_code는 참조 안 함 (값이 있어도 무시)
        }
    }
    "optional_update" -> {
        val currentVersion = BuildConfig.VERSION_CODE
        if (currentVersion < (policy.latestVersionCode ?: 0)) {
            showOptionalUpdateDialog(
                content = policy.content!!,
                downloadUrl = policy.downloadUrl!!
            )
            // min_supported_version은 참조 안 함 (값이 있어도 무시)
        }
    }
    "notice" -> {
        // 기본: download_url 무시
        showNoticeDialog(content = policy.content!!)
        
        // 선택적 구현: download_url이 있으면 활용
        // if (policy.downloadUrl != null) {
        //     showNoticeDialogWithAction(
        //         content = policy.content!!,
        //         actionText = "자세히 보기",
        //         actionUrl = policy.downloadUrl
        //     )
        // } else {
        //     showNoticeDialog(content = policy.content!!)
        // }
    }
    "none", null -> return  // 팝업 없음
}
```

---

## 📚 추가 참고 사항

### 히스토리 테이블 성능 최적화

```sql
-- 인덱스 확인
SELECT 
  indexname,
  indexdef
FROM pg_indexes
WHERE tablename = 'app_policy_history';

-- 느린 쿼리가 있다면 추가 인덱스 생성
CREATE INDEX idx_history_popup_type 
ON app_policy_history (active_popup_type);
```

---

### 백업 및 복원

```sql
-- 히스토리 전체 백업 (CSV 내보내기)
COPY (
  SELECT * FROM app_policy_history
  ORDER BY archived_at DESC
) TO '/tmp/app_policy_history_backup.csv' CSV HEADER;

-- 복원
COPY app_policy_history 
FROM '/tmp/app_policy_history_backup.csv' CSV HEADER;
```

---

## ✅ 빠른 시작 체크리스트

- [ ] 1단계: SQL 실행하여 테이블 생성
- [ ] 초기 데이터 확인 (`SELECT * FROM app_policy`)
- [ ] 2단계: 테스트 정책 변경 (emergency → force → optional → none)
- [ ] 3단계: 히스토리 조회 확인 (`SELECT * FROM app_policy_history`)
- [ ] 4단계: 롤백 테스트 (이전 설정으로 복구)
- [ ] 앱에서 정책 조회 동작 확인
- [ ] 90일 이전 히스토리 정리 스케줄 설정 (선택)

---

## 🎯 요약

### 장점
- ✅ **운영 단순**: 앱은 항상 1건만 조회
- ✅ **빠른 성능**: 인덱스 없이도 빠름
- ✅ **완전한 감사 추적**: 모든 변경 이력 보존
- ✅ **쉬운 롤백**: 언제든 이전 설정 복구
- ✅ **자동 백업**: 트리거가 알아서 처리
- ✅ **유연한 설계**: 선택적 필드로 운영 효율성 극대화

### 운영 원칙
1. **운영 테이블만 UPDATE** (히스토리는 자동)
2. **히스토리는 읽기 전용**으로 사용
3. **정기적으로 오래된 히스토리 정리**
4. **롤백 전 반드시 히스토리 확인**
5. **사용할 필드만 입력** (나머지는 무시됨)

### 설계 철학
- 📐 **"완벽한 데이터"보다 "운영 가능한 시스템"**
- 🚀 **긴급 상황 대응 능력 우선**
- 🛡️ **실수해도 안전한 구조**
- 📈 **확장 가능한 아키텍처**

자세한 내용은 "선택사항 필드 설계 철학" 섹션을 참고하세요.

---

## 📞 문제 해결

### Q1: 히스토리가 저장되지 않아요
**A**: 트리거가 제대로 생성되었는지 확인하세요.
```sql
SELECT tgname FROM pg_trigger WHERE tgrelid = 'app_policy'::regclass;
-- 결과에 'trigger_backup_policy'가 있어야 함
```

### Q2: 히스토리에 is_active가 FALSE만 나와요

**중요 개념**: 트리거는 **변경 전(OLD) 값**을 히스토리에 저장합니다!

| 현재 상태 | 변경 후 | 히스토리에 저장 |
|----------|--------|---------------|
| `is_active = FALSE` | TRUE로 변경 | **FALSE** ✅ |
| `is_active = TRUE` | FALSE로 변경 | **TRUE** ✅ |

**A**: 세 가지 원인이 있을 수 있습니다.

#### 원인 1: 아직 활성화한 적이 없음 (정상)
```sql
-- 확인: 운영 테이블이 FALSE인지 확인
SELECT is_active FROM app_policy;
-- 결과가 FALSE → 정상 (한 번도 TRUE로 설정 안 함)

-- 추가 확인: 히스토리에 TRUE가 있는지
SELECT COUNT(*) FILTER (WHERE is_active = TRUE) as active_count
FROM app_policy_history;
-- active_count = 0 → 정상
```

#### 원인 2: INSERT만 하고 UPDATE를 안 함
트리거는 **UPDATE 시에만 작동**합니다.
```sql
-- 확인: 히스토리 레코드 수
SELECT COUNT(*) FROM app_policy_history;
-- 결과가 0 → UPDATE를 한 번도 안 함 (정상)
```

#### 원인 3: 트리거 오작동 (비정상)

**단계 1: 트리거 존재 확인**
```sql
-- 트리거 상태 확인
SELECT tgname, tgenabled
FROM pg_trigger
WHERE tgrelid = 'app_policy'::regclass;

-- 예상 결과:
-- tgname: trigger_backup_policy
-- tgenabled: O (활성화)
```

**단계 2: 테스트 시나리오**
```sql
-- 1) TRUE로 변경
UPDATE app_policy SET is_active = TRUE 
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 2) 히스토리 확인 (FALSE가 저장되어야 함)
SELECT is_active FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
ORDER BY archived_at DESC LIMIT 1;
-- 예상: FALSE (변경 전 상태)

-- 3) FALSE로 되돌리기
UPDATE app_policy SET is_active = FALSE
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 4) 히스토리 확인 (TRUE가 저장되어야 함)
SELECT is_active FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
ORDER BY archived_at DESC LIMIT 1;
-- 예상: TRUE (변경 전 상태)
```

**여전히 FALSE만 나온다면**: 트리거 재생성 필요

**단계 3: 트리거 재생성**
```sql
-- 기존 트리거 삭제
DROP TRIGGER IF EXISTS trigger_backup_policy ON app_policy;
DROP FUNCTION IF EXISTS backup_policy_to_history();

-- 1단계 SQL의 8번 섹션 전체를 다시 실행하세요
```

**단계 4: 전체 상태 확인 스크립트**
```sql
-- 한 번에 모든 상태 확인
SELECT 
  '운영 테이블' as source,
  is_active,
  active_popup_type,
  created_at as timestamp
FROM app_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug'

UNION ALL

SELECT 
  '히스토리' as source,
  is_active,
  active_popup_type,
  archived_at as timestamp
FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'

ORDER BY timestamp DESC;
```

**단계 5: 완전 테스트 시나리오 (필요시)**
```sql
-- 전체 흐름 테스트
-- 1) emergency로 활성화
UPDATE app_policy SET
  is_active = TRUE,
  active_popup_type = 'emergency',
  content = '테스트',
  download_url = 'https://test.com'
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 2) notice로 변경 (is_active는 TRUE 유지)
UPDATE app_policy SET
  active_popup_type = 'notice',
  content = '테스트2'
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 3) 비활성화
UPDATE app_policy SET is_active = FALSE
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 4) 히스토리 전체 확인
SELECT 
  archived_at,
  is_active,
  active_popup_type,
  CASE is_active
    WHEN TRUE THEN '✅ 활성'
    WHEN FALSE THEN '❌ 비활성'
  END as status
FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
ORDER BY archived_at ASC;

-- 예상 결과:
-- Row 1: FALSE, none (초기)
-- Row 2: TRUE, emergency (emergency로 변경 전)
-- Row 3: TRUE, notice (notice로 변경 전)
```

### Q3: 히스토리가 너무 많이 쌓였어요
**A**: 정기적으로 오래된 기록을 정리하세요.
```sql
DELETE FROM app_policy_history
WHERE archived_at < NOW() - INTERVAL '90 days';
```

### Q4: 운영 테이블을 실수로 삭제했어요
**A**: 히스토리에서 복구하세요 (4단계 참고).

### Q5: 특정 시점의 설정을 정확히 알고 싶어요
**A**: 히스토리를 시간 순으로 조회하세요.
```sql
SELECT * FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
  AND archived_at BETWEEN '2025-01-20 10:00' AND '2025-01-20 18:00'
ORDER BY archived_at ASC;
```

---

## 💻 Supabase UI로 팝업 관리하기 (실전 가이드)

### 🖱️ Supabase Table Editor 사용법

Supabase 대시보드에서 직접 팝업을 관리하는 방법입니다.

#### 1️⃣ 강제 업데이트 설정

**Supabase 대시보드** → **Table Editor** → `app_policy` 테이블 → 행 클릭

```
필드 입력:
┌─────────────────────────┬──────────────────────────────────┐
│ is_active               │ ✅ TRUE 체크                      │
│ active_popup_type       │ force_update 선택                │
│ content                 │ (기본값 그대로 또는 커스텀 메시지) │
│ download_url            │ https://play.google.com/store... │
│ min_supported_version   │ 5 입력                           │
│ latest_version_code     │ (건드리지 않음)                   │
└─────────────────────────┴──────────────────────────────────┘

→ Save 버튼 클릭 ✅
```

**결과**: 버전 5 미만 사용자에게 강제 업데이트 팝업 표시

---

#### 2️⃣ 선택적 업데이트로 전환

같은 행을 다시 클릭하여 수정:

```
필드 입력:
┌─────────────────────────┬──────────────────────────────────┐
│ is_active               │ ✅ TRUE (유지)                    │
│ active_popup_type       │ optional_update로 변경           │
│ latest_version_code     │ 6 입력                           │
│ min_supported_version   │ (건드리지 않음 - 5로 남아있어도 OK)│
└─────────────────────────┴──────────────────────────────────┘

→ Save 버튼 클릭 ✅
```

**결과**: 버전 6 미만 사용자에게 선택적 업데이트 권장

---

#### 3️⃣ 팝업 끄기 (일시 중단)

```
필드 입력:
┌─────────────────────────┬──────────────────────────────────┐
│ is_active               │ ❌ FALSE로 변경                   │
└─────────────────────────┴──────────────────────────────────┘

→ Save 버튼 클릭 ✅
```

**결과**: 모든 팝업 사라짐 (설정은 그대로 유지)

---

#### 4️⃣ 다시 활성화

```
필드 입력:
┌─────────────────────────┬──────────────────────────────────┐
│ is_active               │ ✅ TRUE로 변경                    │
└─────────────────────────┴──────────────────────────────────┘

→ Save 버튼 클릭 ✅
```

**결과**: 이전 설정으로 팝업 즉시 재활성화

---

### 🔑 핵심 원칙

#### ✅ 꼭 기억하세요

1. **타입별로 사용하는 필드만 수정**하면 됩니다:
   - `force_update` → `min_supported_version`만 입력
   - `optional_update` → `latest_version_code`만 입력

2. **다른 필드는 건드리지 않아도 됩니다**:
   - 앱은 `active_popup_type`에 따라 필요한 필드만 확인
   - 사용하지 않는 필드에 값이 남아있어도 **완전히 무시**됨

3. **NULL로 초기화는 선택사항**:
   - 깔끔하게 관리하고 싶다면 사용하지 않는 필드를 비워도 됨
   - 하지만 **필수는 아님** (앱 동작에 영향 없음)

---

### 📋 체크리스트 (Supabase UI 사용)

#### 강제 업데이트 설정 시
- [ ] `is_active` = TRUE
- [ ] `active_popup_type` = force_update
- [ ] `min_supported_version` = (버전 번호)
- [ ] `download_url` = (스토어 URL)
- [ ] `content` = (메시지 - 선택)

#### 선택적 업데이트 설정 시
- [ ] `is_active` = TRUE
- [ ] `active_popup_type` = optional_update
- [ ] `latest_version_code` = (버전 번호)
- [ ] `download_url` = (스토어 URL)
- [ ] `content` = (메시지 - 선택)

#### 긴급 공지 설정 시
- [ ] `is_active` = TRUE
- [ ] `active_popup_type` = emergency
- [ ] `content` = (공지 메시지)
- [ ] `download_url` = (안내 URL)

#### 일반 공지 설정 시
- [ ] `is_active` = TRUE
- [ ] `active_popup_type` = notice
- [ ] `content` = (공지 메시지)

---

### 🎬 실전 예시: 팝업 타입 변경

**상황**: 강제 업데이트에서 선택적 업데이트로 전환

```
초기 상태:
active_popup_type = 'force_update'
min_supported_version = 5
latest_version_code = NULL

↓ Supabase UI에서 수정

1. active_popup_type → 'optional_update'로 변경
2. latest_version_code → 6 입력
3. Save ✅

최종 상태:
active_popup_type = 'optional_update'
min_supported_version = 5 (그대로 남아있음)
latest_version_code = 6 (새로 입력)

→ 앱은 latest_version_code(6)만 확인하고,
   min_supported_version(5)는 완전히 무시함 ✅
```

---

### ⚠️ 주의: 제약조건 에러

Supabase UI에서 저장 시 에러가 발생하면:

**에러 예시**:
```
check constraint "check_min_version_with_force_update" violated
```

**원인**: `force_update`인데 `min_supported_version`이 비어있음

**해결**:
- `force_update` → `min_supported_version` 필수 입력
- `optional_update` → `latest_version_code` 필수 입력
- `emergency`/`notice` → `content` 필수 입력

---

**문서 버전**: 1.0  
**최종 업데이트**: 2025-01-08  
**작성자**: PocketChord Development Team



