# Semantic Layout Placement and Managed Furniture Persistence Design

## 목표

빈 방 추천이 단순 좌표 분산이 아니라 가구의 의미 관계와 벽 방향을 반영하도록 확장한다. 동시에 기존 가구를 삭제한 상태가 추천 생성 과정에서 되살아나지 않고, 가구 관리 화면을 나갔다가 다시 열어도 유지되도록 한다.

이번 변경은 Backend와 Web 두 저장소의 별도 PR로 나눈다.

- Backend: 의미 기반 후보 생성, 벽/코너 스냅, 초기 적층 관계, 회전, 충돌 검증
- Web: manage-furniture 상태의 Room 저장, 적층 높이 렌더링, 독립 이동 시 적층 해제
- Swift: 변경 없음

## 성공 기준

1. `tv`와 `media_console`이 함께 추천되면 같은 x/z 중심에 배치되고 Web에서 TV가 콘솔 상판 위에 렌더링된다.
2. `monitor`와 `desk`가 함께 추천되면 같은 x/z 중심에 배치되고 Web에서 모니터가 책상 상판 위에 렌더링된다.
3. TV와 모니터는 별도 가구 객체로 유지된다. 사용자가 x/z 위치를 독립적으로 옮기면 받침 가구와의 적층 관계가 즉시 해제되고 바닥 기준 높이로 돌아간다.
4. 기존 가구를 `DELETED`로 변경하면 다음 추천의 baseline과 추천 결과에서 제외된다.
5. 가구 관리 화면을 나갔다가 다시 열어도 Backend Room 또는 활성 Layout Draft에서 삭제 상태가 복원된다.
6. 관계형 후보를 만들 수 없거나 검증에 실패하면 기존 4x4 grid 후보로 안전하게 fallback한다.
7. 창문이 없는 방에서 `curtain_blind`를 방 중앙에 독립 가구처럼 배치하지 않는다.

## 조사 결과와 원인

### 빈 방 배치

현재 `RuleBasedPlacementService`는 일부 타입만 전용 좌표를 사용한다. 나머지 타입은 방 비례 좌표 3개와 4x4 grid에 의존하고, 생성되는 추천 가구의 회전은 항상 0도다. 그 결과 충돌과 경계는 피하더라도 가구 관계와 벽 방향이 표현되지 않는다.

Web의 `scenarios.ts`에는 벽 스냅과 중앙 방향 overlap 해소 아이디어가 있지만, UI 중심 좌표와 임의 벽 segment를 사용하는 데모 시나리오 로직이다. Backend의 좌표계, `FurnitureBoundary`, north/east/south/west opening 검증을 기준으로 개념만 가져오고 코드를 직접 복제하지 않는다.

### 적층

Backend가 TV/모니터와 받침 가구에 같은 x/z를 주더라도 현재 Web의 `FurnitureMesh`는 각 가구를 `height / 2`에 놓는다. 따라서 평면상으로만 겹치고 화면에서는 둘 다 바닥에 놓인다.

Backend의 충돌 검증도 모든 가구를 2D footprint로 보므로, 허용된 적층 쌍을 구분하지 않으면 정상 추천이 충돌로 거절된다. 반대로 타입만 보고 모든 겹침을 허용하면 부분 겹침도 통과하므로 적층 판정은 엄격한 중심 정렬과 받침 footprint 포함 조건을 사용한다.

### 삭제 상태 재등장

초기 setup의 manage-furniture 편집은 `localStorage`만 갱신한다. `prepareRecommendationTransitionForEditor`는 추천 직전에 `GET /api/rooms/{roomId}`로 baseline을 다시 읽기 때문에, Room에 저장되지 않은 `DELETED` 상태가 사라지고 원래 가구가 추천 결과에 다시 포함된다.

Backend에는 이미 초기 manage-furniture 단계용 `PUT /api/rooms/{roomId}/layout`이 있다. Web이 이 API를 호출하고, 응답의 전체 furniture snapshot을 다시 로컬 mirror에 반영해야 한다. 활성 Layout Draft가 있는 재편집 흐름은 기존 `PUT /api/layouts/{layoutId}`를 계속 사용한다.

## 선택한 접근

### 후보 정책 계층

기존 타입별 switch를 계속 키우는 대신, Backend 내부 후보를 다음 정보로 표현한다.

- `position`: 방 코너 원점 기준 x/z 중심
- `rotation`: 0/90/180/270도
- `relation`: 관계형, 벽, 코너, fallback을 구분하는 내부 메타데이터
- `order`: 동일 tier 안의 결정적 순서

후보 우선순위는 아래와 같다.

1. 의미 관계 후보: bed-nightstand, sofa-side_table, desk-monitor, media_console-tv 등
2. 벽 또는 코너 후보
3. 기존 타입별 안전 후보
4. 기존 4x4 grid fallback

후보는 순서대로 `FurnitureBoundary`와 `ValidationService`의 검증을 통과한 첫 항목을 채택한다. 같은 입력은 같은 결과를 내도록 무작위성을 사용하지 않는다.

### 대안 평가

