# releaseTest 빌드 타입 제거

## 📋 변경 사항

**날짜**: 2025-11-08  
**작업**: releaseTest 빌드 타입 제거

---

## ✅ 제거 이유

1. **과도한 복잡성**
   - `initWith(debug)`로 상속받아 시작
   - 수작업으로 설정 오버라이드
   - 유지보수 어려움

2. **실제 Release와 너무 다름**
   - 난독화 비활성화
   - Debug 키스토어 사용
   - isDebuggable = true
   - "거의 debug 수준"

3. **일반적이지 않은 방식**
   - 대부분의 앱은 debug + release만 사용
   - 필요시 Product Flavor 사용

---

## 🔧 제거된 코드

```kotlin
// build.gradle.kts에서 제거됨
create("releaseTest") {
    initWith(getByName("debug"))
    buildConfigField("String", "SUPABASE_APP_ID", 
        "\"com.sweetapps.pocketchord.releasetest\"")
    signingConfig = signingConfigs.getByName("debug")
    isMinifyEnabled = false
    isShrinkResources = false
    applicationIdSuffix = ".releasetest"
    versionNameSuffix = "-RELEASE-TEST"
    isDebuggable = true
}
```

---

## 📊 변경 후 빌드 구조

### 이전 (3개):
```
- debug
- releaseTest  ← 제거됨
- release
```

### 현재 (2개):
```
- debug   (개발용)
- release (배포용)
```

---

## 🎯 앞으로의 방향

### Debug 빌드로 개발/테스트
```
- SUPABASE_APP_ID = .debug
- 디버깅 가능
- 빠른 빌드
- 실제 사용자 영향 없음
```

### Release 빌드로 배포
```
- SUPABASE_APP_ID = 실제
- 난독화 적용
- 최적화
- Play Store 업로드
```

### 필요시 추후 검토
- Product Flavor (dev/staging/prod)
- 또는 그냥 debug + release로 충분

---

## 📝 아카이브된 문서

releaseTest 관련 문서들은 참고용으로 보관:
- `QUICKSTART-RELEASE-TEST.md`
- `RELEASE-TEST-CORRECTION.md`
- `release-test-guide.md`
- `debug-mode-button-implementation.md`

---

**작성일**: 2025-11-08  
**상태**: ✅ 제거 완료

