# 🗄️ PocketChord Supabase 가이드

**업데이트**: 2025-11-08  
**목적**: Supabase 설정 및 사용 가이드

---

## 📋 목차

1. [Supabase 개요](#supabase-개요)
2. [테이블 구조](#테이블-구조)
3. [설정 방법](#설정-방법)
4. [테스트](#테스트)
5. [문제 해결](#문제-해결)

---

## Supabase 개요

PocketChord는 Supabase를 사용하여:
- ✅ 앱 정책 관리 (`app_policy`)
- ✅ 광고 제어 (`ad_policy`)
- ✅ 공지사항 관리 (`announcements`)

모든 정책은 **실시간으로 제어** 가능합니다.

---

## 테이블 구조

### 1. app_policy (팝업 정책)

#### 용도
- 긴급 공지
- 강제/선택적 업데이트
- 일반 공지

#### 스키마
```sql
CREATE TABLE app_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMP DEFAULT NOW(),
  app_id TEXT UNIQUE NOT NULL,
  is_active BOOLEAN DEFAULT false,
  active_popup_type TEXT DEFAULT 'none',  -- 'emergency', 'force_update', 'optional_update', 'notice', 'none'
  content TEXT,
  download_url TEXT,
  min_supported_version INT,
  latest_version_code INT
);
```

#### 사용 예
```sql
-- 강제 업데이트
UPDATE app_policy 
SET 
  is_active = true,
  active_popup_type = 'force_update',
  content = '필수 업데이트가 있습니다.',
  download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord',
  min_supported_version = 10
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

### 2. ad_policy (광고 정책)

#### 용도
- 광고 ON/OFF 제어
- 광고 빈도 제한

#### 스키마
```sql
CREATE TABLE ad_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMP DEFAULT NOW(),
  app_id TEXT UNIQUE NOT NULL,
  is_active BOOLEAN DEFAULT true,
  
  -- 광고 ON/OFF
  ad_app_open_enabled BOOLEAN DEFAULT true,
  ad_interstitial_enabled BOOLEAN DEFAULT true,
  ad_banner_enabled BOOLEAN DEFAULT true,
  
  -- 빈도 제한
  ad_interstitial_max_per_hour INT DEFAULT 2,
  ad_interstitial_max_per_day INT DEFAULT 15
);
```

#### 사용 예
```sql
-- 배너 광고만 끄기
UPDATE ad_policy 
SET ad_banner_enabled = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**참고**: `ad-policy-table-creation.sql` 참조

---

### 3. announcements (공지사항)

#### 용도
- 인앱 공지사항
- 버전별 표시 제어
- 읽음 여부 추적

#### 스키마
```sql
CREATE TABLE announcements (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMP DEFAULT NOW(),
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  is_active BOOLEAN DEFAULT true,
  priority INT DEFAULT 0,
  start_date TIMESTAMP,
  end_date TIMESTAMP,
  min_version_code INT,
  max_version_code INT
);
```

---

## 설정 방법

### 1. Supabase 프로젝트 생성

1. https://supabase.com 접속
2. **New Project** 클릭
3. 프로젝트 정보 입력:
   - Name: PocketChord
   - Database Password: (안전하게 보관)
   - Region: Northeast Asia (Seoul)

### 2. 테이블 생성

#### app_policy
```sql
-- 이미 존재함 (기존)
-- 확인만 하면 됨
SELECT * FROM app_policy;
```

#### ad_policy ⭐ 신규
```sql
-- docs/ad-policy-table-creation.sql 실행
-- Supabase Dashboard → SQL Editor
```

**파일**: `ad-policy-table-creation.sql`

### 3. RLS (Row Level Security) 설정

#### app_policy
```sql
ALTER TABLE app_policy ENABLE ROW LEVEL SECURITY;

CREATE POLICY "app_policy_select" ON app_policy
  FOR SELECT USING (is_active = true);
```

#### ad_policy
```sql
ALTER TABLE ad_policy ENABLE ROW LEVEL SECURITY;

CREATE POLICY "ad_policy_select" ON ad_policy
  FOR SELECT USING (is_active = true);
```

**중요**: RLS를 활성화하면 `is_active = true`인 레코드만 조회됩니다!

### 4. 앱 설정

#### build.gradle.kts
```kotlin
android {
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"your-anon-key\"")
        buildConfigField("String", "SUPABASE_APP_ID", "\"com.sweetapps.pocketchord\"")
    }
}
```

#### local.properties (민감 정보)
```properties
supabase.url=https://your-project.supabase.co
supabase.anon.key=your-anon-key
```

**주의**: `local.properties`는 `.gitignore`에 포함!

---

## 테스트

### 1. 연결 테스트

```kotlin
// SupabaseDebugTest.kt
suspend fun testConnection(context: Context) {
    val app = context.applicationContext as PocketChordApplication
    
    try {
        val response = app.supabase
            .from("app_policy")
            .select()
            .decodeList<AppPolicy>()
        
        Log.d("Supabase", "✅ 연결 성공: ${response.size}개 정책")
    } catch (e: Exception) {
        Log.e("Supabase", "❌ 연결 실패", e)
    }
}
```

### 2. 정책 조회 테스트

```kotlin
// AppPolicyRepository
val policy = repository.getPolicy()

if (policy.isSuccess) {
    Log.d("Test", "✅ 정책 조회 성공")
} else {
    Log.e("Test", "❌ 정책 조회 실패: ${policy.exceptionOrNull()}")
}
```

### 3. RLS 테스트

#### is_active = true
```sql
UPDATE app_policy SET is_active = true;
```
→ 앱에서 조회 가능 ✅

#### is_active = false
```sql
UPDATE app_policy SET is_active = false;
```
→ 앱에서 조회 불가 ❌ (RLS 정책)

---

## 문제 해결

### 연결 실패

**증상**:
```
Unable to resolve host
```

**해결**:
1. 인터넷 연결 확인
2. Supabase URL 확인
3. Anon Key 확인

### RLS 문제

**증상**:
```
No rows returned
```

**해결**:
```sql
-- RLS 정책 확인
SELECT * FROM pg_policies WHERE tablename = 'app_policy';

-- RLS 임시 비활성화 (테스트용)
ALTER TABLE app_policy DISABLE ROW LEVEL SECURITY;
```

### 캐싱 문제

**증상**:
- Supabase 변경했는데 앱에 반영 안 됨

**원인**:
- 5분 캐싱

**해결**:
1. 5분 대기
2. 또는 앱 재시작
3. 또는 캐시 초기화:
```kotlin
repository.clearCache()
```

---

## 운영 가이드

### 긴급 상황 대응

#### 1. 모든 광고 끄기
```sql
UPDATE ad_policy SET is_active = false;
```

#### 2. 긴급 공지
```sql
UPDATE app_policy 
SET 
  is_active = true,
  active_popup_type = 'emergency',
  content = '긴급 공지 내용'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 3. 강제 업데이트
```sql
UPDATE app_policy 
SET 
  is_active = true,
  active_popup_type = 'force_update',
  content = '필수 업데이트가 있습니다.',
  download_url = 'https://play.google.com/store/...',
  min_supported_version = 10
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 일상 운영

#### 정책 확인
```sql
-- 앱 정책
SELECT * FROM app_policy WHERE app_id = 'com.sweetapps.pocketchord';

-- 광고 정책
SELECT * FROM ad_policy WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 로그 확인
```bash
adb logcat | findstr "AppPolicyRepo"
adb logcat | findstr "AdPolicyRepo"
```

---

## 참고 문서

### 광고 관련
- `ad-policy-table-creation.sql` - ad_policy 테이블 생성
- `ad-policy-separation-implementation-complete.md` - 광고 정책 분리 가이드
- `SUPABASE-TABLE-CREATION-SUCCESS.md` - 테이블 생성 확인

### 공지사항 관련
- `supabase-announcement-dialog.md`
- `supabase-announcement-management.md`
- `supabase-announcement-viewed-tracking.md`

### 일반
- `SUPABASE-ID-COLUMN-GUIDE.md` - id 컬럼 이해하기
- `force-update-logic-analysis.md` - 강제 업데이트 로직

---

**작성일**: 2025-11-08  
**버전**: 2.0 (ad_policy 분리 반영)

