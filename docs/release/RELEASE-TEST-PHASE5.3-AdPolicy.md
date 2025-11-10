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
- ✅ **Release 빌드**: 배포용 APK 생성
- ✅ **Play Store 준비**: 배포 체크리스트 완료

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

**예상 로그 (첫 번째 광고)**:
```
InterstitialAdManager: 📊 광고 카운트 증가: 시간당 1, 일일 1
InterstitialAdManager: 전면광고 표시됨
```

**예상 로그 (두 번째 시도)**:
```
InterstitialAdManager: ⚠️ 시간당 빈도 제한 초과: 1/1
InterstitialAdManager: 전면광고 표시 조건 미달
```

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

### 3.1 모든 광고 설정 확인

#### SQL로 최종 상태 확인

```sql
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
- [ ] **검증**: Banner 광고 정상 표시 (화면 하단)
- [ ] **검증**: Interstitial 광고 정상 표시 (조건 만족 시)
- [ ] **검증**: 앱 충돌 없음
- [ ] **검증**: Logcat 에러 없음

#### Logcat 최종 확인

```bash
adb logcat | findstr "AdPolicyRepo"
```

**예상 로그**:
```
AdPolicyRepo: ===== Ad Policy Fetch Started =====
AdPolicyRepo: 🔄 Supabase에서 광고 정책 새로 가져오기
AdPolicyRepo: Total rows fetched: 2
AdPolicyRepo: ✅ 광고 정책 발견!
AdPolicyRepo:   - is_active: true
AdPolicyRepo:   - App Open Ad: true
AdPolicyRepo:   - Interstitial Ad: true
AdPolicyRepo:   - Banner Ad: true
AdPolicyRepo: ===== Ad Policy Fetch Completed =====
```

---

## 4 배포 체크리스트

### 4.1 Supabase 최종 확인

#### Step 1: Supabase Dashboard 접속
- [ ] URL: https://supabase.com 접속
- [ ] PocketChord 프로젝트 선택
- [ ] SQL Editor 열기

#### Step 2: RLS 정책 확인

```sql
-- RLS 정책 확인
SELECT schemaname, tablename, policyname, cmd, qual
FROM pg_policies
WHERE tablename = 'ad_policy';
```

**기대 결과**:
```
policyname: ad_policy_select_all
qual: true
```

- [ ] ✅ RLS 정책 수정 적용 확인

#### Step 3: 테이블 데이터 확인

```sql
SELECT * FROM ad_policy 
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

- [ ] ✅ 2개 행 반환 (release, debug)
- [ ] ✅ 모든 광고 플래그 = true
- [ ] ✅ 빈도 제한 = 기본값

---

### 4.2 로컬 빌드 테스트

#### Debug 빌드

```bash
cd G:\Workspace\PocketChord
gradlew assembleDebug
```

**확인 사항**:
- [ ] ✅ BUILD SUCCESSFUL
- [ ] ✅ APK 생성 확인
- [ ] ✅ 컴파일 에러 없음

#### APK 설치 및 테스트

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**기능 확인**:
- [ ] ✅ 앱 시작 시 App Open 광고 표시
- [ ] ✅ 배너 광고 정상 표시
- [ ] ✅ Interstitial 광고 정상 표시 (조건 만족 시)
- [ ] ✅ 충돌 없음

---

### 4.3 Release 빌드

#### Step 1: Release 빌드

```bash
gradlew assembleRelease
```

**확인**:
- [ ] ✅ BUILD SUCCESSFUL
- [ ] ✅ APK 생성: app/release/app-release.apk

#### Step 2: 서명 확인

```bash
jarsigner -verify -verbose app/release/app-release.apk
```

**기대 결과**:
- [ ] ✅ jar verified.

#### Step 3: APK 크기 확인

- [ ] ✅ 적정 크기 (이전 버전과 유사)
- [ ] ✅ 급격한 크기 증가 없음

---

### 4.4 Play Store 준비

#### 버전 확인

```kotlin
// app/build.gradle.kts
versionCode = ___  // 이전 값보다 +1
versionName = "___"  // 적절한 버전 (예: "3.1.0")
```

**체크리스트**:
- [ ] ✅ versionCode 증가 확인
- [ ] ✅ versionName 적절
- [ ] ✅ 변경 사항 문서 작성

#### 변경 사항 작성 예시

```
제목: 광고 시스템 개선 및 안정성 향상

내용:
- 광고 정책 실시간 제어 기능 추가
- 광고 표시 로직 최적화
- 앱 안정성 향상
- 성능 개선
```

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

### 5.2 배포 준비 완료 확인

#### Supabase
- [ ] ✅ RLS 정책 수정 완료
- [ ] ✅ ad_policy 테이블 데이터 확인
- [ ] ✅ 운영 기본값 설정 완료

#### 코드
- [ ] ✅ Debug 빌드 성공
- [ ] ✅ Release 빌드 성공
- [ ] ✅ 서명 확인 완료
- [ ] ✅ 모든 기능 정상 작동

