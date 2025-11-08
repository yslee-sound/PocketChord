# Supabase App Policy (선택지 B) — 스키마 & RLS 가이드

본 문서는 PocketChord가 단일 정책 테이블(app_policy)만 읽어 팝업 1개를 결정하도록 하는 운영 방식(선택지 B)의 SQL 스키마, RLS 정책, 운영 절차를 제공합니다.

목표
- is_active = true만 노출, 단 하나의 정책 레코드로 팝업 결정
- 우선순위: 긴급 공지 > 강제 업데이트 > 선택적 업데이트 > 일반 공지
- 클라이언트는 app_policy 1건만 읽고 판단 (현재 빌드: RPC/기존 테이블 폴백 사용하지 않음)

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

## 전체 설정 스크립트 (한 번에 실행 가능)
아래 스크립트는 테이블 생성 → 인덱스 → RLS → 정책까지 한 번에 구성합니다. (현 빌드 기준: RPC는 사용하지 않으므로 포함하지 않습니다.)

```sql
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

-- 2. 앱당 1행 제약 (UNIQUE 인덱스)
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
```

### 운영 절차 (SOP)
- 긴급 공지 켜기: emergency_is_active=true, emergency_title/content 입력 (필요시 redirect/dismissible)
- 강제 업데이트: min_supported_version를 현재 앱 버전보다 크게 설정
- 선택적 업데이트: update_is_active=true, latest_version_code를 현재 앱 버전보다 크게 설정, (선택) download_url
- 일반 공지: notice_is_active=true, notice_title/notice_content 입력
- 모두 끄기: 위 플래그 false 또는 is_active=false

### 업데이트(강제/선택적) 팝업 설정 상세
업데이트 관련 컬럼은 이미 스키마에 있습니다:
```
min_supported_version INTEGER NULL        -- 강제 업데이트 기준
latest_version_code   INTEGER NULL        -- 선택적 업데이트 최신 버전
update_is_active      BOOLEAN NOT NULL    -- 선택적 업데이트 토글
download_url          TEXT NULL           -- 마켓/링크 이동
```
앱 로직 요약:
1. 강제(Force) 업데이트 조건: `min_supported_version IS NOT NULL AND current_version_code < min_supported_version`
2. 선택적(Optional) 업데이트 조건: `update_is_active = true AND latest_version_code IS NOT NULL AND latest_version_code > current_version_code`
3. 강제 조건이 만족하면 선택적 조건은 무시 (우선순위 상 Force가 더 높음)
4. 사용자가 선택적 업데이트 팝업을 닫으면(디버그에서) `dismissed_version_code` 에 저장되어 같은 versionCode로 다시 뜨지 않음.

#### 강제 업데이트 설정 예시 (현재 앱 VERSION_CODE = 2 가정)
```sql
UPDATE public.app_policy
SET min_supported_version = 3,       -- 2보다 크면 앱이 강제 업데이트 팝업 노출
    download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```
표시 후 다시 해제하려면:
```sql
UPDATE public.app_policy
SET min_supported_version = NULL
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 선택적 업데이트 설정 예시
```sql
UPDATE public.app_policy
SET latest_version_code = 3,   -- 현재 2보다 커야 함
    update_is_active = TRUE,
    download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```
닫은 후 다시 같은 버전을 띄우고 싶다면 앱 데이터(SharedPreferences) 초기화 또는 version code를 4로 올려 재설정:
```sql
UPDATE public.app_policy
SET latest_version_code = 4
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```
선택적 업데이트 비활성화:
```sql
UPDATE public.app_policy
SET update_is_active = FALSE,
    latest_version_code = NULL
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### download_url
- 설정 시 팝업 ‘지금 업데이트’ 버튼이 해당 링크 또는 마켓 scheme (`market://`) 을 엽니다.
- 미설정이면 기본 마켓 패키지 링크를 앱이 구성.

#### 자주 헷갈리는 포인트
- 강제 업데이트는 `min_supported_version` 하나만 보면 됨 (latest_version_code와 무관).
- 선택적 업데이트는 `update_is_active=true` 와 `latest_version_code > current_version_code` 두 조건 모두 필요.
- 두 조건을 동시에 만족하게 설정할 경우(예: min_supported_version=3, latest_version_code=4): 먼저 강제 팝업이 뜨고 선택적 팝업은 표시되지 않음.
- 선택적 팝업이 안 뜰 때: (a) update_is_active=false (b) latest_version_code <= current_version_code (c) 사용자가 동일 latest_version_code를 이미 dismiss (d) 정책 행 비활성.
- 디버그/릴리즈 빌드의 BuildConfig.VERSION_CODE 값이 다르면 테스트 전 꼭 확인.

