# 릴리즈 테스트 SQL 스크립트 - Phase 3 (릴리즈용)

**버전**: v1.1.0  
**최종 업데이트**: 2025-11-09 09:05 KST  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: Notice 테스트 (버전 관리)

---

## 📝 변경 이력

### v1.1.0 (2025-11-09 09:05)
- ✅ 모든 SQL 문에 디버그 버전 추가
- ✅ 릴리즈/디버그 버전 명확히 구분

### v1.0.0 (2025-11-09 06:35)
- ✅ 최초 작성
- ✅ Phase 3 테스트 시나리오 작성

---

## ⚠️ 디버그 버전 사용 시 주의사항

디버그 버전(🔧)을 테스트하기 전에 먼저 디버그 데이터를 생성해야 합니다!

**1회만 실행**: `docs/sql/07-create-debug-test-data.sql`

이미 생성했다면 건너뛰세요!

---

## 📋 Phase 3 개요

이 문서는 릴리즈 테스트의 세 번째 단계입니다.

**포함된 테스트**:
1. ✅ 공지사항 표시 확인
2. ✅ 오타 수정 (버전 유지) → 재표시 안 됨
3. ✅ 새 공지 (버전 증가) → 재표시됨

**소요 시간**: 약 15분

---

## 📢 Step 1: 공지사항 표시 테스트

### 1-1. 현재 버전 확인

**SQL 스크립트 - 릴리즈 버전** ⭐

```sql
-- 3-1. 현재 버전 확인
SELECT notice_version, title, is_active 
FROM notice_policy 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**SQL 스크립트 - 디버그 버전** 🔧

```sql
-- 3-1. 현재 버전 확인 (디버그)
SELECT notice_version, title, is_active 
FROM notice_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과 기록**: `notice_version = _____`

### 1-2. SharedPreferences 초기화

- [ ] 앱 데이터 삭제
- [ ] 또는: 앱 재설치

### 1-3. 앱 실행 및 검증

- [ ] 앱 실행

**검증 체크리스트**:
- [ ] ✅ 공지사항 팝업 표시
- [ ] ✅ 제목 표시됨
- [ ] ✅ 내용 표시됨
- [ ] ✅ X 버튼 있음

### 1-4. X 클릭

- [ ] X 버튼 클릭
- [ ] ✅ 팝업 닫힘

### 1-5. 재실행 (추적 확인)

- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] **예상**: 공지 팝업 표시 **안 됨** ⭐
- [ ] ✅ 팝업 표시 안 됨 (추적됨)

### Logcat 확인

**Filter 설정**: `tag:HomeScreen`

```
예상 로그:
✅ "Phase 3: Checking notice_policy"
✅ "notice_policy found: version=1, title=환영합니다! 🎉"
✅ "Notice already viewed (version=1), skipping"
```

- [ ] ✅ 로그 확인 완료

---

## 📢 Step 2: 오타 수정 테스트 (버전 유지)

### 2-1. 오타 수정

**SQL 스크립트 - 릴리즈 버전** ⭐

```sql
-- 3-2. 오타 수정 (버전 유지)
UPDATE notice_policy 
SET content = 'PocketChord를 이용해 주셔서 정말 감사합니다!'
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version은 변경하지 않음!
```

**SQL 스크립트 - 디버그 버전** 🔧

```sql
-- 3-2. 오타 수정 (버전 유지, 디버그)
UPDATE notice_policy 
SET content = '[DEBUG] PocketChord를 이용해 주셔서 정말 감사합니다!'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- notice_version은 변경하지 않음!
```

### 2-2. 앱 실행 및 검증

- [ ] 앱 완전 종료
- [ ] 앱 재실행

**검증**:
- [ ] **예상**: 공지 팝업 표시 **안 됨** ⭐
- [ ] ✅ 팝업 표시 안 됨 (버전이 같으므로)

### Logcat 확인

**Filter 설정**: `tag:HomeScreen`

```
예상 로그:
✅ "Notice already viewed (version=1), skipping"
```

- [ ] ✅ 로그 확인 완료

**의미**: 오타 수정 시 버전을 유지하면 이미 본 사용자에게 재표시되지 않습니다! ✅

---

## 📢 Step 3: 새 공지 테스트 (버전 증가)

### 3-1. 버전 증가

**SQL 스크립트 - 릴리즈 버전** ⭐

```sql
-- 3-3. 새 공지 (버전 증가)
UPDATE notice_policy 
SET title = '🎉 11월 이벤트',
    content = '11월 특별 이벤트가 시작되었습니다!\n많은 참여 부탁드립니다.',
    notice_version = 2  -- 버전 증가! ⭐
WHERE app_id = 'com.sweetapps.pocketchord';
```

