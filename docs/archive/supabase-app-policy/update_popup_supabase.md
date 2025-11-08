# PocketChord 업데이트 팝업 + Supabase 연동 가이드

## 개요
- 이 문서는 Supabase의 `update_info` 테이블을 통해 Android 앱의 업데이트 팝업을 제어하는 방법, 운영 룰, 시나리오 예시를 설명합니다.
- 앱은 version 비교 시 "version_code(정수)"만을 사용합니다. version_name은 UI 표시에만 사용됩니다.

---

## 테이블 스키마
테이블: `public.update_info`

```sql
create table if not exists public.update_info (
  id bigserial primary key,
  version_code integer not null,
  version_name text not null,
  app_id text not null,
  is_force boolean not null default false,
  release_notes text not null,
  released_at timestamptz not null default now(),
  download_url text null,
  constraint update_info_version_code_key unique (version_code)
);
```

권장 인덱스:
```sql
create index if not exists idx_update_info_app_id on public.update_info(app_id);
create index if not exists idx_update_info_version_code on public.update_info(version_code desc);
```

필드 설명:
- version_code: 업데이트 판단 기준(정수). 기존 앱보다 클 때만 팝업 표시.
- version_name: 사용자에게 표시할 문자열(예: 1.0.2). 비교에는 사용하지 않음.
- app_id: 앱 식별자. debug/release를 구분. 예) com.sweetapps.pocketchord.debug, com.sweetapps.pocketchord
- is_force: 강제 업데이트 여부. true면 닫기/외곽클릭/뒤로가기 모두 불가.
- release_notes: 줄 단위로 변경사항을 작성(앱에서 bullet 리스트로 표시).
- download_url: 스토어 상세 URL 등. 없으면 앱이 기본 스토어 링크로 이동.

---

## RLS(행 수준 보안) & 권한
### 1. RLS 활성화
```sql
alter table public.update_info enable row level security;
```

### 2. 공개 읽기 정책 (스크린샷과 동일 형태)
스크린샷에 표시된 정책은 `Allow public read` 이름으로 `SELECT`만 허용하고 `using (true)` 조건을 둔 **포괄(Permissive)** 정책입니다.

정책 생성 예:
```sql
create policy "Allow public read"
  on public.update_info
  for select
  using (true);
```

> Supabase UI에서 편집 시 `alter policy` 형태로 표시될 수 있습니다:
> ```sql
> alter policy "Allow public read" on "public"."update_info" using ( true );
> ```

### 3. 역할(`public` vs `anon`) 설명
- `anon` 롤: 클라이언트에서 anon key로 접속 시 적용되는 기본 역할.
- `public` 롤: 지정하지 않으면 기본적으로 퍼블릭 역할 전체(anon 포함)에 퍼미시브 정책이 적용.
- 현재 정책이 `to public`을 명시하지 않아도 모든 public 역할(anon 포함)에 읽기가 허용됩니다. 명시적으로 제한하려면:
  ```sql
  drop policy if exists "Allow public read" on public.update_info;
  create policy "Allow anon read"
    on public.update_info
    for select
    to anon
    using (true);
  ```

### 4. 쓰기 제한
클라이언트에서 임의 수정 방지를 위해 anon/공개 롤의 INSERT/UPDATE/DELETE 권한을 제거합니다.
```sql
revoke insert, update, delete on public.update_info from anon;
revoke insert, update, delete on public.update_info from public; -- 필요 시
```
실제 운영에서는 **서비스 키(Server key)** 또는 **Edge Function**만으로 쓰기 수행.

### 5. 선택적 app_id 제한 정책 (고급)
다수 앱/환경을 한 테이블에서 관리할 때 특정 prefix만 읽기 허용하려면:
```sql
create policy "Allow public read filtered"
  on public.update_info
  for select
  using ( app_id like 'com.sweetapps.pocketchord%' );
```

### 6. 정책 점검 SQL
현재 적용된 정책 확인:
```sql
select * from pg_policies where tablename = 'update_info';
```

---

## 앱 환경 변수 & 식별
- 앱은 `BuildConfig.SUPABASE_APP_ID`로 자신의 레코드만 조회합니다.
  - Debug: com.sweetapps.pocketchord.debug
  - Release: com.sweetapps.pocketchord
