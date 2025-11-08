# app_policy 테이블 정리 가이드

**날짜**: 2025-11-08  
**목적**: ad_policy 분리 후 app_policy에서 광고 관련 컬럼 제거  
**상태**: 선택사항 (급하지 않음)

---

## 📋 현재 상황

### app_policy 테이블
```
✅ 팝업 관련 컬럼 (필요)
  - is_active
  - active_popup_type
  - content
  - download_url
  - min_supported_version
  - latest_version_code

⚠️ 광고 관련 컬럼 (더 이상 사용 안 함)
  - ad_app_open_enabled
  - ad_interstitial_enabled
  - ad_banner_enabled
  - ad_interstitial_max_per_hour
  - ad_interstitial_max_per_day
```

### ad_policy 테이블 (신규)
```
✅ 광고 전용 테이블로 분리 완료
  - ad_app_open_enabled
  - ad_interstitial_enabled
  - ad_banner_enabled
  - ad_interstitial_max_per_hour
  - ad_interstitial_max_per_day
```

---

## ❓ 광고 컬럼을 제거해야 하나?

### 제거하지 않아도 됨 ✅
- 앱은 **ad_policy**를 사용하므로 정상 작동
- app_policy의 광고 컬럼은 무시됨
- 기능적으로 아무 문제 없음

### 제거하면 좋은 점 ✅
- 테이블이 깔끔해짐
- 혼란 방지 (어느 테이블을 봐야 하는지 명확)
- 데이터베이스 크기 약간 감소

### 제거의 위험 ⚠️
- **되돌릴 수 없음** (백업 필수)
- 구 버전 앱과 호환 불가
- 실행 시 테이블 잠금 (짧지만 발생)

---

## 🎯 권장 시나리오

### 시나리오 1: 개발/테스트 단계 (즉시 실행 가능)
```
✅ ad_policy 테이블 생성 완료
✅ 앱 코드 업데이트 완료
✅ 사용자 없음 또는 매우 적음
→ 지금 바로 실행해도 됨
```

**실행**:
```sql
-- 파일: app-policy-remove-ad-columns.sql
-- Supabase Dashboard → SQL Editor에서 실행
```

### 시나리오 2: 운영 서비스 중 (1-2주 후 권장)
```
✅ 새 버전 앱 배포 완료
⏳ 사용자들이 업데이트 중
⏳ 광고가 ad_policy로 정상 작동 확인 필요
→ 1-2주 기다린 후 실행
```

**타임라인**:
1. **오늘**: 앱 배포 (AdPolicyRepository 사용)
2. **1주 후**: 대부분 사용자 업데이트 확인
3. **2주 후**: 광고 정상 작동 확인 → SQL 실행

### 시나리오 3: 나중에 또는 안 함 (괜찮음)
```
✅ 앱은 정상 작동 중
❌ 광고 컬럼 제거 급하지 않음
→ 나중에 대규모 마이그레이션 때 정리
```

**이유**:
- 기능적으로 문제 없음
- 급할 필요 없음
- 다음 DB 마이그레이션 때 같이 정리

---

## 📝 실행 절차 (시나리오 1 또는 2)

### Step 1: 사전 확인

#### 1. ad_policy 테이블 존재 확인
```sql
SELECT * FROM ad_policy;
```
**예상**: 2개 행 (Release, Debug)

#### 2. 앱이 ad_policy 사용 확인
```bash
adb logcat | findstr "AdPolicyRepo"
```
**예상**: 
```
D/AdPolicyRepo: ✅ 광고 정책 발견!
D/AdPolicyRepo:   - App Open Ad: true
```

#### 3. 광고 정상 작동 확인
- ✅ 앱 오픈 광고 표시됨
- ✅ 전면 광고 표시됨
- ✅ 배너 광고 표시됨

### Step 2: 백업 (필수!)

```sql
-- 백업 생성
CREATE TABLE app_policy_backup_20251108 AS 
SELECT * FROM app_policy;

-- 확인
SELECT COUNT(*) FROM app_policy_backup_20251108;
```

### Step 3: 광고 컬럼 제거

```sql
-- app-policy-remove-ad-columns.sql 파일 실행
ALTER TABLE app_policy
DROP COLUMN IF EXISTS ad_app_open_enabled,
DROP COLUMN IF EXISTS ad_interstitial_enabled,
DROP COLUMN IF EXISTS ad_banner_enabled,
DROP COLUMN IF EXISTS ad_interstitial_max_per_hour,
DROP COLUMN IF EXISTS ad_interstitial_max_per_day;
```

### Step 4: 확인

```sql
-- 테이블 구조 확인 (광고 컬럼 없어야 함)
SELECT column_name 
FROM information_schema.columns
WHERE table_name = 'app_policy'
ORDER BY ordinal_position;
```

