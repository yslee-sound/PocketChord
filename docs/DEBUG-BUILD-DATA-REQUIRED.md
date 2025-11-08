# 🚨 긴급: Debug 빌드 데이터 추가 필요

**날짜**: 2025-11-08  
**중요도**: ⚠️ 높음  
**소요 시간**: 1분

---

## ❗ 문제

현재 Supabase `ad_policy` 테이블에는 **Release 빌드** 데이터만 있습니다.
```
✅ com.sweetapps.pocketchord (Release) - 존재
❌ com.sweetapps.pocketchord.debug (Debug) - 없음
```

Debug 빌드로 앱을 실행하면 광고 정책을 찾을 수 없어서 **기본값(광고 ON)**으로 동작합니다.

---

## ✅ 해결 방법

### 방법 1: 빠른 추가 (권장)

**Supabase Dashboard → SQL Editor**에서 다음 실행:

```sql
INSERT INTO ad_policy (
  app_id, is_active,
  ad_app_open_enabled, ad_interstitial_enabled, ad_banner_enabled,
  ad_interstitial_max_per_hour, ad_interstitial_max_per_day
) VALUES (
  'com.sweetapps.pocketchord.debug',
  true, true, true, true, 3, 20
)
ON CONFLICT (app_id) DO UPDATE SET
  is_active = EXCLUDED.is_active;
```

**확인:**
```sql
SELECT app_id, is_active FROM ad_policy ORDER BY app_id;
```

**예상 결과:**
```
com.sweetapps.pocketchord        | true
com.sweetapps.pocketchord.debug  | true
```

---

### 방법 2: 파일 실행

**파일**: `docs/ad-policy-add-debug-build.sql`

1. 파일 내용 복사
2. Supabase Dashboard → SQL Editor
3. 붙여넣기 → RUN

---

### 방법 3: 전체 재실행 (이미 업데이트됨)

**파일**: `docs/ad-policy-table-creation.sql` (이미 Debug 추가됨!)

테이블을 처음부터 다시 만들려면:
```sql
-- 기존 테이블 삭제
DROP TABLE IF EXISTS ad_policy CASCADE;

-- 그 다음 ad-policy-table-creation.sql 전체 실행
```

---

## 🎯 확인 사항

### Debug 빌드 실행 시

**로그 확인:**
```bash
adb logcat | findstr "AdPolicyRepo"
```

**기대되는 로그:**
```
D/AdPolicyRepo: Target app_id: com.sweetapps.pocketchord.debug
D/AdPolicyRepo: ✅ 광고 정책 발견!
```

**잘못된 로그 (Debug 데이터 없을 때):**
```
D/AdPolicyRepo: ⚠️ 활성화된 광고 정책 없음 (기본값 사용)
```

---

## 📊 빌드별 app_id

| 빌드 타입 | app_id | 필요성 |
|-----------|---------|--------|
| Release | com.sweetapps.pocketchord | ✅ 필수 (운영) |
| Debug | com.sweetapps.pocketchord.debug | ✅ 필수 (개발/테스트) |

---

## 💡 빌드별 독립 제어

Debug 데이터가 있으면 이런 것이 가능합니다:

### 시나리오 1: Release는 광고 ON, Debug는 광고 OFF
```sql
-- Release: 광고 표시 (실제 사용자)
UPDATE ad_policy 
SET is_active = true 
WHERE app_id = 'com.sweetapps.pocketchord';

-- Debug: 광고 숨김 (개발/테스트 편의)
UPDATE ad_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 시나리오 2: Debug에서만 특정 광고 테스트
```sql
-- Debug에서만 배너 끄기
UPDATE ad_policy 
SET ad_banner_enabled = false 
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- Release는 영향 없음
```

---

## ⏱️ 즉시 실행 필요

**Supabase에서 다음만 실행하면 됩니다:**

```sql
INSERT INTO ad_policy (
  app_id, is_active,
  ad_app_open_enabled, ad_interstitial_enabled, ad_banner_enabled,
  ad_interstitial_max_per_hour, ad_interstitial_max_per_day
) VALUES (
  'com.sweetapps.pocketchord.debug',
  true, true, true, true, 3, 20
);

-- 확인
SELECT app_id FROM ad_policy ORDER BY app_id;
```

**소요 시간**: 30초

---

## ✅ 완료 체크리스트

- [ ] Supabase에서 Debug 데이터 추가 SQL 실행
- [ ] 2개 행 반환 확인 (Release + Debug)
- [ ] Debug 빌드 실행
- [ ] 로그에서 "광고 정책 발견!" 확인
- [ ] 광고 표시 확인

---

**작성일**: 2025-11-08  
**참고**: 
- `ad-policy-add-debug-build.sql` (빠른 추가용)
- `ad-policy-table-creation.sql` (이미 업데이트됨)
- `SUPABASE-TABLE-CREATION-SUCCESS.md` (상세 정보)

