# 릴리즈 테스트 SQL 스크립트 - Phase 2 (릴리즈용)

**버전**: v1.1.0  
**최종 업데이트**: 2025-11-09 06:35 KST  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: Update 테스트 (강제/선택적)

---

## 📝 변경 이력

### v1.1.0 (2025-11-09 06:35)
- ✅ UPDATE-POLICY-USAGE-GUIDE 링크 추가
- ✅ target_version_code 의미 설명 추가
- ✅ 추적 메커니즘 상세 설명 추가

### v1.0.0 (2025-11-08)
- ✅ 최초 작성
- ✅ Phase 2 테스트 시나리오 작성

---

## ⚠️ 디버그 버전 사용 시 주의사항

디버그 버전(🔧)을 테스트하기 전에 먼저 디버그 데이터를 생성해야 합니다!

**1회만 실행**: `docs/sql/07-create-debug-test-data.sql`

```sql
-- 4개 테이블에 디버그 데이터 생성
INSERT INTO emergency_policy ... WHERE app_id = '*.debug'
INSERT INTO update_policy ... WHERE app_id = '*.debug'
INSERT INTO notice_policy ... WHERE app_id = '*.debug'
INSERT INTO ad_policy ... WHERE app_id = '*.debug'
```

**이미 생성했다면 건너뛰세요!**

---

## 📋 Phase 2 개요

이 문서는 릴리즈 테스트의 두 번째 단계입니다.

**포함된 테스트**:
1. ✅ 강제 업데이트 테스트
2. ✅ 선택적 업데이트 테스트
3. ✅ "나중에" 버튼 추적 확인

**소요 시간**: 약 15분

---

## 💡 실제 사용법이 궁금하다면?

**👉 [UPDATE-POLICY-USAGE-GUIDE.md](UPDATE-POLICY-USAGE-GUIDE.md)** 참조!

이 Phase 2는 **테스트 시나리오**입니다. 실제 운영 시에는:
- ✅ Play Store versionCode와 일치하는 숫자 사용 (예: 10, 11, 12...)
- ✅ 항상 증가만 시킴 (절대 낮추지 않기!)

**예시**:
```sql
-- 실제 운영 (Play Store versionCode = 15로 출시 시)
UPDATE update_policy 
SET target_version_code = 15,  -- Play Store와 일치!
    is_force_update = false,
    message = '새로운 기능이 추가되었습니다',
    release_notes = '• 다크 모드 추가\n• 성능 개선'
WHERE app_id = 'com.sweetapps.pocketchord';
```

자세한 내용은 [UPDATE-POLICY-USAGE-GUIDE.md](UPDATE-POLICY-USAGE-GUIDE.md)를 참조하세요.
WHERE app_id = 'com.sweetapps.pocketchord';
```
## 🔄 Step 1: 강제 업데이트 테스트

### 1-1. 강제 업데이트 활성화

**의미**: 
- `target_version_code = 999`는 "앱을 버전 999로 업데이트하라"는 의미입니다
- 현재 앱의 `VERSION_CODE`(예: 1)보다 훨씬 높은 값으로 설정
- → 앱이 "업데이트가 필요하다"고 판단하여 팝업을 표시합니다

**동작**:
```kotlin
if (currentVersionCode < target_version_code) {
    // 업데이트 팝업 표시
    // 현재 1 < 999 → true → 팝업 표시!
}
```

#### SQL 스크립트 - 릴리즈 버전 (주의: 타겟버전확인) ⭐

```sql
-- 2-1. 강제 업데이트 활성화
UPDATE update_policy 
SET is_active = true,
    target_version_code = 999,  -- 목표 버전 (현재 버전보다 높게 설정)
    is_force_update = true,
    message = '[테스트] 필수 업데이트가 있습니다',
    release_notes = '• [테스트] 중요 보안 패치\n• [테스트] 필수 기능 추가'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 (주의: 타겟버전확인) 🔧

