# 릴리즈 테스트 SQL 스크립트 - Phase 1 (릴리즈용)

**버전**: v1.2.0  
**최종 업데이트**: 2025-11-09 16:39:35 KST  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: Emergency 테스트 (긴급공지)

---

## 📋 Phase 1 개요 (간결)
목표: `emergency_policy`의 동작을 검증합니다 — 표시 우선순위, 닫기 동작(X), 비-닫기(강제) 동작.
핵심 시나리오:
1) 긴급공지(닫기 가능) 표시 확인
2) 긴급공지(닫기 불가) 표시 확인
3) 긴급공지 비활성화(정리)

소요 시간: 약 10분

---

## 📢 핵심 테스트 절차
> 사전: 필요 시 앱 데이터 초기화(SharedPreferences 또는 앱 데이터 삭제) 후 시작

### 1) 긴급공지: 닫기 가능
```sql
-- 1-1. 긴급공지 활성화 (닫기 가능) - 릴리즈 & 디버그
UPDATE emergency_policy
SET is_active = true,
    is_dismissible = true,
    title = CASE 
        WHEN app_id LIKE '%.debug' THEN '[DEBUG] 긴급공지'
        ELSE '긴급공지'
    END,
    content = CASE 
        WHEN app_id LIKE '%.debug' THEN '[DEBUG] 긴급 테스트입니다. X 버튼으로 닫을 수 있습니다.'
        ELSE '긴급 테스트입니다. X 버튼으로 닫을 수 있습니다.'
    END,
    button_text = '확인'
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```
검증: 앱 실행 → 긴급 팝업 즉시 표시 → X 클릭 시 팝업 닫힘 → 재실행 시 재표시 여부 확인(정상: 재표시됨)

---

### 2) 긴급공지: 닫기 불가 (강제 유지)
```sql
-- 1-2. 긴급공지 활성화 (닫기 불가) - 릴리즈 & 디버그
UPDATE emergency_policy
SET is_active = true,
    is_dismissible = false,
    title = CASE 
        WHEN app_id LIKE '%.debug' THEN '[DEBUG] 긴급공지'
        ELSE '긴급공지'
    END,
    content = CASE 
        WHEN app_id LIKE '%.debug' THEN '[DEBUG] 이 앱은 더 이상 지원되지 않습니다. 새 앱을 설치하세요.'
        ELSE '이 앱은 더 이상 지원되지 않습니다. 새 앱을 설치하세요.'
    END,
    button_text = '설치'
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```
검증: 앱 실행 → 팝업 표시(닫기 버튼 없음) → 뒤로가기/홈 차단 확인 → 버튼(설치) 동작(redirect) 확인

---

### 3) 정리: 비활성화
```sql
-- 1-3. 긴급공지 비활성화 - 릴리즈 & 디버그
UPDATE emergency_policy
SET is_active = false
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

---

## ✅ 최소 검사 목록
- [ ] 릴리즈/디버그에 SQL 적용
- [ ] 닫기 가능 긴급공지 표시 확인
- [ ] 닫기 불가(강제) 긴급공지 표시 및 뒤로가기 차단 확인
- [ ] 정리(비활성화) 확인

---

**문서 버전**: v1.2.0  
**마지막 수정**: 2025-11-09 16:39:35 KST
