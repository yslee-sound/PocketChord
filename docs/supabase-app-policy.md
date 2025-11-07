# Supabase App Policy (선택지 B) — 스키마 & RLS 가이드

본 문서는 PocketChord가 단일 정책 테이블(app_policy)만 읽어 팝업 1개를 결정하도록 하는 운영 방식(선택지 B)의 SQL 스키마, RLS 정책, 운영 절차를 제공합니다.

목표
- is_active = true만 노출, 단 하나의 정책 레코드로 팝업 결정
- 우선순위: 긴급 공지 > 강제 업데이트 > 선택적 업데이트 > 일반 공지
- 클라이언트는 app_policy 1건만 읽고 판단 (성공 시 다른 테이블 무시)

권장 해석 규칙
- 강제 업데이트: current_version_code < min_supported_version
- 선택적 업데이트: update_is_active = true AND latest_version_code > current_version_code
- 긴급 공지: emergency_is_active = true AND (emergency_title, emergency_content 존재)
- 일반 공지(선택): notice_is_active = true AND (notice_title, notice_content 존재)

## app_id 설계 가이드 (다중 앱 / 다중 환경 운영)
app_id 값은 “해당 정책이 어느 앱(또는 앱 변형/환경)에 적용되는지”를 구분하는 식별자입니다. **반드시 실제 Android 패키지명과 같을 필요는 없습니다.** 아래 전략 중 상황에 맞게 선택하세요.

| 상황 | 추천 app_id 형태 | 예시 |
|------|-----------------|------|
| 단일 Android 앱만 운영 | 패키지명 그대로 | `com.sweetapps.pocketchord` |
| Android / iOS 분리 관리 | 플랫폼 접미/접두 | `pocketchord-android`, `pocketchord-ios` |
| 운영/스테이징 분리 | 환경 접미 | `pocketchord-android-prod`, `pocketchord-android-stg` |
| 여러 브랜드/화이트라벨 | 브랜드 키 + 플랫폼 | `brandA-android`, `brandB-android` |
| 채널(내수/글로벌) 구분 | 채널 코드 포함 | `pocketchord-kr`, `pocketchord-global` |

설계 팁
- 앱 코드에서는 BuildConfig.SUPABASE_APP_ID 값을 서버 app_policy.app_id와 정확히 일치시키기만 하면 됩니다.
- 환경 분리가 필요하다면 별도 env 컬럼을 추가하는 대신 app_id에 환경을 포함(간단)하거나, 스키마 확장으로 (app_id, env) UNIQUE를 둘 수도 있습니다.
- 화이트라벨 다수가 생길 가능성이 있다면 일찍부터 패키지명 대신 논리적 slug를 선택하는 편이 이후 변경 비용이 낮습니다.

빠른 적용 SQL (복사/실행)

```sql
-- 1) 테이블 생성
CREATE TABLE IF NOT EXISTS public.app_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  app_id TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,

  -- 업데이트 정책
  min_supported_version INTEGER NULL,
  latest_version_code INTEGER NULL,
  update_is_active BOOLEAN NOT NULL DEFAULT FALSE,
  download_url TEXT NULL,

  -- 긴급 공지
  emergency_is_active BOOLEAN NOT NULL DEFAULT FALSE,
  emergency_title TEXT NULL,
  emergency_content TEXT NULL,
  emergency_dismissible BOOLEAN NOT NULL DEFAULT FALSE,
  emergency_redirect_url TEXT NULL,

  -- (선택) 정책 기반 일반 공지
  notice_is_active BOOLEAN NULL,
  notice_title TEXT NULL,
  notice_content TEXT NULL
);

-- 2) 앱당 1건 정책 보장 (운영 단순화)
-- NOTE: PostgreSQL은 ADD CONSTRAINT IF NOT EXISTS를 지원하지 않으므로,
--       재실행 가능한 스크립트를 위해 UNIQUE 인덱스를 사용합니다.
CREATE UNIQUE INDEX IF NOT EXISTS idx_app_policy_app_id_unique
  ON public.app_policy(app_id);

-- 3) 성능 인덱스 (선택)
CREATE INDEX IF NOT EXISTS idx_app_policy_active ON public.app_policy (is_active);

-- 4) RLS 활성화
ALTER TABLE public.app_policy ENABLE ROW LEVEL SECURITY;

-- 5) 누구나(is anon) 활성 정책 읽기 허용 (클라이언트는 anon key)
DROP POLICY IF EXISTS "read active policy" ON public.app_policy;
CREATE POLICY "read active policy"
ON public.app_policy
FOR SELECT
USING (is_active = TRUE);

-- 6) 쓰기 정책 없음 (대시보드/SQL 에디터/서버 키로만 변경)
--    필요 시 관리 계정 허용 정책 예시를 아래 참고하세요.
```

