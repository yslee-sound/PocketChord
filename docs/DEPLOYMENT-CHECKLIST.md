# ✅ 배포 체크리스트 - ad_policy 테이블 분리

**날짜**: 2025-01-08  
**목적**: 방안 1(테이블 분리) 배포 전 최종 확인

---

## 📝 배포 전 체크리스트

### 1. 코드 작업 확인 ✅
- [x] SQL 스키마 작성 (`ad-policy-table-creation.sql`)
- [x] AdPolicy 모델 생성
- [x] AdPolicyRepository 생성
- [x] InterstitialAdManager 수정
- [x] AppOpenAdManager 수정
- [x] MainActivity 수정
- [x] 문서 작성 완료
- [x] 컴파일 에러 없음

### 2. Supabase 작업
- [ ] **Supabase Dashboard 로그인**
  - URL: https://supabase.com
  - 프로젝트: PocketChord

- [ ] **SQL Editor에서 테이블 생성**
  ```
  파일: docs/ad-policy-table-creation.sql
  전체 내용 복사 → 붙여넣기 → RUN
  ```

- [ ] **테이블 생성 확인**
  ```sql
  SELECT * FROM ad_policy 
  WHERE app_id = 'com.sweetapps.pocketchord';
  ```
  
  **예상 결과:**
  ```
  ✅ 1개 행 반환
  ✅ is_active = true
  ✅ ad_app_open_enabled = true
  ✅ ad_interstitial_enabled = true
  ✅ ad_banner_enabled = true
  ✅ ad_interstitial_max_per_hour = 2
  ✅ ad_interstitial_max_per_day = 15
  ```

- [ ] **RLS 정책 확인**
  ```sql
  SELECT * FROM pg_policies 
  WHERE tablename = 'ad_policy';
  ```
  
  **예상 결과:**
  ```
  ✅ policy_name = 'ad_policy_select'
  ✅ cmd = 'SELECT'
  ✅ qual = (is_active = true)
  ```

### 3. 로컬 빌드 테스트
- [ ] **Debug 빌드**
  ```bash
  cd G:\Workspace\PocketChord
  gradlew assembleDebug
  ```

- [ ] **빌드 성공 확인**
  ```
  ✅ BUILD SUCCESSFUL
  ✅ APK 생성: app/build/outputs/apk/debug/app-debug.apk
  ```

- [ ] **APK 설치**
  ```bash
  adb install app/build/outputs/apk/debug/app-debug.apk
  ```

### 4. 기능 테스트
- [ ] **앱 시작**
  - ✅ 앱 오픈 광고 표시됨
  - ✅ 충돌 없음

- [ ] **홈 화면**
  - ✅ 배너 광고 표시됨
  - ✅ 레이아웃 정상

- [ ] **화면 전환**
  - ✅ 전면 광고 표시됨 (조건 만족 시)
  - ✅ 화면 전환 정상

- [ ] **로그 확인**
  ```bash
  adb logcat | findstr "AdPolicyRepo"
  ```
  
  **기대되는 로그:**
  ```
  D/AdPolicyRepo: ✅ 광고 정책 발견!
  D/AdPolicyRepo:   - App Open Ad: true
  D/AdPolicyRepo:   - Interstitial Ad: true
  D/AdPolicyRepo:   - Banner Ad: true
  ```

### 5. Supabase 제어 테스트
- [ ] **배너 광고 끄기**
  ```sql
  UPDATE ad_policy 
  SET ad_banner_enabled = false 
  WHERE app_id = 'com.sweetapps.pocketchord';
  ```

- [ ] **5분 이내 확인**
  - ✅ 배너 광고가 사라짐
  - ✅ 로그: `🔄 배너 광고 정책 변경: 활성화 → 비활성화`

- [ ] **배너 광고 다시 켜기**
  ```sql
  UPDATE ad_policy 
  SET ad_banner_enabled = true 
  WHERE app_id = 'com.sweetapps.pocketchord';
  ```

- [ ] **5분 이내 확인**
  - ✅ 배너 광고가 다시 나타남

- [ ] **모든 광고 끄기**
  ```sql
  UPDATE ad_policy 
  SET 
    ad_app_open_enabled = false,
    ad_interstitial_enabled = false,
    ad_banner_enabled = false
  WHERE app_id = 'com.sweetapps.pocketchord';
  ```

- [ ] **앱 재시작 후 확인**
  - ✅ 앱 오픈 광고 안 나옴
  - ✅ 전면 광고 안 나옴
  - ✅ 배너 광고 안 나옴

- [ ] **광고 다시 켜기**
  ```sql
  UPDATE ad_policy 
  SET 
    ad_app_open_enabled = true,
    ad_interstitial_enabled = true,
    ad_banner_enabled = true
  WHERE app_id = 'com.sweetapps.pocketchord';
  ```

