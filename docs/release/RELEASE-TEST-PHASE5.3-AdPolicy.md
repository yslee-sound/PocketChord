# 릴리즈 테스트 - Phase 5.3 (최종 검증 + 배포)

**버전**: v3.1  
**최종 업데이트**: 2025-11-11  
**소요 시간**: 약 30-40분

---

## 📋 목차

1. [개요](#1-개요)
2. [빈도 제한 테스트 (선택사항)](#2-빈도-제한-테스트-선택사항)
3. [최종 검증](#3-최종-검증)
4. [배포 체크리스트](#4-배포-체크리스트)
5. [완료 체크리스트](#5-완료-체크리스트)

---

## 1 개요

### 1.1 Phase 5.3의 목적

Phase 5.3에서는 최종 검증 및 배포 준비를 수행합니다:
- ✅ **빈도 제한 테스트**: 시간당/일일 제한 확인 (선택사항)
- ✅ **최종 검증**: 모든 광고 설정 최종 확인
- ✅ **Supabase 최종 확인**: RLS 정책 및 데이터 확인
- ✅ **배포 준비**: 최종 체크리스트 완료

### 1.2 선행 조건

- ✅ **Phase 5.1 완료**: RLS 정책 수정 및 is_active 테스트 통과
- ✅ **Phase 5.2 완료**: 개별 광고 제어 테스트 통과
- ✅ **모든 광고 정상 작동**: Debug 빌드에서 확인 완료

---

## 2 빈도 제한 테스트 (선택사항)

### 2.1 Interstitial 빈도 제한 테스트

#### 목적
시간당/일일 빈도 제한이 정상적으로 작동하는지 확인

#### Step 1: 빈도 제한 낮추기

**테스트용 설정**:
```sql
UPDATE ad_policy
SET ad_interstitial_max_per_hour = 1,
    ad_interstitial_max_per_day = 3
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

**기대 결과**:
- 시간당 최대 1회
- 하루 최대 3회

#### Step 2: 테스트 수행

**첫 번째 광고 (성공 예상)**:
- [ ] 앱 재실행 (캐시 초기화)
- [ ] **전면 광고 표시 조건 만족**:
  - 코드 상세 → 홈 (3회 반복)
  - 광고 간격 대기 (필요 시)
  - 다시 코드 상세 → 홈
- [ ] **검증**: 전면 광고 표시 ✅ (1회)

**두 번째 광고 시도 (차단 예상)**:
- [ ] **다시 시도**:
  - 코드 상세 → 홈 (3회 반복)
  - 60초 대기
  - 다시 코드 상세 → 홈
- [ ] **검증**: 전면 광고 표시 안 됨 ❌ (시간당 제한 1회 도달)

#### Step 3: Logcat 확인

**필터 설정**: `tag:InterstitialAdManager | tag:AdPolicyRepo`

**예상 로그 (첫 번째 광고 - 성공)**:
```
AdPolicyRepo: ===== Ad Policy Fetch Completed =====
InterstitialAdManager: [정책] 전면 광고 활성화
InterstitialAdManager: ⏰ 시간당 카운트 리셋
InterstitialAdManager: 📅 일일 카운트 리셋
AdPolicyRepo: 📦 캐시된 광고 정책 사용 (유효 시간: xxx초 남음)
InterstitialAdManager: ✅ 빈도 제한 통과: 시간당 0/1, 일일 0/3
InterstitialAdManager: 📊 광고 카운트 증가: 시간당 1, 일일 1
InterstitialAdManager: 전면광고 표시됨
InterstitialAdManager: 전면광고 닫힘
InterstitialAdManager: 전면광고 로드 성공
```

**예상 로그 (두 번째 시도 - 60초 미달로 차단)**:
```
InterstitialAdManager: 화면 전환 카운트: 1
InterstitialAdManager: 광고 간격 미달: xx초/60초
InterstitialAdManager: 전면광고 표시 조건 미달
```

**예상 로그 (60초 경과 후 시도 - 빈도 제한으로 차단)**:
```
InterstitialAdManager: [정책] 전면 광고 활성화
AdPolicyRepo: 📦 캐시된 광고 정책 사용 (유효 시간: xxx초 남음)
InterstitialAdManager: ⚠️ 시간당 빈도 제한 초과: 1/1
InterstitialAdManager: ⚠️ 빈도 제한: 광고 표시 안 함
InterstitialAdManager: 전면광고 표시 조건 미달
```

**로그 설명**:
- `⏰ 시간당 카운트 리셋`: 1시간 경과 시 자동 리셋
- `📅 일일 카운트 리셋`: 24시간 경과 시 자동 리셋
- `📦 캐시된 광고 정책 사용`: 3분 이내 재조회 시 캐시 사용
- `✅ 빈도 제한 통과`: 시간당/일일 제한 확인
- `광고 간격 미달`: 이전 광고로부터 60초 미경과
- `⚠️ 시간당 빈도 제한 초과`: 시간당 최대 횟수 도달

#### Step 4: 운영 설정 복구

**중요**: 테스트 완료 후 반드시 원래 설정으로 복구!

```sql
UPDATE ad_policy
SET ad_interstitial_max_per_hour = 2,
    ad_interstitial_max_per_day = 15
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

**확인**:
- [ ] ✅ 운영 설정 복구 완료
- [ ] ✅ max_per_hour = 2
- [ ] ✅ max_per_day = 15

---

## 3 최종 검증

### 3.0 출시 전 모든 테이블 기본값 설정 (한번에)

#### 4개 테이블 기본값 일괄 설정 SQL

**목적**: Release & Debug 버전의 모든 정책 테이블을 안전한 기본값으로 설정

**4개 테이블**:
1. **ad_policy**: 광고 정책
2. **update_policy**: 업데이트 정책
3. **emergency_policy**: 긴급 팝업 정책
4. **notice_policy**: 공지사항 정책

**⚠️ 주의사항**:
- **기존 행 수정**: 이미 존재하는 행의 값만 변경됩니다 (ID 유지)
- **없으면 건너뜀**: 해당 app_id가 없으면 아무것도 하지 않습니다
- **안전한 실행**: ID가 변경되지 않으므로 안전합니다

```sql
-- ============================================
-- 출시 전 모든 정책 테이블 기본값 설정 (4개)
-- ============================================
-- 작성일: 2025-11-11
-- 목적: 안전한 운영 기본값으로 설정
-- ============================================

-- 1. ad_policy: 모든 광고 활성화
UPDATE ad_policy 
SET is_active = true,
    ad_app_open_enabled = true,
    ad_interstitial_enabled = true,
    ad_banner_enabled = true,
    ad_interstitial_max_per_hour = 2,
    ad_interstitial_max_per_day = 15
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

-- 2. update_policy: 비활성화
UPDATE update_policy
SET is_active = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

-- 3. emergency_policy: 비활성화
UPDATE emergency_policy
SET is_active = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

-- 4. notice_policy: 비활성화
UPDATE notice_policy
SET is_active = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

-- 5. 확인
SELECT 'ad_policy' as table_name, app_id, is_active FROM ad_policy WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
UNION ALL
SELECT 'update_policy', app_id, is_active FROM update_policy WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
UNION ALL
SELECT 'emergency_policy', app_id, is_active FROM emergency_policy WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
UNION ALL
SELECT 'notice_policy', app_id, is_active FROM notice_policy WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
ORDER BY table_name, app_id;
```

**실행 확인**:
- [ ] ✅ SQL 실행 완료
- [ ] ✅ 에러 없음
- [ ] ✅ 4개 테이블 × 2개 버전 = 총 8개 행 업데이트됨
- [ ] ✅ 확인 쿼리 결과 확인

**기대 결과**:
```
table_name        | app_id                          | is_active
------------------|---------------------------------|-----------
ad_policy         | com.sweetapps.pocketchord       | true
ad_policy         | com.sweetapps.pocketchord.debug | true
emergency_policy  | com.sweetapps.pocketchord       | false
emergency_policy  | com.sweetapps.pocketchord.debug | false
notice_policy     | com.sweetapps.pocketchord       | false
notice_policy     | com.sweetapps.pocketchord.debug | false
update_policy     | com.sweetapps.pocketchord       | false
update_policy     | com.sweetapps.pocketchord.debug | false
```

**설정 요약**:
- ✅ **ad_policy**: 모든 광고 활성화 (is_active = true)
- ✅ **update_policy**: 비활성화 (is_active = false)
- ✅ **emergency_policy**: 비활성화 (is_active = false)
- ✅ **notice_policy**: 비활성화 (is_active = false)

**✅ 왜 이렇게 간단한가?**:
- **핵심만**: ad_policy는 광고 설정 포함, 나머지는 is_active만 설정
- **빠름**: 불필요한 컬럼 업데이트 제거
- **명확함**: 출시 시 필요한 최소한의 설정만 변경

---

### 3.1 모든 광고 설정 확인

#### SQL로 최종 상태 확인

**목적**: 섹션 3.0에서 설정한 값이 실제로 올바르게 저장되었는지 확인

**확인 방법**:
1. Supabase Dashboard 접속
2. SQL Editor 열기
3. 아래 SQL 실행
4. 결과가 기대값과 일치하는지 확인

**SQL 쿼리**:
```sql
SELECT app_id, 
       is_active, 
       ad_app_open_enabled, 
       ad_interstitial_enabled, 
       ad_banner_enabled,
       ad_interstitial_max_per_hour,
       ad_interstitial_max_per_day
FROM ad_policy
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug')
ORDER BY app_id;
```

**기대 결과**:
```
app_id                          | is_active | ad_app_open | ad_interstitial | ad_banner | max_per_hour | max_per_day
--------------------------------|-----------|-------------|-----------------|-----------|--------------|-------------
com.sweetapps.pocketchord       | true      | true        | true            | true      | 2            | 15
com.sweetapps.pocketchord.debug | true      | true        | true            | true      | 2            | 15
```

**✅ 체크포인트**:
- [ ] 2개 행이 조회됨 (Release + Debug)
- [ ] 모든 값이 아래 기본값과 일치

#### 운영 기본값 확인

**Release 버전**:
- [ ] ✅ app_id = 'com.sweetapps.pocketchord'
- [ ] ✅ is_active = true
- [ ] ✅ ad_app_open_enabled = true
- [ ] ✅ ad_interstitial_enabled = true
- [ ] ✅ ad_banner_enabled = true
- [ ] ✅ max_per_hour = 2
- [ ] ✅ max_per_day = 15

**Debug 버전**:
- [ ] ✅ app_id = 'com.sweetapps.pocketchord.debug'
- [ ] ✅ is_active = true
- [ ] ✅ ad_app_open_enabled = true
- [ ] ✅ ad_interstitial_enabled = true
- [ ] ✅ ad_banner_enabled = true
- [ ] ✅ max_per_hour = 2
- [ ] ✅ max_per_day = 15

---

### 3.2 실제 동작 최종 확인

#### Debug 빌드 최종 테스트

- [ ] 앱 완전 재시작
- [ ] **검증**: App Open 광고 정상 표시 (백그라운드 복귀 시)
- [ ] **검증**: Banner 광고 정상 표시
- [ ] **검증**: Interstitial 광고 정상 표시 (조건 만족 시)
- [ ] **검증**: 앱 충돌 없음
- [ ] **검증**: Logcat 에러 없음

#### Logcat 최종 확인

**Android Studio Logcat 방식** (권장):
- Logcat 창 상단의 필터 입력란에 입력:
```
tag:AdPolicyRepo | tag:InterstitialAdManager | tag:AppOpenAdManager | tag:MainActivity
```

**Windows cmd 방식**:
```bash
adb logcat | findstr "AdPolicyRepo"
```

**PowerShell 방식**:
```powershell
adb logcat | Select-String "AdPolicyRepo"
```

**예상 로그**:
```
AdPolicyRepo: ===== Ad Policy Fetch Started =====
AdPolicyRepo: 🔄 Supabase에서 광고 정책 새로 가져오기
AdPolicyRepo: Target app_id: com.sweetapps.pocketchord.debug
AdPolicyRepo: Total rows fetched: 2
AdPolicyRepo: ✅ 광고 정책 발견!
AdPolicyRepo:   - is_active: true
AdPolicyRepo:   - App Open Ad: true
AdPolicyRepo:   - Interstitial Ad: true
AdPolicyRepo:   - Banner Ad: true
AdPolicyRepo:   - Max Per Hour: 2
AdPolicyRepo:   - Max Per Day: 15
AdPolicyRepo: ===== Ad Policy Fetch Completed =====
```

---

## 4 배포 체크리스트

### 4.1 Supabase 최종 확인

#### Step 1: Supabase Dashboard 접속
- [ ] URL: https://supabase.com 접속
- [ ] PocketChord 프로젝트 선택
- [ ] SQL Editor 열기

#### Step 2: 테이블 데이터 확인

```sql
SELECT * FROM ad_policy 
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

- [ ] ✅ 2개 행 반환 (release, debug)
- [ ] ✅ 모든 광고 플래그 = true
- [ ] ✅ 빈도 제한 = 기본값

---

## 5 완료 체크리스트

### 5.1 Phase 5.1, 5.2, 5.3 통합 결과

#### 시나리오 테스트 결과

| Phase | 시나리오 | 결과 | 비고 |
|-------|----------|------|------|
| 5.1 | RLS 정책 수정 | ⬜ PASS / ⬜ FAIL | 필수 |
| 5.1 | is_active 전체 제어 | ⬜ PASS / ⬜ FAIL | 필수 |
| 5.2 | App Open 제어 | ⬜ PASS / ⬜ FAIL | 필수 |
| 5.2 | Interstitial 제어 | ⬜ PASS / ⬜ FAIL | 필수 |
| 5.2 | Banner 제어 | ⬜ PASS / ⬜ FAIL | 필수 |
| 5.3 | 빈도 제한 | ⬜ PASS / ⬜ FAIL / ⬜ SKIP | 선택 |
| 5.3 | 최종 검증 | ⬜ PASS / ⬜ FAIL | 필수 |

---

### 5.2 최종 승인

**배포 승인 체크리스트**:
- [ ] ✅ 모든 테스트 통과
- [ ] ✅ is_active 정상 작동 확인
- [ ] ✅ 모든 광고 타입 정상 작동
- [ ] ✅ 빈도 제한 정상 작동 (테스트 완료 or SKIP)
- [ ] ✅ Supabase 최종 확인 완료
- [ ] ✅ Play Store 업로드 준비 완료 (별도 문서 참고)

---

## 6 문제 해결

### 6.1 광고가 표시되지 않을 때

**체크리스트**:
1. **Supabase 설정 확인**
   ```sql
   SELECT * FROM ad_policy WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
   ```
2. **정책 반영 확인**
   - 앱 재시작 (즉시 반영) 또는 3분 대기
3. **Logcat 확인**
   ```bash
   adb logcat | findstr "AdPolicyRepo"
   ```
4. **앱 데이터 초기화** (최후 수단)
   ```bash
   adb shell pm clear com.sweetapps.pocketchord.debug
   ```
5. **캐시 초기화** (최후 수단)
   ```bash
   adb shell pm clear com.sweetapps.pocketchord
   ```

---

### 6.2 긴급 광고 제어

```sql
-- 모든 광고 즉시 차단
UPDATE ad_policy
SET is_active = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

**반영 시간**: 
- 즉시 (앱 재시작 시) 또는 최대 3분 (실행 중)

---


**문서 작성**: GitHub Copilot  
**최종 업데이트**: 2025-11-11  
**버전**: v3.1  
**Phase 5.3 완료**: ⬜ PASS / ⬜ FAIL  
**배포 준비**: ⬜ 완료 / ⬜ 미완료

