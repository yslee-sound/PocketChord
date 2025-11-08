# 디버그 테스트 데이터 생성 완료!

**작성일**: 2025-11-09  
**목적**: 디버그 버전 테스트를 위한 초기 데이터 생성  
**상태**: ✅ 완료

---

## ⚠️ 문제 상황

### 오류 메시지
```
SQL 스크립트 - 디버그 버전 실행 시
Error: Failed to fetch (api.supabase.com)
```

### 원인
디버그 버전(`com.sweetapps.pocketchord.debug`) 데이터가 Supabase에 없음!

---

## ✅ 해결 방법

### 1. 디버그 데이터 생성 SQL 생성
**파일**: `docs/sql/07-create-debug-test-data.sql`

**포함 내용**:
- ✅ emergency_policy 디버그 데이터
- ✅ update_policy 디버그 데이터
- ✅ notice_policy 디버그 데이터
- ✅ ad_policy 디버그 데이터

### 2. Phase 문서에 안내 추가
모든 Phase 문서 상단에 경고 추가:

```markdown
## ⚠️ 디버그 버전 사용 시 주의사항

디버그 버전(🔧)을 테스트하기 전에 먼저 디버그 데이터를 생성해야 합니다!

**1회만 실행**: `docs/sql/07-create-debug-test-data.sql`

이미 생성했다면 건너뛰세요!
```

---

## 🚀 사용 방법

### Step 1: 디버그 데이터 생성 (1회만)

```bash
1. Supabase 대시보드 접속
2. SQL Editor 열기
3. docs/sql/07-create-debug-test-data.sql 열기
4. 전체 복사 → 붙여넣기 → 실행
```

### Step 2: 확인

SQL 실행 후 마지막에 나오는 결과:

```
table_name          | count
--------------------|-------
emergency_policy    | 1
update_policy       | 1
notice_policy       | 1
ad_policy           | 1
```

모두 1이면 성공! ✅

### Step 3: 디버그 버전 테스트

이제 Phase 문서의 **🔧 디버그 버전** SQL을 안전하게 사용할 수 있습니다!

---

## 📊 생성되는 데이터

### emergency_policy (debug)
```sql
app_id: 'com.sweetapps.pocketchord.debug'
is_active: false
content: '[DEBUG 테스트] ...'
is_dismissible: true
```

### update_policy (debug)
```sql
app_id: 'com.sweetapps.pocketchord.debug'
is_active: true
target_version_code: 1
is_force_update: false
message: '[DEBUG] 앱 업데이트'
```

### notice_policy (debug)
```sql
app_id: 'com.sweetapps.pocketchord.debug'
is_active: true
title: '[DEBUG] 환영합니다! 🎉'
notice_version: 1
```

### ad_policy (debug)
```sql
app_id: 'com.sweetapps.pocketchord.debug'
is_active: true
ad_app_open_enabled: true
ad_interstitial_enabled: true
ad_banner_enabled: true
```

---

## 💡 중요 사항

### 1회만 실행!
```
✅ 처음 디버그 테스트 시: 반드시 실행
⚠️ 이미 실행했다면: 건너뛰기
```

### ON CONFLICT 처리
```sql
ON CONFLICT (app_id) WHERE is_active = true DO NOTHING;
```
→ 이미 있으면 무시 (안전함)

### 릴리즈 vs 디버그
```
릴리즈(⭐): com.sweetapps.pocketchord (이미 있음)
디버그(🔧): com.sweetapps.pocketchord.debug (새로 생성)
```

---

## 🎯 체크리스트

- [ ] `07-create-debug-test-data.sql` 실행
- [ ] 확인 쿼리에서 4개 테이블 모두 count=1 확인
- [ ] Phase 1 디버그 버전 테스트 가능
- [ ] Phase 2 디버그 버전 테스트 가능
- [ ] Phase 3 디버그 버전 테스트 가능
- [ ] Phase 4 디버그 버전 테스트 가능

---

## 🎉 완료!

- ✅ 디버그 데이터 생성 SQL 작성
- ✅ Phase 1~4 문서에 안내 추가
- ✅ 사용 가이드 작성

**이제 Supabase에서 SQL을 실행하면 디버그 버전 테스트가 가능합니다!** 🚀