**SQL 스크립트 - 디버그 버전** 🔧

```sql
-- 3-3. 새 공지 (버전 증가, 디버그)
UPDATE notice_policy 
SET title = '[DEBUG] 🎉 11월 이벤트',
    content = '[DEBUG] 11월 특별 이벤트가 시작되었습니다!\n많은 참여 부탁드립니다.',
    notice_version = 2  -- 버전 증가! ⭐
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 3-2. 앱 실행 및 검증

- [ ] 앱 완전 종료
- [ ] 앱 재실행

**검증**:
- [ ] **예상**: 공지 팝업 **다시 표시됨** ⭐
- [ ] ✅ 팝업 다시 표시됨
- [ ] ✅ 새 제목: "🎉 11월 이벤트"
- [ ] ✅ 새 내용 표시됨

### 3-3. X 클릭 후 재실행

- [ ] X 버튼 클릭
- [ ] 앱 완전 종료
- [ ] 앱 재실행
- [ ] **예상**: 공지 팝업 표시 **안 됨** (버전 2 추적됨)
- [ ] ✅ 팝업 표시 안 됨

### Logcat 확인

**Filter 설정**: `tag:HomeScreen`

```
예상 로그:
✅ "Decision: NOTICE from notice_policy (version=2)"
✅ "Marked notice version 2 as viewed"
```

- [ ] ✅ 로그 확인 완료

**의미**: 버전을 증가시키면 모든 사용자에게 다시 표시됩니다! ✅

---

## 🧹 Step 4: Notice 정리

### 4-1. 원래대로 복구

**SQL 스크립트 - 릴리즈 버전** ⭐

```sql
-- 3-4. Notice 정리 (원래대로)
UPDATE notice_policy 
SET title = '환영합니다! 🎉',
    content = 'PocketChord를 이용해 주셔서 감사합니다!\n더 나은 서비스를 제공하기 위해 노력하겠습니다.',
    notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord';
```

**SQL 스크립트 - 디버그 버전** 🔧

```sql
-- 3-4. Notice 정리 (원래대로, 디버그)
UPDATE notice_policy 
SET title = '[DEBUG] 환영합니다! 🎉',
    content = '[DEBUG] PocketChord를 이용해 주셔서 감사합니다!\n더 나은 서비스를 제공하기 위해 노력하겠습니다.',
    notice_version = 1
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

- [ ] ✅ 정리 완료

---

## 💡 버전 관리 정리

### 오타 수정 (버전 유지)

**SQL 스크립트 - 릴리즈 버전** ⭐

```sql
-- 버전 유지 = 재표시 안 됨
UPDATE notice_policy 
SET content = '수정된 내용'
WHERE app_id = 'com.sweetapps.pocketchord';
-- notice_version은 건드리지 않음!
```

**SQL 스크립트 - 디버그 버전** 🔧

```sql
-- 버전 유지 = 재표시 안 됨 (디버그)
UPDATE notice_policy 
SET content = '[DEBUG] 수정된 내용'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
-- notice_version은 건드리지 않음!
```

**결과**: 이미 본 사용자에게 재표시 **안 됨** ✅

---

### 새 공지 (버전 증가)

**SQL 스크립트 - 릴리즈 버전** ⭐

```sql
-- 버전 증가 = 모두에게 재표시
UPDATE notice_policy 
SET title = '새 공지',
    content = '새 내용',
    notice_version = 2  -- 증가!
WHERE app_id = 'com.sweetapps.pocketchord';
```

**SQL 스크립트 - 디버그 버전** 🔧

```sql
-- 버전 증가 = 모두에게 재표시 (디버그)
UPDATE notice_policy 
SET title = '[DEBUG] 새 공지',
    content = '[DEBUG] 새 내용',
    notice_version = 2  -- 증가!
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**결과**: 모든 사용자에게 **재표시됨** ✅

---

## ✅ Phase 3 완료 체크리스트

- [ ] 공지사항 표시 확인 완료
- [ ] 오타 수정 (버전 유지) 테스트 완료
- [ ] 새 공지 (버전 증가) 테스트 완료
- [ ] Notice 정리 완료
- [ ] 버전 관리 동작 이해 완료
- [ ] 모든 로그 확인 완료

---

## 🔜 다음 단계

**Phase 4**로 이동하세요!
- Phase 4: 우선순위 테스트 + 최종 확인

---

**Phase 3 완료!** 🎉

---

**문서 버전**: v1.1.0  
**마지막 수정**: 2025-11-09 09:05 KST

