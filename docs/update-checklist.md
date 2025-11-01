# PocketChord 앱 업데이트 체크리스트

## 📦 코드 데이터 업데이트 절차

### 1. 코드 데이터 수정
`app/src/main/assets/chords_seed_by_root.json` 파일에서:
- 코드 추가/수정/삭제 작업 수행
- `_metadata.version` 값 업데이트 (YYYYMMDD 형식)
- `_metadata.lastUpdated` 날짜 업데이트

### 2. 앱 버전 업데이트 (필수!)
`app/build.gradle.kts` 파일에서:
```kotlin
versionCode = 2  // ← 반드시 1씩 증가 (1 → 2 → 3 → 4...)
versionName = "1.0.1"  // ← 사용자에게 보이는 버전
```

⚠️ **중요:** `versionCode`를 증가시키지 않으면 자동 리시딩이 작동하지 않습니다!

### 3. 빌드 및 테스트
```cmd
gradlew clean
gradlew assembleDebug
```

### 4. 테스트 체크리스트
- [ ] 앱 설치 후 최초 실행 시 데이터 로딩 확인
- [ ] Logcat에서 "SeedVersion" 태그로 로그 확인
  - `Reseeded on app update to versionCode=X` 메시지 확인
- [ ] 기존 즐겨찾기가 유지되는지 확인
- [ ] 새로 추가된 코드가 표시되는지 확인
- [ ] 삭제된 코드가 사라졌는지 확인
- [ ] 수정된 코드가 올바르게 표시되는지 확인

## 🔄 자동 업데이트 메커니즘 동작 원리

### 트리거
- 앱의 `versionCode`가 변경되었을 때
- `MainActivity.onCreate()`에서 자동 실행

### 프로세스
1. 현재 앱 `versionCode` 조회
2. SharedPreferences에 저장된 마지막 시드 버전과 비교
3. 버전이 다르면:
   - 현재 즐겨찾기를 (name, root) 키로 백업
   - DB 전체 삭제 (`clearAllTables()`)
   - `chords_seed_by_root.json`에서 모든 루트 다시 로딩
   - 즐겨찾기 복원
   - 새 versionCode 저장

### 로그 예시
```
I/SeedVersion: Starting reseed: versionCode changed from 1 to 2
I/SeedVersion: Seed data version: 20250101
I/SeedVersion: Captured 5 favorites before reseed
I/SeedVersion: Database cleared
D/SeedVersion: Reseeded root: C (1/12)
D/SeedVersion: Reseeded root: C# (2/12)
...
I/SeedVersion: Reseeded 12 roots
I/SeedVersion: ✓ Reseed completed in 234ms: versionCode=2, favorites restored=5/5
```

## ⚠️ 주의사항

### DO ✅
- 앱 업데이트 시마다 `versionCode` 증가
- JSON 파일 수정 후 반드시 빌드 전 유효성 검사
- 메타데이터 버전 정보 업데이트
- 릴리스 전 테스트 디바이스에서 업데이트 테스트

### DON'T ❌
- `versionCode` 증가 없이 JSON만 수정
- JSON 문법 오류 (쉼표, 괄호 누락)
- 데이터 구조 변경 (Room 스키마와 일치해야 함)
- 프로덕션에서 바로 테스트

## 🔍 트러블슈팅

### 업데이트 후 데이터가 갱신되지 않음
→ `versionCode`를 증가시켰는지 확인
→ Logcat에서 "Already seeded for versionCode=X" 메시지 확인

### 앱이 크래시하거나 데이터가 로딩되지 않음
→ JSON 파일 문법 오류 확인 (온라인 JSON 검증기 사용)
→ Logcat에서 "SeedVersion" 에러 로그 확인

### 즐겨찾기가 사라짐
→ 코드의 `name`이나 `root` 값이 변경되었는지 확인
→ natural key (name + root)가 일치해야 복원됨

## 📊 데이터 무결성 검증

### JSON 스키마 검증
```json
{
  "_metadata": {
    "version": "YYYYMMDD",
    "description": "설명",
    "lastUpdated": "YYYY-MM-DD",
    "totalRoots": 12
  },
  "루트": [
    {
      "name": "코드명",
      "variants": [
        {
          "positions": [-1,3,2,0,1,0],
          "fingers": [0,3,2,0,1,0],
          "firstFretIsNut": true,
          "barres": [...]  // optional
        }
      ]
    }
  ]
}
```

### 필수 필드
- `name`: 코드 이름 (예: "C", "Cm7")
- `variants`: 최소 1개 이상의 배리에이션
- `positions`: 정확히 6개 요소 배열
- `fingers`: positions와 같은 길이 (optional)

## 🚀 릴리스 프로세스

1. 개발 환경에서 JSON 수정
2. `versionCode` 증가
3. 로컬 빌드 및 테스트
4. 스테이징 테스트
5. 프로덕션 릴리스
6. 사용자 업데이트 후 모니터링

## 📝 변경 이력 기록

각 업데이트마다 CHANGELOG.md에 기록 권장:
```markdown
## v1.0.1 (versionCode 2)
- C 메이저 코드 새로운 보이싱 2개 추가
- Dm7 코드 오타 수정
- G#aug 코드 삭제
```

