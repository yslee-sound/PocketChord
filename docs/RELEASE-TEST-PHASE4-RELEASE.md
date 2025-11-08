# 릴리즈 테스트 SQL 스크립트 - Phase 4 (릴리즈용)

**버전**: v1.0.0  
**최종 업데이트**: 2025-11-09 06:35 KST  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: 우선순위 테스트 + 최종 확인 + 정리

---

## 📝 변경 이력

### v1.0.0 (2025-11-09 06:35)
- ✅ 최초 작성
- ✅ Phase 4 테스트 시나리오 작성

---

## ⚠️ 디버그 버전 사용 시 주의사항

디버그 버전(🔧)을 테스트하기 전에 먼저 디버그 데이터를 생성해야 합니다!

**1회만 실행**: `docs/sql/07-create-debug-test-data.sql`

이미 생성했다면 건너뛰세요!

---

## 📋 Phase 4 개요

이 문서는 릴리즈 테스트의 마지막 단계입니다.

**포함된 테스트**:
1. ✅ 우선순위 테스트 (emergency > update)
2. ✅ 우선순위 테스트 (update > notice)
3. ✅ 최종 상태 확인
4. ✅ 전체 초기화 (평상시 복구)

**소요 시간**: 약 10분

---

## 🎯 Step 1: 우선순위 테스트 (emergency > update)

### 1-1. 두 정책 모두 활성화

```sql
-- 4-1. Emergency + Update 동시 활성화
UPDATE emergency_policy 
SET is_active = true,
    content = '🚨 [우선순위 테스트] 긴급'
WHERE app_id = 'com.sweetapps.pocketchord';

UPDATE update_policy 
SET target_version_code = 999,
    is_force_update = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 1-2. 앱 실행 및 검증

- [ ] 앱 데이터 삭제
- [ ] 앱 실행

**검증**:
- [ ] **예상**: **emergency 팝업만** 표시 ⭐
- [ ] ✅ emergency 팝업 표시됨
- [ ] ✅ update 팝업 표시 안 됨

### Logcat 확인

**Filter 설정**: `tag:HomeScreen`

```
예상 로그:
✅ "Decision: EMERGENCY from emergency_policy"
✅ "return@LaunchedEffect" (다른 팝업 건너뜀)
```

- [ ] ✅ 로그 확인 완료

**의미**: Emergency가 최우선순위! 다른 팝업은 무시됩니다. ✅

---

## 🎯 Step 2: 우선순위 테스트 (update > notice)

### 2-1. Emergency 비활성화

```sql
-- 4-2. Emergency 비활성화
UPDATE emergency_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 2-2. Update와 Notice 활성화 확인

```sql
-- Update 활성화 확인
SELECT target_version_code, is_force_update 
FROM update_policy 
WHERE app_id = 'com.sweetapps.pocketchord';

-- Notice 활성화 확인 (이미 활성화되어 있음)
SELECT is_active, notice_version 
FROM notice_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 2-3. 앱 실행 및 검증

- [ ] 앱 데이터 삭제
- [ ] 앱 실행

**검증**:
- [ ] **예상**: **update 팝업만** 표시 ⭐
- [ ] ✅ update 팝업 표시됨
- [ ] ✅ notice 팝업 표시 안 됨

### 2-4. "나중에" 클릭 후 재실행

- [ ] "나중에" 클릭
- [ ] 앱 완전 종료
- [ ] 앱 재실행

**검증**:
- [ ] **예상**: **notice 팝업** 표시 ⭐
- [ ] ✅ notice 팝업 표시됨 (update 추적되었으므로)

### Logcat 확인

**Filter 설정**: `tag:HomeScreen`

```
예상 로그:
✅ "update_policy exists but no update needed" (dismissed)
✅ "Decision: NOTICE from notice_policy"
```

- [ ] ✅ 로그 확인 완료

**의미**: Update > Notice 우선순위 확인! ✅

---

## 🎯 Step 3: 정리

### 3-1. Update 정리

```sql
-- 4-3. Update 정리
UPDATE update_policy 
SET target_version_code = 1 
WHERE app_id = 'com.sweetapps.pocketchord';
```

- [ ] ✅ 정리 완료

---

## 🔍 Step 4: 최종 상태 확인

### 4-1. 전체 상태 확인

```sql
-- ===== 최종 상태 확인 =====
SELECT 
    'emergency_policy' as policy,
    CAST(is_active AS TEXT) as is_active,
    CAST(is_dismissible AS TEXT) as detail,
    LEFT(content, 30) as preview