**예상 결과**:
```
column_name
-----------------------
id
created_at
app_id
is_active
active_popup_type
content
download_url
min_supported_version
latest_version_code
```

**확인**: `ad_`로 시작하는 컬럼이 없어야 함 ✅

### Step 5: 앱 테스트

- [ ] 앱 실행 확인
- [ ] 광고 표시 확인 (ad_policy 사용)
- [ ] 팝업 표시 확인 (app_policy 사용)
- [ ] Supabase에서 광고 제어 테스트

---

## 🔄 롤백 (문제 발생 시)

### 1. 컬럼 복원
```sql
ALTER TABLE app_policy
ADD COLUMN ad_app_open_enabled BOOLEAN DEFAULT true NOT NULL,
ADD COLUMN ad_interstitial_enabled BOOLEAN DEFAULT true NOT NULL,
ADD COLUMN ad_banner_enabled BOOLEAN DEFAULT true NOT NULL,
ADD COLUMN ad_interstitial_max_per_hour INT DEFAULT 2 NOT NULL,
ADD COLUMN ad_interstitial_max_per_day INT DEFAULT 15 NOT NULL;
```

### 2. 백업에서 데이터 복원
```sql
UPDATE app_policy SET
  ad_app_open_enabled = backup.ad_app_open_enabled,
  ad_interstitial_enabled = backup.ad_interstitial_enabled,
  ad_banner_enabled = backup.ad_banner_enabled,
  ad_interstitial_max_per_hour = backup.ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day = backup.ad_interstitial_max_per_day
FROM app_policy_backup_20251108 backup
WHERE app_policy.app_id = backup.app_id;
```

### 3. 앱 코드도 되돌려야 함!
⚠️ **주의**: SQL만 롤백하면 안 됨!
- 앱도 이전 버전으로 배포 필요
- 또는 AppPolicyRepository를 다시 사용하도록 수정

---

## 💡 자주 묻는 질문

### Q1: 지금 당장 해야 하나요?
**A**: 아니요. 앱은 정상 작동합니다. 편한 시기에 하면 됩니다.

### Q2: 안 하면 문제가 생기나요?
**A**: 아니요. 단지 테이블이 조금 지저분할 뿐입니다.

### Q3: 롤백이 어려운가요?
**A**: SQL은 쉽게 롤백되지만, 앱도 함께 되돌려야 합니다.

### Q4: 사용자에게 영향이 있나요?
**A**: 아니요. 순간적인 테이블 잠금만 있고 바로 풀립니다.

### Q5: 백업은 꼭 필요한가요?
**A**: 네! 혹시 모를 상황을 대비해 반드시 백업하세요.

---

## 📊 비교표

| 항목 | 제거 전 | 제거 후 |
|------|---------|---------|
| app_policy 컬럼 수 | 14개 | 9개 |
| 테이블 명확성 | 혼재 | 명확 |
| 앱 동작 | 정상 | 정상 |
| 롤백 가능성 | N/A | 어려움 |
| 권장 시기 | - | 1-2주 후 |

---

## ✅ 체크리스트

### 실행 전
- [ ] ad_policy 테이블 생성됨
- [ ] 앱이 AdPolicyRepository 사용
- [ ] 새 버전 앱 배포 완료
- [ ] 사용자 업데이트 충분히 진행 (운영 시)
- [ ] 광고 정상 작동 확인

### 실행 시
- [ ] 백업 완료
- [ ] SQL 실행
- [ ] 테이블 구조 확인
- [ ] 앱 테스트 완료

### 실행 후
- [ ] 광고 정상 작동
- [ ] 팝업 정상 작동
- [ ] 에러 로그 없음
- [ ] 백업 보관 (1개월)

---

## 🎯 결론

### 핵심 요약
1. ✅ **필수 아님** - 앱은 정상 작동
2. ⏰ **서두르지 않아도 됨** - 편한 시기에 실행
3. 💾 **백업 필수** - 롤백 대비
4. 📅 **권장 시기** - 앱 배포 1-2주 후

### 우리의 선택
```
[ ] 지금 즉시 실행 (개발 단계인 경우)
[ ] 1-2주 후 실행 (운영 중인 경우)
[✓] 나중에 또는 안 함 (급하지 않음)
```

**추천**: 일단은 **하지 않고** 앱이 잘 동작하는지 확인한 후, 나중에 여유 있을 때 실행하세요.

---

**참고 파일**:
- `app-policy-remove-ad-columns.sql` - 실행할 SQL
- `ad-policy-separation-implementation-complete.md` - 전체 구현 가이드

**작성일**: 2025-11-08  
**상태**: 선택사항, 급하지 않음

