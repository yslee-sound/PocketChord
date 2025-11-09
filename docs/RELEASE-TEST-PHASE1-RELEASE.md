# 릴리즈 테스트 SQL 스크립트 - Phase 1 (릴리즈용)

**버전**: v1.2.0  
**최종 업데이트**: 2025-11-09 06:50 KST  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: 초기 상태 확인 + Emergency 테스트

---

## 📝 변경 이력

### v1.2.0 (2025-11-09 06:50)
- ✅ 로그 Phase 번호 수정 (Phase 2 → Phase 1)
- ✅ emergency_policy가 최우선순위임을 명확히 함

### v1.1.0 (2025-11-09 06:35)
- ✅ emergency_policy에 button_text 필드 추가 반영
- ✅ 검증 체크리스트 업데이트

### v1.0.0 (2025-11-08)
- ✅ 최초 작성
- ✅ Phase 1 테스트 시나리오 작성

---

## ⚠️ 디버그 버전 사용 시 주의사항

디버그 버전(🔧)을 테스트하기 전에 먼저 디버그 데이터를 생성해야 합니다!

**1회만 실행**: `docs/sql/07-create-debug-test-data.sql`

이미 생성했다면 건너뛰세요!

---

## 📋 Phase 1 개요

이 문서는 릴리즈 테스트의 첫 번째 단계입니다.

**포함된 테스트**:
1. ✅ 초기 상태 확인 (스냅샷)
2. ✅ Emergency 팝업 테스트 (X 버튼 있음/없음)

**소요 시간**: 약 10분

---

## 🎯 Step 1: 초기 상태 확인

**목적**: 테스트 시작 전 현재 상태를 기록합니다.

### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- ===== 초기 상태 확인 (릴리즈) =====
SELECT 'emergency_policy' as table_name, 
       CAST(is_active AS TEXT) as is_active, 
       LEFT(content, 30) as content_preview 
FROM emergency_policy 
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 'update_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('target:', target_version_code, ' force:', is_force_update)
FROM update_policy 
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 'notice_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('v', notice_version, ': ', LEFT(title, 20))
FROM notice_policy 
WHERE app_id = 'com.sweetapps.pocketchord'
UNION ALL
SELECT 'ad_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('open:', ad_app_open_enabled, ' inter:', ad_interstitial_enabled, ' banner:', ad_banner_enabled)
FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

### SQL 스크립트 - 디버그 버전 🔧

```sql
-- ===== 초기 상태 확인 (디버그) =====
SELECT 'emergency_policy' as table_name, 
       CAST(is_active AS TEXT) as is_active, 
       LEFT(content, 30) as content_preview 
FROM emergency_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT 'update_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('target:', target_version_code, ' force:', is_force_update)
FROM update_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT 'notice_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('v', notice_version, ': ', LEFT(title, 20))
FROM notice_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug'
UNION ALL
SELECT 'ad_policy', 
       CAST(is_active AS TEXT), 
       CONCAT('open:', ad_app_open_enabled, ' inter:', ad_interstitial_enabled, ' banner:', ad_banner_enabled)
FROM ad_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 예상 결과

```
table_name          | is_active | content_preview
--------------------+-----------+----------------------------------
emergency_policy    | false     | ⚠️ [테스트] 이 앱은...
update_policy       | true      | target:1 force:false
notice_policy       | true      | v1: 환영합니다! 🎉
ad_policy           | true      | open:true inter:true banner:true
```

### 결과 기록

```
emergency: is_active = _____
update: target = _____, force = _____
notice: version = _____
ad_policy: open = _____, inter = _____, banner = _____
```

---

## 🔥 Step 2: Emergency 테스트 (X 버튼 있음)

### 2-1. Emergency 활성화

#### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- 1-1. Emergency 활성화 (X 버튼 있음)
UPDATE emergency_policy 
SET is_active = true,
    is_dismissible = true,
    content = '🚨 [테스트] 긴급 테스트입니다. X 버튼으로 닫을 수 있습니다.',
    button_text = '확인'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 1-1. Emergency 활성화 (X 버튼 있음)
UPDATE emergency_policy 
SET is_active = true,
    is_dismissible = true,
    content = '🚨 [DEBUG 테스트] 긴급 테스트입니다. X 버튼으로 닫을 수 있습니다.',
    button_text = '확인'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 2-2. 앱 실행 및 검증

- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] **예상**: 긴급 팝업 즉시 표시

**검증 체크리스트**:
- [ ] ✅ 긴급 팝업 표시됨
- [ ] ✅ 제목: "🚨 긴급공지"
- [ ] ✅ 배지: "긴급" 표시
- [ ] ✅ **X 버튼 있음** (우측 상단)
- [ ] ✅ 내용: 설정한 content 표시
- [ ] ✅ "확인" 버튼 있음

### 2-3. X 버튼 클릭

- [ ] X 버튼 클릭
- [ ] **예상**: 팝업 닫힘
- [ ] ✅ 팝업 닫힘
- [ ] ✅ 홈 화면 정상 표시

### 2-4. 재실행 (추적 없음 확인)

- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] **예상**: 긴급 팝업 다시 표시 (추적 안 함!)
- [ ] ✅ 긴급 팝업 **다시 표시됨** ⭐

### Logcat 확인

**Filter 설정**: `tag:HomeScreen`

```
예상 로그:
✅ "Phase 1: Checking emergency_policy"
✅ "emergency_policy found: isDismissible=true"
✅ "Decision: EMERGENCY from emergency_policy"
✅ "Displaying EmergencyRedirectDialog from emergency_policy"
```

- [ ] ✅ 로그 확인 완료

---

## 🔥 Step 3: Emergency 테스트 (X 버튼 없음)

### 3-1. Emergency 수정

#### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- 1-2. Emergency 수정 (X 버튼 없음)
UPDATE emergency_policy 
SET is_dismissible = false,
    content = '🚨 [테스트] 이 앱은 더 이상 지원되지 않습니다. 새 앱을 설치해야 합니다.',
    button_text = '확인'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 1-2. Emergency 수정 (X 버튼 없음)
UPDATE emergency_policy 
SET is_dismissible = false,
    content = '🚨 [DEBUG 테스트] 이 앱은 더 이상 지원되지 않습니다. 새 앱을 설치해야 합니다.',
    button_text = '확인'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 3-2. 앱 실행 및 검증

- [ ] 앱 완전 종료
- [ ] 앱 재실행

**검증 체크리스트**:
- [ ] ✅ 긴급 팝업 표시됨
- [ ] ✅ **X 버튼 없음** ⭐
- [ ] ✅ 뒤로가기 버튼 막힘 (테스트 해보기)
- [ ] ✅ "확인" 버튼만 있음

---

## 🧹 Step 4: Emergency 정리

### 4-1. 비활성화

#### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- 1-3. Emergency 비활성화 (정리)
UPDATE emergency_policy 
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 1-3. Emergency 비활성화 (정리)
UPDATE emergency_policy 
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

- [ ] ✅ 비활성화 완료

---

## ✅ Phase 1 완료 체크리스트

- [ ] 초기 상태 확인 완료
- [ ] Emergency (X 버튼 있음) 테스트 완료
- [ ] Emergency (X 버튼 없음) 테스트 완료
- [ ] Emergency 비활성화 완료
- [ ] 모든 로그 확인 완료

---

## 🔜 다음 단계

**Phase 2**로 이동하세요!
- Phase 2: Update 테스트 (강제/선택적)

---

**Phase 1 완료!** 🎉

---

**문서 버전**: v1.2.0  
**마지막 수정**: 2025-11-09 06:50 KST

