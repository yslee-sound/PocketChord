# app_policy 테이블 정리 가이드

**작성일**: 2025-11-09  
**현재 상태**: ⚠️ **아직 삭제하면 안 됩니다!**  
**이유**: HomeScreen.kt에서 fallback으로 사용 중

---

## 🔴 현재 상황

### app_policy가 사용되는 곳 (3군데)

#### 1. Fallback 조회 로직
```kotlin
// HomeScreen.kt 라인 ~263
// ===== Fallback: app_policy 조회 (기존 로직 유지) =====
AppPolicyRepository(supabaseClient)
    .getPolicy()
    .onSuccess { ... }
```

**목적**: 새 테이블(update/emergency/notice)이 없을 때 기존 로직 사용

#### 2. Emergency Fallback
```kotlin
// HomeScreen.kt 라인 ~445
// 1순위 Fallback: Emergency - 기존 app_policy 사용 (호환성)
else if (showEmergencyDialog && appPolicy != null) {
    // app_policy의 emergency 타입 표시
}
```

**목적**: emergency_policy가 없을 때 app_policy 사용

#### 3. Notice Fallback
```kotlin
// HomeScreen.kt 라인 ~518
// ===== Fallback: 기존 app_policy ID 기반 추적 =====
announcement?.id?.let { id ->
    // app_policy의 notice ID 저장
}
```

**목적**: notice_policy가 없을 때 app_policy 사용

---

## ⚠️ 삭제하면 안 되는 이유

### 1. 점진적 마이그레이션 전략
현재는 **병행 운영** 중입니다:
- ✅ **새 테이블 우선** 시도
- ✅ **실패 시 app_policy fallback**
- ✅ 안전한 전환 보장

### 2. 광고 정책도 포함
`app_policy`에는 **광고 관련 필드**도 있습니다:
- `ad_app_open_enabled`
- `ad_interstitial_enabled`
- `ad_banner_enabled`
- `ad_interstitial_max_per_hour`
- `ad_interstitial_max_per_day`

이것들은 아직 사용 중입니다!

### 3. 기존 시스템 호환성
다른 코드에서 `app_policy`를 참조할 수 있습니다.

---

## ✅ 안전한 삭제 순서

### Phase 4-1: 테스트 및 검증 (1-2일)
- [ ] 3개 신규 테이블 모두 정상 작동 확인
- [ ] 프로덕션 환경에서 충분히 테스트
- [ ] fallback 로직이 실행되지 않는지 로그 확인

### Phase 4-2: Fallback 제거 (1일)
- [ ] HomeScreen.kt에서 app_policy fallback 코드 제거
- [ ] AppPolicyRepository import 제거
- [ ] 테스트 실행

### Phase 4-3: 광고 정책 분리 (선택사항, 1일)
옵션 A: **ad_policy 테이블 생성**
```sql
CREATE TABLE public.ad_policy (
    id BIGINT PRIMARY KEY,
    app_id TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    ad_app_open_enabled BOOLEAN DEFAULT TRUE,
    ad_interstitial_enabled BOOLEAN DEFAULT TRUE,
    ad_banner_enabled BOOLEAN DEFAULT TRUE,
    ad_interstitial_max_per_hour INT DEFAULT 2,
    ad_interstitial_max_per_day INT DEFAULT 15
);
```

옵션 B: **app_policy 유지하고 불필요한 컬럼만 제거**
```sql
ALTER TABLE app_policy
DROP COLUMN active_popup_type,
DROP COLUMN content,
DROP COLUMN download_url,
DROP COLUMN min_supported_version,
DROP COLUMN latest_version_code;
-- 광고 관련 컬럼만 남김
```

### Phase 4-4: 최종 삭제 (모든 검증 완료 후)
```sql
-- app_policy_history 삭제 (먼저)
DROP TABLE IF EXISTS public.app_policy_history CASCADE;

-- app_policy 삭제 (나중에, 광고 정책 이전 후)
DROP TABLE IF EXISTS public.app_policy CASCADE;
```

---

## 📊 현재 vs 목표 상태

### 현재 (병행 운영)
```
┌─────────────────┐
│   app_policy    │ ← 아직 사용 중! (Fallback + 광고)
├─────────────────┤
│ • emergency     │ ← Fallback
│ • force_update  │ ← Fallback
│ • optional_...  │ ← Fallback
│ • notice        │ ← Fallback
│ • ad_control    │ ← 활성 사용
└─────────────────┘

┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│ update_policy    │   │ notice_policy    │   │ emergency_policy │
├──────────────────┤   ├──────────────────┤   ├──────────────────┤
│ ✅ 우선 사용      │   │ ✅ 우선 사용      │   │ ✅ 우선 사용      │
└──────────────────┘   └──────────────────┘   └──────────────────┘
```

