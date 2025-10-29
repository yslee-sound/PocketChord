# PocketChord: 내가 정의해야 할 항목(체크리스트 + 템플릿)

이 문서는 코드 데이터베이스 설계/구현(see: `docs/chords-db-architecture.md`, `docs/chords-db-implementation-plan.md`)에서 제품 오너가 선결로 "정의"해야 실제 개발을 막힘없이 진행할 수 있는 결정 사항을 한 데 모은 체크리스트입니다. 각 항목은 선택지와 템플릿을 제공하며, 미정이면 추천 기본값(Recommended)을 사용해도 됩니다.

---

## 1) 데이터 범위와 수용 정책(Scope)
- 지원 악기(필수):
  - [ ] 6현 기타 표준 튜닝(EADGBE)만
  - [ ] 멀티 악기(예: 7현/12현/우쿨렐레 등) — 목록: ______________________
- 프렛 범위(기본):
  - 최대 프렛 수: [ 20 ]  (권장 20~22)
  - 기본 다이어그램 시작 프렛 baseFret 허용 범위: [ 1 ] ~ [ 15 ]
- 보이싱 생성기 사용 정책:
  - [ ] 시드 데이터만 사용 (생성기 미사용)
  - [x] 시드 + 런타임 생성 혼합(Recommended)
  - 생성기 최대 탐색 깊이/시간/상위 N: 깊이=[ 6 ], 시간(ms)=[ 40 ], 상위 N=[ 50 ]
- 난이도 구간 정의(초/중/고 또는 1..5):
  - 구간 수: [ 3 ]  라벨: [ 초 ] [ 중 ] [ 고 ]

## 2) 표기 규칙 / 이명 / 지역화(Notation & i18n)
- 루트 표기(enharmonic): # vs b 우선순위(권장: 조키/사용자 설정 기반)
  - [x] 기본은 # 우선 (C#, F#)
  - [ ] 기본은 b 우선 (Db, Gb)
  - 사용자 선택 허용: [x] Yes  [ ] No
- 품질 표기 표준(동의어 묶음):
  - maj: ["maj", "M", "△"]  | min: ["m", "min", "-" ]  | dim: ["dim", "°"]  | aug: ["aug", "+"]
  - 7류: maj7=["maj7","M7","△7"], dom7=["7"], min7=["m7"], dim7=["dim7","°7"]
  - 확정/수정 내역을 alias 테이블에 반영 여부: [x] 반영
- 텐션 표기 규칙(공백/괄호):
  - [x] Cmaj7(add9) 스타일  |  [ ] Cmaj7 add9  |  [ ] CΔ7(add9)  | 기타: __________
- 로케일 지원 우선순위: [ ko-KR, en-US ] (UI 텍스트/검색 토크나이저 동의어 사전 포함)

## 3) 스키마 세부 규약(Schema Conventions)
- 줄 인덱스/정렬(positions[]) — 반드시 고정:
  - [x] index 0=6번줄(저음 E) → index 5=1번줄(고음 E) (Recommended)
  - [ ] index 0=1번줄(고음) → index 5=6번줄(저음)
- positions 값 의미: -1=뮤트, 0=개방, n(>0)=해당 프렛
- baseFret: 1-based(Recommended)  |  [ ] 0-based
- barres 구조: {fret, from, to} 에서 from/to 인덱스 단위
  - [x] positions 인덱스와 동일(0..5)  |  [ ] 1..6  |  [ ] 기타: _________
- fingers 값: 0=미지정, 1~4=검/중/약/소, 5=엄지 사용 허용 여부: [x] 허용  [ ] 금지
- 난이도 저장 단위: [ 1..5 ]  |  [ 0..10 ]  | 라벨 매핑표 별첨: yes/no
- 고유키 규약:
  - chord key = root(0..11)+qualityId(+정렬된 tensions)
  - voicing key = baseFret + positions[] + tuning 해시(SHA-1 8바이트)

## 4) 랭킹/정렬 가중치 정의(Scoring)
가중치는 0.0~1.0 권장. 합이 1일 필요는 없으나 상대값이 중요합니다.
- 기본 정렬: [x] rank DESC → difficulty ASC → baseFret ASC
- 가중치(Recommended 기본값):
  - openStringBonus: [ 0.15 ]
  - barrePenalty: [ 0.10 ]
  - spanPenalty(프렛 폭): [ 0.20 ]
  - baseFretPenalty(높은 포지션): [ 0.10 ]
  - bassRootBonus(저현 루트): [ 0.15 ]
  - topNotePreference(탑노트 선호): [ 0.15 ]
  - voiceCompleteness(필수음 포함): [ 0.15 ]
- 필수음 규칙(Invalid 컷오프): 루트/3rd/7th 필수 여부
  - [x] 3rd/7th 중 최소 1개 필수, 루트 권장  |  [ ] 루트/3rd/7th 모두 필수  |  [ ] 자유

