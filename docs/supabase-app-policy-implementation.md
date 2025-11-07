# Supabase 앱 정책 연동 구현 완료 보고서

## 📋 작업 요약

Supabase 기반 앱 정책 관리 시스템을 Android 앱에 성공적으로 연동했습니다.

**작업 일시**: 2025년 11월 8일  
**프로젝트**: PocketChord  
**문서 참조**: `docs/supabase-app-policy-hybrid.md`

---

## ✅ 완료된 작업

### 1. 데이터 모델 업데이트 ✅
**파일**: `app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/AppPolicy.kt`

- 기존 복잡한 구조를 하이브리드 방식으로 단순화
- `active_popup_type` ENUM 기반 설계로 변경
- 4가지 팝업 타입 지원:
  - `emergency`: 긴급 공지
  - `force_update`: 강제 업데이트
  - `optional_update`: 선택적 업데이트
  - `notice`: 일반 공지
  - `none`: 팝업 없음
- 헬퍼 메서드 추가:
  - `requiresForceUpdate(currentVersionCode)`: 강제 업데이트 필요 여부
  - `recommendsUpdate(currentVersionCode)`: 선택적 업데이트 권장 여부

### 2. Repository 업데이트 ✅
**파일**: `app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/AppPolicyRepository.kt`

- RLS(Row Level Security) 정책 적용
- `is_active = TRUE`인 정책만 자동으로 조회됨
- 깔끔한 로깅으로 디버깅 용이성 향상

### 3. 팝업 UI 구현 ✅
**파일**: `app/src/main/java/com/sweetapps/pocketchord/ui/dialog/AppPolicyDialogs.kt`

#### 긴급 공지 다이얼로그
```kotlin
EmergencyDialog(policy, onDismiss)
```
- ❌ X 버튼 없음
- ❌ 뒤로가기/외부 터치로 닫기 불가
- ✅ URL 이동 버튼만 제공

#### 강제 업데이트 다이얼로그
```kotlin
ForceUpdateDialog(policy)
```
- ❌ X 버튼 없음
- ❌ 뒤로가기/외부 터치로 닫기 불가
- ✅ 업데이트 버튼 클릭 시 스토어 이동 후 앱 종료
- ✅ 버전 체크: `currentVersion < min_supported_version`

#### 선택적 업데이트 다이얼로그
```kotlin
OptionalUpdateDialog(policy, onDismiss)
```
- ✅ X 버튼 있음
- ✅ 뒤로가기/외부 터치로 닫기 가능
- ✅ "지금 업데이트" + "나중에" 버튼
- ✅ 버전 체크: `currentVersion < latest_version_code`

#### 일반 공지 다이얼로그
```kotlin
NoticeDialog(policy, onDismiss)
```
- ✅ X 버튼 있음
- ✅ 뒤로가기/외부 터치로 닫기 가능
- ✅ 확인 버튼

### 4. MainActivity 연동 ✅
**파일**: `app/src/main/java/com/sweetapps/pocketchord/MainActivity.kt`

- 앱 시작 시 정책 자동 조회
- Supabase 미설정 시 안전하게 스킵
- 버전 체크 로직 구현
- 타입별 팝업 자동 분기 처리
- 상세 로깅으로 디버깅 지원

---

## 🎯 작동 원리

### 1. 앱 시작 흐름

```
MainActivity.onCreate()
    ↓
setContent (Compose 시작)
    ↓
LaunchedEffect(Unit) - 정책 체크
    ↓
AppPolicyRepository.getPolicy()
    ↓
Supabase 조회 (RLS 적용)
    ↓
정책 타입 확인
    ↓
팝업 표시 (조건에 따라)
```

### 2. 정책 타입별 로직

#### Emergency (긴급 공지)
```kotlin
when (policy.activePopupType) {
    "emergency" -> {
        // 즉시 표시 (조건 없음)
        showEmergencyDialog()
    }
}
```

#### Force Update (강제 업데이트)
```kotlin
"force_update" -> {
    if (currentVersion < policy.minSupportedVersion) {
        // 현재 버전이 최소 지원 버전보다 낮으면 강제 업데이트
        showForceUpdateDialog()
    }
}
```

