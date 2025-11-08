# 🚀 방안 1 구현 - 빠른 시작 가이드

**5분 안에 배포하기**

---

## 1️⃣ Supabase SQL 실행 (2분)

### Supabase Dashboard 접속
1. https://supabase.com 로그인
2. PocketChord 프로젝트 선택
3. 왼쪽 메뉴 → **SQL Editor** 클릭

### SQL 실행
`docs/ad-policy-table-creation.sql` 파일 내용 복사 → 붙여넣기 → **RUN** 클릭

**중요**: 이 SQL은 Release와 Debug 빌드 데이터를 모두 생성합니다!

### 확인
```sql
SELECT * FROM ad_policy ORDER BY app_id;
```

**예상 결과:**
```
✅ 2개 행이 반환됨
✅ com.sweetapps.pocketchord (Release)
✅ com.sweetapps.pocketchord.debug (Debug)
✅ 모두 is_active = true
✅ 모두 광고 활성화 상태
```

---

## 2️⃣ 앱 빌드 (1분)

```bash
cd G:\Workspace\PocketChord
gradlew assembleDebug
```

---

## 3️⃣ 앱 테스트 (2분)

### 설치
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 기본 동작 확인
1. ✅ 앱 시작 → 앱 오픈 광고 표시
2. ✅ 홈 화면 → 배너 광고 표시
3. ✅ 화면 전환 → 전면 광고 표시

### 광고 끄기 테스트 (Supabase에서)
```sql
UPDATE ad_policy 
SET ad_banner_enabled = false 
WHERE app_id = 'com.sweetapps.pocketchord';
```

**5분 이내 확인:**
- ✅ 배너 광고가 사라짐
- ✅ 로그: `🔄 배너 광고 정책 변경: 활성화 → 비활성화`

### 광고 다시 켜기
```sql
UPDATE ad_policy 
SET ad_banner_enabled = true 
WHERE app_id = 'com.sweetapps.pocketchord';
```

---

## ✅ 완료!

**이제 다음이 가능합니다:**

### 팝업 OFF + 광고 ON
```sql
UPDATE app_policy SET is_active = false;
UPDATE ad_policy SET is_active = true;
```

### 팝업 ON + 광고 OFF
```sql
UPDATE app_policy SET is_active = true;
UPDATE ad_policy SET is_active = false;
```

### 둘 다 OFF (명절)
```sql
UPDATE app_policy SET is_active = false;
UPDATE ad_policy SET is_active = false;
```

---

## 🆘 문제 해결

### 광고가 안 나와요
```sql
-- 1. ad_policy 확인
SELECT * FROM ad_policy WHERE app_id = 'com.sweetapps.pocketchord';

-- 2. is_active가 false면 true로 변경
UPDATE ad_policy SET is_active = true WHERE app_id = 'com.sweetapps.pocketchord';

-- 3. 광고 플래그 확인
UPDATE ad_policy 
SET 
  ad_app_open_enabled = true,
  ad_interstitial_enabled = true,
  ad_banner_enabled = true
WHERE app_id = 'com.sweetapps.pocketchord';
```

### 로그 확인
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

---

## 📱 Release 빌드 (배포용)

```bash
gradlew assembleRelease
```

**APK 위치:**
```
app/release/app-release.apk
```

Play Store에 업로드하세요!

---

**상세 문서:** `ad-policy-separation-implementation-complete.md`

