# PocketChord 코드 DB 구축/표현 단계별 세부 계획

전체 로드맵(요약)
1) 데이터 모델/Room 스키마 구축 + 시드 임포트
2) 탐색/검색/필터 + Paging 3로 리스트/상세 연결
3) 다이어그램 렌더러 고도화(바레/베이스프렛/카포/튜닝)
4) 런타임 보이싱 생성기(옵션)와 캐시, 원격 업데이트
5) 성능/품질/마이그레이션/테스트 자동화

세부 단계

단계 1. Room 기반 스키마 및 시드 데이터
- 모듈링
  - :app 내부에 data 패키지 생성: entity/dao/db/seed
- Entity
  - InstrumentEntity, ChordQualityEntity, ChordEntity, ChordAliasEntity, VoicingEntity, TagEntity, ChordTagEntity
  - FTS용 ChordSearchEntity(room FTS5)
- DAO
  - InstrumentDao, QualityDao, ChordDao, AliasDao, VoicingDao, TagDao, SearchDao
- DB
  - PocketChordDatabase(version=1) + 마이그레이션 템플릿 준비
- 시드
  - assets/chords.seed.json(.zst) 포함 → WorkManager로 최초 실행 시 import
  - Import 파이프라인: JsonReader 스트리밍 → Bulk insert(Room upsert)
- 검증
  - 단위 테스트: 각 DAO CRUD, 간단한 쿼리

단계 2. 검색/필터/페이징 UI
- Repository 레이어
  - 질의 파라미터 ChordQuery(root, qualityId, tensions[], page, sort, filters)
  - PagingSource(Room 쿼리) 구성
- ViewModel
  - ChordListViewModel: StateFlow<PagingData<ChordWithTopVoicing>>
- UI
  - LazyColumn + PagingDataAdapter(Compose Paging)로 무한 스크롤
  - 카드: 제목, 최상위 rank 보이싱 1개(또는 미리보기 2~3개)
- 기능
  - FTS5 기반 이름 검색 + alias 통합
  - 태그/품질/루트/난이도/베이스프렛 범위 필터

단계 3. 다이어그램 렌더러 확장
- 입력: Voicing(baseFret, positions[], barres[])
- 기능
  - baseFret 렌더(예: 5fr 표시), 0/개방현 ‘O’, 뮤트 ‘X’
  - barres: RoundRect/Path로 렌더
  - 스케일 파라미터화(cardSizeDp)
- 캡처/공유(옵션)
  - Compose 캔버스를 Bitmap으로 변환해 공유

단계 4. 런타임 보이싱 생성기 + 원격 업데이트
- 생성기
  - 알고리즘을 Kotlin 멀티모듈(:core)로 분리 → 단위 테스트 용이
  - 입력 제약(최대 스트레치, 프렛 폭, 개방현 선호, 바레 허용 등)
  - 결과는 상위 N개만 노출, 사용자가 저장 시 로컬 DB에 persist(is_generated=1)
- 원격 업데이트
  - endpoint: /chords/v{dataset}/manifest.json (checksum/size)
  - 새 버전이 있으면 백그라운드 Work로 다운로드, 검증, import

단계 5. 성능/품질/테스트
- 성능
  - Room 쿼리 인덱스 점검(chord_id, rank, difficulty)
  - Render 측정(Compose Macrobenchmark)으로 스크롤 FPS 확인
- 품질
  - Schema Dump/Migration 테스트
  - Seed 파일 크기 관리(압축, 중복 제거)
- 텔레메트리(옵션)
  - 어떤 보이싱이 많이 열렸는지 로컬 집계(개인정보 X)

데이터 예시(JSON)
```json
{
  "instrument": {"id":1, "name":"Guitar", "stringCount":6, "tuning":[40,45,50,55,59,64]},
  "qualities": [{"id":1,"symbol":"maj7","intervals":[0,4,7,11]}],
  "chords": [{"id":100,"root":0,"qualityId":1,"canonicalName":"Cmaj7"}],
  "aliases": [{"chordId":100,"alias":"CM7"}],
  "voicings": [{
    "id":2000,
    "chordId":100,
    "baseFret":1,
    "positions":[-1,3,2,0,1,0],
    "fingers":[0,3,2,0,1,0],
    "barres":[{"fret":1,"from":5,"to":1}],
    "difficulty":2,
    "rank":0.92,
    "isGenerated":0
  }]
}
```

화면 흐름 연결 계획
- 홈 → 루트/품질 선택 → `ChordList`(Paging)
- `ChordList` 아이템 탭 → `ChordDetail`(보이싱 리스트)
- `ChordDetail`에서 보이싱 탭 → 확대/공유/저장
- 검색 탭 → FTS + 필터로 `ChordList` 재사용

마일스톤/산출물
- M1: Room 스키마/DAO/시드 import + 간단 리스트(2주)
- M2: 검색/필터/Paging 통합 + 카드 렌더 성능(2주)
- M3: 다이어그램 고도화 + 상세/공유(1주)
- M4: 보이싱 생성기 + 캐시 + 원격 업데이트(3주)
- M5: 최적화/테스트/문서화(1주)

리스크 및 대응
- 데이터 폭증 → 생성/저장 하이브리드로 DB 크기 관리
- 표기/이명 혼선 → canonical + alias 이중 구조 유지
- 마이그레이션 복잡 → 버전별 스크립트와 테스트 필수

