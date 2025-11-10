# 릴리즈 테스트 - Phase 6 (빈도 제한 + 최종 검증 + 배포)

**버전**: v3.1  
**최종 업데이트**: 2025-11-11  
**소요 시간**: 약 30-40분

---

## 📋 목차

1. [개요](#1-개요)
2. [빈도 제한 테스트](#2-빈도-제한-테스트)
3. [최종 검증](#3-최종-검증)
4. [배포 체크리스트](#4-배포-체크리스트)
5. [완료 체크리스트](#5-완료-체크리스트)

---

## 1 개요

### 1.1 Phase 6의 목적

Phase 6에서는 다음을 수행합니다:
- ✅ 전면광고 빈도 제한 테스트 (선택사항)
- ✅ 모든 광고 설정 최종 검증
- ✅ Release 빌드 및 배포 준비
- ✅ 최종 승인 체크리스트

### 1.2 선행 조건

- ✅ **Phase 5 완료**: is_active, App Open, Interstitial, Banner 광고 테스트 통과
- ✅ **RLS 정책 수정 완료**: `USING (true)` 정책 적용
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

### 5.1 Phase 5 + 6 통합 결과

#### 시나리오 테스트 결과

| Phase | 시나리오 | 결과 | 비고 |
|-------|----------|------|------|
| Phase 5 | S1: is_active 전체 제어 | ⬜ PASS / ⬜ FAIL | RLS 수정 필수 |
| Phase 5 | S2: App Open 제어 | ⬜ PASS / ⬜ FAIL | |
| Phase 5 | S3: Interstitial 제어 | ⬜ PASS / ⬜ FAIL | |
| Phase 5 | S4: Banner 제어 | ⬜ PASS / ⬜ FAIL | |
| Phase 6 | S5: 빈도 제한 | ⬜ PASS / ⬜ FAIL / ⬜ SKIP | 선택사항 |
| Phase 6 | S6: 최종 검증 | ⬜ PASS / ⬜ FAIL | |

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
- [ ] ✅ Phase 5 문서 완료
- [ ] ✅ Phase 6 문서 완료
- [ ] ✅ 변경 이력 작성
- [ ] ✅ 배포 가이드 작성

---

### 5.3 최종 승인

**배포 승인 체크리스트**:
- [ ] ✅ 모든 테스트 통과
- [ ] ✅ is_active 정상 작동 확인
- [ ] ✅ RLS 정책 수정 완료
- [ ] ✅ 모든 광고 타입 정상 작동
- [ ] ✅ 빈도 제한 정상 작동 (테스트 완료)
- [ ] ✅ Release 빌드 성공
- [ ] ✅ 문서 업데이트 완료
- [ ] ✅ Play Store 업로드 준비 완료

**승인자**: _______________  
**배포 일시**: 2025-__-__  
**배포 버전**: v___  

---

### 5.4 발견된 이슈

**Phase 5 + 6 테스트 중 발견된 이슈**:

1. _____________________________________________
2. _____________________________________________
3. _____________________________________________

**해결 여부**:
- [ ] ⬜ 모든 이슈 해결됨
- [ ] ⬜ 일부 이슈 남음 (배포 전 해결 필요)
- [ ] ⬜ 이슈 없음

---

## 6 참고 문서

### 관련 문서
- `docs/release/RELEASE-TEST-PHASE5-RELEASE.md` - Phase 5: 광고 정책 테스트
- `docs/sql/fix-rls-policy.sql` - RLS 정책 수정 SQL
- `docs/archive/IS-ACTIVE-FIX-COMPLETE.md` - is_active 근본 해결 가이드

### 변경 이력
- **v3.1 (2025-11-11)**: 
  - Phase 5: is_active 근본 해결, RLS 정책 수정, 3분 캐싱
  - Phase 6: 빈도 제한 테스트, 최종 검증, 배포 준비

---

**문서 작성**: GitHub Copilot  
**최종 업데이트**: 2025-11-11  
**버전**: v3.1  
**테스트 담당자**: _______________  
**결과**: ⬜ PASS / ⬜ FAIL

