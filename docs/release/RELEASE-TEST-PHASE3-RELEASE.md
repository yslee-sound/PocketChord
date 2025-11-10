# 릴리즈 테스트 SQL 스크립트 - Phase 3 (릴리즈용)

**버전**: v1.6.0  
**최종 업데이트**: 2025-11-09 16:39:35 KST  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: Notice 테스트 (버전 관리)

---

## 📋 Phase 3 개요 (간결)

목표: 공지사항(notice_policy)의 동작(활성화, 재표시 조건, 버전 관리)을 검증합니다.
핵심 시나리오:
1) 공지 활성화 → 표시 확인
2) 동일 버전(오타 수정) → 재표시 안 됨
3) 버전 증가 → 모든 사용자에게 재표시

소요 시간: 약 10~15분

---

## 📢 핵심 테스트 절차

> 사전: 필요 시 앱 데이터 초기화(SharedPreferences 또는 앱 데이터 삭제) 후 시작

### 1) 공지 활성화
```sql
-- 1-1. 공지 활성화 - 릴리즈 & 디버그
UPDATE notice_policy
SET is_active = true,
    title = CASE 
        WHEN app_id LIKE '%.debug' THEN '[DEBUG] 공지: 서비스 안내'
        ELSE '공지: 서비스 안내'
    END,
    content = CASE 
        WHEN app_id LIKE '%.debug' THEN '[DEBUG] 테스트용 공지 내용입니다.'
        ELSE '중요 공지입니다. 앱을 최신 버전으로 유지해 주세요.'
    END,
    notice_version = 251109  -- YYMMDD 형식 권장
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```
검증: 앱 실행 → 팝업 표시 / X로 닫기 → 재실행 시 재표시 여부 확인(정상: 재표시 안 됨)

---

### 2) 오타 수정(버전 유지)

요약: 같은 `notice_version`으로는 이미 본 사용자에게 재표시되지 않습니다. 따라서 단순 오타 수정은 버전을 변경하지 마세요.

```sql
-- 오타 수정 - 릴리즈 & 디버그
UPDATE notice_policy 
SET content = CASE 
    WHEN app_id LIKE '%.debug' THEN '[DEBUG] 수정된 내용'
    ELSE '수정된 내용'
END
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```
검증: 이미 본 사용자는 변경 후에도 팝업 재표시 안 됨.

---

### 3) 새 공지(버전 증가)
```sql
-- 3-1. 새 공지 (버전 증가) - 릴리즈 & 디버그
UPDATE notice_policy
SET title = CASE 
        WHEN app_id LIKE '%.debug' THEN '[DEBUG] 🎉 11월 이벤트'
        ELSE '🎉 11월 이벤트'
    END,
    content = CASE 
        WHEN app_id LIKE '%.debug' THEN '[DEBUG] 이벤트 내용'
        ELSE '11월 특별 이벤트가 시작되었습니다! 참여하세요.'
    END,
    notice_version = 251110  -- 이전(251109)보다 큰 값
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```
검증: 앱 재시작 → 팝업이 모든 사용자에게 재표시되어야 함 → X 클릭 후 재실행 시 재표시 안 됨(버전 추적)

---

## 🔧 버전 관리 권장(간결)

권장: 날짜 기반(YYMMDD) 또는 날짜+시간(YYMMDDHH) 사용.
- 날짜 기반(YYMMDD): 간단하고 직관적 (단, 하루 1건 권장)
- 날짜+시간(YYMMDDHH): 하루 여러 건 필요 시 사용
- 자동 증가: DB에서 `notice_version = notice_version + 1` 방식은 실수 방지에 안전함

중요: 절대 자릿수를 혼용(예: 6자리와 7자리 혼합)하지 마세요 — 숫자 비교가 깨집니다.

---

## ✅ 최소 검사 목록
- [ ] 릴리즈/디버그에 각각 SQL 적용
- [ ] 앱에서 팝업 표시 확인
- [ ] 동일 버전 수정 시 재표시 안 됨 확인
- [ ] 버전 증가 시 재표시 확인

---

### 정리: 초기화
```sql
-- 3-4. Notice 정리 (초기화) - 릴리즈 & 디버그
UPDATE notice_policy
SET is_active = false,
    notice_version = 251109
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

---

**문서 버전**: v1.6.0  
**마지막 수정**: 2025-11-09 16:39:35 KST
