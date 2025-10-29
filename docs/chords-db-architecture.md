# PocketChord 코드 데이터베이스 아키텍처/표현 설계

목표
- 코드(Chord) 수가 조합상 매우 많아져도 빠르게 탐색/검색/표시할 수 있어야 함.
- 오프라인에서도 동작. 필요 시 원격에서 데이터셋 업데이트.
- 저장 공간과 렌더링 성능을 최적화. 이미지 대신 “구성값(포지션)”으로 표현.
- 악기/튜닝/카포/왼손/바레 등 다양한 조건을 확장 가능.

핵심 전략(요약)
- 하이브리드 접근: (1) 선별된 우수 보이싱은 로컬 DB에 시드로 저장, (2) 조합/필터 기반 “런타임 보이싱 생성기”로 무한 확장.
- 저장 형식은 Room(SQLite) 중심 + FTS5(전문검색) 사용. 대용량 페이지네이션(Paging 3)으로 UI 표시.
- “그림” 저장이 아니라, 다음의 미니멀 스키마로 보이싱을 표현하고, Compose에서 즉시 그려서 메모리/용량 절감.

데이터 모델(개념)
- Instrument(id, name, stringCount, tuning)
  - tuning: 예) [E2,A2,D3,G3,B3,E4] → MIDI 또는 반음 오프셋 배열로 정규화 저장
- ChordQuality(id, symbol, intervals)
  - intervals: 루트 기준 음정 세트(예: maj7 = [0,4,7,11])
  - aliases: [“M7”, “maj7”, “△7”] 등 별칭은 별도 테이블로 관리 가능
- Chord(id, root, qualityId)
  - root: C..B(또는 0..11), enharmonic 표기 통일
  - optional: canonicalName(예: Cmaj7), displayName(applied preferences)
- ChordTension(chordId, interval) [옵션]
  - 9, 11, 13, b9, #11 등 추가 음정. 혹은 Chord.formula에 통합 표현도 가능
- Voicing(id, chordId, baseFret, positions[6], fingers[6]?, barres[], difficulty, rank, isGenerated, source)
  - positions: 각 줄의 프렛 번호. -1=뮤트, 0=개방현, n=프렛
  - baseFret: 다이어그램 기준 시작 프렛(예: 1~17). 큰 번호일 때를 대비해 필요
  - barres: [{fret: n, from: stringIdx, to: stringIdx}] 형태
  - rank: 점수(발현 용이성/사운드 균형/빈번도)
  - difficulty: 초급/중급/고급 또는 1..5
- Tag(id, name), ChordTag(chordId, tagId)
  - “재즈”, “오픈 코드”, “CAGED”, “Drop2” 등 필터/탐색용
- Settings/Meta: datasetVersion, lastUpdateAt, generatorDefaults 등

물리 스키마(Room 요약)
- instrument(id INTEGER PK, name TEXT, string_count INT, tuning TEXT(JSON))
- chord_quality(id INTEGER PK, symbol TEXT UNIQUE, intervals TEXT(JSON))
- chord(id INTEGER PK, root INT, quality_id INT, canonical_name TEXT, FOREIGN KEY(quality_id)→chord_quality)
- chord_alias(id INTEGER PK, chord_id INT, alias TEXT, UNIQUE(chord_id, alias))
- chord_tension(chord_id INT, interval INT, PRIMARY KEY(chord_id, interval)) [옵션]
- voicing(id INTEGER PK, chord_id INT, base_fret INT, positions BLOB(JSON/Pack), fingers BLOB?, barres BLOB?, difficulty INT, rank REAL, is_generated INT, source TEXT, INDEX(chord_id), INDEX(rank))
- tag(id INTEGER PK, name TEXT UNIQUE)
- chord_tag(chord_id INT, tag_id INT, PRIMARY KEY(chord_id, tag_id))
- chord_search(name TEXT) USING FTS5(name, content='');
  - 시드/동기화 시 chord와 alias를 합쳐 색인

표현(렌더링) 규칙
- 다이어그램은 positions[], baseFret, barres[]만으로 그린다.
- UI 스케일은 카드 레이아웃에 맞춰 dp로 비율 고정. 이미지 캐시는 필요 시 캐노니컬 키(chordId+voicingId)로 메모리 캐시.

검색/필터/랭킹
- 필터: root, quality, tensions, 카포, 튜닝, 허용 프렛 범위, 바레 허용, 개방현 우대 등.
- 정렬: rank DESC, difficulty ASC, baseFret ASC, 개방현 수 DESC 등 가중치 조합.
- 이름 검색: FTS5에서 “Cmaj7 add9” → 토큰화해 chord + alias 매칭.

스케일링/성능
- 보이싱 수가 10만~100만이 되더라도: 인덱스 + Paging 3로 UI 분할 로딩.
- positions[]는 6개 정수 + 부가정보로 수십 바이트 수준. 이미지보다 매우 작다.
- 생성기는 IO가 아닌 CPU 바운드 → 백그라운드 코루틴 + 캐싱.

데이터 소스와 업데이트
- 시드: 앱 번들에 포함된 압축 JSON(ZSTD/LZ4). 최초 실행 시 Room에 import.
- 업데이트: 원격 버전 파일(JSON/Protobuf) 다운로드 → checksum 검증 → Room 트랜잭션 업서트.
- 라이선스 주의: 외부 코드 북/이미지 그대로 포함 금지. 알고리즘 생성 + 퍼블릭 도메인/자가 제작 데이터만.

런타임 보이싱 생성기(개요)
- 입력: chord(root+quality+tensions), tuning, 제약(maxSpan, minStrings, allowBarre, allowOpen, startingFretRange)
- 절차:
  1) 각 줄의 프렛 후보 생성(루트/코드톤 포함 프렛 계산)
  2) DFS/백트래킹으로 조합 탐색(줄당 0/1 노트 선택) + 제약 조건 즉시 가지치기
  3) 보이싱 유효성 체크: 최소 음 구성(루트/3rd/7th 포함 등), 프렛 폭과 손가락 수, 바레 가능성
  4) 점수화: 음 밸런스, 루트/탑노트 선호, 개방현 보너스, 저현/고현 분포
  5) 중복 제거(normalized positions key)
  6) 상위 N개만 반환(페이지 당)
- 출력: List<Voicing>
- 캐시: (chordId, constraints) → LRU에 일부 저장

표준화/고유키
- chord: root(0..11)+qualityId(+sorted tensions)
- voicing: baseFret + positions[] + tuning → SHA1 8바이트 등으로 해시 키 생성

다국어/표기
- 표기 규칙 설정: #/b, M/m, △/maj 표기 등은 사용자 설정으로 결정 → canonical_name과 별도로 렌더링 시 적용

엣지 케이스
- 드롭/대체 튜닝, 카포, 7/8현 기타, 왼손잡이(미러), 스케일/아르페지오 등 확장

보안/안정성
- 모든 원격 파일은 HTTPS + 체크섬. 실패 시 이전 버전 유지.
- Room 마이그레이션 명시적 관리.

성공 기준
- 검색 50ms 이내(로컬), 스크롤 시 60fps 유지
- 초기 시드 import < 5초(중간 기기 기준)
- APK 증분 < 5MB(시드 압축 기준)

