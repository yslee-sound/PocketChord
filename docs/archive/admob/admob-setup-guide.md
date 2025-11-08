# AdMob 광고 ID 설정 가이드

## ⚠️ 중요: 실제 광고 ID 설정 필수!

현재 앱은 **테스트 광고 ID**를 사용하고 있습니다. Play Store에 출시하기 전에 반드시 **실제 AdMob 광고 ID**로 교체해야 합니다!

---

## 1. AdMob 계정 생성 및 앱 등록

### 1.1 AdMob 계정 생성
1. https://admob.google.com/ 접속
2. Google 계정으로 로그인
3. 새 AdMob 계정 생성

### 1.2 앱 등록
1. AdMob 대시보드에서 **"앱" > "앱 추가"** 클릭
2. 플랫폼: **Android** 선택
3. 앱 이름: **PocketChord** 입력
4. 패키지명: `com.sweetapps.pocketchord` 확인
5. 앱 추가 완료

---

## 2. 광고 단위 생성

AdMob에서 3가지 광고 단위를 생성해야 합니다:

### 2.1 배너 광고 (Banner Ad)
1. **"광고 단위" > "광고 단위 추가"** 클릭
2. 광고 형식: **배너** 선택
3. 광고 단위 이름: `PocketChord Banner`
4. 생성 완료 후 **광고 단위 ID** 복사
   - 형식: `ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY`

### 2.2 전면 광고 (Interstitial Ad)
1. **"광고 단위 추가"** 클릭
2. 광고 형식: **전면 광고** 선택
3. 광고 단위 이름: `PocketChord Interstitial`
4. 생성 완료 후 **광고 단위 ID** 복사

### 2.3 앱 오픈 광고 (App Open Ad)
1. **"광고 단위 추가"** 클릭
2. 광고 형식: **앱 오픈 광고** 선택
3. 광고 단위 이름: `PocketChord App Open`
4. 생성 완료 후 **광고 단위 ID** 복사

---

## 3. 광고 ID 설정 방법

### 방법 1: local.properties 사용 (권장)

`local.properties` 파일에 광고 ID 추가:

```properties
# AdMob 광고 단위 ID
BANNER_AD_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
INTERSTITIAL_AD_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
APP_OPEN_AD_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/WWWWWWWWWW
```

**장점:**
- Git에 포함되지 않아 보안에 안전
- 로컬 개발 환경에서만 사용

### 방법 2: 환경변수 사용 (CI/CD)

PowerShell에서 환경변수 설정:

```powershell
$env:BANNER_AD_UNIT_ID="ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
$env:INTERSTITIAL_AD_UNIT_ID="ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ"
$env:APP_OPEN_AD_UNIT_ID="ca-app-pub-XXXXXXXXXXXXXXXX/WWWWWWWWWW"
```

**장점:**
- CI/CD 파이프라인에서 사용하기 좋음
- GitHub Actions 등에서 Secrets로 관리 가능

---

## 4. 빌드 및 테스트

### 4.1 릴리즈 빌드 생성

```powershell
# 1. local.properties 또는 환경변수에 실제 광고 ID 설정
# 2. 릴리즈 빌드 생성
.\gradlew.bat clean :app:bundleRelease
```

### 4.2 실제 광고 확인

1. 릴리즈 APK를 실제 기기에 설치
2. 앱 실행 후 광고 영역 확인
3. **주의:** 본인의 광고를 반복적으로 클릭하지 마세요 (계정 정지 위험)

---

## 5. 현재 상태

### Debug 빌드
- ✅ 테스트 광고 ID 사용 (Google 제공)
- ✅ 개발 중 광고 테스트 가능

### Release 빌드
- ⚠️ **현재: 테스트 광고 ID 사용 중**
- ⚠️ **필요: local.properties 또는 환경변수에 실제 광고 ID 설정**
- ⚠️ **미설정 시: 광고는 표시되지만 수익 발생 안 함!**

---

## 6. 출시 전 체크리스트

### 필수 확인 사항

- [ ] AdMob 계정 생성 완료
- [ ] 앱 등록 완료 (패키지명: `com.sweetapps.pocketchord`)
- [ ] 배너 광고 단위 생성 및 ID 복사
- [ ] 전면 광고 단위 생성 및 ID 복사
- [ ] 앱 오픈 광고 단위 생성 및 ID 복사
- [ ] `local.properties`에 실제 광고 ID 설정
- [ ] 릴리즈 빌드로 실제 광고 표시 확인
- [ ] 광고가 정상적으로 로드되는지 확인 (Logcat)

### 빌드 설정 확인

```kotlin
// app/build.gradle.kts의 release 블록에서:
buildConfigField("String", "BANNER_AD_UNIT_ID", "\"${bannerAdId}\"")
buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"${interstitialAdId}\"")
buildConfigField("String", "APP_OPEN_AD_UNIT_ID", "\"${appOpenAdId}\"")
```

---

## 7. 주의사항

### 🚨 절대 하지 말아야 할 것

1. **본인의 광고를 반복적으로 클릭**
   - AdMob 정책 위반: 계정 정지 위험
   - 테스트는 테스트 광고 ID 사용

2. **테스트 광고 ID로 출시**
   - 광고는 표시되지만 수익 발생 안 함
   - 반드시 실제 광고 ID로 교체

3. **광고 ID를 Git에 커밋**
   - `local.properties`는 `.gitignore`에 포함됨
   - 실수로 공개 저장소에 올리지 않도록 주의

### ✅ 권장사항

1. **테스트는 Debug 빌드로**
   - 테스트 광고 ID 사용
   - 원하는 만큼 클릭 가능

2. **실제 광고는 Release 빌드로**
   - 실제 광고 ID 사용
   - 실제 사용자에게만 표시

3. **광고 정책 준수**
   - 우발적 클릭 유도 금지
   - 충분한 여백과 명확한 구분
   - 필수 콘텐츠 위에 광고 배치 금지

---

## 8. 질문과 답변

### Q: 릴리즈 빌드에서 테스트 광고가 나오면 출시 후에도 정상 작동하나요?

**A: 아니요!** 테스트 광고 ID는 Google이 제공하는 더미 광고입니다.

- ❌ 테스트 광고: 광고는 표시되지만 **수익 발생 안 함**
- ✅ 실제 광고: 광고 표시 + **실제 수익 발생**

### Q: local.properties에 광고 ID를 설정했는데 어떻게 확인하나요?

```powershell
# BuildConfig 내용 확인
Get-Content "app\build\generated\source\buildConfig\release\com\sweetapps\pocketchord\BuildConfig.java" | Select-String "AD_UNIT_ID"
```

실제 광고 ID가 표시되어야 합니다 (테스트 ID가 아닌).

### Q: 광고가 표시되지 않으면?

1. Logcat 확인:
   ```
   adb logcat | Select-String "AdMob"
   ```

2. 광고 ID가 올바른지 확인
3. 인터넷 연결 확인
4. AdMob 대시보드에서 광고 단위 활성화 확인

---

## 9. 참고 자료

- [AdMob 시작 가이드](https://developers.google.com/admob/android/quick-start)
- [AdMob 정책](https://support.google.com/admob/answer/6128543)
- [광고 단위 생성 가이드](https://support.google.com/admob/answer/7356431)

---

**⚠️ 다시 한번 강조: Play Store 출시 전 반드시 실제 AdMob 광고 ID로 교체하세요!**