#### 테스트 순서 추천 (디버그 빌드)
1. 모든 업데이트 관련 컬럼 NULL/false → 어떤 업데이트 팝업도 안 뜸.
2. 선택적 업데이트만 활성: latest_version_code=3, update_is_active=true → Optional 팝업 확인 후 닫기.
3. 동일 버전 재확인 → 닫힌 후 다시 안 나옴.
4. latest_version_code=4로 변경 → 새 Optional 팝업 뜸.
5. 강제 업데이트 설정: min_supported_version=5 → 바로 Force 팝업 뜨며 뒤로가기 차단.
6. 강제 해제: min_supported_version=NULL → Optional 로직 복귀.

#### 초기화 스크립트 (모든 팝업 비활성化)
```sql
UPDATE public.app_policy
SET min_supported_version = NULL,
    latest_version_code = NULL,
    update_is_active = FALSE,
    download_url = NULL,
    emergency_is_active = FALSE,
    emergency_title = NULL,
    emergency_content = NULL,
    notice_is_active = NULL,
    notice_title = NULL,
    notice_content = NULL
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 검증/조회 쿼리
```sql
SELECT app_id, is_active, emergency_is_active, emergency_title, emergency_content
FROM public.app_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug' AND is_active = TRUE;
```

### 클라이언트 연동 (현 빌드 기준: app_policy 직접 조회)
- RLS가 is_active = TRUE 조건을 보장하므로, 클라이언트 쿼리에서 eq('is_active', true)를 중복 적용하지 마세요. 특정 환경에서 결과가 0건이 될 수 있습니다(실제 사례 반영).
- Kotlin 예시:

```kotlin
val policy = supabase.postgrest
    .from("app_policy")
    .select {
        filter { eq("app_id", BuildConfig.SUPABASE_APP_ID) }
        limit(1)
    }
    .decodeList<AppPolicy>()
    .firstOrNull()
```

### 트러블슈팅 체크리스트
1) app_id 불일치/디버그-릴리즈 혼동: 디버그 기본값은 `com.sweetapps.pocketchord.debug`, 릴리즈는 `com.sweetapps.pocketchord`.
2) 숨은 공백/오타: `SELECT app_id, length(app_id) FROM app_policy;`로 확인 후 `UPDATE ... SET app_id = trim(app_id)`로 정리.
3) RLS 정책 부재: `read active policy (USING is_active = TRUE)` 존재 확인.
4) 프로젝트 URL/키 오기: 앱 로그(PocketChordApp: Supabase URL (debug)=...)로 현재 프로젝트 확인.
5) REST로 실제 조회 확인(anon 키):
```bash
curl -s "https://<PROJECT>.supabase.co/rest/v1/app_policy?app_id=eq.com.sweetapps.pocketchord.debug&is_active=eq.true&select=app_id,emergency_is_active,emergency_title,emergency_content" \
  -H "apikey: <ANON_KEY>" \
  -H "Authorization: Bearer <ANON_KEY>"
```
6) 선택적 업데이트가 안 뜨면: 사용자가 닫은 `dismissed_version_code` 때문일 수 있음(앱 데이터 삭제로 초기화).

---

## (옵션) RPC 함수 사용 시
현재 빌드에서는 RPC를 사용하지 않지만, 서버 쪽 일관 로직을 선호한다면 아래 함수를 추가로 배포해 사용할 수 있습니다.

```sql
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
    RETURN NULL;
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

  RETURN NULL;
END;
$$;

GRANT EXECUTE ON FUNCTION public.rpc_get_app_popup(TEXT, INTEGER) TO anon, authenticated;
```

(참고) RPC를 사용할 경우에도 클라이언트에서는 app_id만 전달하고, is_active 필터는 RLS에 맡기는 것을 권장합니다.

---

## 클라이언트 호출 예시 (Kotlin)
현 빌드 기본 방식(Postgrest 직접 조회) 예시를 우선 제시합니다.

```kotlin
val policy = supabase.postgrest
    .from("app_policy")
    .select {
        filter { eq("app_id", BuildConfig.SUPABASE_APP_ID) }
        limit(1)
    }
    .decodeList<AppPolicy>()
    .firstOrNull()
```

(옵션) RPC 사용 시:
```kotlin
val result = supabase.postgrest.rpc(
    "rpc_get_app_popup",
    kotlinx.serialization.json.JsonObject(mapOf(
        "p_app_id" to kotlinx.serialization.json.JsonPrimitive(BuildConfig.SUPABASE_APP_ID),
        "p_current_version" to kotlinx.serialization.json.JsonPrimitive(BuildConfig.VERSION_CODE)
    ))
)
val json = result.data?.toString()
// json을 PopupDecision으로 디코드하여 분기
```