FROM emergency_policy 
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 
    'update_policy',
    CAST(is_active AS TEXT),
    CAST(is_force_update AS TEXT),
    CONCAT('target:', target_version_code)
FROM update_policy 
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 
    'notice_policy',
    CAST(is_active AS TEXT),
    CAST(NULL AS TEXT),
    CONCAT('v', notice_version, ': ', LEFT(title, 20))
FROM notice_policy 
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 
    'ad_policy',
    CAST(is_active AS TEXT),
    CAST(NULL AS TEXT),
    CONCAT('open:', ad_app_open_enabled, ' inter:', ad_interstitial_enabled, ' banner:', ad_banner_enabled)
FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 4-2. 프로덕션(평상시) 상태 기준

```
emergency: is_active = false (긴급 상황 없음)
update: target = 1 (현재 버전과 같거나 낮음)
notice: is_active = true, version = 1 (기본 환영 메시지)
ad_policy: is_active = true, 광고 타입별 설정 (프로덕션 설정)
```

### 4-3. 확인 항목

- [ ] emergency가 비활성화 되어 있는가?
- [ ] update target이 1 (또는 현재 버전 이하)인가?
- [ ] notice가 버전 1로 복구되었는가?
- [ ] ad_policy 광고 설정이 프로덕션 상태인가?
- [ ] 테스트용 메시지가 남아있지 않은가?

---

## 🧹 Step 5: 전체 초기화 (평상시 상태)

### 5-1. 모든 정책 복구

```sql
-- ===== 전체 초기화 (평상시 상태) =====
UPDATE emergency_policy 
SET is_active = false 
WHERE app_id = 'com.sweetapps.pocketchord';

UPDATE update_policy 
SET target_version_code = 1,
    is_force_update = false
WHERE app_id = 'com.sweetapps.pocketchord';

UPDATE notice_policy 
SET is_active = true,
    title = '환영합니다! 🎉',
    content = 'PocketChord를 이용해 주셔서 감사합니다!\n더 나은 서비스를 제공하기 위해 노력하겠습니다.',
    notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord';
```

- [ ] ✅ 초기화 완료

### 5-2. 최종 확인

- [ ] 위의 "최종 상태 확인" SQL 다시 실행
- [ ] 모든 값이 평상시 상태인지 확인
- [ ] ✅ 평상시 상태로 복구 확인

---

## 📊 우선순위 정리

### 최종 우선순위

```
1순위: emergency_policy (최우선!)
   ↓ 없으면
2순위: update_policy
   ↓ 없으면
3순위: notice_policy
```

**테스트 결과**:
- [ ] ✅ emergency > update 확인
- [ ] ✅ update > notice 확인
- [ ] ✅ 우선순위 로직 정상 작동

---

## ✅ Phase 4 완료 체크리스트

- [ ] emergency > update 우선순위 확인 완료
- [ ] update > notice 우선순위 확인 완료
- [ ] 최종 상태 확인 완료
- [ ] 전체 초기화 완료
- [ ] 평상시 상태 복구 확인 완료
- [ ] 모든 로그 확인 완료

---

## 🎊 전체 테스트 완료!

### 완료된 Phase

- ✅ Phase 1: Emergency 테스트
- ✅ Phase 2: Update 테스트
- ✅ Phase 3: Notice 테스트
- ✅ Phase 4: 우선순위 + 최종 확인

### 최종 승인

- [ ] ✅ 모든 Phase 테스트 PASS
- [ ] ✅ 이슈 0개 또는 모두 해결됨
- [ ] ✅ 프로덕션 상태 확인 완료
- [ ] ✅ 데이터베이스 상태 정상

### 릴리즈 승인

- [ ] ✅ **릴리즈 준비 완료** 🚀

---

**문서 버전**: v1.0.0  
**마지막 수정**: 2025-11-09 06:35 KST

**테스트 완료 일시**: _______________  
**테스터**: _______________  
**승인자**: _______________

---

**전체 테스트 완료!** 🎉🎉🎉

