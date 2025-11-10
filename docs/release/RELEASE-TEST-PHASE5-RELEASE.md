# 릴리즈 테스트 - Phase 5 (Ad Policy + 배포)

**버전**: v3.1 (is_active 근본 해결)  
**최종 업데이트**: 2025-11-11  
**소요 시간**: 약 20-30분

---

## 📋 목차

1. [개요](#1-개요)
2. [중요: RLS 정책 수정 (최초 1회)](#2-중요-rls-정책-수정-최초-1회)
3. [테스트 준비](#3-테스트-준비)
4. [시나리오 테스트](#4-시나리오-테스트)
5. [문제 해결](#5-문제-해결)
6. [배포 체크리스트](#6-배포-체크리스트)
7. [완료 체크리스트](#7-완료-체크리스트)

---

## 1 개요

### 1.1 ad_policy 테이블 구조

| 필드명 | 기본값 | 설명 |
|--------|--------|------|
| `is_active` | true | 전체 광고 ON/OFF (메인 제어) |
| `ad_app_open_enabled` | true | App Open 광고 |
| `ad_interstitial_enabled` | true | Interstitial 광고 |
| `ad_banner_enabled` | true | Banner 광고 |
| `ad_interstitial_max_per_hour` | 2 | 시간당 최대 횟수 |
| `ad_interstitial_max_per_day` | 15 | 일일 최대 횟수 |

### 1.2 광고 정책 제어 방식

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
4. ✅ **업계 표준**: 대부분의 앱이 3~5분 캐싱 사용 (Firebase: 12시간, 광고 정책: 3~5분)
5. ✅ **실용성**: 실제 운영에서 새로 앱을 여는 사용자는 즉시 반영(0초)되므로 3분으로 충분

**실제 시나리오**:
```
긴급 광고 차단 필요 시:
  → Supabase에서 is_active = false 설정
  → 새로 시작하는 사용자: 즉시 차단 (0초) ✅
  → 실행 중인 사용자: 최대 3분 이내 차단 ✅
  → 결과: 3분이면 충분히 효과적!
```

---

## 2 중요: RLS 정책 수정 (최초 1회)

### 2.1 왜 수정이 필요한가?

**이전 문제**:
- ❌ RLS 정책이 `is_active = false`인 행을 숨김
- ❌ 앱에서 정책을 찾을 수 없어 기본값 적용
- ❌ 결과: `is_active = false` 설정 시 광고가 켜짐 (역설!)

**해결 방안**:
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
- [ ] Logcat 필터 설정: `tag:AdPolicy` 또는 `tag:AdMob`

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
WHERE app_id = 'com.sweetapps.pocketchord';
```

**기대 결과** (운영 기본값):
```
```
- [ ] **검증**: App Open 광고 표시 안 됨 ✅ (즉시 반영)

## 4 시나리오 테스트

### 4.1 전체 광고 비활성화 (is_active 테스트)
**방법 B: 앱 실행 중 대기 (캐싱 테스트)**
- [ ] 앱을 종료하지 않고 계속 실행
- [ ] **최대 3분 대기** (캐시 만료)
- [ ] 배너 광고가 자동으로 사라지는지 확인
- [ ] **검증**: 3분 이내 배너 광고 사라짐 ✅


#### Step 2: 앱 실행 및 검증

**방법 A: 즉시 반영 (권장)**
- [ ] 앱 완전 종료 (백그라운드에서 제거)
✅ **RLS 정책 수정 후 정상 작동 검증**
- [ ] **검증**: App Open 광고 표시 안 됨 ✅ (즉시 반영)
```sql
UPDATE ad_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
**방법 B: 앱 실행 중 대기 (캐싱 테스트)**
- [ ] 앱을 종료하지 않고 계속 실행
- [ ] **최대 3분 대기** (캐시 만료)
- [ ] 배너 광고가 자동으로 사라지는지 확인
- [ ] **검증**: 3분 이내 배너 광고 사라짐 ✅

#### Logcat 확인
```
예상 로그:
AdPolicyRepo: ✅ 광고 정책 발견!
AdPolicyRepo:   - is_active: false
InterstitialAdManager: [정책] is_active = false - 모든 광고 비활성화
MainActivity: [정책] is_active = false - 모든 광고 비활성화
```

#### Step 3: 복구
```sql
UPDATE ad_policy
SET is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```
- [ ] ✅ 재활성화 완료
- [ ] ✅ **중요**: is_active가 정상 작동함을 확인!

---

### 4.2 App Open 광고 제어

#### 목적
App Open 광고만 개별 제어

#### Step 1: App Open만 비활성화
```sql
UPDATE ad_policy
SET ad_app_open_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행 및 검증
- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] **검증**: App Open 광고 표시 안 됨
- [ ] 백그라운드 → 포그라운드 전환
- [ ] **검증**: App Open 광고 표시 안 됨
- [ ] 코드 조회 (3회)
- [ ] **검증**: Interstitial 광고 정상 표시
- [ ] **검증**: Banner 광고 정상 표시

#### Logcat 확인
```
AdPolicy: App Open enabled=false
AdMob: App Open Ad disabled by policy
```

#### Step 3: 복구
```sql
UPDATE ad_policy
SET ad_app_open_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord';
```
- [ ] ✅ 재활성화 완료

---

### 4.3 Interstitial 광고 제어

#### 목적
Interstitial 광고만 개별 제어

#### Step 1: Interstitial만 비활성화
```sql
UPDATE ad_policy
SET ad_interstitial_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행 및 검증
- [ ] 앱 재실행
- [ ] **검증**: App Open 광고 정상 표시 (백그라운드 복귀 시)
- [ ] 코드 여러 개 조회 (3회 이상)
- [ ] **검증**: Interstitial 광고 표시 안 됨
- [ ] **검증**: Banner 광고 정상 표시

#### Logcat 확인
```
AdPolicy: Interstitial enabled=false
AdMob: Interstitial Ad disabled by policy
```

#### Step 3: 복구
```sql
UPDATE ad_policy
SET ad_interstitial_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord';
```
- [ ] ✅ 재활성화 완료

---

### 4.4 Banner 광고 제어

#### 목적
Banner 광고만 개별 제어

#### Step 1: Banner만 비활성화
```sql
UPDATE ad_policy
SET ad_banner_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행 및 검증
- [ ] 앱 재실행
- [ ] **검증**: App Open 광고 정상 표시
- [ ] **검증**: 화면 하단 배너 표시 안 됨
- [ ] 코드 조회 (3회)
- [ ] **검증**: Interstitial 광고 정상 표시

#### Logcat 확인
```
AdPolicy: Banner enabled=false
AdMob: Banner Ad disabled by policy
```

#### Step 3: 복구
```sql
UPDATE ad_policy
SET ad_banner_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord';
```
- [ ] ✅ 재활성화 완료

---

### 4.5 빈도 제한 테스트 (선택사항)

#### 목적
Interstitial 광고 빈도 제한 동작 확인

#### Step 1: 빈도 제한 낮추기
```sql
UPDATE ad_policy
SET ad_interstitial_max_per_hour = 1,
    ad_interstitial_max_per_day = 3
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 테스트
- [ ] 앱 재실행
- [ ] 코드 조회 → **검증**: 전면 광고 표시 (1회)
- [ ] 코드 조회 → **검증**: 전면 광고 표시 안 됨 (제한 도달)

#### Logcat 확인
```
InterstitialAdManager: Ad shown (1/1 per hour)
InterstitialAdManager: ⚠️ Hourly limit reached
```

#### Step 3: 운영 설정 복구
```sql
UPDATE ad_policy
SET ad_interstitial_max_per_hour = 2,
    ad_interstitial_max_per_day = 15
WHERE app_id = 'com.sweetapps.pocketchord';
```
- [ ] ✅ 운영 설정 복구 완료

---

### 4.6 최종 확인

#### Step 1: 모든 광고 정상화 확인
```sql
SELECT app_id, 
       is_active, 
       ad_app_open_enabled, 
       ad_interstitial_enabled, 
       ad_banner_enabled,
       ad_interstitial_max_per_hour,
       ad_interstitial_max_per_day
FROM ad_policy
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 최종 검증
- [ ] ✅ is_active = true
- [ ] ✅ ad_app_open_enabled = true
- [ ] ✅ ad_interstitial_enabled = true
- [ ] ✅ ad_banner_enabled = true
- [ ] ✅ max_per_hour = 2
- [ ] ✅ max_per_day = 15

#### Step 3: 실제 동작 확인
- [ ] 앱 재실행
- [ ] ✅ 모든 광고 정상 표시
- [ ] ✅ Phase 5 완료!

---
3. **정책 반영 확인**
   
   **즉시 반영 (권장)**:
   - 앱 완전 종료 (백그라운드에서 제거)
   - 앱 재시작
   - 로그 확인: `🔄 Supabase에서 광고 정책 새로 가져오기`
   
   **앱 실행 중 (캐시 대기)**:
   - 정책 변경 후 **최대 3분** 대기
   - 배너 광고가 자동으로 변경됨
   - 전면/앱오픈 광고는 다음 표시 시점에 반영
   - 전면/앱오픈 광고는 다음 표시 시점에 반영
### 5.1 광고가 표시되지 않을 때

**체크리스트**:
1. **RLS 정책 수정 확인** (섹션 2)
   - RLS 정책 수정 SQL을 실행했는가?
   - `USING (true)` 정책이 적용되었는가?

2. **Supabase 설정 확인**
   ```sql
   SELECT * FROM ad_policy 
   WHERE app_id = 'com.sweetapps.pocketchord';
   ```
   - `is_active = true`인가?
   - 해당 광고 플래그가 `true`인가?

3. **정책 반영 확인**
   
   **즉시 반영 (권장)**:
   - 앱 완전 종료 (백그라운드에서 제거)
   - 앱 재시작
   - 로그 확인: `🔄 Supabase에서 광고 정책 새로 가져오기`
   
   **앱 실행 중 (캐시 대기)**:
   - 정책 변경 후 **최대 3분** 대기
   - 배너 광고가 자동으로 변경됨
   - 전면/앱오픈 광고는 다음 표시 시점에 반영

4. **Logcat 확인**
   ```bash
   adb logcat | findstr "AdPolicyRepo"
   adb logcat | findstr "정책"
   ```
   예상 로그:
   ```
3. **정책 반영 확인**
   
   **즉시 반영 (권장)**:
   - 앱 완전 종료 (백그라운드에서 제거)
   - 앱 재시작
   - 로그 확인: `🔄 Supabase에서 광고 정책 새로 가져오기`
   
   **앱 실행 중 (캐시 대기)**:
   - 정책 변경 후 **최대 3분** 대기
   - 배너 광고가 자동으로 변경됨
   - 전면/앱오픈 광고는 다음 표시 시점에 반영
   ```

5. **빈도 제한 확인**
   - 시간당/일일 제한 도달했는지 확인
   - 로그에서 `⚠️ limit reached` 메시지 확인

6. **캐시 초기화** (최후 수단)
   ```bash
   adb shell pm clear com.sweetapps.pocketchord
   ```

### 5.2 is_active = false인데 광고가 나올 때

**원인**: RLS 정책 수정을 하지 않았거나 실패했습니다.

**해결**:
1. 섹션 2의 RLS 정책 수정 SQL을 다시 실행
2. 다음 SQL로 정책 확인:
   ```sql
   -- RLS 정책 확인
   SELECT schemaname, tablename, policyname, cmd, qual
-- 모든 광고 즉시 차단
   WHERE tablename = 'ad_policy';
   ```
   예상 결과:
   ```
   policyname: ad_policy_select_all
   qual: true
   ```

3. 앱 완전 종료 후 재시작

**반영 시간**:
- ✅ **즉시 반영**: 사용자가 앱 재시작 시 (권장)
- ⏰ **최대 1분**: 앱 실행 중 (캐시 만료 대기)

### 5.3 긴급 광고 제어

**⚠️ 2025-11-11 업데이트: is_active 정상화 완료!**
- ✅ 앱 재시작 시 즉시 적용
#### 방법 1: is_active 사용 (✅ 권장, 간단하고 직관적)

```sql
-- 모든 광고 차단 (최대 3분 소요, 앱 재시작 시 즉시)
UPDATE ad_policy
SET is_active = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

-- 모든 광고 다시 활성화
UPDATE ad_policy
SET is_active = true
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

**장점**:
- ✅ 한 줄로 모든 광고 제어
- ✅ 직관적이고 명확
- ✅ 앱 재시작 시 즉시, 실행 중 3분 이내 반영

#### 방법 2: 개별 플래그 사용 (세밀한 제어)

```sql
-- 모든 광고 플래그를 개별적으로 끄기
UPDATE ad_policy
SET 
  ad_app_open_enabled = false,
  ad_interstitial_enabled = false,
  ad_banner_enabled = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

**장점**:
- ✅ 개별 광고 독립 제어 가능
- ✅ 유연성 높음

#### 현재 설정 확인

```sql
SELECT app_id, is_active, ad_app_open_enabled, ad_interstitial_enabled, ad_banner_enabled
FROM ad_policy
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

#### 예상 로그 (정책 적용 성공 시)

```
AdPolicyRepo: 🔄 Supabase에서 광고 정책 새로 가져오기
AdPolicyRepo: Total rows fetched: 1
AdPolicyRepo: ✅ 광고 정책 발견!
AdPolicyRepo:   - is_active: false
AdPolicyRepo:   - App Open Ad: true
AdPolicyRepo:   - Interstitial Ad: true
AdPolicyRepo:   - Banner Ad: true
InterstitialAdManager: [정책] is_active = false - 모든 광고 비활성화
MainActivity: [정책] is_active = false - 모든 광고 비활성화
```

#### 예상 로그 (정책 조회 실패 시 - 기본값 적용)

```
AdPolicyRepo: Total rows fetched: 0
AdPolicyRepo: ⚠️ 광고 정책 없음 (app_id: xxx)
AdPolicyRepo: ⚠️ 기본값 사용됨
InterstitialAdManager: [정책] 정책 없음 - 기본값(true) 사용
MainActivity: [정책] 정책 없음 - 기본값(true) 사용
```

**참고**: 정책 변경은 **최대 3분** 소요됩니다 (캐시 만료 주기). 앱 재시작 시 즉시 반영됩니다.

---

## 6 배포 체크리스트

### 6.1 Supabase 작업

#### Step 1: Supabase Dashboard 로그인
- [ ] URL: https://supabase.com 접속
- [ ] PocketChord 프로젝트 선택
**참고**: 정책 변경은 **최대 3분** 소요됩니다 (캐시 만료 주기).

#### Step 2: RLS 정책 수정 (최초 1회, 중요!)
**섹션 2의 RLS 정책 수정 SQL을 실행하세요.**

#### Step 3: ad_policy 테이블 확인
```sql
SELECT * FROM ad_policy 
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

**기대 결과**:
- [ ] ✅ 2개 행 반환 (release, debug)
- [ ] ✅ is_active = true
- [ ] ✅ 모든 광고 enabled = true

---

### 6.2 로컬 빌드 테스트

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
- [ ] ✅ Interstitial 광고 정상 표시
- [ ] ✅ 충돌 없음

---

### 3 Supabase 제어 테스트

#### 테스트 1: 배너 광고 제어
```sql
-- 배너 OFF
UPDATE ad_policy 
SET ad_banner_enabled = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

- [ ] ✅ 1분 이내 배너 광고 사라짐
- [ ] ✅ Logcat 확인: `배너 광고 정책 변경`

```sql
-- 배너 ON
- [ ] ✅ 3분 이내 배너 광고 사라짐
SET ad_banner_enabled = true 
WHERE app_id = 'com.sweetapps.pocketchord';
```

- [ ] ✅ 1분 이내 배너 광고 다시 나타남

#### 테스트 2: 전체 광고 제어
```sql
-- 모든 광고 OFF
- [ ] ✅ 3분 이내 배너 광고 다시 나타남
SET 
  ad_app_open_enabled = false,
  ad_interstitial_enabled = false,
  ad_banner_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

- [ ] ✅ 앱 재시작 후 모든 광고 표시 안 됨

```sql
-- 모든 광고 ON
UPDATE ad_policy 
SET 
  ad_app_open_enabled = true,
  ad_interstitial_enabled = true,
  ad_banner_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

- [ ] ✅ 모든 광고 정상 표시

---

### 4 Release 빌드

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

---

### 5 Play Store 준비

#### 버전 확인
```kotlin
// app/build.gradle.kts
// versionCode: 이전 값보다 +1로 설정하세요 (예: 이전=100 -> versionCode = 101)
versionCode = /* previous_version_code + 1 */  // placeholder: 실제 값을 넣어주세요
versionName = "x.y.z" // 적절한 버전 (예: "1.2.3")
```

**체크리스트**:
- [ ] ✅ 버전 코드 증가
- [ ] ✅ 버전 이름 적절
- [ ] ✅ 변경 사항 문서 작성

#### 변경 사항 예시
```
제목: 광고 시스템 개선

내용:
- 광고 표시 로직 최적화
- 안정성 향상
- 성능 개선
```

---

### 6 최종 상태 확인

#### Supabase 테이블 상태
```sql
SELECT * FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**운영 기본값 확인**:
- [ ] ✅ is_active = true
- [ ] ✅ ad_app_open_enabled = true
- [ ] ✅ ad_interstitial_enabled = true
- [ ] ✅ ad_banner_enabled = true
- [ ] ✅ max_per_hour = 2
- [ ] ✅ max_per_day = 15

#### 앱 동작 최종 확인
- [ ] ✅ 모든 광고 정상 표시
- [ ] ✅ 충돌 없음
- [ ] ✅ Logcat 정상

---

### 7 배포 승인

**최종 체크리스트**:
- [ ] ✅ 모든 테스트 완료
- [ ] ✅ 문서 업데이트 완료
- [ ] ✅ Release 빌드 성공
- [ ] ✅ Play Store 업로드 준비 완료

**승인자**: _______________  
**배포 일시**: 2025-__-__

---

## 7 완료 체크리스트

### 7.1 RLS 정책 수정 확인
- [ ] ✅ RLS 정책 수정 SQL 실행 완료
- [ ] ✅ `USING (true)` 정책 적용 확인
- [ ] ✅ is_active = false 테스트 통과

### 7.2 시나리오 통과 여부

| 시나리오 | 결과 | 비고 |
|----------|------|------|
| S1: is_active 전체 제어 | ⬜ PASS / ⬜ FAIL | **중요: RLS 수정 후 테스트** |
| S2: App Open 제어 | ⬜ PASS / ⬜ FAIL | |
| S3: Interstitial 제어 | ⬜ PASS / ⬜ FAIL | |
| S4: Banner 제어 | ⬜ PASS / ⬜ FAIL | |
| S5: 빈도 제한 (선택) | ⬜ PASS / ⬜ FAIL / ⬜ SKIP | |
| S6: 최종 확인 | ⬜ PASS / ⬜ FAIL | |

### 7.3 배포 준비

**최종 체크리스트**:
- [ ] ✅ RLS 정책 수정 완료
- [ ] ✅ 모든 시나리오 테스트 통과
- [ ] ✅ is_active 정상 작동 확인
- [ ] ✅ 문서 업데이트 완료
- [ ] ✅ Release 빌드 성공
- [ ] ✅ Play Store 업로드 준비 완료

**승인자**: _______________  
**배포 일시**: 2025-__-__

---

## 8 참고 문서

### 관련 문서
- `docs/sql/fix-rls-policy.sql` - RLS 정책 수정 SQL
- `docs/archive/IS-ACTIVE-FIX-COMPLETE.md` - is_active 근본 해결 완전 가이드
- `docs/archive/RLS-POLICY-ANALYSIS.md` - RLS 정책 설계 분석
- `docs/archive/AD-POLICY-SETTINGS-2025-11-10.md` - 광고 정책 설정 상세

### 변경 이력
- **v3.1 (2025-11-11)**: is_active 근본 해결, RLS 정책 수정, 3분 캐싱 최적화
- **v3.0 (2025-11-10)**: 1분 캐싱 도입, 기본값 true 변경
- **v2.0**: ad_policy 테이블 분리

---

**문서 작성**: GitHub Copilot  
**최종 업데이트**: 2025-11-11  
**버전**: v3.1

### 2 최종 상태 확인

- [ ] ✅ 모든 광고 설정이 운영 기본값으로 복구됨
- [ ] ✅ 실제 광고 동작 정상 확인
- [ ] ✅ Phase 5 테스트 완료

### 3 발견된 이슈

- **v3.1 (2025-11-11)**: is_active 근본 해결, RLS 정책 수정, 3분 캐싱
1. _____________________________________________
2. _____________________________________________
3. _____________________________________________
```

---

## 7. 관련 문서

- **[RELEASE-TEST-CHECKLIST.md](RELEASE-TEST-CHECKLIST.md)** - 전체 릴리즈 테스트
- **[RELEASE-TEST-PHASE1-RELEASE.md](RELEASE-TEST-PHASE1-RELEASE.md)** - Phase 1: Emergency (팝업 시스템 개요 포함)

---

**테스트 완료 일시**: ___________  
**테스트 담당자**: ___________  
**결과**: ⬜ PASS / ⬜ FAIL
