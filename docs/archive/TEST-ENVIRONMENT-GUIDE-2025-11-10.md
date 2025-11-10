# 테스트 환경 선택 가이드

**질문**: SQL의 `WHERE app_id = 'com.sweetapps.pocketchord'`는 릴리즈 버전? Debug 버전?

---

## 🎯 답변: **릴리즈 버전 사용 권장!**

### 이유

릴리즈 테스트 체크리스트는 **실제 사용자가 받을 빌드**를 검증하는 것이 목적입니다.

```
릴리즈 테스트 = 프로덕션 검증
→ 'com.sweetapps.pocketchord' 사용 ✅
```

---

## 📊 비교: 릴리즈 vs Debug

### 옵션 A: 프로덕션(릴리즈) 테스트 ⭐ **권장**

```sql
WHERE app_id = 'com.sweetapps.pocketchord'
```

**목적**:
- ✅ 실제 사용자가 받을 릴리즈 빌드 검증
- ✅ 프로덕션 환경과 동일한 조건
- ✅ 릴리즈 전 최종 승인 근거

**언제**:
- 릴리즈 전 최종 검증
- 메이저 버전 업데이트
- 중요한 기능 변경 후

**장점**:
- ✅ 프로덕션 데이터로 테스트
- ✅ 실제 사용자 경험과 동일
- ✅ 릴리즈 승인 근거로 사용 가능

---

### 옵션 B: 개발(Debug) 테스트

```sql
WHERE app_id = 'com.sweetapps.pocketchord.debug'
```

**목적**:
- 개발 중 빠른 테스트
- 프로덕션 데이터에 영향 없이 테스트

**언제**:
- 개발 단계
- 빠른 검증 필요
- 프로덕션 데이터 보호 필요

**장점**:
- ✅ 프로덕션 데이터 보호
- ✅ 실험적 기능 테스트 가능
- ✅ 테스트 데이터 관리 용이

**단점**:
- ⚠️ 프로덕션과 데이터가 다를 수 있음
- ⚠️ 릴리즈 승인 근거로 부적합

---

## 🎯 권장 사용법

### 1. 일반적인 릴리즈 테스트

```sql
-- 프로덕션 버전 사용
WHERE app_id = 'com.sweetapps.pocketchord'
```

**시나리오**:
1. 개발 완료
2. 릴리즈 테스트 체크리스트 실행 ← **프로덕션 데이터**
3. 모든 테스트 PASS
4. 릴리즈 승인 🚀

---

### 2. 개발 단계 빠른 테스트

```sql
-- Debug 버전 사용
WHERE app_id = 'com.sweetapps.pocketchord.debug'
```

**시나리오**:
1. 새 기능 개발 중
2. 빠른 테스트 필요 ← **Debug 데이터**
3. 문제 발견 → 수정
4. 다시 테스트
5. 완료 후 → 프로덕션 최종 테스트

---

## 📝 체크리스트 업데이트

체크리스트에 **테스트 환경 선택 섹션**이 추가되었습니다:

```markdown
### 1. 테스트 환경 선택 ⭐

#### 옵션 A: 프로덕션(릴리즈) 테스트 ✅ **권장**
app_id: 'com.sweetapps.pocketchord'

#### 옵션 B: 개발(Debug) 테스트
app_id: 'com.sweetapps.pocketchord.debug'

**이 체크리스트의 기본값**: 'com.sweetapps.pocketchord' (프로덕션)

**Debug 버전으로 테스트하려면**:
- 모든 SQL의 WHERE 절을 변경
```

---

## 🔄 Debug로 전환하는 방법

### 방법 1: SQL 에디터에서 찾기/바꾸기

Supabase SQL Editor에서:
```sql
-- 찾기
'com.sweetapps.pocketchord'

-- 바꾸기
'com.sweetapps.pocketchord.debug'
```

### 방법 2: 체크리스트 복사본 만들기

```bash
# 프로덕션용
docs/RELEASE-TEST-CHECKLIST.md (원본)

# Debug용 (복사본)
docs/RELEASE-TEST-CHECKLIST-DEBUG.md
```

---

## 💡 실전 팁

### Tip 1: 프로덕션 우선, Debug는 보조

```
개발 → Debug 테스트 (빠른 검증)
     ↓
완료 → 프로덕션 테스트 (최종 검증) ⭐
     ↓
릴리즈 🚀
```

### Tip 2: 프로덕션 테스트 후 복구

```sql
-- 테스트 전 상태 기록
SELECT * FROM emergency_policy WHERE app_id = 'com.sweetapps.pocketchord';

-- 테스트 진행...

-- 테스트 후 원래대로 복구
-- (체크리스트에 복구 스크립트 포함)
```

### Tip 3: Debug로 실험, 프로덕션으로 검증

```
1. Debug 버전으로 새 기능 테스트
2. 문제 발견 → 수정
3. 프로덕션 버전으로 최종 검증 ⭐
4. 릴리즈
```

---

## 🎯 결론

### 릴리즈 테스트는 프로덕션!

```
✅ WHERE app_id = 'com.sweetapps.pocketchord'  (권장)
⚠️ WHERE app_id = 'com.sweetapps.pocketchord.debug'  (개발용)
```

**이유**:
- ✅ 실제 사용자 환경 검증
- ✅ 프로덕션 데이터 기준
- ✅ 릴리즈 승인 근거

**언제 Debug 사용**:
- 개발 단계
- 빠른 실험
- 프로덕션 데이터 보호 필요

---

**업데이트**: `RELEASE-TEST-CHECKLIST.md`에 환경 선택 가이드 추가됨! ✅

