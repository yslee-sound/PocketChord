# Update Policy 사용 가이드

**버전**: v1.0.1  
**최종 업데이트**: 2025-11-09 06:35 KST  
**작성일**: 2025-11-09  
**목적**: update_policy의 실제 사용법과 target_version_code 운영 가이드  
**상태**: ✅ 완료

---

## 📝 변경 이력

### v1.0.1 (2025-11-09 06:35)
- ✅ 문서 업데이트 (emergency_policy button_text 관련 정보 동기화)

### v1.0.0 (2025-11-09)
- ✅ 최초 작성
- ✅ target_version_code 실제 사용법 정리
- ✅ Play Store versionCode와의 동기화 방법 설명
- ✅ 추적 메커니즘 상세 설명
- ✅ 테스트용 숫자(999, 1000) 관련 내용 제거

---

## 🎯 핵심 개념

### target_version_code란?

**Play Store에 올릴 다음 버전의 versionCode입니다.**

```
현재 앱: versionCode = 10
다음 업데이트: versionCode = 11

→ target_version_code = 11 로 설정
→ 버전 10 사용자에게 "업데이트하세요" 팝업 표시
```

---

## 📋 실제 사용 시나리오

### 시나리오 1: 새 버전 출시 (정상적인 흐름)

#### Step 1: 새 버전 빌드
```
현재 Play Store: versionCode = 10
새 APK 빌드: versionCode = 11
```

#### Step 2: Supabase 설정 (Play Store 출시 전)
```sql
-- 선택적 업데이트 (권장)
UPDATE update_policy 
SET target_version_code = 11,  -- 다음 버전!
    is_force_update = false,
    message = '새로운 기능이 추가되었습니다',
    release_notes = '• 다크 모드 추가\n• 성능 개선'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 3: Play Store 출시
```
APK (versionCode=11) 업로드 → 출시
```

#### Step 4: 사용자 반응
```
버전 10 사용자:
→ 앱 실행
→ "새로운 기능이 추가되었습니다" 팝업 표시
→ "지금 업데이트" 또는 "나중에" 선택 가능
```

---

### 시나리오 2: 긴급 보안 패치 (강제 업데이트)

#### Step 1: 긴급 패치 빌드
```
현재 Play Store: versionCode = 10
긴급 패치 APK: versionCode = 11
```

#### Step 2: Supabase 설정 (강제)
```sql
-- 강제 업데이트
UPDATE update_policy 
SET target_version_code = 11,
    is_force_update = true,  -- ⭐ 강제!
    message = '필수 보안 업데이트가 있습니다',
    release_notes = '• 중요 보안 패치\n• 필수 업데이트'
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### Step 3: Play Store 출시

#### Step 4: 사용자 반응
```
버전 10 사용자:
→ 앱 실행
→ "필수 보안 업데이트가 있습니다" 팝업 표시
→ "지금 업데이트" 버튼만 있음 (나중에 없음!)
→ 반드시 업데이트해야 앱 사용 가능
```

---

## ⚠️ 중요: 추적 메커니즘

### "나중에" 클릭 시 저장되는 것

```kotlin
// 사용자가 "나중에" 클릭
SharedPreferences에 저장:
dismissed_version_code = 11  // ← 이 버전은 다시 안 보여줌!
```

### 다음 실행 시 판단

```kotlin
if (dismissed_version_code != target_version_code) {
    // 팝업 표시
}

예시:
dismissed = 11, target = 11 → false → 표시 안 됨 ✅
dismissed = 11, target = 12 → true → 표시됨! ⭐
dismissed = 11, target = 10 → true이지만... 10 < 11 → 표시 안 됨 ❌
```

---

## 🔄 버전 관리 전략

### 올바른 방법 ✅

#### 버전 12 출시
```sql
UPDATE update_policy 
SET target_version_code = 12
WHERE app_id = 'com.sweetapps.pocketchord';
```

#### 버전 13 출시
```sql
UPDATE update_policy 
SET target_version_code = 13  -- 계속 증가!
WHERE app_id = 'com.sweetapps.pocketchord';
```

**결과**: 
- 버전 11 사용자가 12를 "나중에" 눌렀어도
- 버전 13이 나오면 다시 팝업 표시됨 ✅

---

### 잘못된 방법 ❌

#### 버전을 낮춤
```sql
-- 버전 12 설정 후 다시 11로 되돌림
UPDATE update_policy 
SET target_version_code = 11  -- ❌ 낮춤!
WHERE app_id = 'com.sweetapps.pocketchord';
```

**문제**:
- 사용자가 버전 12를 "나중에" 눌렀다면
- dismissed_version_code = 12로 저장됨
- target = 11 < 12 → 팝업 절대 표시 안 됨! ❌

---

## 📊 실전 운영 가이드

### Play Store versionCode와 동기화

```
build.gradle.kts:
versionCode = 15

Supabase update_policy:
target_version_code = 15

→ 완벽하게 일치시키기!
```

### 업데이트 주기

```
1월: versionCode = 10 → target = 10 (업데이트 없음)
2월: versionCode = 11 출시 → target = 11 (업데이트 안내)
3월: versionCode = 12 출시 → target = 12 (업데이트 안내)
4월: versionCode = 13 출시 → target = 13 (업데이트 안내)
```

**핵심**: 
- ✅ Play Store versionCode와 항상 일치
- ✅ 항상 증가만 시킴
- ❌ 절대 낮추지 않음

---


## 🎯 실전 예시

### 예시 1: 정상적인 업데이트

```
현재 Play Store: versionCode = 14
사용자 앱: versionCode = 13

Supabase:
target_version_code = 14
is_force_update = false

결과:
→ "새 버전이 있습니다" 팝업 표시
→ 사용자가 "나중에" 가능
```

### 예시 2: 긴급 업데이트

```
현재 Play Store: versionCode = 15 (긴급 패치)
사용자 앱: versionCode = 13, 14

Supabase:
target_version_code = 15
is_force_update = true

결과:
→ "필수 업데이트" 팝업 표시
→ "나중에" 버튼 없음
→ 반드시 업데이트해야 사용 가능
```

### 예시 3: 업데이트 불필요

```
현재 Play Store: versionCode = 15
사용자 앱: versionCode = 15 (최신)

Supabase:
target_version_code = 15

결과:
→ 15 < 15 → false
→ 팝업 표시 안 됨 ✅
```

---

## ✅ 체크리스트

### 새 버전 출시 시

- [ ] 새 APK 빌드 (versionCode 증가)
- [ ] Supabase target_version_code 업데이트 (같은 숫자로)
- [ ] is_force_update 설정 (강제 or 선택)
- [ ] message, release_notes 작성
- [ ] Play Store 출시
- [ ] 이전 버전 사용자 테스트

### 주의사항

- [ ] ✅ target_version_code는 항상 증가만!
- [ ] ✅ Play Store versionCode와 일치!
- [ ] ❌ 절대 낮추지 않기!

---

**문서 버전**: v1.0.1  
**마지막 수정**: 2025-11-09 06:35 KST  
**작성자**: GitHub Copilot