- 타입 switch 확장: 빠르지만 위치와 회전 계산이 중복되고 후속 규칙 추가 시 충돌한다.
- 존 기반 planner 전면 개편: 장기적으로 가장 자연스럽지만 현재 순차 `recommend()` 구조와 Validation 흐름을 크게 바꾼다.
- 후보 정책 계층: 기존 순차 구조와 fallback을 유지하면서 관계와 방향을 단계적으로 추가할 수 있어 이번 변경에 가장 적합하다.

Phase 3의 zone planner는 이번 PR에서 제외하고 후보 정책의 다음 계층으로 후속 설계한다.

## Backend 설계

### 배치 순서

받침/anchor가 dependent보다 먼저 배치되도록 타입 우선순위를 명시한다. 최소한 다음 선행 관계를 보장한다.

- `bed` -> `nightstand`
- `desk` -> `desk_chair`, `monitor`
- `sofa` 또는 `sofa_bed` -> `side_table`
- `media_console` -> `tv`

required/optional의 의미는 유지하되, 동일 그룹 안에서 위 의존 순서를 적용한다. 배치하지 못한 required item 처리와 기존 partial-success 계약은 바꾸지 않는다.

### 타입별 규칙

| 타입 | 1순위 규칙 | 방향 |
| --- | --- | --- |
| `bed` | 헤드 쪽을 사용 가능한 벽에 스냅 | 방 안쪽 |
| `nightstand` | 배치된 bed 좌우 | bed와 평행 |
| `desk` | 사용 가능한 벽에 스냅 | 방 안쪽 |
| `desk_chair` | desk 앞쪽 | desk를 향함 |
| `monitor` | desk와 동일 중심 | desk 방향 유지 |
| `media_console` | 사용 가능한 벽에 스냅 | 방 안쪽 |
| `tv` | media_console과 동일 중심 | console 방향 유지 |
| `sofa`, `sofa_bed` | 사용 가능한 벽에 스냅 | 방 안쪽 |
| `side_table` | sofa 좌우 | sofa와 평행 |
| `plant` | 사용 가능한 코너 | 코너별 결정적 순서 |
| `wardrobe`, `bookshelf`, `drawer_chest`, `hanger`, `partition_shelf`, `full_length_mirror` | 사용 가능한 벽에 스냅 | 방 안쪽 |
| `mood_lamp` | desk 근처, 없으면 bed/sofa 근처 | anchor와 조화 |
| `rug` | 기존 floor-overlay 규칙 | 회전 유지 |
| `curtain_blind` | 사용 가능한 window anchor | 창 방향 |
| `multi_table` | 이번 변경에서는 fallback | 0도 또는 fallback 회전 |

벽 방향은 Backend 좌표계에서 다음과 같이 정의한다.

- south(z=0)에서 안쪽 +z: 0도
- east(x=width)에서 안쪽 -x: 90도
- north(z=depth)에서 안쪽 -z: 180도
- west(x=0)에서 안쪽 +x: 270도

문과 창의 clearance는 기존 ValidationService를 그대로 통과해야 한다. 한 벽 후보가 opening과 충돌하면 다음 벽 후보를 시도한다.

### 적층 허용 정책

허용 쌍은 두 개로 제한한다.

- supporter `desk`, dependent `monitor`
- supporter `media_console`, dependent `tv`

2D collision 예외는 타입만 일치한다고 적용하지 않는다. 다음을 모두 만족할 때만 적층으로 간주한다.

- 중심 x/z 차이가 작은 epsilon 이내
- 회전된 dependent footprint가 supporter footprint 안에 들어감
- 두 가구 모두 `DELETED`가 아님

이 조건을 벗어난 부분 겹침은 기존 collision 오류로 처리한다. Backend 응답 계약에는 새 관계 필드를 추가하지 않는다. 초기 관계는 좌표와 타입으로 결정하고, Web도 동일한 엄격 조건으로 렌더 높이를 계산한다.

### 커튼/블라인드

Backend가 유효한 window anchor를 만들 수 없으면 `curtain_blind`는 일반 grid fallback으로 보내지 않는다. required item이면 기존 unplaced/partial-success 규칙을 따르고, optional item이면 생략한다.

## Web 설계

### 원본 타입 보존

Backend furniture를 `Furniture`로 변환할 때 canonical source type을 보존한다. broad rendering category(`cabinet`, `desk`, `chair`)는 화면 렌더링용으로만 사용하고, 아래 기능은 source type을 사용한다.

- Room 전체 furniture 저장 요청 직렬화
- monitor-desk, tv-media_console 적층 판정
- 향후 semantic behavior policy

기존 mock/legacy 가구처럼 source type이 없는 항목은 현재 category 기반 동작을 유지하고 적층 관계를 추측하지 않는다.

### manage-furniture 저장

`LayoutWorkflowApi`에 초기 Room snapshot 저장 함수를 추가한다.

- 초기 setup: `PUT /api/rooms/{roomId}/layout`
- 활성 미확정 Layout Draft: 기존 `PUT /api/layouts/{layoutId}`

Room 저장 요청은 현재 화면의 전체 furniture 배열을 보내며 `EXISTING`, `RECOMMENDED`, `USER_MODIFIED`, `DELETED` 상태를 손실 없이 직렬화한다. source type도 broad category로 축약하지 않는다.

