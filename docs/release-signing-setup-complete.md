# ✅ 릴리즈 서명 설정 완료!

## 🎯 완료된 작업

1. **build.gradle.kts 서명 설정 추가**
   - 환경변수 기반 서명 config
   - Release 빌드에 서명 자동 적용

2. **a_RELEASE_SIGNING.md 문서 업데이트**
   - PocketChord 앱에 맞게 경로 및 alias 수정

---

## 📝 서명 설정 확인

### signingReport 결과

**이전 (서명 없음)**:
```
Variant: release
Config: null      ← ❌ 서명 설정 없음
Store: null
Alias: null
```

**현재 (환경변수 기반)**:
```
환경변수 없으면 경고 메시지 출력
환경변수 있으면 서명 적용
```

---

## 🔑 릴리즈 빌드 절차

### 1단계: 키스토어 생성 (최초 1회)

```powershell
# PowerShell 실행
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore "G:\secure\PocketChord\pocketchord-key.jks" -alias pocketchord-alias -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -validity 36500
```

**입력 정보**:
- 키스토어 비밀번호
- 키 비밀번호  
- 이름 (CN)
- 조직 (O)
- 도시 (L)
- 국가 코드 (C): KR

### 2단계: 환경변수 설정

```powershell
# PowerShell에서 실행 (비밀번호는 실제 값으로 변경)
$env:KEYSTORE_PATH="G:/secure/PocketChord/pocketchord-key.jks"
$env:KEYSTORE_STORE_PW="your_keystore_password"
$env:KEY_ALIAS="pocketchord-alias"
$env:KEY_PASSWORD="your_key_password"
```

### 3단계: 서명 확인

```powershell
# 프로젝트 디렉터리로 이동
G:
cd G:\Workspace\PocketChord

# 서명 리포트 확인
.\gradlew.bat :app:signingReport
```

**예상 출력**:
```
Variant: release
Config: release
Store: G:\secure\PocketChord\pocketchord-key.jks
Alias: pocketchord-alias
SHA1: XX:XX:...
SHA-256: YY:YY:...
Valid until: ...
```

### 4단계: 릴리즈 빌드

```powershell
# 클린 빌드 + AAB 생성
.\gradlew.bat clean :app:bundleRelease

# 출력 위치
# app\build\outputs\bundle\release\app-release.aab
```

---

## ⚠️ 환경변수 없이 빌드 시

환경변수가 설정되지 않으면 **경고 메시지** 출력:

```
⚠️ WARNING: Release signing config missing!
Required environment variables:
  - KEYSTORE_PATH
  - KEYSTORE_STORE_PW
  - KEY_ALIAS
  - KEY_PASSWORD
```

이 상태로 빌드하면 **서명되지 않은 APK/AAB**가 생성됩니다.

---

## 🔍 트러블슈팅

### 문제 1: signingReport에 release 정보 안 나옴

**증상**:
```
Variant: release
Config: null
```

**원인**: 구성 캐시 문제

**해결**:
```powershell
.\gradlew.bat --stop
.\gradlew.bat purgeConfigCache
.\gradlew.bat --no-configuration-cache :app:signingReport
```

### 문제 2: 환경변수가 적용 안 됨

**확인**:
```powershell
# PowerShell에서 환경변수 확인
echo $env:KEYSTORE_PATH
echo $env:KEY_ALIAS
```

**해결**: 
- PowerShell에서 `$env:변수명="값"` 형식으로 재설정
- 따옴표 포함 여부 확인

### 문제 3: 키스토어 파일을 찾을 수 없음

**증상**:
```
java.io.FileNotFoundException: G:\secure\PocketChord\pocketchord-key.jks
```

**해결**:
- 경로 확인 (백슬래시 vs 슬래시)
- 파일 존재 여부 확인
- PowerShell: `Test-Path "G:\secure\PocketChord\pocketchord-key.jks"`

---

## 📋 체크리스트

### 빌드 전
- [ ] versionCode 증가
- [ ] versionName 업데이트
- [ ] 키스토어 파일 존재 확인
- [ ] 환경변수 4개 설정 확인
- [ ] signingReport 실행하여 서명 정보 확인

### 빌드
- [ ] `.\gradlew.bat clean :app:bundleRelease` 실행
- [ ] 빌드 성공 확인
- [ ] AAB 파일 생성 확인

### 빌드 후
- [ ] AAB 크기 확인 (정상 범위)
- [ ] 파일명 표준화 (`pocketchord-v1.0.0-1.aab`)
- [ ] mapping.txt 백업
- [ ] 테스트 기기에서 설치 테스트

---

## 🔐 보안 주의사항

### 절대 Git에 커밋하지 말 것
- ❌ `pocketchord-key.jks`
- ❌ 비밀번호가 포함된 파일
- ✅ `.gitignore`에 이미 포함됨

### 백업 필수
- 💾 키스토어 파일 2곳 이상 백업
- 💾 비밀번호 안전한 곳에 기록
- 💾 SHA-256 fingerprint 기록

### 비밀번호 관리
- 🔑 복잡한 비밀번호 사용
- 🔑 공용 채팅에 공유 금지
- 🔑 비밀번호 관리자 사용 권장

---

## 📚 참고 문서

- **a_RELEASE_SIGNING.md**: 전체 릴리즈 서명 가이드
- **release-build-guide.md**: 릴리즈 빌드 기본 가이드

---

## ✅ 다음 단계

1. **키스토어 생성** (아직 안 했다면)
2. **환경변수 설정**
3. **signingReport 실행**하여 서명 확인
4. **릴리즈 빌드** 실행
5. **Google Play Console 업로드**

---

**완료!** 이제 서명된 릴리즈 빌드를 생성할 수 있습니다! 🎉

