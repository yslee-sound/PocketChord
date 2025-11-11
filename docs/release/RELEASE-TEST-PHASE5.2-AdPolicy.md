# 릴리즈 테스트 - Phase 5.2 (개별 광고 제어 테스트)

**버전**: v3.1  
**최종 업데이트**: 2025-11-11  
**소요 시간**: 약 20-25분

---

## 📋 목차

1. [개요](#1-개요)
2. [App Open 광고 제어](#2-app-open-광고-제어)
3. [Interstitial 광고 제어](#3-interstitial-광고-제어)
4. [Banner 광고 제어](#4-banner-광고-제어)
5. [다음 단계](#5-다음-단계)

---

## 1 개요

### 1.1 Phase 5.2의 목적

Phase 5.2에서는 개별 광고 타입을 독립적으로 제어하는 테스트를 수행합니다:
- ✅ **App Open 광고**: 앱 시작/복귀 시 광고 제어
- ✅ **Interstitial 광고**: 전면광고 상세 조건 이해 및 제어
- ✅ **Banner 광고**: 배너 광고 제어

### 1.2 선행 조건

- ✅ **Phase 5.1 완료**: RLS 정책 수정 및 is_active 테스트 통과
- ✅ **is_active = true**: 모든 광고가 기본적으로 활성화된 상태
- ✅ **앱 정상 작동**: Debug 빌드에서 모든 광고 표시 확인

---

## 2 App Open 광고 제어

### 2.1 목적
App Open 광고만 개별 제어

### 2.2 Step 1: App Open만 비활성화

```sql
UPDATE ad_policy
SET ad_app_open_enabled = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

### 2.3 Step 2: 앱 실행 및 검증

- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] **검증**: App Open 광고 표시 안 됨 ✅
- [ ] 백그라운드 → 포그라운드 전환
- [ ] **검증**: App Open 광고 표시 안 됨 ✅
- [ ] **검증**: Interstitial 광고 정상 표시 (조건 만족 시)
- [ ] **검증**: Banner 광고 정상 표시 ✅

### 2.4 Logcat 확인

**필터 설정**: `tag:AdPolicyRepo | tag:AppOpenAdManager`

**예상 로그**:
```
AdPolicyRepo: ===== Ad Policy Fetch Started =====
AdPolicyRepo: 🔄 Supabase에서 광고 정책 새로 가져오기
AdPolicyRepo: Target app_id: com.sweetapps.pocketchord.debug
AdPolicyRepo: Total rows fetched: 2
AdPolicyRepo: ✅ 광고 정책 발견!
AdPolicyRepo:   - is_active: true
AdPolicyRepo:   - App Open Ad: false
AdPolicyRepo:   - Interstitial Ad: true
AdPolicyRepo:   - Banner Ad: true
AdPolicyRepo:   - Max Per Hour: 2
AdPolicyRepo:   - Max Per Day: 15
AdPolicyRepo: ===== Ad Policy Fetch Completed =====
AppOpenAdManager: 앱 오프닝 광고 로드 성공
```

**백그라운드 복귀 시 로그**:
```
AppOpenAdManager: 앱이 포그라운드로 왔습니다 (백그라운드에서 복귀)
AdPolicyRepo: ✅ 광고 정책 발견!
AdPolicyRepo:   - App Open Ad: false
AppOpenAdManager: [정책] 앱 오픈 광고 비활성화
AppOpenAdManager: 🔍 앱 오픈 광고 정책 확인:
AppOpenAdManager:   - Supabase 정책: 비활성화
AppOpenAdManager: ❌ Supabase 정책: 앱 오픈 광고 비활성화
```

**로그 순서 설명**:
1. **`[정책] 앱 오픈 광고 비활성화`**: 정책 조회 함수에서 출력 (조회 단계)
2. **`🔍 앱 오픈 광고 정책 확인:`**: 광고 표시 함수에서 출력 (적용 단계)
3. **`❌ Supabase 정책: 앱 오픈 광고 비활성화`**: 최종 결정 (표시 안 함)

**💡 왜 2번 체크?**
- 1단계: 정책 **조회** (isAppOpenEnabledFromPolicy)
- 2단계: 정책 **적용** (showAdIfAvailable)
- 중복이 아니라 **조회 → 적용**의 두 단계 프로세스

**참고**:
- `앱 오프닝 광고 로드 성공`은 정상입니다 (준비 단계)
- 실제 차단은 정책 적용 단계에서 발생

### 2.5 Step 3: 복구

```sql
UPDATE ad_policy
SET ad_app_open_enabled = true
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

- [ ] ✅ 재활성화 완료
- [ ] ✅ 앱 재시작 후 App Open 광고 정상 표시

---

## 3 Interstitial 광고 제어

### 3.1 목적
Interstitial 광고만 개별 제어 + 표시 조건 이해

---

### 3.2 📋 전면광고(Interstitial) 표시 조건 상세

**필수 조건 (모두 AND 조건으로 만족해야 표시됨)**:
```
1. 광고 로드 완료
2. 이전 광고 표시 후 60초 이상 경과 (광고 간격 - 하드코딩)
3. 화면 전환 3회 이상 누적
4. 특정 화면 전환 패턴 중 하나:
   - 코드 상세 화면 → 홈으로 돌아가기
   - 메트로놈 → 홈으로 돌아가기
   - 튜너 → 홈으로 돌아가기
   - 더보기 → 설정으로 이동
5. 시간당 제한 (기본 2회) 미초과 (Supabase 제어)
6. 일일 제한 (기본 15회) 미초과 (Supabase 제어)
7. Supabase 정책 활성화

→ 하나라도 실패하면 광고 안 나옴!
```

**⚠️ 조건들의 역할**:
- **60초 간격** (조건 2): 연속 광고 방지 (짧은 시간 보호)
- **시간당 제한** (조건 5): 시간 단위 총량 제한 (중간 시간 보호)
- **일일 제한** (조건 6): 하루 단위 총량 제한 (긴 시간 보호)

**예시 시나리오**:
```
1) 60초 간격 통과 + 시간당 제한 실패
   10:00 광고 1번, 10:02 광고 2번 (시간당 2/2 도달)
   10:05 시도 → 차단! (60초는 경과했지만 시간당 제한)
   
2) 모든 조건 통과
   10:00 광고 1번
   10:02 시도 (화면 전환 3회 완료)
   → 광고 표시! ✅ (60초 경과 + 시간당 1/2 + 일일 1/15)
```

**⚠️ 주의사항**: 
- 단순히 여러 화면을 이동하는 것만으로는 표시되지 않습니다.
- 특정 패턴의 화면 전환이 3회 누적되어야 합니다.
- 이전 광고를 본 후 60초가 지나야 다음 광고가 가능합니다.

**💡 60초 간격이 적절한 이유**:

✅ **현재 설정 (60초 + 시간당 2회 + 일일 15회)는 최적의 균형입니다!**

| 측면 | 평가 | 설명 |
|------|------|------|
| **사용자 경험** | ✅ 좋음 | 1분은 충분히 긴 간격 |
| **광고 빈도** | ✅ 적절 | 시간당 제한이 실질적 제약 |
| **수익성** | ✅ 균형적 | 과도하지 않고 적절함 |
| **업계 표준** | ✅ 준수 | 60~120초가 일반적 |

**3중 보호 시스템**:
```
레벨 1: 60초 간격 (즉각 보호)
  → "방금 광고 봤는데 또?" 방지

레벨 2: 시간당 2회 (중기 보호)
  → "1시간에 광고 너무 많아" 방지

레벨 3: 일일 15회 (장기 보호)
  → "하루 종일 광고만 봤어" 방지
```

**실제 효과**:
- 60초 간격만 있으면 이론상 시간당 60회 가능
- 하지만 **시간당 2회 제한**이 실질적으로 광고 빈도 결정
- 따라서 60초는 "연속 광고 방지"용으로 충분하며 더 늘릴 필요 없음

**조정 고려 사항**:
- ✅ **현재 유지 권장**: 60초는 적절함
- 📈 **모니터링**: Play Store 리뷰, 사용자 유지율 관찰
- ⚠️ **조정 시나리오**:
  - 부정적 리뷰 증가 시 → 90초로 증가 고려
  - 수익 극대화 필요 시 → 45초로 감소 (주의 필요)

---

### 3.3 Step 1: Interstitial만 비활성화

```sql
UPDATE ad_policy
SET ad_interstitial_enabled = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

### 3.4 Step 2: 앱 실행 및 검증

- [ ] 앱 재실행
- [ ] **검증**: App Open 광고 정상 표시 (백그라운드 복귀 시) ✅
- [ ] **전면 광고 테스트**:
  - 코드 상세 → 홈 (3회 반복하여 카운트 쌓기)
  - 또는 메트로놈 → 홈 (3회 반복)
  - **광고 간격 대기** (이전 광고 본 후 60초)
  - 다시 특정 패턴 실행
- [ ] **검증**: Interstitial 광고 표시 안 됨 ✅ (정책에 의해 차단)
- [ ] **검증**: Banner 광고 정상 표시 ✅

**참고**: 전면광고는 특정 화면 전환 패턴(코드→홈 등)에서만 표시되며, 단순히 여러 화면 이동만으로는 표시되지 않습니다.

### 3.5 Logcat 확인

**필터 설정**: `tag:AdPolicyRepo | tag:InterstitialAdManager`

**앱 시작 시 로그**:
```
AdPolicyRepo: ===== Ad Policy Fetch Started =====
AdPolicyRepo: 🔄 Supabase에서 광고 정책 새로 가져오기
AdPolicyRepo: Target app_id: com.sweetapps.pocketchord.debug
AdPolicyRepo: Total rows fetched: 2
AdPolicyRepo: ✅ 광고 정책 발견!
AdPolicyRepo:   - is_active: true
AdPolicyRepo:   - App Open Ad: true
AdPolicyRepo:   - Interstitial Ad: false
AdPolicyRepo:   - Banner Ad: true
AdPolicyRepo:   - Max Per Hour: 2
AdPolicyRepo:   - Max Per Day: 15
AdPolicyRepo: ===== Ad Policy Fetch Completed =====
InterstitialAdManager: 전면광고 로드 성공
```

**전면광고 표시 시도 시 로그** (3회 패턴 + 60초 경과 후):
```
InterstitialAdManager: [정책] 전면 광고 비활성화
InterstitialAdManager: 전면광고 표시 조건 미달
```

**캐시 사용 로그** (3분 이내 재조회 시):
```
AdPolicyRepo: 🗃️ 캐시된 광고 정책 사용 (유효 시간: xxx초 남음)
```

**⚠️ 중요**:
- **`전면광고 로드 성공`은 정상입니다**: 광고가 준비된 것일 뿐, 표시되는 것은 아닙니다
- **차단 로그는 실제 표시 시도 시에만 출력**됩니다

**검증 방법**:
1. 앱 시작 → 위 정책 조회 로그 확인
2. 코드→홈을 3회 반복
3. 60초 대기
4. 다시 코드→홈 실행
5. **이때 차단 로그 확인** ← 여기서 비로소 `[정책] 전면 광고 비활성화` 출력

### 3.6 Step 3: 복구

```sql
UPDATE ad_policy
SET ad_interstitial_enabled = true
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

- [ ] ✅ 재활성화 완료
- [ ] ✅ 전면광고 조건 이해 완료

---

## 4 Banner 광고 제어

### 4.1 목적
Banner 광고만 개별 제어

### 4.2 Step 1: Banner만 비활성화

```sql
UPDATE ad_policy
SET ad_banner_enabled = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

### 4.3 Step 2: 앱 실행 및 검증

- [ ] 앱 재실행
- [ ] **검증**: App Open 광고 정상 표시 ✅
- [ ] **검증**: 배너 광고 표시 안 됨 ✅
- [ ] **전면 광고 테스트** (조건 만족):
  - 코드 상세 → 홈 (3회 반복)
  - 이전 광고 본 후 60초 경과
  - 다시 코드 상세 → 홈
- [ ] **검증**: Interstitial 광고 정상 표시 ✅

### 4.4 Logcat 확인

```
예상 로그:
AdPolicyRepo: ✅ 광고 정책 발견!
AdPolicyRepo:   - is_active: true
AdPolicyRepo:   - App Open Ad: true
AdPolicyRepo:   - Interstitial Ad: true
AdPolicyRepo:   - Banner Ad: false
MainActivity: [정책] 배너 광고 비활성화
MainActivity: 🔄 배너 광고 정책 변경: 활성화 → 비활성화
```

### 4.5 Step 3: 복구

```sql
UPDATE ad_policy
SET ad_banner_enabled = true
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

- [ ] ✅ 재활성화 완료
- [ ] ✅ 배너 광고 정상 표시

---

## 5 다음 단계

### 5.1 Phase 5.2 완료 확인

**완료된 테스트**:
- [ ] ✅ App Open 광고 개별 제어
- [ ] ✅ Interstitial 광고 개별 제어
- [ ] ✅ Banner 광고 개별 제어
- [ ] ✅ 전면광고 표시 조건 이해

**주요 검증 사항**:
- [ ] ✅ 개별 광고 플래그 정상 작동
- [ ] ✅ 다른 광고는 영향받지 않음
- [ ] ✅ 전면광고 7가지 조건 이해
- [ ] ✅ Logcat 로그 분석 완료

---

### 5.2 다음 단계: Phase 5.3

**Phase 5.3에서 수행할 내용**:
- 📋 **빈도 제한 테스트**: 시간당/일일 제한 확인 (선택사항)
- 📋 **최종 검증**: 모든 광고 설정 최종 확인
- 📋 **배포 준비**: Release 빌드 및 Play Store 준비

**Phase 5.3 문서**: [RELEASE-TEST-PHASE5.3-AdPolicy.md](RELEASE-TEST-PHASE5.3-AdPolicy.md)

---

## 6 긴급 광고 제어

### 6.1 운영 중 긴급 상황 대응

#### 방법 1: is_active 사용 (✅ 권장)

```sql
-- 모든 광고 차단 (최대 3분 소요, 앱 재시작 시 즉시)
UPDATE ad_policy
SET is_active = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

#### 방법 2: 개별 플래그 사용

```sql
-- 특정 광고만 끄기
UPDATE ad_policy
SET ad_banner_enabled = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

**반영 시간**:
- ✅ **즉시 반영**: 사용자가 앱 재시작 시
- ⏱️ **최대 3분**: 앱 실행 중 (캐시 만료 대기)

---

## 7 참고 문서

### 관련 문서
- `docs/release/RELEASE-TEST-PHASE5.1-AdPolicy.md` - Phase 5.1: RLS 정책 수정
- `docs/release/RELEASE-TEST-PHASE5.3-AdPolicy.md` - Phase 5.3: 최종 검증 + 배포

### 변경 이력
- **v3.1 (2025-11-11)**: 개별 광고 제어 테스트, 전면광고 조건 상세화

---

**문서 작성**: GitHub Copilot  
**최종 업데이트**: 2025-11-11  
**버전**: v3.1  
**Phase 5.2 완료**: ☐ PASS / ☐ FAIL
