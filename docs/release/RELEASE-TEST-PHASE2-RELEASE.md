# 릴리즈 테스트 SQL 스크립트 - Phase 2 (릴리즈용)

**버전**: v2.2.0  
**최종 업데이트**: 2025-11-09 16:39:35 KST  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: Update 테스트 (강제/선택적)

---

## 📋 Phase 2 개요 (간결)
목표: `update_policy`의 동작(강제 업데이트 / 선택적 업데이트 / 추적)과 앱측 추적(SharedPreferences) 동작을 검증합니다.
핵심 시나리오:
1) 강제 업데이트 활성화 → 팝업/차단 확인
2) 선택적 업데이트 활성화 → '나중에' 동작과 재표시(SharedPreferences 초기화 필요 여부) 확인
3) 정리(원복)

소요 시간: 약 15분

---

## 📢 핵심 테스트 절차
> 사전: 필요 시 앱 데이터 초기화(SharedPreferences 또는 앱 데이터 삭제) 후 시작

### 1) 강제 업데이트
```sql
-- 2-1. 강제 업데이트 활성화 - 릴리즈 & 디버그
UPDATE update_policy
SET is_active = true,
    target_version_code = 4,
    is_force_update = true,
    release_notes = CASE 
        WHEN app_id LIKE '%.debug' THEN '• [DEBUG] 중요 보안 패치'
        ELSE '• 중요 보안 패치'
    END,
    download_url = 'https://play.google.com/'
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```
검증: 앱 실행 → 강제 업데이트 팝업 표시 및 뒤로가기 차단 확인

---

### 2) 선택적 업데이트
```sql
-- 2-2. 선택적 업데이트 - 릴리즈 & 디버그
UPDATE update_policy
SET is_active = true,
    target_version_code = 4,
    is_force_update = false,
    release_notes = CASE 
        WHEN app_id LIKE '%.debug' THEN '• [DEBUG] 다크 모드 추가'
        ELSE '• 다크 모드 추가'
    END,
    download_url = 'https://play.google.com/'
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```
검증: 앱 실행 → 선택적 팝업 표시 → '나중에' 클릭 시 SharedPreferences에 추적 저장 → 동일 버전은 재표시 안 됨

---

### 2-3. SharedPreferences 초기화 (팝업을 다시 보려면 필요)
ADB 예시(특정 기기):
```bash
# 디버그 앱 SharedPreferences 전체 삭제 (emulator-5554 예시)
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm -r /data/data/com.sweetapps.pocketchord.debug/shared_prefs/
```
검증: 초기화 후 앱 재시작 → 선택적 업데이트 팝업 재표시

---

### 3) 정리: 원복
```sql
-- 2-3. Update 정리 (원래대로) - 릴리즈 & 디버그
UPDATE update_policy
SET is_active = false,
    target_version_code = 3,
    is_force_update = false,
    download_url = 'https://play.google.com/'
WHERE app_id IN ('com.sweetapps.pocketchord', 'com.sweetapps.pocketchord.debug');
```

---

## ✅ 최소 검사 목록
- [ ] 릴리즈/디버그에 SQL 적용
- [ ] 강제 업데이트 팝업 표시 및 차단 확인
- [ ] 선택적 업데이트에서 '나중에' 동작 확인
- [ ] SharedPreferences 초기화 후 팝업 재표시 확인
- [ ] 정리(원복) 확인

---

**문서 버전**: v2.2.0  
**마지막 수정**: 2025-11-09 16:39:35 KST