관리 계정 쓰기 허용 (선택)
- Supabase Auth를 사용하고, 특정 관리자만 정책을 수정하도록 하고 싶다면 다음 예시를 참고하세요.
- 이메일 화이트리스트 방식(예시):

```sql
-- 관리자 이메일 목록 (배포 전 실제 이메일로 교체)
DO $$ BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type WHERE typname = 'text_list'
  ) THEN
    CREATE TYPE text_list AS TEXT[];
  END IF;
END $$;

-- 정책 생성 전 변수처럼 쓸 배열 (직접 문자열로 치환해도 무방)
-- 예: '{"admin@example.com","owner@example.com"}'::text[]

DROP POLICY IF EXISTS "admin can write" ON public.app_policy;
CREATE POLICY "admin can write"
ON public.app_policy
FOR ALL
TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM auth.users u
    WHERE u.id = auth.uid()
      AND u.email = ANY ('{"admin@example.com"}'::text[])
  )
)
WITH CHECK (
  EXISTS (
    SELECT 1
    FROM auth.users u
    WHERE u.id = auth.uid()
      AND u.email = ANY ('{"admin@example.com"}'::text[])
  )
);
```

시드 예제

```sql
-- 앱별 1건 정책 입력 예시 (app_id: 패키지명 또는 논리적 식별자 slug 선택)
INSERT INTO public.app_policy (
  app_id,
  is_active,
  min_supported_version,
  latest_version_code,
  update_is_active,
  download_url,
  emergency_is_active,
  emergency_title,
  emergency_content,
  emergency_dismissible,
  emergency_redirect_url,
  notice_is_active,
  notice_title,
  notice_content
) VALUES (
  'pocketchord-android-prod',  -- 예시: 논리적 식별자
  TRUE,
  NULL,                -- 강제 없음: 필요 시 빌드보다 큰 값
  NULL,                -- 선택적 업데이트 없음
  FALSE,               -- 선택적 업데이트 off
  NULL,                -- 스토어/직링크 URL (선택)
  FALSE,               -- 긴급 공지 off
  NULL,
  NULL,
  FALSE,
  NULL,
  NULL,
  NULL,
  NULL
)
ON CONFLICT (app_id) DO UPDATE SET
  is_active = EXCLUDED.is_active,
  min_supported_version = EXCLUDED.min_supported_version,
  latest_version_code = EXCLUDED.latest_version_code,
  update_is_active = EXCLUDED.update_is_active,
  download_url = EXCLUDED.download_url,
  emergency_is_active = EXCLUDED.emergency_is_active,
  emergency_title = EXCLUDED.emergency_title,
  emergency_content = EXCLUDED.emergency_content,
  emergency_dismissible = EXCLUDED.emergency_dismissible,
  emergency_redirect_url = EXCLUDED.emergency_redirect_url,
  notice_is_active = EXCLUDED.notice_is_active,
  notice_title = EXCLUDED.notice_title,
  notice_content = EXCLUDED.notice_content;
```

운영 절차 (SOP)
- 긴급 공지 켜기: emergency_is_active=true, emergency_title/content 입력 (필요시 redirect/dismissible)
- 강제 업데이트: min_supported_version를 현재 앱 버전보다 크게 설정
- 선택적 업데이트: update_is_active=true, latest_version_code를 현재 앱 버전보다 크게 설정, (선택) download_url
- 일반 공지: notice_is_active=true, notice_title/notice_content 입력
- 모두 끄기: 위 플래그 false 또는 is_active=false

검증/조회 쿼리

```sql
-- 현재 활성 정책 확인
SELECT *
FROM public.app_policy
WHERE app_id = 'pocketchord-android-prod' AND is_active = TRUE;
```