- Supabase의 update_info 레코드 `app_id`가 빌드와 반드시 일치해야 합니다.

---

## 앱 동작 개요
- 업데이트 필요 조건: `server.version_code > BuildConfig.VERSION_CODE`
- 강제 업데이트(`is_force = true`):
  - 닫기/외곽 클릭/뒤로가기 불가
  - 스토어 다녀와 뒤로 돌아와도 팝업 유지
- 선택적 업데이트(나중에 제공):
  - 사용자가 '나중에'를 누르면 `dismissed_version_code`로 저장되어 같은 version_code는 다시 표시하지 않음
  - 다음에 더 큰 version_code가 올라오면 다시 표시
- 스토어 이동:
  - `download_url`이 있으면 해당 URL로 이동
  - 없으면 `market://details?id=<package>` 시도 → 실패 시 `https://play.google.com/store/apps/details?id=<package>` 폴백
- 릴리즈 노트 표시:
  - `release_notes`를 줄 단위로 분리하여 bullet 리스트로 표시(선행 기호 `-`, `*`, `•` 제거)

---

## ‘나중에’ 버튼 동작(아주 쉽게 설명)
- 아주 쉽게 말해, 앱이 ‘나중에’를 누르면 “이번 버전은 나중에 보기로 했어”라고 작은 메모를 폰 안에 저장해 둡니다.
- 그리고 다음에 앱을 켤 때 그 메모를 먼저 확인해, 같은 버전이면 팝업을 안 띄우는 거예요. 
- 숫자가 더 큰 새 버전이 나오면 메모와 다르니까 다시 팝업을 띄웁니다. 
- 강제 업데이트(is_force = true)면 ‘나중에’ 버튼이 없어서 항상 뜹니다.

동작 흐름
1. ‘나중에’ 누름 → update_prefs 라는 저장소에 dismissed_version_code(예: 11) 저장.
2. 앱 재실행 → 서버에서 최신 version_code 가져옴.
3. 서버 버전이 내 앱보다 크면 업데이트 대상.
4. 그 서버 버전이 dismissed_version_code와 같으면 같은 버전에서 이미 ‘나중에’를 눌렀으므로 팝업 미표시.
5. 서버 버전이 더 크거나 달라지면 다시 표시.

메모를 지우면(예: update_prefs 초기화) 다시 팝업이 뜹니다.

### 메모(업데이트 알림 기록) 지우는 방법
아래 방법 중 하나만 따라 하면 돼요.

- 방법 A) 앱 안에서 초기화 버튼이 있을 때(있는 경우에만)
  1) 앱을 엽니다.
  2) 설정/도움말(또는 개발자 메뉴)에서 ‘업데이트 알림 초기화’ 같은 버튼을 찾습니다.
  3) 버튼을 누릅니다.
  4) 앱을 완전히 종료했다가 다시 실행합니다.

- 방법 B) 휴대폰 설정에서 지우기(누구나 가능)
  1) 휴대폰에서 ‘설정’ 앱을 엽니다.
  2) ‘앱’(또는 ‘애플리케이션’) 메뉴로 들어갑니다.
  3) ‘PocketChord’를 선택합니다.
  4) ‘저장공간’(또는 ‘메모리’)을 누릅니다.
  5) ‘데이터 지우기’(또는 ‘저장공간 지우기’)를 누릅니다.
     - 주의: 앱에 저장된 설정/기록이 함께 지워질 수 있어요.
  6) 앱을 다시 실행합니다.

- 방법 C) 개발자용(선택, Android Studio/ADB)
  1) PC에 기기를 연결하고 USB 디버깅을 켭니다.
  2) 아래 명령으로 앱 데이터를 지웁니다.

```bat
adb shell pm clear com.sweetapps.pocketchord.debug
:: 릴리즈 앱 패키지는 보통 다음과 같습니다
adb shell pm clear com.sweetapps.pocketchord
```

왜 명령이 2개인가요? (debug vs release)
- 디버그/릴리즈는 패키지 이름이 다릅니다. (디버그 빌드는 보통 `.debug` 접미사)
- 안드로이드에선 패키지명이 다르면 “서로 다른 앱”으로 인식하고, 데이터도 완전히 따로 저장됩니다.
- 그래서 디버그/릴리즈 각각에 대해 별도의 초기화 명령이 존재합니다.

