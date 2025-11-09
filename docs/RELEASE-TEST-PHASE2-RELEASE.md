# 릴리즈 테스트 SQL 스크립트 - Phase 2 (릴리즈용)

**버전**: v2.2.0  
**최종 업데이트**: 2025-11-09 08:30 KST  
**app_id**: `com.sweetapps.pocketchord` (프로덕션)  
**포함 내용**: Update 테스트 (강제/선택적)

---

## 📝 변경 이력

### v2.2.0 (2025-11-09 08:30) 🔍 동작 명확화
- ✅ **선택적 업데이트의 "업데이트" 버튼 동작 명확화**
  - "업데이트" 버튼 클릭 시 팝업이 닫히지 않음 (의도된 동작)
  - Play Store로 이동하고 업데이트하지 않으면 팝업이 **다시 표시됨** ✅
  - 실제로 업데이트하면 VERSION_CODE가 증가하여 팝업이 자동으로 사라짐
- ✅ Logcat에 `showUpdateDialog` 로그 추가

### v2.1.0 (2025-11-09 08:15) 🔥 중요 수정
- ✅ **선택적 업데이트 테스트 가이드 수정**
- ❌ 강제 업데이트로 변경하라는 잘못된 가이드 제거
- ✅ SharedPreferences 전체 삭제(`rm -r`) 방법 강조
- ✅ 트러블슈팅 5️⃣, 6️⃣ 항목 개선

### v2.0.0 (2025-11-09 08:10) 🔥 중요 업데이트
- ✅ **선택적 업데이트 팝업이 표시되지 않는 문제 해결**
- ✅ 원인: `is_force_update=false`일 때 dismissedVersionCode 추적
- ✅ 해결책: `is_force_update=true`로 변경 또는 앱 데이터 삭제
- ✅ 트러블슈팅 6️⃣ 항목 추가

### v1.9.0 (2025-11-09 07:40)
- ✅ "No such file or directory" 에러 설명 추가
- ✅ 파일이 없는 경우는 정상임을 명시
- ✅ SharedPreferences 확인 명령 추가

### v1.8.0 (2025-11-09 07:35)
- ✅ 현재 앱 버전코드 반영 (VERSION_CODE = 3)
- ✅ 모든 target_version_code를 6→4로 변경
- ✅ 진단 SQL 스크립트 업데이트 (12-diagnose-update-policy.sql)

### v1.7.0 (2025-11-09 07:30)
- ✅ 트러블슈팅 섹션 추가
- ✅ SharedPreferences 삭제 후에도 팝업이 표시되지 않는 문제 해결 가이드
- ✅ 진단용 SQL 스크립트 생성 (12-diagnose-update-policy.sql)
- ✅ 5가지 체크리스트로 문제 원인 빠르게 파악

### v1.6.0 (2025-11-09 07:25)
- ✅ ADB 명령어에 특정 기기 지정 옵션(`-s`) 추가
- ✅ `adb devices`로 기기 확인 방법 추가
- ✅ 여러 기기 동시 연결 시 대응 방법 명시

### v1.5.0 (2025-11-09 07:20)
- ✅ 버전 증가 테스트 섹션 삭제 (불필요)
- ✅ SharedPreferences 초기화 방법 3가지 상세 설명 추가
  - ADB 명령어 (추천)
  - AVD Device Explorer (GUI)
  - 앱 데이터 삭제 (간단)

### v1.4.0 (2025-11-09 07:15)
- ✅ 모든 SQL에 target_version_code와 download_url 명시적으로 추가
- ✅ Supabase 적용 문제 해결 (필드 명시)

