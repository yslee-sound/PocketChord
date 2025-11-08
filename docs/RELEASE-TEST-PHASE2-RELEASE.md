# 릴리즈 테스트 SQL 스크립트 - Phase 2 (릴리즈용)

**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: Update 테스트 (강제/선택적)

---

## 📋 Phase 2 개요

이 문서는 릴리즈 테스트의 두 번째 단계입니다.

**포함된 테스트**:
1. ✅ 강제 업데이트 테스트
2. ✅ 선택적 업데이트 테스트
3. ✅ "나중에" 버튼 추적 확인

**소요 시간**: 약 15분

---

## 🔄 Step 1: 강제 업데이트 테스트

### 1-1. 강제 업데이트 활성화

#### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- 2-1. 강제 업데이트 활성화
UPDATE update_policy 
SET is_active = true,
    target_version_code = 999,
    is_force_update = true,
    message = '[테스트] 필수 업데이트가 있습니다',
    release_notes = '• [테스트] 중요 보안 패치\n• [테스트] 필수 기능 추가'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 2-1. 강제 업데이트 활성화
UPDATE update_policy 
SET is_active = true,
    target_version_code = 999,
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

#### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- 2-3. 버전 더 높게 (추가 테스트)
UPDATE update_policy 
SET target_version_code = 1000
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 2-3. 버전 더 높게 (추가 테스트)
UPDATE update_policy 
SET target_version_code = 1000
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 3-2. 앱 실행 및 검증

...existing code...

## 🧹 Step 4: Update 정리

### 4-1. 원래대로 복구

#### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- 2-4. Update 정리 (원래대로)
UPDATE update_policy 
SET target_version_code = 1
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 2-4. Update 정리 (원래대로)
UPDATE update_policy 
SET target_version_code = 1
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

