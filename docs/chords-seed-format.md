# PocketChord 시드 데이터 포맷 제안서

목표
- 앱 번들에 포함할 초기 데이터(시드)를 작고, 안정적으로 파싱 가능하게 정의
- 추후 원격 업데이트 파일도 동일 스키마로 배포

컨테이너
- 파일명: `chords.seed.json.zst` (또는 `.json`) 
- 압축: Zstandard(zstd) 권장. 미지원 환경이면 GZIP로 대체 가능
- 인코딩: UTF-8
- 상위 구조
```json
{
  "version": 1,
  "datasetVersion": 20250101,
  "instrument": {"id":1,"name":"Guitar","stringCount":6,"tuning":[40,45,50,55,59,64]},
  "qualities": [ {"id":1, "symbol":"maj7", "intervals":[0,4,7,11], "aliases":["M7","maj7","△7"]} ],
  "chords": [ {"id":100, "root":0, "qualityId":1, "canonicalName":"Cmaj7"} ],
  "aliases": [ {"chordId":100, "alias":"CM7"} ],
  "voicings": [ {
      "id":2000,
      "chordId":100,
      "baseFret":1,
      "positions":[-1,3,2,0,1,0],
      "fingers":[0,3,2,0,1,0],
      "barres":[{"fret":1,"from":5,"to":1}],
      "difficulty":2,
      "rank":0.92,
      "isGenerated":0,
      "source":"seed"
  } ],
  "tags": [ {"id":1,"name":"open"} ],
  "chordTags": [ {"chordId":100, "tagId":1} ]
}
```

필드 정의/검증 규칙
- version: 시드 파일 스키마 버전(앱 파서 마이그레이션 판단)
- datasetVersion: 데이터셋 자체 버전(원격 업데이트 비교)
- instrument.tuning: MIDI 넘버 배열 또는 반음 오프셋 배열(0=Concert A 기준 아님, 고정 음고)
- qualities.intervals: 루트 기준 반음수. 예) maj7=[0,4,7,11]
- chords.root: 0..11(C=0)
- voicings
  - positions: 길이=stringCount. -1=뮤트, 0=개방, n(>0)=프렛 번호
  - baseFret: 다이어그램 좌측 기준 프렛(1..). 큰 프렛의 경우 필수
  - barres: from/to는 줄 인덱스(6줄일 때 6..1 또는 0..5; 구현 통일 필요)
  - rank: 0.0..1.0 권장(정규화 점수)
  - difficulty: 1..5 또는 0..10 등 내부 규약 통일

무결성 체크 리스트
- 모든 chord.qualityId는 qualities.id에 존재
- 모든 alias.chordId, voicing.chordId, chordTag.chordId는 chord.id에 존재
- positions 길이는 instrument.stringCount와 동일
- baseFret + max(positions) 가 음수/0이 아닌 합리 범위(예: 1~20 프렛)
- 중복 보이싱 키: (baseFret, positions[]) 단위로 중복 제거

서명/체크섬(원격 배포용)
- manifest.json
```json
{
  "datasetVersion": 20250101,
  "files": [
    {"name":"chords.seed.json.zst","sha256":"...","size":123456}
  ]
}
```
- 다운로드 후 sha256과 size 검증 → 실패 시 롤백

마이그레이션 지침
- 시드 version 변경 시 앱에 `Room` 마이그레이션과 파서 업데이트 포함
- 이전 버전 지원이 필요하면 변환 레이어 추가

확장 고려
- 다중 악기 지원: instrument[] 리스트화 + 각 voicing에 instrumentId 추가
- 왼손잡이/튜닝: 렌더링 시 변환하거나 별도 필드로 저장
- 오디오 샘플: 별도 CDN(선택), 로컬에는 URI만 보관

