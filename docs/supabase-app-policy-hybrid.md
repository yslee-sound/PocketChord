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
-- 1) 오전 9시: 강제 업데이트 설정 준비 (아직 비활성)
UPDATE app_policy SET
  active_popup_type = 'force_update',
  is_active = false,  -- 아직 활성화 안 함
  content = DEFAULT,
  download_url = 'https://play.google.com/...',
  min_supported_version = 5
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- ✅ 설정은 저장되지만 사용자에게는 팝업이 표시되지 않음

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

### 요약

| 필드 | 역할 |
|------|------|
| `active_popup_type` | **어떤** 팝업을 보여줄지 결정 |
| `is_active` | **언제** 팝업을 보여줄지 제어 |

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
  is_active BOOLEAN NOT NULL DEFAULT TRUE,  -- 팝업 활성화 여부 (켜기/끄기 스위치)
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
-- 6. RLS (Row Level Security)
-- ==========================================
ALTER TABLE public.app_policy ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "allow_read_policy" ON public.app_policy;
CREATE POLICY "allow_read_policy"
ON public.app_policy
FOR SELECT
USING (is_active = TRUE);  -- 활성화된 정책만 읽기 가능

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
INSERT INTO public.app_policy (app_id, active_popup_type)
VALUES
  ('com.sweetapps.pocketchord.debug', 'none'),
  ('com.sweetapps.pocketchord', 'none')
ON CONFLICT (app_id) DO NOTHING;  -- 이미 있으면 건너뛰기
```

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
  active_popup_type = 'force_update',
  content = DEFAULT,  -- 기본 메시지 사용
  download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord',
  min_supported_version = 5
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과**:
- ✅ 버전 5 미만 사용자에게 강제 업데이트 팝업 표시
- ✅ 뒤로가기 버튼 차단 (앱 사용 불가)
- ✅ 이전 '긴급 공지' 설정이 히스토리에 저장됨

---

### 📝 예시 3: 선택적 업데이트로 전환

**상황**: 버전 6이 출시되어 사용자에게 업데이트 권장

```sql
UPDATE app_policy SET
  active_popup_type = 'optional_update',
  content = '새로운 기능이 추가되었습니다. 업데이트를 권장합니다.',
  download_url = 'market://details?id=com.sweetapps.pocketchord',
  min_supported_version = NULL,  -- 강제 업데이트 해제
  latest_version_code = 6
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과**:
- ✅ 버전 6 미만 사용자에게 선택적 업데이트 팝업 표시
- ✅ 닫기 버튼 있음 (사용자가 나중에 업데이트 가능)
- ✅ 이전 '강제 업데이트' 설정이 히스토리에 저장됨

---

### 📝 예시 4: 일반 공지

**상황**: 이벤트 안내나 새 기능 소개

```sql
UPDATE app_policy SET
  active_popup_type = 'notice',
  content = '🎉 코드 편집 기능이 추가되었습니다! 설정 메뉴에서 확인하세요.',
  download_url = NULL  -- 공지는 URL 불필요
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과**:
- ✅ 일반 공지 팝업 표시 (닫기 가능)
- ✅ 이전 '선택적 업데이트' 설정이 히스토리에 저장됨

---

### 📝 예시 5: 모든 팝업 끄기

**상황**: 정상 운영 모드로 복귀

```sql
UPDATE app_policy SET
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

| `active_popup_type` | `content` | `download_url` | 추가 필드 | 동작 |
|---------------------|-----------|----------------|----------|------|
| `emergency` | **필수** | **필수** | - | X 버튼 없음, URL 이동 |
| `force_update` | **필수** | **필수** | `min_supported_version` **(필수)** | 뒤로가기 차단 |
| `optional_update` | **필수** | **필수** | `latest_version_code` **(필수)** | 닫기 가능 |
| `notice` | **필수** | 선택 | - | 단순 공지 |
| `none` | NULL 가능 | NULL 가능 | - | 모든 팝업 끄기 |

---

## 🚀 실전 시나리오 예시

### 시나리오: 강제 업데이트 → 선택적 업데이트 → 정상 운영

```sql
-- 1) 오전 10시: 강제 업데이트 설정
UPDATE app_policy SET
  active_popup_type = 'force_update',
  content = DEFAULT,
  download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord',
  min_supported_version = 5
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- ✅ 히스토리에 이전 설정('none') 자동 저장됨

-- 2) 오후 2시: 문의가 많아서 선택적 업데이트로 완화
UPDATE app_policy SET
  active_popup_type = 'optional_update',
  min_supported_version = NULL,  -- 강제 해제
  latest_version_code = 5
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- ✅ 히스토리에 오전 설정('force_update') 자동 저장됨

-- 3) 오후 5시: 충분히 업데이트되어 팝업 끄기
UPDATE app_policy SET
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
val policy = supabase.postgrest
    .from("app_policy")
    .select {
        filter { eq("app_id", BuildConfig.SUPABASE_APP_ID) }
        limit(1)
    }
    .decodeList<AppPolicy>()
    .firstOrNull()

// 팝업 분기 처리
when (policy?.activePopupType) {
    "emergency" -> showEmergencyDialog(
        content = policy.content!!,
        downloadUrl = policy.downloadUrl!!
    )
    "force_update" -> {
        val currentVersion = BuildConfig.VERSION_CODE
        if (currentVersion < (policy.minSupportedVersion ?: 0)) {
            showForceUpdateDialog(
                content = policy.content!!,
                downloadUrl = policy.downloadUrl!!
            )
        }
    }
    "optional_update" -> {
        val currentVersion = BuildConfig.VERSION_CODE
        if (currentVersion < (policy.latestVersionCode ?: 0)) {
            showOptionalUpdateDialog(
                content = policy.content!!,
                downloadUrl = policy.downloadUrl!!
            )
        }
    }
    "notice" -> showNoticeDialog(content = policy.content!!)
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

### 운영 원칙
1. **운영 테이블만 UPDATE** (히스토리는 자동)
2. **히스토리는 읽기 전용**으로 사용
3. **정기적으로 오래된 히스토리 정리**
4. **롤백 전 반드시 히스토리 확인**

---

## 📞 문제 해결

### Q1: 히스토리가 저장되지 않아요
**A**: 트리거가 제대로 생성되었는지 확인하세요.
```sql
SELECT tgname FROM pg_trigger WHERE tgrelid = 'app_policy'::regclass;
-- 결과에 'trigger_backup_policy'가 있어야 함
```

### Q2: 히스토리가 너무 많이 쌓였어요
**A**: 정기적으로 오래된 기록을 정리하세요.
```sql
DELETE FROM app_policy_history
WHERE archived_at < NOW() - INTERVAL '90 days';
```

### Q3: 운영 테이블을 실수로 삭제했어요
**A**: 히스토리에서 복구하세요 (4단계 참고).

### Q4: 특정 시점의 설정을 정확히 알고 싶어요
**A**: 히스토리를 시간 순으로 조회하세요.
```sql
SELECT * FROM app_policy_history
WHERE app_id = 'com.sweetapps.pocketchord.debug'
  AND archived_at BETWEEN '2025-01-20 10:00' AND '2025-01-20 18:00'
ORDER BY archived_at ASC;
```

---

**문서 버전**: 1.0  
**최종 업데이트**: 2025-01-08  
**작성자**: PocketChord Development Team