### 6. 독립성 테스트 (핵심!)
- [ ] **시나리오 1: 팝업 OFF + 광고 ON**
  ```sql
  UPDATE app_policy SET is_active = false;
  UPDATE ad_policy SET is_active = true;
  ```
  
  **확인:**
  - ✅ 팝업 안 나옴
  - ✅ 광고는 나옴 (핵심 시나리오!)

- [ ] **시나리오 2: 팝업 ON + 광고 OFF**
  ```sql
  UPDATE app_policy 
  SET is_active = true, 
      active_popup_type = 'notice',
      content = '테스트 공지';
  
  UPDATE ad_policy SET is_active = false;
  ```
  
  **확인:**
  - ✅ 팝업 나옴
  - ✅ 광고 안 나옴

- [ ] **시나리오 3: 둘 다 OFF**
  ```sql
  UPDATE app_policy SET is_active = false;
  UPDATE ad_policy SET is_active = false;
  ```
  
  **확인:**
  - ✅ 팝업 안 나옴
  - ✅ 광고 안 나옴

- [ ] **정상 상태로 복구**
  ```sql
  UPDATE app_policy SET is_active = false;  -- 평소에는 팝업 OFF
  UPDATE ad_policy SET is_active = true;    -- 광고는 ON
  ```

### 7. Release 빌드
- [ ] **Release 빌드 실행**
  ```bash
  gradlew assembleRelease
  ```

- [ ] **빌드 성공 확인**
  ```
  ✅ BUILD SUCCESSFUL
  ✅ APK 생성: app/release/app-release.apk
  ```

- [ ] **서명 확인**
  ```bash
  jarsigner -verify -verbose app/release/app-release.apk
  ```
  
  **예상 결과:**
  ```
  ✅ jar verified.
  ```

### 8. Play Store 준비
- [ ] **APK 크기 확인**
  ```
  ✅ 적정 크기 (이전 버전과 비슷)
  ```

- [ ] **버전 코드 확인**
  ```kotlin
  // app/build.gradle.kts
  versionCode = ?  // 이전보다 +1
  versionName = "?" // 적절한 버전
  ```

- [ ] **변경 사항 정리**
  ```
  제목: 광고 시스템 개선
  
  내용:
  - 광고 표시 로직 개선
  - 안정성 향상
  - 버그 수정
  ```

---

## 🚨 문제 발생 시

### 문제 1: ad_policy 테이블 조회 안 됨
**원인**: RLS 정책 문제

**해결:**
```sql
-- RLS 재설정
DROP POLICY IF EXISTS "ad_policy_select" ON ad_policy;

CREATE POLICY "ad_policy_select" ON ad_policy
  FOR SELECT USING (is_active = true);

-- 테이블 확인
SELECT * FROM ad_policy;  -- RLS 무시하고 전체 확인
```

### 문제 2: 광고가 전혀 안 나옴
**원인**: ad_policy의 is_active가 false

**해결:**
```sql
UPDATE ad_policy 
SET is_active = true 
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 문제 3: 빌드 에러
**원인**: 캐시 문제

**해결:**
```bash
gradlew clean
gradlew assembleDebug
```

### 문제 4: 앱이 구 버전 정책 사용
**원인**: 캐시가 5분 유효

**해결:**
- 5분 기다리기
- 또는 앱 재시작

---

## 📊 최종 확인

### Supabase 테이블 상태
```sql
-- 정상 운영 상태
SELECT * FROM app_policy WHERE app_id = 'com.sweetapps.pocketchord';
-- 예상: is_active = false (팝업 OFF)

SELECT * FROM ad_policy WHERE app_id = 'com.sweetapps.pocketchord';
-- 예상: is_active = true (광고 ON)
```

### 앱 동작 확인
- ✅ 팝업 없음
- ✅ 광고 표시됨
- ✅ 충돌 없음
- ✅ 로그 정상

---

## ✅ 배포 승인

모든 체크리스트 항목이 완료되면:

- [ ] **팀 리뷰 완료**
- [ ] **테스트 완료**
- [ ] **문서 업데이트 완료**
- [ ] **Play Store 업로드 준비 완료**

**승인자**: _______________  
**날짜**: 2025-01-__

---

## 📚 참고 문서

1. **상세 가이드**: `ad-policy-separation-implementation-complete.md`
2. **빠른 시작**: `QUICKSTART-AD-POLICY-SEPARATION.md`
3. **요약**: `IMPLEMENTATION-SUMMARY.md`
4. **SQL 스크립트**: `ad-policy-table-creation.sql`
5. **분석 문서**: `app-policy-ad-policy-separation-analysis.md`

---

**작성일**: 2025-01-08  
**버전**: 1.0  
**상태**: 배포 대기 중