### 목표 (Phase 4 완료 후)
```
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│ update_policy    │   │ notice_policy    │   │ emergency_policy │
├──────────────────┤   ├──────────────────┤   ├──────────────────┤
│ ✅ 유일한 소스    │   │ ✅ 유일한 소스    │   │ ✅ 유일한 소스    │
└──────────────────┘   └──────────────────┘   └──────────────────┘

┌──────────────────┐
│   ad_policy      │ (선택)
├──────────────────┤
│ • 광고 정책 전용  │
└──────────────────┘

app_policy, app_policy_history → 삭제됨
```

---

## 🚀 권장 순서

### 1단계: 신규 테이블 충분히 테스트 (현재)
```
✅ update_policy 생성 완료
✅ emergency_policy 생성 완료
✅ notice_policy 생성 완료
→ 최소 1-2주 프로덕션 테스트 권장
```

### 2단계: 로그 확인
```kotlin
// HomeScreen.kt에서 로그 확인
// Fallback 로직이 실행되는지 체크

// 만약 이 로그가 보이면:
"===== Querying app_policy (fallback) ====="
→ 아직 삭제하면 안 됨!

// 만약 이 로그가 안 보이면:
→ 신규 테이블이 정상 작동 중
→ Fallback 제거 고려 가능
```

### 3단계: Fallback 제거 (충분한 테스트 후)
```kotlin
// HomeScreen.kt에서 제거할 부분:
// 1. AppPolicyRepository import
// 2. app_policy 조회 코드
// 3. emergency fallback 코드
// 4. notice fallback 코드
```

### 4단계: 광고 정책 처리
- 옵션 A: ad_policy 테이블 생성
- 옵션 B: app_policy 유지하고 컬럼만 정리

### 5단계: 최종 삭제
```sql
DROP TABLE IF EXISTS public.app_policy_history CASCADE;
DROP TABLE IF EXISTS public.app_policy CASCADE;
```

---

## ⚠️ 주의사항

### 즉시 삭제하면 발생하는 문제
```
❌ app_policy를 지우면:
  1. Fallback 로직 실패
  2. 광고 정책 조회 실패
  3. 기존 사용자에게 영향
  4. 롤백 어려움
```

### 안전한 삭제 조건
```
✅ 다음 조건을 모두 만족해야 삭제 가능:
  1. 신규 테이블이 1-2주 이상 정상 작동
  2. Fallback 로그가 관찰되지 않음
  3. 광고 정책이 별도 테이블로 이전 완료
  4. 모든 참조가 제거됨
  5. 백업 완료
```

---

## 📝 체크리스트

### 삭제 전 확인사항
- [ ] update_policy 정상 작동 확인 (1주 이상)
- [ ] emergency_policy 정상 작동 확인 (1주 이상)
- [ ] notice_policy 정상 작동 확인 (1주 이상)
- [ ] Fallback 로그 없음 확인
- [ ] 광고 정책 이전 완료 (ad_policy 또는 별도 처리)
- [ ] 백업 완료
- [ ] 롤백 계획 수립

### 삭제 후 확인사항
- [ ] 앱 빌드 성공
- [ ] 앱 실행 성공
- [ ] 모든 팝업 정상 작동
- [ ] 광고 정상 표시
- [ ] 에러 로그 없음

---

## 🎯 결론

### ❌ 지금 당장 삭제하면 안 됩니다!

**이유**:
1. HomeScreen.kt에서 fallback으로 사용 중
2. 광고 정책도 포함되어 있음
3. 충분한 테스트가 필요함

### ✅ 안전한 타이밍

**최소 조건**:
- 신규 테이블 1-2주 정상 작동 확인 후
- Fallback 코드 제거 후
- 광고 정책 이전 후

**권장**:
- Phase 4 (app_policy 정리) 완료 후
- 상세 가이드: `IMPLEMENTATION-PLAN.md` 참조

---

## 📚 관련 작업

### 당장 할 수 있는 것
- ✅ app_policy_history는 삭제해도 됨 (사용 안 함)
- ✅ 신규 테이블 테스트 계속 진행

### 나중에 할 것
- ⏳ Phase 4-1: 충분한 테스트 (1-2주)
- ⏳ Phase 4-2: Fallback 제거
- ⏳ Phase 4-3: 광고 정책 분리
- ⏳ Phase 4-4: app_policy 삭제

---

**작성일**: 2025-11-09  
**상태**: app_policy는 아직 유지 필요 ⚠️  
**다음 단계**: 신규 테이블 충분히 테스트 → Fallback 제거 → 최종 삭제