삭제, 초기화, 회전, drag 종료 시 snapshot 저장을 직렬 queue에 넣는다. 다음 단계 이동 시 queue를 flush하고 최신 snapshot 저장 성공 후에만 이동한다. SPA 내 이전 단계/홈 이동 중 이미 시작된 요청은 취소하지 않으므로, 화면을 다시 열 때 Backend snapshot이 복원된다.

저장 실패 시:

- 현재 로컬 편집 상태는 유지한다.
- 오류 메시지를 표시한다.
- 다음 단계 이동은 막아 오래된 Backend baseline으로 추천하지 않는다.
- 재시도 시 같은 전체 snapshot을 보내므로 멱등적으로 복구된다.

추천 직전에는 Backend Room을 읽기 전에 최신 manage-furniture 저장을 완료한다. 추천 baseline은 저장 응답 또는 그 이후의 Room 조회 결과를 사용해 `DELETED` 가구를 다시 포함하지 않는다.

### 적층 렌더링

RoomViewer에서 visible furniture 전체를 기준으로 각 항목의 layout y를 계산하는 순수 함수를 둔다.

- strict support pair가 아니면 `item.height / 2`
- strict pair이면 `supporter.height + dependent.height / 2`
- x/z 중심이 epsilon을 벗어나면 즉시 floor height
- 삭제된 supporter는 후보에서 제외

가구 데이터 자체의 x/z와 독립 ID는 변경하지 않는다. TransformControls는 기존처럼 x/z만 조작한다. 따라서 dependent를 드래그하면 저장 데이터의 위치가 독립적으로 바뀌고 다음 렌더에서 적층이 해제된다.

## 데이터 흐름

### 초기 가구 관리와 추천

1. ManageFurniture에서 사용자가 삭제/이동/회전한다.
2. Web이 local mirror를 갱신하고 Room 전체 snapshot 저장을 queue에 넣는다.
3. `PUT /api/rooms/{roomId}/layout`이 status와 pose를 저장한다.
4. 다음 단계 이동 시 pending 저장을 flush한다.
5. 추천 직전에 Backend Room baseline을 읽는다.
6. Backend 추천은 `DELETED` 가구를 제외하고 semantic 후보를 생성한다.
7. Web이 Backend x/z 결과를 변환하고 strict pair의 y만 상판 높이로 올린다.

### 재편집

활성 Layout이 있으면 기존 Draft lifecycle을 유지한다. Room endpoint와 Layout endpoint를 혼용하지 않고, 편집 snapshot은 `PUT /api/layouts/{layoutId}`로 저장한다.

## 테스트 전략

### Backend

- desk가 monitor보다 먼저 배치되고 두 중심이 일치한다.
- media_console이 tv보다 먼저 배치되고 두 중심이 일치한다.
- strict support pair는 collision-free이고 부분 겹침은 collision으로 남는다.
- nightstand/side_table/mood_lamp가 anchor 근처 후보를 우선 사용한다.
- plant가 corner 후보를 우선 사용한다.
- 대형 가구가 벽에 붙고 해당 벽에서 방 안쪽 회전을 가진다.
- opening과 충돌하는 벽 후보를 건너뛴다.
- window가 없는 curtain_blind는 중앙 grid에 놓이지 않는다.
- 관계 후보가 막히면 4x4 grid fallback으로 배치된다.
- 기존 추천/검증/feedback/confirm 전체 테스트가 회귀 없이 통과한다.

### Web

- Backend source type이 Furniture에 보존되고 Room 저장 요청에도 유지된다.
- Room 저장 요청이 전체 ID, center-to-corner 좌표 변환, rotation, `DELETED`를 보존한다.
- 초기 setup에서 삭제 후 저장, 재진입 load, 추천 전 baseline에 삭제 상태가 유지된다.
- 활성 Draft에서는 Room endpoint 대신 Layout endpoint를 사용한다.
- 저장 실패가 로컬 상태를 지우지 않고 다음 단계 이동을 막는다.
- monitor/TV는 strict pair일 때만 supporter 상판 위 y를 가진다.
- dependent x/z 이동 또는 supporter 삭제 시 floor y로 돌아간다.
- 기존 blind layout position과 TransformControls 동작이 유지된다.
- `npm run test:run`, `npm run lint`, `npm run build`가 통과한다.

## 범위 제외 및 후속 작업

- lifestyleGoal 기반 수면/업무/미디어 zone 분할
- sofa가 TV/media_console을 바라보는 target-facing 회전
- rug + sofa + multi_table의 복합 composition
- 동일 타입 여러 개의 zone 분산
- 후보 전체의 미학 점수화와 최적 후보 비교
- 서버 재시작을 견디는 영구 DB 전환

Phase 3은 이번 후보 정책과 테스트가 안정화된 뒤 별도 설계/PR로 진행한다. Phase 4도 이번에는 벽에서 방 안쪽을 보는 4A까지만 포함하고, 가구가 다른 가구를 바라보는 4B는 zone planner와 함께 다룬다.