#### Optional Update (선택적 업데이트)
```kotlin
"optional_update" -> {
    if (currentVersion < policy.latestVersionCode) {
        // 현재 버전이 최신 버전보다 낮으면 권장
        showOptionalUpdateDialog()
    }
}
```

#### Notice (일반 공지)
```kotlin
"notice" -> {
    // 즉시 표시 (조건 없음)
    showNoticeDialog()
}
```

---

## 🔒 보안 (RLS)

### Supabase RLS 정책
```sql
CREATE POLICY "allow_read_policy"
ON public.app_policy
FOR SELECT
USING (is_active = TRUE);
```

**효과**:
- ✅ `is_active = FALSE`인 준비 중 설정은 클라이언트에서 **절대 조회 불가**
- ✅ 데이터베이스 레벨에서 보안 보장
- ✅ 앱 코드 실수로 인한 노출 방지

---

## 📝 사용 방법

### 1. Supabase에서 정책 설정

#### 긴급 공지 활성화
```sql
UPDATE app_policy SET
  is_active = TRUE,
  active_popup_type = 'emergency',
  content = '서버 긴급 점검 중입니다. 15:00까지 완료 예정입니다.',
  download_url = 'https://status.example.com'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 강제 업데이트 설정
```sql
UPDATE app_policy SET
  is_active = TRUE,
  active_popup_type = 'force_update',
  content = DEFAULT,  -- 기본 메시지 사용
  download_url = 'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord',
  min_supported_version = 5  -- 버전 5 미만은 강제 업데이트
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 선택적 업데이트 설정
```sql
UPDATE app_policy SET
  is_active = TRUE,
  active_popup_type = 'optional_update',
  content = '새로운 기능이 추가되었습니다. 업데이트를 권장합니다.',
  download_url = 'market://details?id=com.sweetapps.pocketchord',
  latest_version_code = 6  -- 버전 6 미만에게 권장
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 팝업 끄기
```sql
UPDATE app_policy SET
  is_active = FALSE,
  active_popup_type = 'none'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 2. 테스트 방법

#### 로컬 테스트
1. Supabase 설정 확인
   - `local.properties`에 `SUPABASE_URL`, `SUPABASE_ANON_KEY` 설정
   - 또는 환경변수로 설정
   
2. 앱 빌드 및 실행
   ```cmd
   gradlew assembleDebug
   ```

3. 로그 확인
   ```
   adb logcat | findstr MainActivity
   adb logcat | findstr AppPolicyRepo
   ```

#### 버전별 테스트
1. **현재 버전**: `BuildConfig.VERSION_CODE = 2`
2. **강제 업데이트 테스트**: `min_supported_version = 3` 설정 → 팝업 표시됨
3. **선택적 업데이트 테스트**: `latest_version_code = 3` 설정 → 팝업 표시됨

---

## 🔧 문제 해결

### Supabase 미설정 경고
```
W/PocketChordApp: Supabase 미설정: 환경변수 SUPABASE_URL / SUPABASE_ANON_KEY 를 확인하세요
W/MainActivity: Supabase 미설정: 정책 체크 스킵
```

**해결 방법**:
1. `local.properties` 파일에 추가:
   ```properties
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key
   ```

2. 또는 환경변수 설정:
   ```cmd
   set SUPABASE_URL=https://your-project.supabase.co
   set SUPABASE_ANON_KEY=your-anon-key
   ```

### RLS 정책으로 인한 빈 결과
```
D/AppPolicyRepo: No active policy found for app_id=com.sweetapps.pocketchord.debug (RLS may be filtering)
```

**원인**:
- `is_active = FALSE`로 설정되어 있음
- 또는 정책이 아직 생성되지 않음

