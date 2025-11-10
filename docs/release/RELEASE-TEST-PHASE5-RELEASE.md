# 릴리즈 테스트 - Phase 5 (Ad Policy + 배포)

**버전**: v3.0  
**최종 업데이트**: 2025-11-10  
**소요 시간**: 약 20-30분

---

## 📋 목차

1. [개요](#1-개요)
2. [테스트 준비](#2-테스트-준비)
3. [시나리오 테스트](#3-시나리오-테스트)
4. [문제 해결](#4-문제-해결)
5. [배포 체크리스트](#5-배포-체크리스트)
6. [완료 체크리스트](#6-완료-체크리스트)

---

## 1 개요

### 1 ad_policy 테이블 구조

| 필드명 | 기본값 | 설명 |
|--------|--------|------|
| `is_active` | true | 전체 광고 ON/OFF |
| `ad_app_open_enabled` | true | App Open 광고 |
| `ad_interstitial_enabled` | true | Interstitial 광고 |
| `ad_banner_enabled` | true | Banner 광고 |
| `ad_interstitial_max_per_hour` | 2 | 시간당 최대 횟수 |
| `ad_interstitial_max_per_day` | 15 | 일일 최대 횟수 |

---

## 2 테스트 준비

### 1 사전 확인
- [ ] Supabase SQL Editor 접속 완료
- [ ] 테스트 기기/에뮬레이터 연결 확인
- [ ] Logcat 필터 설정: `tag:AdPolicy` 또는 `tag:AdMob`

### 2 초기 상태 확인

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

## 3 시나리오 테스트

### 1 전체 광고 비활성화

#### 목적
`is_active = false` 설정 시 모든 광고가 표시되지 않는지 확인

#### Step 1: 전체 광고 OFF
```sql
UPDATE ad_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 2: 앱 실행 및 검증
- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] **검증**: App Open 광고 표시 안 됨
- [ ] 코드 여러 개 조회
- [ ] **검증**: Interstitial 광고 표시 안 됨
- [ ] **검증**: Banner 광고 표시 안 됨

#### Logcat 확인
```
예상 로그:
AdPolicy: is_active=false
AdMob: All ads disabled by policy
```

#### Step 3: 복구
```sql
UPDATE ad_policy
SET is_active = true
WHERE app_id = 'com.sweetapps.pocketchord';
```
- [ ] ✅ 재활성화 완료

---

### 2 App Open 광고 제어

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

### 3 Interstitial 광고 제어

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

### 4 Banner 광고 제어

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

### 5 빈도 제한 테스트 (선택사항)

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

### 6 최종 확인

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

## 4 문제 해결

### 1 광고가 표시되지 않을 때

**체크리스트**:
1. **Supabase 설정 확인**
   ```sql
   SELECT * FROM ad_policy 
   WHERE app_id = 'com.sweetapps.pocketchord';
   ```
   - `is_active = true`인가?
   - 해당 광고 플래그가 `true`인가?

2. **Logcat 확인**
   ```bash
   adb logcat | findstr "AdPolicy"
   adb logcat | findstr "AdMob"
   ```

3. **빈도 제한 확인**
   - 시간당/일일 제한 도달했는지 확인
   - 로그에서 `⚠️ limit reached` 메시지 확인

4. **캐시 초기화**
   ```bash
   adb shell pm clear com.sweetapps.pocketchord
   ```

### 2 긴급 조치

**모든 광고 즉시 끄기**:
```sql
UPDATE ad_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

**특정 광고만 즉시 끄기**:
```sql
-- Interstitial만
UPDATE ad_policy
SET ad_interstitial_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';

-- App Open만
UPDATE ad_policy
SET ad_app_open_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';

-- Banner만
UPDATE ad_policy
SET ad_banner_enabled = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## 5 배포 체크리스트

### 1 Supabase 작업

#### Step 1: Supabase Dashboard 로그인
- [ ] URL: https://supabase.com 접속
- [ ] PocketChord 프로젝트 선택
- [ ] SQL Editor 열기

#### Step 2: ad_policy 테이블 생성 (최초 1회)
```sql
-- ad_policy 테이블 생성
CREATE TABLE IF NOT EXISTS ad_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
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

-- 초기 데이터 삽입 (Release)
INSERT INTO ad_policy (
  app_id, is_active,
  ad_app_open_enabled, ad_interstitial_enabled, ad_banner_enabled,
  ad_interstitial_max_per_hour, ad_interstitial_max_per_day
) VALUES (
  'com.sweetapps.pocketchord',
  true, true, true, true, 2, 15
);

-- 초기 데이터 삽입 (Debug)
INSERT INTO ad_policy (
  app_id, is_active,
  ad_app_open_enabled, ad_interstitial_enabled, ad_banner_enabled,
  ad_interstitial_max_per_hour, ad_interstitial_max_per_day
) VALUES (
  'com.sweetapps.pocketchord.debug',
  true, true, true, true, 2, 15
)
ON CONFLICT (app_id) DO UPDATE SET
  is_active = EXCLUDED.is_active,
  ad_app_open_enabled = EXCLUDED.ad_app_open_enabled,
  ad_interstitial_enabled = EXCLUDED.ad_interstitial_enabled,
  ad_banner_enabled = EXCLUDED.ad_banner_enabled,
  ad_interstitial_max_per_hour = EXCLUDED.ad_interstitial_max_per_hour,
  ad_interstitial_max_per_day = EXCLUDED.ad_interstitial_max_per_day;

-- RLS 정책 생성
CREATE POLICY "ad_policy_select" ON ad_policy
  FOR SELECT USING (is_active = true);
```

#### Step 3: 테이블 생성 확인
```sql
SELECT * FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**기대 결과**:
- [ ] ✅ 1개 행 반환
- [ ] ✅ is_active = true
- [ ] ✅ 모든 광고 enabled = true
- [ ] ✅ max_per_hour = 2
- [ ] ✅ max_per_day = 15

---

### 2 로컬 빌드 테스트

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

- [ ] ✅ 5분 이내 배너 광고 사라짐
- [ ] ✅ Logcat 확인: `배너 광고 정책 변경`

```sql
-- 배너 ON
UPDATE ad_policy 
SET ad_banner_enabled = true 
WHERE app_id = 'com.sweetapps.pocketchord';
```

- [ ] ✅ 5분 이내 배너 광고 다시 나타남

#### 테스트 2: 전체 광고 제어
```sql
-- 모든 광고 OFF
UPDATE ad_policy 
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
versionCode = ?  // 이전보다 +1
versionName = "?" // 적절한 버전
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

## 6. 완료 체크리스트

### 1 시나리오 통과 여부

| 시나리오 | 결과 | 비고 |
|----------|------|------|
| S1: 전체 광고 ON/OFF | ⬜ PASS / ⬜ FAIL | |
| S2: App Open 제어 | ⬜ PASS / ⬜ FAIL | |
| S3: Interstitial 제어 | ⬜ PASS / ⬜ FAIL | |
| S4: Banner 제어 | ⬜ PASS / ⬜ FAIL | |
| S5: 빈도 제한 (선택) | ⬜ PASS / ⬜ FAIL / ⬜ SKIP | |
| S6: 최종 확인 | ⬜ PASS / ⬜ FAIL | |

### 2 최종 상태 확인

- [ ] ✅ 모든 광고 설정이 운영 기본값으로 복구됨
- [ ] ✅ 실제 광고 동작 정상 확인
- [ ] ✅ Phase 5 테스트 완료

### 3 발견된 이슈

```
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