## 5) 검색/필터 정책(Search)
- FTS 색인 대상: canonicalName + aliases (Recommended)  |  [ ] canonicalName만
- 토크나이저/정규화:
  - 대소문자 무시, 특수문자(공백/괄호/+,#) 분리
  - 한글: 초성 분해 여부 [ ] Yes [x] No
  - 동의어 전개: maj7=M7=△7 등 사전 사용 [x] Yes
- 기본 페이지 사이즈: [ 30 ]  |  프리페치 윈도우: [ 2 ] 페이지
- 기본 필터(초기 화면): root: [ All ], quality: [ All ], tension: [ None ], baseFret <= [ 8 ]

## 6) 시드/업데이트 거버넌스(Data Ops)
- datasetVersion 규칙: YYYYMMDD (예: 20250101)
- 배포 경로: __________________________
- 체크섬 서명: sha256 (manifest.json 사용) — 필수 [x]
- 실패 전략: 다운로드/검증 실패 시 이전 버전 유지 [x]
- 라이선스 정책:
  - 외부 저작물(책/이미지/상용 DB) 원본 저장 금지 [x]
  - 생성 알고리즘/자가 제작 데이터만 허용 [x]
  - 기여자 라이선스(선택): CLA 필요 [ ]Yes [x]No

## 7) 성능/UX 목표
- 로컬 검색 응답: ≤ [ 50 ] ms
- 스크롤 성능: 60fps 유지 (Compose List → Dropped frame < [ 5 ]%)
- 초기 시드 임포트: ≤ [ 5 ] s (중간급 디바이스)
- APK 증분(시드 압축): ≤ [ 5 ] MB

## 8) 접근성/국제화(A11y/i18n)
- 다이어그램 대체 텍스트: `"Cmaj7 voicing: X-3-2-0-1-0 @1fr"` 형식 사용 [x]
- 색 대비: WCAG AA 준수 [x]
- Dynamic Type 대응: 텍스트 스케일 최대 1.3x [x]

## 9) QA 수용 기준(DoD)
- DB 무결성 검사(시드/업데이트) 100% 통과
- 성능 기준 충족(위 목표)
- 마이그레이션 테스트 통과(Room schema)
- 예시 쿼리/화면 스냅샷 리그레션 테스트 녹화

---

# 작성 템플릿(복사해 채우세요)

## A. 품질/텐션 카탈로그 확정
| qualityId | symbol | intervals(반음) | aliases | 노트 |
|---|---|---|---|---|
| 1 | maj | [0,4,7] | ["M","maj"] |  |
| 2 | maj7 | [0,4,7,11] | ["M7","maj7","△7"] |  |
| 3 | 7 | [0,4,7,10] | ["dom7","7"] |  |
| ... |  |  |  |  |

추가 텐션 표준: 예) add9, b9, #11, 13 → 내부 표현은 루트 기준 반음 배열에 포함/혹은 별도 `ChordTension`

## B. 튜닝/악기 정의
| instrumentId | name | stringCount | tuning(MIDI or semitone) | 활성 |
|---|---|---:|---|---|
| 1 | Guitar Std | 6 | [40,45,50,55,59,64] | Yes |
| 2 | Drop D | 6 | [38,45,50,55,59,64] |  |
| ... |  |  |  |  |

## C. 생성기 제약 기본값
```
maxSpanFrets = 4
allowBarre = true
allowOpenStrings = true
minStringsSounding = 3
startingFretRange = 1..8
```

## D. 점수식 파라미터(예시)
```
score = 0.15*openStringBonus
      - 0.10*barrePenalty
      - 0.20*spanPenalty
      - 0.10*baseFretPenalty
      + 0.15*bassRootBonus
      + 0.15*topNotePreference
      + 0.15*voiceCompleteness
```

## E. 검색 동의어/정규화 사전(발췌)
```
maj7 => M7, △7
min => m, minor
aug => +, augmented
sus4 => sus, sus4
"C#" => "Db" (양방향)
```

## F. 페이지/UX 파라미터
```
pageSize = 30
prefetch = 2
cardScaleDp = compact|regular|comfortable (default=regular)
```

---

# 최소 결정 세트(이것만 정하면 구현 시작 가능)
1. positions 인덱싱(6→1) 채택 여부: [x] Yes
2. baseFret 1-based: [x] Yes
3. 표기 규칙: # 우선 + Cmaj7(add9) 양식 [x]
4. 생성기 사용: 하이브리드 + 상위 N=50 [x]
5. 기본 정렬: rank DESC → difficulty ASC [x]
6. 페이지 사이즈: 30 [x]
7. 데이터셋 버전 규칙: YYYYMMDD [x]

위 7가지만 확정되면, `Room 스키마/DAO/시드 임포트(M1)` 구현을 바로 시작할 수 있습니다.