### v1.3.0 (2025-11-09 07:00)
- ✅ message 필드 제거 (release_notes로 통합)
- ✅ download_url NOT NULL 및 기본값 설정 (https://play.google.com/)
- ✅ SQL 스크립트에 download_url 추가

### v1.2.0 (2025-11-09 06:55)
- ✅ target_version_code 예시를 현실적인 숫자로 변경 (999, 1000 → 5, 6, 7)
- ✅ 테스트 시나리오를 실제 앱 버전과 유사하게 수정

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
    release_notes = '• 다크 모드 추가\n• 성능 개선'
    -- download_url은 기본값 사용 (https://play.google.com/)
WHERE app_id = 'com.sweetapps.pocketchord';
```

자세한 내용은 [UPDATE-POLICY-USAGE-GUIDE.md](UPDATE-POLICY-USAGE-GUIDE.md)를 참조하세요.

---

## 🔄 Step 1: 강제 업데이트 테스트

### 1-1. 강제 업데이트 활성화

**의미**: 
- `target_version_code = 4`은 "앱을 버전 4으로 업데이트하라"는 의미입니다
- 현재 앱의 `VERSION_CODE`(예: 3)보다 높은 값으로 설정
- → 앱이 "업데이트가 필요하다"고 판단하여 팝업을 표시합니다

**동작**:
```kotlin
if (currentVersionCode < target_version_code) {
    // 업데이트 팝업 표시
    // 현재 3 < 4 → true → 팝업 표시!
}
```

#### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- 2-1. 강제 업데이트 활성화
UPDATE update_policy 
SET is_active = true,
    target_version_code = 4,  -- 다음 버전 (현재 3보다 높게)
    is_force_update = true,
    release_notes = '• [테스트] 중요 보안 패치\n• [테스트] 필수 기능 추가'
    -- download_url은 기본값 사용 (https://play.google.com/)
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 2-1. 강제 업데이트 활성화
UPDATE update_policy 
SET is_active = true,
    target_version_code = 4,  -- 다음 버전 (현재 3보다 높게)
    is_force_update = true,
    release_notes = '• [DEBUG] 중요 보안 패치\n• [DEBUG] 필수 기능 추가'
    -- download_url은 기본값 사용 (https://play.google.com/)
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
SET target_version_code = 4,  -- 버전 유지 (명시)
    is_force_update = false,
    release_notes = '• [테스트] 다크 모드 추가\n• [테스트] 성능 개선\n• [테스트] UI 업데이트',
    download_url = 'https://play.google.com/'  -- 기본값 명시
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 2-2. 선택적 업데이트로 변경
UPDATE update_policy 
SET target_version_code = 4,  -- 버전 유지 (명시)
    is_force_update = false,
    release_notes = '• [DEBUG] 다크 모드 추가\n• [DEBUG] 성능 개선\n• [DEBUG] UI 업데이트',
    download_url = 'https://play.google.com/'  -- 기본값 명시
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 2-2. SharedPreferences 초기화

**목적**: "나중에" 버튼 클릭 시 저장된 추적 정보를 삭제하여 선택적 업데이트 팝업을 다시 표시합니다.

**왜 필요한가?**:
- 선택적 업데이트에서 "나중에"를 누르면 `dismissed_optional_update_version` 값이 저장됨
- 이 값이 있으면 같은 버전의 팝업이 다시 표시되지 않음
- 테스트를 위해 이 값을 삭제해야 팝업을 다시 볼 수 있음

---

#### 방법 1: ADB 명령어 사용 (추천) 💻

**먼저 연결된 기기 확인**:
```bash
adb devices
```

**결과 예시**:
```
List of devices attached
emulator-5554   device
emulator-5556   device
```

**여러 기기가 있을 때**: `-s` 옵션으로 기기 지정 필요!

---

**릴리즈 버전**:
```bash
# 기기 지정 (emulator-5554 예시)
# 방법 1: 특정 파일만 삭제
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord rm /data/data/com.sweetapps.pocketchord/shared_prefs/update_preferences.xml

# 방법 2: SharedPreferences 전체 삭제 (더 확실함!)
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord rm -r /data/data/com.sweetapps.pocketchord/shared_prefs/
```

**디버그 버전**:
```bash
# 기기 지정 (emulator-5554 예시)
# 방법 1: 특정 파일만 삭제
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm /data/data/com.sweetapps.pocketchord.debug/shared_prefs/update_preferences.xml

# 방법 2: SharedPreferences 전체 삭제 (더 확실함! 추천!) 🔥
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm -r /data/data/com.sweetapps.pocketchord.debug/shared_prefs/
```

**💡 추천**: 선택적 업데이트 테스트 시에는 **방법 2 (전체 삭제)**를 사용하세요!
- 특정 파일만 삭제해도 다른 파일에 추적 정보가 남아있을 수 있습니다
- 디렉토리 전체를 삭제하면 모든 추적 정보가 완전히 삭제됩니다

**💡 팁**: 기기 번호는 `adb devices`로 확인하세요!

**실행 후**: 앱을 재시작하세요 (완전히 종료 후 다시 실행)

---

#### 방법 2: AVD Device Explorer 사용 (GUI) 🖱️

1. **Android Studio에서 Device File Explorer 열기**:
   - `View` → `Tool Windows` → `Device File Explorer` (또는 우측 하단 탭)

2. **SharedPreferences 파일 찾기**:
   ```
   📁 /data
     📁 /data
       📁 /com.sweetapps.pocketchord  (릴리즈 버전)
       또는
       📁 /com.sweetapps.pocketchord.debug  (디버그 버전)
         📁 /shared_prefs
           📄 update_preferences.xml  ← 이 파일을 찾기
   ```

3. **파일 삭제**:
   - `update_preferences.xml` 우클릭
   - `Delete` 선택
   - 확인

4. **앱 재시작**: 완전히 종료 후 다시 실행

---

#### 방법 3: 앱 데이터 삭제 (가장 간단) 🗑️

**AVD 설정에서**:
```
1. AVD 홈 화면 → Settings
2. Apps → PocketChord (또는 PocketChord Debug)
3. Storage & cache
4. Clear storage (또는 Clear data)
5. 확인
```

**주의**: 앱의 모든 데이터가 삭제됩니다!

---

#### 삭제 확인 방법 ✅

**Logcat에서 확인**:
```
Filter: tag:HomeScreen

예상 로그:
"No dismissed version found"  ← SharedPreferences가 비어있음을 의미
"Decision: OPTIONAL UPDATE from update_policy"  ← 팝업 다시 표시
```

---

**다음**: 앱을 재시작하고 Step 2-3으로 이동

---

## 🧹 Step 3: Update 정리

### 3-1. 원래대로 복구

**의미**: 
- `target_version_code = 3`로 복구 (현재 앱 버전과 같게)
- → 앱이 "업데이트가 필요 없다"고 판단
- → 업데이트 팝업이 더 이상 표시되지 않음

**동작**:
```kotlin
if (currentVersionCode < target_version_code) {
    // 팝업 표시
    // 현재 3 < 3 → false → 팝업 표시 안 됨 ✅
}
```

#### SQL 스크립트 - 릴리즈 버전 ⭐

```sql
-- 2-3. Update 정리 (원래대로)
UPDATE update_policy 
SET target_version_code = 3,  -- 현재 버전과 같게 설정 (업데이트 불필요)
    download_url = 'https://play.google.com/'  -- 기본값 유지
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### SQL 스크립트 - 디버그 버전 🔧

```sql
-- 2-3. Update 정리 (원래대로)
UPDATE update_policy 
SET target_version_code = 3,  -- 현재 버전과 같게 설정 (업데이트 불필요)
    download_url = 'https://play.google.com/'  -- 기본값 유지
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

- [ ] ✅ 정리 완료

---

## ✅ Phase 2 완료 체크리스트

- [ ] 강제 업데이트 테스트 완료
- [ ] 선택적 업데이트 테스트 완료
- [ ] "나중에" 추적 확인 완료
- [ ] Update 정리 완료
- [ ] 모든 로그 확인 완료

---

## 🔜 다음 단계

**Phase 3**으로 이동하세요!
- Phase 3: Notice 테스트 (버전 관리)

---

## 🔧 트러블슈팅

### ❓ "No such file or directory" 에러가 나요

```bash
rm: /data/data/.../update_preferences.xml: No such file or directory
```

**이것은 정상입니다!** ✅

**의미**: 삭제하려는 파일이 없습니다.

**원인**:
1. ✅ **정상**: 아직 "나중에" 버튼을 누른 적이 없음
2. ✅ **정상**: 앱을 처음 설치함
3. ✅ **정상**: 이미 이전에 삭제함

**해결**: 
- **아무것도 하지 않아도 됩니다!**
- 파일이 없다 = 추적 정보가 없다 = 팝업이 정상 표시됨

**확인 방법**:
```bash
# SharedPreferences 디렉토리 확인
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug ls /data/data/com.sweetapps.pocketchord.debug/shared_prefs/

# 결과가 비어있으면 정상!
```

---

### ❓ SharedPreferences를 삭제했는데도 업데이트 팝업이 표시되지 않아요

**원인 확인용 SQL**: `docs/sql/12-diagnose-update-policy.sql` 실행

```sql
-- 1. 현재 상태 확인
SELECT app_id, is_active, target_version_code, is_force_update
FROM public.update_policy
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**체크리스트**:

#### 1️⃣ is_active = true 인가요?
```
❌ is_active = false
   → 팝업이 표시되지 않습니다!
   
✅ 해결 방법:
UPDATE public.update_policy
SET is_active = true
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 2️⃣ target_version_code가 4 이상인가요?
```
❌ target_version_code = 3 (또는 그 이하)
   → 현재 앱 버전(3)보다 높아야 팝업 표시!
   
✅ 해결 방법:
UPDATE public.update_policy
SET target_version_code = 4
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 3️⃣ 앱을 완전히 재시작했나요?
```
❌ 백그라운드에서 실행 중
   → 완전히 종료 후 재시작 필요!
   
✅ 해결 방법:
1. 앱 완전 종료 (최근 앱 목록에서 스와이프)
2. 다시 실행
```

#### 4️⃣ Logcat에서 로그 확인
```bash
Filter: tag:HomeScreen

예상 로그:
✅ "Phase 2: Trying update_policy"
✅ "✅ update_policy found: targetVersion=4, isForce=true"
✅ "Decision: FORCE UPDATE from update_policy (target=4)"

문제 로그:
❌ "⚠️ update_policy not found or error: ..."
   → Supabase 연결 문제 또는 데이터 없음
   
❌ "update_policy exists but no update needed (current=3 >= target=3)"
   → target_version_code가 너무 낮음
```

#### 5️⃣ 확실하게 재설정하기

**선택적 업데이트를 테스트하고 싶다면**:
```sql
-- 선택적 업데이트로 재설정
UPDATE public.update_policy
SET is_active = true,
    target_version_code = 4,
    is_force_update = false,  -- 선택적 업데이트!
    release_notes = '• [DEBUG] 선택적 테스트',
    download_url = 'https://play.google.com/'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**실행 후**:
1. **SharedPreferences 전체 삭제** (필수!):
   ```bash
   adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm -r /data/data/com.sweetapps.pocketchord.debug/shared_prefs/
   ```
2. 앱 완전 종료
3. 앱 재시작
4. Logcat 확인 → 선택적 업데이트 팝업 표시 ✅

---

**강제 업데이트를 테스트하고 싶다면**:
```sql
-- 강제 업데이트로 재설정
UPDATE public.update_policy
SET is_active = true,
    target_version_code = 4,
    is_force_update = true,  -- 강제 업데이트!
    release_notes = '• [DEBUG] 강제 테스트',
    download_url = 'https://play.google.com/'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

**실행 후**:
1. 앱 완전 종료
2. 앱 재시작 (SharedPreferences 삭제 불필요!)
3. Logcat 확인 → 강제 업데이트 팝업 표시 ✅

---

#### 6️⃣ 선택적 업데이트(is_force_update=false)인데 팝업이 안 나와요
```
Logcat: "update_policy exists but no update needed (current=3 >= target=4)"
```

**원인**: 
- 선택적 업데이트는 `dismissedVersionCode`를 추적합니다
- 이전에 "나중에"를 눌렀다면 같은 버전의 팝업은 표시되지 않습니다
- `update_preferences.xml` 파일만 삭제해도 다른 SharedPreferences에 저장되어 있을 수 있습니다

**해결 방법**:

**방법 A: SharedPreferences 디렉토리 전체 삭제** (추천) 🔥:
```bash
# 모든 SharedPreferences 삭제 (디버그)
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm -r /data/data/com.sweetapps.pocketchord.debug/shared_prefs/

# 모든 SharedPreferences 삭제 (릴리즈)
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord rm -r /data/data/com.sweetapps.pocketchord/shared_prefs/
```

**실행 후**:
1. 앱 완전 종료
2. 앱 재시작
3. 선택적 업데이트 팝업이 표시됨! ✅

---

**방법 B: 앱 데이터 완전 삭제** (가장 확실):
```
AVD Settings → Apps → PocketChord Debug → Storage → Clear data
```

---

**방법 C: 어떤 파일이 있는지 먼저 확인**:
```bash
# SharedPreferences 디렉토리 내용 확인
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug ls -la /data/data/com.sweetapps.pocketchord.debug/shared_prefs/

# 결과 예시:
# update_preferences.xml         ← 업데이트 추적 파일
# dismissed_announcements.xml    ← 공지 추적 파일
# ... 등등
```

**💡 왜 update_preferences.xml만 삭제해도 안 되나요?**

선택적 업데이트의 dismissedVersionCode는 다른 파일에도 저장될 수 있습니다:
- HomeScreen에서 여러 SharedPreferences 파일을 사용할 수 있음
- 캐시 문제로 인해 삭제가 제대로 반영되지 않을 수 있음

**확실한 방법**: SharedPreferences 디렉토리 전체를 삭제(`rm -r`)하면 모든 추적 정보가 완전히 삭제됩니다.

---

**Phase 2 완료!** 🎉

---

**문서 버전**: v2.2.0  
**마지막 수정**: 2025-11-09 08:30 KST

