# String numbering and chord-diagram data contract

목표
- 앱(Compose)과 데이터베이스/외부 API 간에 일관된 기타 줄 번호 규칙을 정의합니다.
- 데이터베이스에는 사람이 읽기 쉬운 규칙(맨 윗줄부터 1번)을 사용하고, 앱 내부 렌더러(Fretboard)에서는 기존 내부 리스트 포맷을 사용합니다.
- 변환 유틸을 제공하여 DB ↔ 앱 내부를 안전하게 변환할 수 있도록 합니다.

요약 규칙
- 외부/DB/JSON 포맷(‘DB 포맷’)에서 문자열 번호(stringNumber)는 "맨 윗줄부터" 1,2,...N 순서로 지정합니다. 즉 1 = 화면 맨 위(첫 번째 줄), N = 화면 맨 아래(마지막 줄).
- 앱 내부(fretboard 컴포저블에서 사용하는 리스트) 포맷은 기존 코드 호환성을 위해 인덱스 0이 가장 낮은 음(일반적으로 6번 줄)이며, 인덱스 증가 방향은 낮은음 → 높은음(6→1) 순서입니다(이전 구현의 관례 유지).
- 변환 규칙: 내부Index = (stringCount - stringNumberDB)
  - 예: stringCount=6, DB stringNumber=1 -> internalIndex = 6 - 1 = 5 (리스트[5])
  - DB stringNumber=6 -> internalIndex = 6 - 6 = 0 (리스트[0])

데이터 타입 (권장)
- DB/JSON에서 저장할 한 줄 정보 레코드 예시 (문서화된 형식)
  - stringNumber: INTEGER (1..stringCount) — 맨 윗줄이 1
  - fret: INTEGER — -1 = mute(X), 0 = open(O), >0 = fret number
  - finger: INTEGER? — 0 또는 NULL = 숨김, >0 = fingering number

JSON 예시 (배열)
```
[
  { "stringNumber": 1, "fret": 0, "finger": 0 },
  { "stringNumber": 2, "fret": -1, "finger": 0 },
  { "stringNumber": 3, "fret": 3, "finger": 3 },
  { "stringNumber": 4, "fret": 2, "finger": 2 },
  { "stringNumber": 5, "fret": 1, "finger": 1 },
  { "stringNumber": 6, "fret": 0, "finger": 0 }
]
```
(위는 6줄 기타 기준. 1=맨 위 줄)

SQL 스키마 예시
```
CREATE TABLE chord (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE chord_string (
  chord_id INTEGER REFERENCES chord(id),
  string_number INTEGER NOT NULL, -- 1 = top
  fret INTEGER NOT NULL,
  finger INTEGER,
  PRIMARY KEY(chord_id, string_number)
);
```
- 또는 `diagram_json TEXT` 칼럼 하나에 JSON 배열을 저장하는 방식도 유연합니다.

앱 내부 사용 형식 (Compose)
- 기존 `FretboardDiagramOnly` / `FretboardDiagram` 는 `positions: List<Int>` 형태를 사용합니다. 이 리스트는 길이 `stringCount`이며, 인덱스 0 = (DB의 stringNumber == stringCount), 즉 "가장 낮은 줄" 입니다.
- 예: 내부 positions 예시 (index 0..5) => `[ -1, 3, 2, 0, 1, 0 ]`
  - index 0 (internal) => DB stringNumber 6
  - index 3 (internal) => DB stringNumber 3

변환 유틸 (Kotlin)
- `dbListToInternalPositions(db: Map<Int,Int>, stringCount: Int = 6): List<Int>`
  - db 키는 `stringNumber`(1..stringCount), 값은 `fret`
  - 반환값은 길이 `stringCount`인 리스트(인덱스 0 = lowest string)
- `internalPositionsToDbList(positions: List<Int>): Map<Int,Int>`
  - 반환 키: stringNumber (1..stringCount)

엣지 케이스 및 검증
- 누락된 stringNumber: 변환 함수는 누락된 줄에 대해 기본값 `-1` 또는 `0`을 선택할 수 있도록 옵션 파라미터를 노출합니다. 권장 기본은 `-1`(mute)로 하여 안전하게 연주되지 않음을 명시할 수 있음.
- stringCount 가 동적일 때(예: 우쿨렐레 4현, 7현 기타 등)에는 `stringCount` 값을 명확히 표기하여 변환을 수행합니다.
- DB에 저장되는 값은 항상 검증해서 `stringNumber ∈ [1,stringCount]` 범위를 유지해야 합니다.

마이그레이션/호환성
- 기존 레거시 데이터(내부 리스트 형태)를 DB로 옮길 때는 `internalPositionsToDbList`를 사용하면 DB 규칙(1=top)을 준수하는 레코드를 얻을 수 있습니다.

사용 예제 (Kotlin)
- 예: 서버에서 받아온 JSON을 DB 포맷으로 파싱한 뒤 앱 내부에서 렌더링하려면:

```kotlin
val dbMap: Map<Int,Int> = mapOf(1 to 0, 2 to -1, 3 to 3, 4 to 2, 5 to 1, 6 to 0)
val positionsInternal = dbListToInternalPositions(dbMap, stringCount = 6)
// positionsInternal -> [-1, 3, 2, 0, 1, 0]
FretboardDiagramOnly(positions = positionsInternal, fingers = /* convert fingers similarly */)
```

권장사항
- DB/JSON 및 API 문서에 반드시 "stringNumber starts at 1 = topmost string" 문구를 명시하세요.
- 앱 코드 작성 시, 외부 입력을 내부 포맷으로 변환하는 레이어를 반드시 통과시키도록 하여 렌더러는 항상 내부 포맷을 기대하도록 단일화하세요.

문의 사항
- 특정 DB (Room, SQLite, Realm 등)용 예제 DAO/엔티티가 필요하시면 알려주세요. 해당 DB 맞춤 예제를 추가해 드립니다.