관리 방법
- 지금 테스트 중인 빌드가 무엇인지 확인하고, 해당 패키지에만 `pm clear`를 실행하세요.
- 두 빌드가 모두 설치돼 있다면, 필요할 때 각각 따로 지울 수 있습니다.
- 패키지 확인:

```bat
:: 설치된 PocketChord 관련 패키지 확인(Windows)
adb shell pm list packages | findstr pocketchord
```

- 선택 삭제 예시(테스트 중인 빌드만):
```bat
:: 디버그만 지우기
adb shell pm clear com.sweetapps.pocketchord.debug

:: 또는 릴리즈만 지우기
adb shell pm clear com.sweetapps.pocketchord
```

지우고 나면, 앱을 다시 켰을 때 같은 버전의 팝업이 다시 나타납니다.

---

### ADB 오류 트러블슈팅: `adb.exe: more than one device/emulator`
여러 기기(예: 실제 휴대폰 + 에뮬레이터)가 동시에 연결되어 있으면 `pm clear` 실행 시 대상 패키지를 어느 기기에 적용해야 할지 몰라서 이 오류가 뜹니다. 해결 방법은 아래 중 하나를 선택합니다.

1) 연결된 기기 목록 확인
```bat
adb devices
```
출력 예:
```
List of devices attached
emulator-5554   device
R3CR30ABCXYZ    device
```
2) 특정 기기(시리얼) 지정해서 명령 실행 (실행하면, 앱 종료됨)
```bat
adb -s emulator-5554 shell pm clear com.sweetapps.pocketchord.debug
adb -s R3CR30ABCXYZ shell pm clear com.sweetapps.pocketchord
```
3) 사용하지 않는 에뮬레이터/기기 연결 해제 후 다시 실행
- 에뮬레이터 종료: AVD Manager 또는 창 닫기
- 실제 기기 USB 케이블 분리

4) 에뮬레이터만 남기고 다시 시도 (혹은 실제 기기만 남기기)
```bat
adb shell pm clear com.sweetapps.pocketchord.debug
```
(이 때 목록에 1개만 있어야 오류가 사라집니다.)

추가 팁
- WSA(윈도우 서브시스템 Android)나 다른 디버그 기기가 자동으로 뜨는 경우 `adb devices`로 시리얼을 먼저 확인하고 사용하지 않는 것을 종료하세요.
- 스크립트로 자동화할 때는 가장 먼저 `adb devices`로 목록을 파싱한 뒤 첫 번째 원하는 시리얼을 변수에 담아 `adb -s <serial> shell ...` 형태로 실행합니다.

---

핵심 로직(Kotlin, 단순화 예시):
```kotlin
// 1) 앱 시작 시: 팝업 표시 여부 판단
val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
val dismissed = prefs.getInt("dismissed_version_code", -1)

val localCode = BuildConfig.VERSION_CODE
val remoteCode = latest.version_code
val isForce = latest.is_force

val needUpdate = remoteCode > localCode
val suppressed = !isForce && (dismissed == remoteCode)
val showDialog = needUpdate && !suppressed
// showDialog == true 면 팝업 표시

// 2) 선택적 업데이트 팝업에서 '나중에' 클릭 시: 현재 서버 버전 코드 저장
fun onClickLater(remoteCode: Int) {
    prefs.edit().putInt("dismissed_version_code", remoteCode).apply()
}

// 3) QA나 초기화 필요 시: 저장된 메모 삭제(다시 팝업 보이게)
fun resetUpdatePrefs() {
    prefs.edit().clear().apply()
}
```


---

## 서버 API(REST) 예시
최신 1건 조회:
```http
GET /rest/v1/update_info?app_id=eq.com.sweetapps.pocketchord.debug&order=version_code.desc&limit=1
apikey: <anon-key>
Authorization: Bearer <anon-key>
```

서버에서 바로 비교(`version_code`가 N보다 큰 최신 1건):
```http
GET /rest/v1/update_info?app_id=eq.com.sweetapps.pocketchord.debug&version_code=gt.N&order=version_code.desc&limit=1
```

