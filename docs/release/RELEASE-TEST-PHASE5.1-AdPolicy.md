# 릴리즈 테스트 - Phase 5.1 (RLS 정책 수정 + 기본 테스트)

**버전**: v3.1 (is_active 근본 해결)  
**최종 업데이트**: 2025-11-11  
**소요 시간**: 약 15-20분

---

## 📋 목차

1. [개요](#1-개요)
2. [중요: RLS 정책 수정 (최초 1회)](#2-중요-rls-정책-수정-최초-1회)
3. [테스트 준비](#3-테스트-준비)
4. [is_active 전체 광고 제어 테스트](#4-is_active-전체-광고-제어-테스트)
5. [다음 단계](#5-다음-단계)

---

## 1 개요

### 1.1 Phase 5.1의 목적

Phase 5.1에서는 가장 중요한 기초 작업을 수행합니다:
- ✅ **RLS 정책 수정**: is_active가 정상 작동하도록 근본 해결
- ✅ **is_active 테스트**: 메인 제어 기능 검증
- ✅ **3분 캐싱 이해**: 정책 반영 시간 파악

**⚠️ 중요: 캐싱 타임 구분**
```
광고 정책 (ad_policy):   3분 캐싱 ✅ ← 이 문서의 대상
앱 정책 (app_policy):    5분 캐싱

이유: 광고는 긴급 제어가 필요하므로 캐싱 타임을 짧게 설정
```

### 1.2 ad_policy 테이블 구조

| 필드명 | 기본값 | 설명 |
|--------|--------|------|
| `is_active` | true | 전체 광고 ON/OFF (메인 제어) |
| `ad_app_open_enabled` | true | App Open 광고 |
| `ad_interstitial_enabled` | true | Interstitial 광고 |
| `ad_banner_enabled` | true | Banner 광고 |
| `ad_interstitial_max_per_hour` | 2 | 시간당 최대 횟수 |
| `ad_interstitial_max_per_day` | 15 | 일일 최대 횟수 |

### 1.3 광고 정책 제어 방식

**핵심 변경사항 (2025-11-11)**:
- ✅ **is_active 정상화**: RLS 정책 수정으로 is_active가 의도대로 작동
- ✅ **3분 캐싱**: 앱 실행 중 정책 변경 시 최대 3분 이내 반영
- ✅ **즉시 반영**: 앱 재시작 시 즉시 반영 (캐시 초기화)
- ✅ **직관적인 제어**: is_active 하나로 모든 광고 제어 가능

**제어 우선순위**:
```
1. is_active = false → 모든 광고 비활성화 (가장 강력)
2. is_active = true → 개별 플래그 확인
   - ad_app_open_enabled
   - ad_interstitial_enabled
   - ad_banner_enabled
3. 정책 없음 → 기본값 true (Supabase 장애 대응)
```

**정책 반영 시간**:
```
방법 1: 앱 재시작
  → 즉시 반영 (0초) ✅ 권장

방법 2: 앱 실행 중 대기
  → 최대 3분 대기 (캐시 만료)
  → 배너 광고는 자동으로 3분마다 체크하여 반영
```

**3분 캐싱을 선택한 이유**:

| 측면 | 1분 | 3분 (선택) ✅ | 5분 |
|------|-----|---------------|-----|
| 긴급 대응 | 매우 빠름 | **충분히 빠름** | 느림 |
| 네트워크 부담 | 높음 (60회/시간) | **적절함 (20회/시간)** | 낮음 (12회/시간) |
| 배터리 소모 | 높음 | **적절함** | 낮음 |
| 실제 효과 | 과도함 | **균형적** | 여유로움 |

**선택 근거**:
1. ✅ **긴급 대응 충분**: 3분이면 심각한 상황에 충분히 빠르게 대응 가능
2. ✅ **효율성**: 1분 대비 네트워크 요청 66% 감소 (960회/일 절감)
3. ✅ **배터리 절약**: 요청 빈도 감소로 사용자 배터리 수명 향상
4. ✅ **업계 표준**: 대부분의 앱이 3~5분 캐싱 사용
5. ✅ **실용성**: 실제 운영에서 새로 앱을 여는 사용자는 즉시 반영(0초)

---

## 2 중요: RLS 정책 수정 (최초 1회)

### 2.1 왜 수정이 필요한가?

**이전 문제**:
- ❌ RLS 정책이 `is_active = false`인 행을 숨김
- ❌ 앱에서 정책을 찾을 수 없어 기본값 적용
- ❌ 결과: `is_active = false` 설정 시 광고가 켜짐 (역설!)

**해결 방안**:
- ✅ RLS를 보안 용도가 아닌 public 테이블로 변경
- ✅ 앱 코드에서 `is_active`를 명시적으로 체크
- ✅ 이제 `is_active`가 정상적으로 작동

### 2.2 RLS 정책 수정 SQL

**Supabase Dashboard → SQL Editor에서 실행**:

```sql
-- ============================================
-- ad_policy RLS 정책 수정 (근본 해결)
-- ============================================
-- 작성일: 2025-11-11
-- 목적: is_active를 정상적으로 사용 가능하도록 RLS 정책 수정
-- ============================================

-- 1. 기존 RLS 정책들 제거
DROP POLICY IF EXISTS "ad_policy_select" ON ad_policy;
DROP POLICY IF EXISTS "ad_policy_select_all" ON ad_policy;

-- 2. 새로운 RLS 정책: 모든 행 조회 가능
CREATE POLICY "ad_policy_select_all" ON ad_policy
  FOR SELECT USING (true);

-- 3. 확인
SELECT app_id, is_active, ad_banner_enabled 
FROM ad_policy;
-- 이제 is_active = false인 행도 조회됨
```

**실행 확인**:
- [ ] ✅ SQL 실행 완료
- [ ] ✅ 에러 없음
- [ ] ✅ 모든 행이 조회됨

**참고**: 이 수정은 **최초 1회만** 실행하면 됩니다.

---

## 3 테스트 준비

### 3.1 사전 확인
- [ ] Supabase SQL Editor 접속 완료
- [ ] **RLS 정책 수정 완료** (섹션 2)
- [ ] 테스트 기기/에뮬레이터 연결 확인
- [ ] **Logcat 필터 설정** (Android Studio):
  
  **방법 1: 태그 필터 사용 (권장)**
  - Logcat 창 상단의 필터 입력란에 입력:
  ```
  tag:AdPolicyRepo | tag:InterstitialAdManager | tag:AppOpenAdManager | tag:MainActivity
  ```
  - 또는 간단히:
  ```
  tag:AdPolicyRepo | tag:InterstitialAdManager
  ```
  - `|` (파이프)는 OR 조건으로, 여러 태그를 동시에 볼 수 있습니다
  
  **방법 2: 텍스트 검색**
  - 필터 입력란에 `policy` 또는 `광고` 입력 (대소문자 구분 없음)
  
  **확인**: 로그가 보이지 않으면
  - Logcat 레벨이 **"Verbose"** 또는 **"Debug"**로 설정됐는지 확인
  - 앱이 실행 중인지 확인
  - 패키지: `com.sweetapps.pocketchord.debug` 선택 확인

### 3.2 초기 상태 확인

**SQL 스크립트**:
```sql
-- 현재 광고 설정 확인
SELECT app_id, 
       is_active, 
       ad_app_open_enabled, 
       ad_interstitial_enabled, 
       ad_banner_enabled,
       ad_interstitial_max_per_hour,
       ad_interstitial_max_per_day
FROM ad_policy
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

**기대 결과** (운영 기본값):
```
is_active: true
ad_app_open_enabled: true
ad_interstitial_enabled: true
ad_banner_enabled: true
ad_interstitial_max_per_hour: 2
ad_interstitial_max_per_day: 15
```

**현재 값 기록**:
```
is_active: _____
ad_app_open_enabled: _____
ad_interstitial_enabled: _____
ad_banner_enabled: _____
max_per_hour: _____
max_per_day: _____
```

---

## 4 is_active 전체 광고 제어 테스트

### 4.1 목적

✅ **is_active = false 설정 시 모든 광고가 표시되지 않는지 확인**  
✅ **RLS 정책 수정 후 정상 작동 검증**

### 4.2 Step 1: 전체 광고 OFF

```sql
UPDATE ad_policy
SET is_active = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

### 4.3 Step 2: 앱 실행 및 검증

**방법 A: 즉시 반영 (권장)**

**1) 앱 재시작**
- [ ] 앱 완전 종료 (백그라운드에서 제거)
- [ ] 앱 재실행

**2) App Open 광고 테스트**
- [ ] 홈 버튼으로 백그라운드 전환
- [ ] 앱 아이콘 터치하여 다시 포그라운드로 복귀
- [ ] **검증**: App Open 광고 표시 안 됨 ✅

**3) Interstitial 광고 테스트**
- [ ] 코드 선택 → 상세 화면 → 뒤로가기 (3회 반복)
- [ ] 60초 대기 (전면광고 간격 제한 - 3분 캐싱과는 별개)
  > 📌 전면광고는 이전 광고 표시 후 60초가 지나야 다음 광고 표시 가능 (하드코딩된 조건)
- [ ] 다시 코드 선택 → 뒤로가기
- [ ] **검증**: Interstitial 광고 표시 안 됨 ✅

**4) Banner 광고 테스트**
- [ ] 앱의 배너 광고 위치 확인
- [ ] **검증**: Banner 광고 표시 안 됨 ✅

---

**방법 B: 앱 실행 중 대기 (캐싱 테스트)**

- [ ] 앱을 종료하지 않고 계속 실행
- [ ] **최대 3분 대기** (캐시 만료)
- [ ] 배너 광고가 자동으로 사라지는지 확인
- [ ] **검증**: 3분 이내 배너 광고 사라짐 ✅

**참고**: 
- App Open 광고는 앱이 포그라운드로 복귀할 때만 표시됩니다
- Interstitial 광고는 특정 화면 전환 패턴에서만 표시됩니다

### 4.4 Logcat 확인

**필터 설정**: `tag:AdPolicyRepo | tag:InterstitialAdManager`

**예상 로그**:
```
---------------------------- PROCESS STARTED (xxxxx) for package com.sweetapps.pocketchord.debug ----------------------------
AdPolicyRepo: ===== Ad Policy Fetch Started =====
AdPolicyRepo: 🔄 Supabase에서 광고 정책 새로 가져오기
AdPolicyRepo: Target app_id: com.sweetapps.pocketchord.debug
AdPolicyRepo: Total rows fetched: 2
AdPolicyRepo: ✅ 광고 정책 발견!
AdPolicyRepo:   - is_active: false
AdPolicyRepo:   - App Open Ad: true
AdPolicyRepo:   - Interstitial Ad: true
AdPolicyRepo:   - Banner Ad: true
AdPolicyRepo:   - Max Per Hour: 2
AdPolicyRepo:   - Max Per Day: 15
AdPolicyRepo: ===== Ad Policy Fetch Completed =====
InterstitialAdManager: 전면광고 로드 성공
```

**추가로 확인할 로그** (is_active = false 검증):
```
InterstitialAdManager: [정책] is_active = false - 모든 광고 비활성화
AppOpenAdManager: [정책] is_active = false - 모든 광고 비활성화
MainActivity: [정책] is_active = false - 모든 광고 비활성화
MainActivity: 🔄 배너 광고 정책 변경: 활성화 → 비활성화
```

**참고**: 
- 실제 로그에는 타임스탬프와 프로세스 정보가 포함됩니다
- ✅ **`전면광고 로드 성공`은 정상입니다**: 광고 SDK가 광고를 미리 준비한 것으로, 실제 표시와는 별개입니다
- is_active = false 체크는 **광고 표시 시도 시** 나타나며, 이때 비로소 광고가 차단됩니다

### 4.5 Step 3: 복구

```sql
UPDATE ad_policy
SET is_active = true
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

**확인**:
- [ ] ✅ 재활성화 완료
- [ ] ✅ **중요**: is_active가 정상 작동함을 확인!
- [ ] ✅ 앱 재시작 후 모든 광고 정상 표시

---

## 5 다음 단계

### 5.1 Phase 5.1 완료 확인

**완료된 테스트**:
- [ ] ✅ RLS 정책 수정 완료
- [ ] ✅ is_active = false 테스트 통과
- [ ] ✅ is_active = true 복구 확인
- [ ] ✅ 3분 캐싱 동작 이해

**주요 검증 사항**:
- [ ] ✅ RLS 정책 수정 완료 (`USING (true)`)
- [ ] ✅ is_active가 정상적으로 작동
- [ ] ✅ 즉시 반영 (앱 재시작) 확인
- [ ] ✅ 캐싱 동작 (앱 실행 중) 확인

---

### 5.2 다음 단계: Phase 5.2

**Phase 5.2에서 수행할 내용**:
- 📋 **개별 광고 제어**: App Open, Interstitial, Banner 개별 테스트
- 📋 **전면광고 조건**: 상세한 표시 조건 이해
- 📋 **Logcat 분석**: 각 광고 타입별 로그 확인

**Phase 5.2 문서**: [RELEASE-TEST-PHASE5.2-AdPolicy.md](RELEASE-TEST-PHASE5.2-AdPolicy.md)

---

## 6 문제 해결

### 6.1 is_active = false인데 광고가 나올 때

**원인**: RLS 정책 수정을 하지 않았거나 실패했습니다.

**해결**:
1. 섹션 2의 RLS 정책 수정 SQL을 다시 실행
2. 다음 SQL로 정책 확인:
   ```sql
   -- RLS 정책 확인
   SELECT schemaname, tablename, policyname, cmd, qual
   FROM pg_policies
   WHERE tablename = 'ad_policy';
   ```
   예상 결과:
   ```
   policyname: ad_policy_select_all
   qual: true
   ```

3. 앱 완전 종료 후 재시작

---

### 6.2 광고가 표시되지 않을 때

**체크리스트**:
1. **RLS 정책 수정 확인** (섹션 2)
2. **Supabase 설정 확인**
   ```sql
   SELECT * FROM ad_policy 
   WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
   ```
3. **정책 반영 확인**
   - 앱 재시작 (즉시 반영)
   - 또는 3분 대기 (캐시 만료)

4. **Logcat 확인**
   ```bash
   adb logcat | findstr "AdPolicyRepo"
   ```

---

**문서 작성**: GitHub Copilot  
**최종 업데이트**: 2025-11-11  
**버전**: v3.1  
**Phase 5.1 완료**: ⬜ PASS / ⬜ FAIL