롤백/삭제 (주의)

```sql
-- 정책 테이블/정책 제거 (필요 시)
DROP POLICY IF EXISTS "read active policy" ON public.app_policy;
DROP POLICY IF EXISTS "admin can write" ON public.app_policy;
ALTER TABLE public.app_policy DISABLE ROW LEVEL SECURITY;
DROP TABLE IF EXISTS public.app_policy;
```

클라이언트 연동 메모
- 앱은 `app_policy`를 먼저 조회해 결정합니다(정책이 있으면 그것만 사용). 실패/부재 시 기존 테이블(announcements/update_info)로 폴백.
- 정책이 강제를 요구하지 않으면, 클라이언트는 기존의 강제 업데이트 캐시를 즉시 정리해 “계속 뜨는” 현상을 방지합니다.

보안 주의
- 앱(anon key)은 SELECT만 허용됩니다. 쓰기 정책은 기본적으로 없음(대시보드/SQL 에디터/서버 키로만 변경).
- 관리 정책을 여는 경우, 반드시 허용 이메일/역할을 제한하세요.

부록: 참고 규칙
- 강제 업데이트: current_version_code < min_supported_version
- 선택적 업데이트: update_is_active AND latest_version_code > current_version_code
- 우선순위: 긴급 > 강제 > 선택적 > 공지 (항상 1개만 표시)

## 전체 설정 스크립트 (한 번에 실행 가능)
아래 스크립트는 테이블 생성 → 인덱스 → RLS → 정책 → RPC 함수까지 한 번에 구성합니다.

```sql
-- =====================================================
-- 0. 사전: 기존 announcements/update_info 유지하되 점진적 마이그레이션
--    앱은 우선 rpc_get_app_popup → 없으면 app_policy → 없으면 기존 테이블 폴백
-- =====================================================

-- 1. 정책 테이블 생성
CREATE TABLE IF NOT EXISTS public.app_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  app_id TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,

  -- 업데이트 정책
  min_supported_version INTEGER NULL,         -- 현재 버전보다 크면 강제 업데이트
  latest_version_code INTEGER NULL,           -- 선택적 업데이트 대상 최신 버전
  update_is_active BOOLEAN NOT NULL DEFAULT FALSE,
  download_url TEXT NULL,                     -- 스토어/직접 업데이트 링크

  -- 긴급 공지
  emergency_is_active BOOLEAN NOT NULL DEFAULT FALSE,
  emergency_title TEXT NULL,
  emergency_content TEXT NULL,
  emergency_dismissible BOOLEAN NOT NULL DEFAULT FALSE,
  emergency_redirect_url TEXT NULL,

  -- 일반 공지(정책 기반)
  notice_is_active BOOLEAN NULL,
  notice_title TEXT NULL,
  notice_content TEXT NULL
);

-- 2. 앱당 1행 제약
-- 재실행 가능하게 UNIQUE 인덱스로 보장
CREATE UNIQUE INDEX IF NOT EXISTS idx_app_policy_app_id_unique
  ON public.app_policy(app_id);

-- 3. 인덱스(활성 행 빠른 조회)
CREATE INDEX IF NOT EXISTS idx_app_policy_active ON public.app_policy (is_active);

-- 4. RLS 활성화 및 읽기 정책
ALTER TABLE public.app_policy ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "read active policy" ON public.app_policy;
CREATE POLICY "read active policy"
ON public.app_policy FOR SELECT
USING (is_active = TRUE);

-- (선택) 관리자 쓰기 정책: 이메일 화이트리스트 적용 시
-- DROP POLICY IF EXISTS "admin can write" ON public.app_policy;
-- CREATE POLICY "admin can write"
-- ON public.app_policy FOR ALL TO authenticated
-- USING (EXISTS (SELECT 1 FROM auth.users u WHERE u.id = auth.uid() AND u.email = ANY ('{"admin@example.com"}'::text[])))
-- WITH CHECK (EXISTS (SELECT 1 FROM auth.users u WHERE u.id = auth.uid() AND u.email = ANY ('{"admin@example.com"}'::text[])));

-- 5. RPC: 단 1건 팝업 결정 (우선순위: 긴급 > 강제 > 선택 > 공지)
-- 반환 필드: type, title, content, dismissible, redirect_url, version_code, download_url, is_force
CREATE OR REPLACE FUNCTION public.rpc_get_app_popup(
  p_app_id TEXT,
  p_current_version INTEGER
) RETURNS JSONB
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
  v_policy RECORD;
  v_result JSONB;
BEGIN
  SELECT * INTO v_policy
  FROM public.app_policy
  WHERE app_id = p_app_id AND is_active = TRUE
  LIMIT 1;

  IF v_policy IS NULL THEN
    RETURN NULL; -- 정책 없음 → 클라이언트 폴백 동작
  END IF;

  -- 1) 긴급 공지
  IF v_policy.emergency_is_active
     AND v_policy.emergency_title IS NOT NULL
     AND v_policy.emergency_content IS NOT NULL THEN
     v_result := jsonb_build_object(
       'type','emergency',
       'title', v_policy.emergency_title,
       'content', v_policy.emergency_content,
       'dismissible', COALESCE(v_policy.emergency_dismissible, false),
       'redirect_url', v_policy.emergency_redirect_url
     );
     RETURN v_result;
  END IF;

  -- 2) 강제 업데이트
  IF v_policy.min_supported_version IS NOT NULL
     AND p_current_version < v_policy.min_supported_version THEN
     v_result := jsonb_build_object(
       'type','force_update',
       'is_force', true,
       'version_code', v_policy.min_supported_version,
       'download_url', v_policy.download_url
     );
     RETURN v_result;
  END IF;

  -- 3) 선택적 업데이트
  IF COALESCE(v_policy.update_is_active, false)
     AND v_policy.latest_version_code IS NOT NULL
     AND v_policy.latest_version_code > p_current_version THEN
     v_result := jsonb_build_object(
       'type','optional_update',
       'is_force', false,
       'version_code', v_policy.latest_version_code,
       'download_url', v_policy.download_url
     );
     RETURN v_result;
  END IF;

  -- 4) 일반 공지
  IF COALESCE(v_policy.notice_is_active, false)
     AND v_policy.notice_title IS NOT NULL
     AND v_policy.notice_content IS NOT NULL THEN
     v_result := jsonb_build_object(
       'type','notice',
       'title', v_policy.notice_title,
       'content', v_policy.notice_content,
       'dismissible', true
     );
     RETURN v_result;
  END IF;

  RETURN NULL; -- 아무 것도 없음
END;
$$;

GRANT EXECUTE ON FUNCTION public.rpc_get_app_popup(TEXT, INTEGER) TO anon, authenticated;
```