---

## 운영 룰
릴리즈 시 반드시:
1) `version_code`를 1 증가 (Android 표준)
2) Supabase에 새 행 등록
3) `app_id`를 대상 빌드와 일치하게 입력
4) 필요 시 `is_force` 설정
   - true: 치명적 버그 수정, 비호환 등으로 반드시 업데이트 필요
   - false: 선택적 업데이트
5) `release_notes`는 한 줄당 하나의 변경사항으로 작성

---

## 시나리오 예시
1) 선택적 업데이트
- 현 앱: version_code = 10
- 서버: version_code = 11, is_force = false
- 결과: 팝업 표시. '나중에' 클릭 시 11은 재표시 안 함. 12 등록 시 다시 표시.

2) 강제 업데이트
- 현 앱: version_code = 10
- 서버: version_code = 11, is_force = true
- 결과: 닫기/외곽 클릭/뒤로가기 불가. 업데이트 완료 전까지 진행 불가.

3) 동일 코드, 이름만 변경
- 서버: version_code = 10, version_name만 변경
- 결과: 팝업 없음(정책상 version_code만 비교).

4) 환경 분리
- 디버그 앱: app_id = com.sweetapps.pocketchord.debug
- 서버는 디버그용 행에만 새 버전 등록
- 결과: 디버그 빌드에서만 팝업. 릴리즈에는 영향 없음.

---

## 관리자 INSERT 샘플
선택적 업데이트:
```sql
insert into public.update_info
(version_code, version_name, app_id, is_force, release_notes, download_url)
values
(11, '1.1.0', 'com.sweetapps.pocketchord.debug', false,
'- 신규 코드 추가\n- 일부 UI 개선\n- 안정성 향상',
null);
```

강제 업데이트:
```sql
insert into public.update_info
(version_code, version_name, app_id, is_force, release_notes, download_url)
values
(12, '1.2.0', 'com.sweetapps.pocketchord', true,
'- 크리티컬 버그 수정(앱 실행 불가)\n- 구버전과 데이터 호환 불가',
'https://play.google.com/store/apps/details?id=com.sweetapps.pocketchord');
```

---

## QA 체크리스트
- [ ] Supabase `app_id`가 빌드와 일치하는지 (debug/release)
- [ ] 서버 `version_code`가 로컬보다 큰지
- [ ] RLS 정책(`Allow public read`)이 존재하고 SELECT만 허용되는지
- [ ] anon/public 쓰기 권한 제거됨
- [ ] 과거 '나중에' 저장 여부(`dismissed_version_code`) 초기화 필요 여부
- [ ] 로그캣에 `isUpdate`, `localCode`, `remoteCode` 로그 확인

---

## 문제 해결(FAQ)
Q. 팝업이 안 떠요.
A. 대부분 version_code 동일, app_id 불일치, 과거 '나중에' 저장, RLS 차단, 혹은 쓰기 정책 오해로 인한 데이터 미등록 중 하나입니다.

Q. version_name만 바꾸면?
A. 정책상 팝업 없음. 반드시 version_code를 증가시키세요.

Q. 스토어 앱이 없는 기기에서는?
A. `market://` 실패 시 자동으로 `https://play.google.com/...` 링크로 폴백합니다.

Q. 정책을 수정했는데 반영이 안 되는 것 같아요.
A. `select * from pg_policies where tablename='update_info';` 로 실제 정책 존재 여부와 이름/조건을 먼저 확인하세요.

---

## 보안 & 거버넌스
- update_info는 읽기 전용 공개(SELECT 정책)으로 운영, 쓰기는 서버 키/Edge Function만.
- app_id로 타 앱/빌드 데이터 혼입 방지.
- 필요 시 감사 컬럼(예: created_by, updated_by) 또는 soft delete(`is_active`) 추가 고려.

---

## 부록: cURL 샘플
```bash
curl -H "apikey: $SUPABASE_ANON_KEY" \
     -H "Authorization: Bearer $SUPABASE_ANON_KEY" \
     "https://<project-id>.supabase.co/rest/v1/update_info?app_id=eq.com.sweetapps.pocketchord.debug&order=version_code.desc&limit=1"
```
