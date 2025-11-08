# 새 채팅창 시작용 프롬프트

아래 내용을 새 채팅창에 복사해서 붙여넣으세요.

---

## 📋 작업 요청

안녕하세요. Supabase 기반 앱 정책 관리 시스템을 Android 앱에 연동하는 작업을 진행하려고 합니다.

### 프로젝트 정보
- **프로젝트**: PocketChord (Android 앱)
- **위치**: `G:\Workspace\PocketChord`
- **언어**: Kotlin
- **백엔드**: Supabase

### 작업 목표
Supabase에 구축된 4가지 팝업 시스템을 Android 앱에 연동하여 다음 기능을 구현:

1. **긴급 공지** (`emergency`): X 버튼 없음, URL 이동
2. **강제 업데이트** (`force_update`): 뒤로가기 차단, 버전 체크
3. **선택적 업데이트** (`optional_update`): 닫기 가능, 버전 체크
4. **일반 공지** (`notice`): 단순 공지

### 완료된 작업 ✅
- ✅ Supabase 테이블 설계 완료 (`app_policy`, `app_policy_history`)
- ✅ 제약조건 및 트리거 설정 완료
- ✅ RLS (Row Level Security) 설정 완료
- ✅ 완전한 문서화 완료: `docs/supabase-app-policy-hybrid.md`

### 필요한 작업 🎯
1. **Supabase Kotlin 클라이언트 연동**
   - 정책 조회 API 구현
   - 데이터 모델 클래스 작성

2. **팝업 UI 구현**
   - 4가지 타입별 다이얼로그 작성
   - 버전 체크 로직 구현

3. **앱 시작 시 정책 확인**
   - MainActivity에서 정책 조회
   - 타입별 팝업 분기 처리

4. **테스트**
   - 각 팝업 타입별 동작 확인
   - RLS 정책 확인

---

## 📄 핵심 참고 문서

**문서 위치**: `G:\Workspace\PocketChord\docs\supabase-app-policy-hybrid.md`

### 주요 내용 요약:

#### 1. 테이블 구조
```sql
CREATE TABLE public.app_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  app_id TEXT NOT NULL UNIQUE,
  is_active BOOLEAN NOT NULL DEFAULT FALSE,
  active_popup_type popup_type NOT NULL DEFAULT 'none',
  content TEXT DEFAULT '...',
  download_url TEXT,
  min_supported_version INTEGER,
  latest_version_code INTEGER
);
```

#### 2. popup_type ENUM
```sql
CREATE TYPE popup_type AS ENUM (
  'emergency',         -- 긴급 공지
  'force_update',      -- 강제 업데이트
  'optional_update',   -- 선택적 업데이트
  'notice',            -- 일반 공지
  'none'               -- 팝업 없음
);
```

#### 3. 타입별 필수 필드

| 타입 | is_active | content | download_url | min_supported_version | latest_version_code |
|------|-----------|---------|--------------|----------------------|---------------------|
| `emergency` | TRUE | 필수 | 필수 | - | - |
| `force_update` | TRUE | 필수 | 필수 | **필수** | - |
| `optional_update` | TRUE | 필수 | 필수 | - | **필수** |
| `notice` | TRUE | 필수 | 선택 | - | - |
| `none` | - | - | - | - | - |

#### 4. RLS 정책
```sql
-- is_active = TRUE인 행만 클라이언트에서 조회 가능
CREATE POLICY "allow_read_policy"
ON public.app_policy
FOR SELECT
USING (is_active = TRUE);
```

#### 5. 클라이언트 코드 예시 (문서에서 발췌)
```kotlin
val policy = supabase.postgrest
    .from("app_policy")
    .select {
        filter { eq("app_id", BuildConfig.SUPABASE_APP_ID) }
        limit(1)
    }
    .decodeList<AppPolicy>()
    .firstOrNull()

when (policy?.activePopupType) {
    "emergency" -> showEmergencyDialog(...)
    "force_update" -> {
        if (currentVersion < policy.minSupportedVersion) {
            showForceUpdateDialog(...)
        }
    }
    "optional_update" -> {
        if (currentVersion < policy.latestVersionCode) {
            showOptionalUpdateDialog(...)
        }
    }
    "notice" -> showNoticeDialog(...)
    "none", null -> return
}
```

---

## 🎯 작업 시작 요청

위 내용을 바탕으로 다음 작업을 진행해주세요:

1. **먼저 프로젝트 구조 파악**
   - 기존 코드 확인
   - Supabase 연동 상태 확인
   - 의존성 확인

2. **단계별 구현**
   - 데이터 모델 클래스 작성
   - Supabase 클라이언트 연동
   - 팝업 UI 구현
   - 메인 로직 연동

3. **테스트 및 검증**
   - 각 팝업 타입 테스트
   - 에러 처리 확인

---

## 📌 중요 설계 원칙 (문서에서 강조된 내용)

1. **선택적 필드 철학**
   - `force_update`는 `min_supported_version`만 사용
   - `optional_update`는 `latest_version_code`만 사용
   - 다른 필드에 값이 있어도 **무시**됨 (앱이 안전하게 처리)

2. **is_active 역할**
   - `is_active = TRUE`: 팝업 활성화 (사용자에게 표시)
   - `is_active = FALSE`: 팝업 비활성화 (RLS로 차단됨)
   - 설정은 미리 준비하고 `is_active`로 활성화 시점 제어

3. **버전 체크 로직**
   - `force_update`: `currentVersion < min_supported_version` → 강제
   - `optional_update`: `currentVersion < latest_version_code` → 권장

---

## 🔗 추가 참고

- Supabase 설정 파일 위치: 프로젝트 내 `BuildConfig` 또는 설정 파일 확인 필요
- App ID: `com.sweetapps.pocketchord.debug` (디버그) / `com.sweetapps.pocketchord` (릴리즈)

---

**준비 완료!**
위 내용을 바탕으로:
1. 먼저 프로젝트의 MainActivity와 Supabase 연동 상태를 확인해주세요
2. 그 다음 데이터 모델 클래스부터 단계별로 구현해주세요
질문이나 불명확한 부분이 있으면 `docs/supabase-app-policy-hybrid.md` 문서를 참고하거나 질문해주세요.