```sql
-- 2-1. 강제 업데이트 활성화
UPDATE update_policy 
SET is_active = true,
    target_version_code = 999,  -- 목표 버전 (현재 버전보다 높게 설정)
    is_force_update = true,
    message = '[DEBUG 테스트] 필수 업데이트가 있습니다',
    release_notes = '• [DEBUG] 중요 보안 패치\n• [DEBUG] 필수 기능 추가'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 1-2. 앱 실행 및 검증

...existing code...

## 🔄 Step 2: 선택적 업데이트 테스트

### 2-1. 선택적으로 변경

#### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- 2-2. 선택적 업데이트로 변경
UPDATE update_policy 
SET is_force_update = false,
    message = '[테스트] 새로운 기능이 추가되었습니다',
    release_notes = '• [테스트] 다크 모드 추가\n• [테스트] 성능 개선\n• [테스트] UI 업데이트'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 2-2. 선택적 업데이트로 변경
UPDATE update_policy 
SET is_force_update = false,
    message = '[DEBUG 테스트] 새로운 기능이 추가되었습니다',
    release_notes = '• [DEBUG] 다크 모드 추가\n• [DEBUG] 성능 개선\n• [DEBUG] UI 업데이트'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 2-2. SharedPreferences 초기화

...existing code...

## 🔄 Step 3: 버전 증가 테스트

### 3-1. 버전을 더 높게 변경

**의미**: 
- `target_version_code = 1000`으로 변경 (999 → 1000)
- **999는 이미 추적되고 있음!** SharedPreferences에 저장됨
- 999보다 **낮거나 같은** 버전은 다시 표시되지 않음
- 1000으로 **높이면** → 새로운 버전! → 다시 팝업 표시 ⭐

**추적 메커니즘**:
```kotlin
// "나중에" 클릭 시
dismissedVersionCode = 999  // ← 저장됨!

// 다음 실행 시
if (dismissedVersionCode != targetVersionCode) {
    // 팝업 표시
    // 999 != 999 → false → 표시 안 됨 ✅
    // 999 != 1000 → true → 표시됨! ⭐
}
```

**중요**: 
- ⚠️ 999로 설정하고 "나중에"를 눌렀다면, **999 이하의 모든 버전은 무시됨**
- ✅ 1000, 1001 등 **더 높은 버전만** 다시 팝업 표시
- ❌ 998, 500 등 **낮은 버전**으로 변경하면 팝업이 **절대 표시되지 않음!**

**시나리오**:
```
1. target = 999 설정 → "나중에" 클릭 → dismissedVersionCode = 999 저장
2. target = 1000 변경 → 999 != 1000 → 팝업 다시 표시 ✅
3. target = 998 변경 → 999 != 998 → 하지만 998 < 999(현재 앱 버전 간주) → 표시 안 됨 ❌
```

#### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- 2-3. 버전 더 높게 (추가 테스트)
UPDATE update_policy 
SET target_version_code = 1000  -- 999보다 높게! (낮추면 표시 안 됨)
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 2-3. 버전 더 높게 (추가 테스트)
UPDATE update_policy 
SET target_version_code = 1000  -- 999보다 높게! (낮추면 표시 안 됨)
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 3-2. 앱 실행 및 검증

...existing code...

## 🧹 Step 4: Update 정리

### 4-1. 원래대로 복구

**의미**: 
- `target_version_code = 1`로 복구 (현재 앱 버전과 같거나 낮게)
- → 앱이 "업데이트가 필요 없다"고 판단
- → 업데이트 팝업이 더 이상 표시되지 않음

**동작**:
```kotlin
if (currentVersionCode < target_version_code) {
    // 팝업 표시
    // 현재 1 < 1 → false → 팝업 표시 안 됨 ✅
}
```

#### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- 2-4. Update 정리 (원래대로)
UPDATE update_policy 
SET target_version_code = 1  -- 현재 버전과 같게 설정 (업데이트 불필요)
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 2-4. Update 정리 (원래대로)
UPDATE update_policy 
SET target_version_code = 1  -- 현재 버전과 같게 설정 (업데이트 불필요)
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

- [ ] ✅ 정리 완료

---

## ✅ Phase 2 완료 체크리스트

- [ ] 강제 업데이트 테스트 완료
- [ ] 선택적 업데이트 테스트 완료
- [ ] "나중에" 추적 확인 완료
- [ ] 버전 증가 테스트 완료
- [ ] Update 정리 완료
- [ ] 모든 로그 확인 완료

---

## 🔜 다음 단계

**Phase 3**으로 이동하세요!
- Phase 3: Notice 테스트 (버전 관리)

---

**Phase 2 완료!** 🎉

---

**문서 버전**: v1.1.0  
**마지막 수정**: 2025-11-09 06:35 KST

