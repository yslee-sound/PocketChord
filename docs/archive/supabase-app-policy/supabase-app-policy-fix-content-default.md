# content 기본값 복구 SQL

기존 테이블의 `content` 컬럼에 기본값이 없는 경우 아래 SQL을 실행하세요.

## 기본값 추가

```sql
-- content 컬럼에 기본값 설정
ALTER TABLE public.app_policy 
ALTER COLUMN content 
SET DEFAULT '더 안정적이고 개선된 환경을 위해 최신 버전으로 업데이트해 주세요. 지금 업데이트하시면 앱을 계속 이용하실 수 있습니다.';
```

## 확인

```sql
-- content 컬럼의 기본값 확인
SELECT column_name, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'app_policy' 
  AND column_name = 'content';
```

**예상 결과**:
```
column_name | column_default
------------|----------------
content     | '더 안정적이고 개선된 환경을 위해 최신 버전으로 업데이트해 주세요. 지금 업데이트하시면 앱을 계속 이용하실 수 있습니다.'::text
```

## 초기 데이터 다시 삽입 (필요시)

기본값 설정 후 초기 데이터를 다시 삽입하려면:

```sql
-- 기존 데이터 삭제
DELETE FROM public.app_policy 
WHERE app_id IN ('com.sweetapps.pocketchord.debug', 'com.sweetapps.pocketchord');

-- 새로 삽입 (content는 기본값 자동 적용)
INSERT INTO public.app_policy (app_id, active_popup_type)
VALUES
  ('com.sweetapps.pocketchord.debug', NULL),
  ('com.sweetapps.pocketchord', NULL);
```

## 참고

- 기본값은 **새로 INSERT되는 행**에만 적용됩니다
- 이미 존재하는 행의 `content`는 자동으로 변경되지 않습니다
- 기존 행을 업데이트하려면 `UPDATE` 문을 사용하세요