## RPC 결과 JSON 구조
```json
{
  "type": "emergency | force_update | optional_update | notice",
  "title": "문자열 (emergency/notice)",
  "content": "문자열 (emergency/notice)",
  "dismissible": true,
  "redirect_url": "https://...",
  "is_force": true,
  "version_code": 123,
  "download_url": "https://play.google.com/..."
}
```
누락될 수 있는 필드는 상황(type)에 따라 존재하지 않을 수 있습니다.

## 마이그레이션 단계 제안
1. 테이블+RPC 배포 (위 스크립트 실행)
2. 각 앱별 app_id 행 시드
3. 클라이언트: RPC 호출 추가 후 정책/기존 테이블 폴백 유지
4. 일정 기간 모니터링(Log: type별 카운트)
5. 안정화 후 announcements/update_info 비활성화 또는 제거(선택)

## 클라이언트 호출 예시 (Kotlin)
```kotlin
val decision = supabase.postgrest.rpc(
    "rpc_get_app_popup",
    mapOf(
        "p_app_id" to BuildConfig.SUPABASE_APP_ID,
        "p_current_version" to BuildConfig.VERSION_CODE
    )
).decodeOrNull<PopupDecision>()
```

## 운영 체크리스트
- 긴급 공지 설정 후 강제/선택 버전 플래그를 동시에 켜지 말 것 (긴급이 가장 먼저 소비되므로 다른 것들은 숨겨짐)
- 강제 업데이트 해제 시: min_supported_version를 현재 버전 이하로 조정하거나 NULL
- 선택적 업데이트 일시 중지: update_is_active=false
- 일반 공지 교체: notice_title/content 교체 후 notice_is_active=true