**해결 방법**:
```sql
-- 정책 확인 (SQL Editor에서 실행)
SELECT app_id, is_active, active_popup_type 
FROM app_policy 
WHERE app_id = 'com.sweetapps.pocketchord.debug';

-- 활성화
UPDATE app_policy SET is_active = TRUE
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

### 팝업이 표시되지 않음
1. 로그 확인:
   ```
   adb logcat | findstr "MainActivity\|AppPolicyRepo"
   ```

2. 버전 체크 로직 확인:
   - `force_update`: `currentVersion < minSupportedVersion`
   - `optional_update`: `currentVersion < latestVersionCode`
   - 현재 버전이 조건을 만족하지 않으면 팝업이 표시되지 않음

---

## 📊 구현 상태

| 항목 | 상태 | 비고 |
|------|------|------|
| 데이터 모델 | ✅ 완료 | AppPolicy.kt |
| Repository | ✅ 완료 | AppPolicyRepository.kt |
| 긴급 공지 UI | ✅ 완료 | EmergencyDialog |
| 강제 업데이트 UI | ✅ 완료 | ForceUpdateDialog |
| 선택적 업데이트 UI | ✅ 완료 | OptionalUpdateDialog |
| 일반 공지 UI | ✅ 완료 | NoticeDialog |
| MainActivity 연동 | ✅ 완료 | 정책 체크 로직 |
| RLS 보안 | ✅ 완료 | Supabase 설정 완료 |
| 버전 체크 | ✅ 완료 | force/optional 분리 |
| 에러 처리 | ✅ 완료 | try-catch + 로깅 |

---

## 🎓 주요 설계 결정

### 1. 하이브리드 방식 선택
- **운영 테이블** + **히스토리 테이블** 분리
- 운영 테이블은 항상 1개 레코드만 유지 (UPDATE만 사용)
- 히스토리는 자동으로 트리거가 저장 (변경 추적)

### 2. RLS 활용
- 클라이언트는 `is_active = TRUE`인 정책만 조회 가능
- 준비 중 설정은 데이터베이스 레벨에서 보호
- 관리자는 Supabase 대시보드에서 모든 데이터 확인 가능

### 3. 선택적 필드 철학
- `force_update`는 `min_supported_version`만 사용
- `optional_update`는 `latest_version_code`만 사용
- 다른 필드에 값이 있어도 앱이 무시 (안전성 보장)

### 4. 타입별 UI 특성
- **Emergency**: 강제성 최고 (X 버튼 없음)
- **Force Update**: 앱 사용 차단 (업데이트 필수)
- **Optional Update**: 사용자 선택권 보장 (나중에 가능)
- **Notice**: 단순 정보 전달

---

## 📚 참고 문서

- **메인 문서**: `docs/supabase-app-policy-hybrid.md`
- **다음 단계**: `docs/supabase-app-policy-next-step.md`

---

## 🚀 다음 단계 (선택)

### 1. SharedPreferences 캐싱 (선택)
- 정책을 로컬에 캐싱하여 오프라인에서도 작동
- 특히 `force_update`는 캐싱 권장

### 2. 팝업 노출 횟수 제한 (선택)
- `optional_update`나 `notice`는 하루 1회만 표시
- SharedPreferences로 마지막 표시 시간 저장

### 3. 딥링크 지원 (선택)
- `download_url`에 커스텀 URL 스킴 지원
- 예: `pocketchord://settings`로 앱 내 특정 화면 이동

### 4. A/B 테스팅 (선택)
- 사용자 그룹별로 다른 메시지 테스트
- Supabase Functions로 사용자 ID 기반 분기

---

## ✅ 체크리스트

프로덕션 배포 전 확인사항:

- [ ] Supabase 프로덕션 URL/Key 설정 확인
- [ ] RLS 정책 활성화 확인
- [ ] 초기 정책 데이터 생성 (`is_active = FALSE, active_popup_type = 'none'`)
- [ ] 테스트 계정으로 각 팝업 타입 테스트
- [ ] 버전별 팝업 표시 조건 테스트
- [ ] 로깅 확인 (민감 정보 제거)
- [ ] ProGuard 규칙 확인 (모델 클래스 keep)

---

## 🎉 완료!

모든 구현이 완료되었습니다. 이제 Supabase 대시보드에서 정책을 설정하고 앱에서 즉시 반영되는 것을 확인할 수 있습니다!

**문의사항이나 추가 작업이 필요하면 언제든지 알려주세요.**

