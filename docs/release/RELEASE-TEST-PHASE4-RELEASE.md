# 릴리즈 테스트 SQL 스크립트 - Phase 4 (릴리즈용)

**버전**: v1.0.0  
**최종 업데이트**: 2025-11-09 16:39:35 KST  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: 우선순위 테스트 + 최종 확인

---

## 📋 Phase 4 개요 (간결)
목표: 팝업 우선순위(emergency > update > notice) 작동을 검증합니다.
핵심 시나리오:
1) emergency + update 동시 활성화 → emergency만 표시
2) emergency 비활성화 후 update + notice 동시 활성화 → update만 표시
3) 최종 상태 확인 및 초기화

소요 시간: 약 10분

---

## 📢 핵심 테스트 절차
> 사전: 필요 시 앱 데이터 초기화(SharedPreferences 또는 앱 데이터 삭제) 후 시작

### 1) emergency + update 동시 활성화
```sql
-- 4-1. Emergency + Update 동시 활성화 - 릴리즈 & 디버그
UPDATE emergency_policy 
SET is_active = true, 
    content = CASE 
        WHEN app_id LIKE '%.debug' THEN '[DEBUG] 🚨 긴급 테스트'
        ELSE '🚨 긴급 테스트'
    END
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

UPDATE update_policy 
SET is_active = true, 
    target_version_code = 4, 
    is_force_update = true 
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```
검증: 앱 실행 → emergency 팝업만 표시(우선순위 확인)

---

### 2) emergency 비활성화 → update > notice 우선순위 확인
```sql
-- 4-2. Emergency 비활성화, Update/Notice 활성화 - 릴리즈 & 디버그
UPDATE emergency_policy 
SET is_active = false 
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

UPDATE update_policy 
SET is_active = true, 
    target_version_code = 4, 
    is_force_update = false 
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

UPDATE notice_policy 
SET is_active = true, 
    notice_version = 251109 
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```
검증: 앱 실행 → update 팝업만 표시, notice는 표시되지 않음

---

### 3) 최종 상태 확인 및 초기화
```sql
-- 4-3. 초기화(평상시 상태) - 릴리즈 & 디버그
UPDATE emergency_policy 
SET is_active = false 
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

UPDATE update_policy 
SET is_active = false, 
    target_version_code = 3, 
    is_force_update = false 
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');

UPDATE notice_policy 
SET is_active = false, 
    notice_version = 251109 
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

---

## ✅ 최소 검사 목록
- [ ] emergency + update 동시 활성화 시 emergency만 표시
- [ ] emergency 비활성화 후 update 우선표시 확인
- [ ] 최종 초기화 및 평상시 상태 복구 확인

---

**문서 버전**: v1.0.0  
**마지막 수정**: 2025-11-09 16:39:35 KST