#### 문서
- [ ] ✅ Phase 5.1 문서 완료
- [ ] ✅ Phase 5.2 문서 완료
- [ ] ✅ Phase 5.3 문서 완료
- [ ] ✅ 변경 이력 작성
- [ ] ✅ 배포 가이드 작성

---

### 5.3 최종 승인

**배포 승인 체크리스트**:
- [ ] ✅ 모든 테스트 통과
- [ ] ✅ RLS 정책 수정 완료
- [ ] ✅ is_active 정상 작동 확인
- [ ] ✅ 모든 광고 타입 정상 작동
- [ ] ✅ 빈도 제한 정상 작동 (테스트 완료 or SKIP)
- [ ] ✅ Release 빌드 성공
- [ ] ✅ 문서 업데이트 완료
- [ ] ✅ Play Store 업로드 준비 완료

**승인자**: _______________  
**배포 일시**: 2025-__-__  
**배포 버전**: v___  

---

### 5.4 발견된 이슈

**Phase 5.1, 5.2, 5.3 테스트 중 발견된 이슈**:

1. _____________________________________________
2. _____________________________________________
3. _____________________________________________

**해결 여부**:
- [ ] ⬜ 모든 이슈 해결됨
- [ ] ⬜ 일부 이슈 남음 (배포 전 해결 필요)
- [ ] ⬜ 이슈 없음

---

## 6 문제 해결

### 6.1 광고가 표시되지 않을 때

**체크리스트**:
1. **RLS 정책 수정 확인** (Phase 5.1)
2. **Supabase 설정 확인**
   ```sql
   SELECT * FROM ad_policy WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
   ```
3. **정책 반영 확인**
   - 앱 재시작 (즉시 반영) 또는 3분 대기
4. **Logcat 확인**
   ```bash
   adb logcat | findstr "AdPolicyRepo"
   ```
5. **빈도 제한 확인**
   - 시간당/일일 제한 초과 여부
6. **캐시 초기화** (최후 수단)
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

## 7 참고 문서

### 관련 문서
- `docs/release/RELEASE-TEST-PHASE5.1-RELEASE.md` - Phase 5.1: RLS 정책 수정
- `docs/release/RELEASE-TEST-PHASE5.2-RELEASE.md` - Phase 5.2: 개별 광고 제어
- `docs/sql/fix-rls-policy.sql` - RLS 정책 수정 SQL
- `docs/archive/IS-ACTIVE-FIX-COMPLETE.md` - is_active 근본 해결 가이드

### 변경 이력
- **v3.1 (2025-11-11)**: 
  - Phase 5.1: RLS 정책 수정, is_active 테스트
  - Phase 5.2: 개별 광고 제어, 전면광고 조건 상세화
  - Phase 5.3: 빈도 제한, 최종 검증, 배포 준비

---

**문서 작성**: GitHub Copilot  
**최종 업데이트**: 2025-11-11  
**버전**: v3.1  
**Phase 5.3 완료**: ⬜ PASS / ⬜ FAIL  
**배포 준비**: ⬜ 완료 / ⬜ 미완료
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
- [ ] Logcat 필터 설정: `tag:AdPolicyRepo`

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
- [ ] 앱 완전 종료 (백그라운드에서 제거)
- [ ] 앱 재실행
- [ ] **검증**: App Open 광고 표시 안 됨 ✅ (즉시 반영)
- [ ] 코드 여러 개 조회
- [ ] **검증**: Interstitial 광고 표시 안 됨 ✅
- [ ] **검증**: Banner 광고 표시 안 됨 ✅

**방법 B: 앱 실행 중 대기 (캐싱 테스트)**
- [ ] 앱을 종료하지 않고 계속 실행
- [ ] **최대 3분 대기** (캐시 만료)
- [ ] 배너 광고가 자동으로 사라지는지 확인
- [ ] **검증**: 3분 이내 배너 광고 사라짐 ✅

### 4.4 Logcat 확인

```
예상 로그:
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
InterstitialAdManager: [정책] is_active = false - 모든 광고 비활성화
MainActivity: [정책] is_active = false - 모든 광고 비활성화
```

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

**Phase 5.2 문서**: [RELEASE-TEST-PHASE5.2-RELEASE.md](RELEASE-TEST-PHASE5.2-RELEASE.md)

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

## 7 참고 문서

### 관련 문서
- `docs/sql/fix-rls-policy.sql` - RLS 정책 수정 SQL
- `docs/archive/IS-ACTIVE-FIX-COMPLETE.md` - is_active 근본 해결 완전 가이드
- `docs/release/RELEASE-TEST-PHASE5.2-RELEASE.md` - Phase 5.2: 개별 광고 제어

### 변경 이력
- **v3.1 (2025-11-11)**: is_active 근본 해결, RLS 정책 수정, 3분 캐싱

---

**문서 작성**: GitHub Copilot  
**최종 업데이트**: 2025-11-11  
**버전**: v3.1  
**Phase 5.1 완료**: ⬜ PASS / ⬜ FAIL

