# 다중 사용자 격리 및 배포 준비 감사

## 2026-07-18 코드 감사 결과

이 변경 전에는 로그인·사용자 ID·세션 ID가 없었고, `RoomRepository`와
`LayoutRepository` 모두 전역 ID 조회만 수행했다. 따라서 `/api/rooms/uploads/recent`는
모든 `ROOMPLAN` Room을 반환했고, Room ID 또는 Layout ID를 아는 클라이언트는 다른
브라우저의 Room 조회·수정·삭제와 Layout 조회·수정·Draft·confirm·feedback을 수행할 수
있었다. `AgentContext`도 Room ID만 보관하며 별도 소유자는 없었다.

| 대상 | 감사 당시 동작 | 현재 보호 방식 |
| --- | --- | --- |
| Room upload/read/update/delete/recent list | 전역 ID/전역 목록 | `clientScope` UUID와 현재 요청 scope 비교 |
| AgentContext/recommendation | `roomId`, `contextId` 전역 조회 | Context 생성과 추천 모두 소유 Room 쓰기 권한 확인 |
| Layout read/validate/update/draft/confirm/feedback/latest | `layoutId` 전역 조회 | Layout의 Room 소유권을 확인; 타 client에는 404 |
| 샘플 Room | 일반 `Room` Entity의 `SAMPLE` row, 직접 수정 가능 | 모든 client 읽기 가능, UUID header client는 `/copy` 후 편집 |

`Layout`은 별도 사용자 ID를 중복 저장하지 않고, 생성·Draft·Feedback 모두 소속
`Room`의 scope를 상속한다. Layout을 읽거나 변경할 때마다 소속 Room 권한을 확인하므로
Layout ID만으로 scope를 우회할 수 없다. 이는 Room의 소유자가 변경되지 않는 현재
모델에서 중복 필드보다 안전하다.

`DELETED` 가구 정책, confirmed source Layout 불변성, Recommendation/Feedback 응답
계약은 변경하지 않았다.

## 익명 client header 계약

`X-RoomFit-Client-Id`는 canonical UUID여야 한다. 잘못된 형식과 64자를 넘는 값은
`400 INVALID_CLIENT_ID`다. 설정은 다음과 같다.

```yaml
roomfit.client-scope.enabled: true
roomfit.client-scope.required: false
```

`enabled=true, required=false`가 현 배포 기본값이다. header가 없거나 공백이면 기존
클라이언트를 위한 `legacy` scope로 처리한다. 이는 현재 RoomPlan과 웹이 같은 익명 ID를
전달하지 않는 동안의 호환 계층이다. UUID header가 있는 client는 legacy 또는 다른 UUID
소유 Room을 볼 수 없다. `required=true` 전환은 Web과 RoomPlan 모두가 같은 UUID를
명시적으로 전달한 뒤에만 수행한다.

샘플은 읽기 전용이다. UUID client는 `POST /api/rooms/{sampleRoomId}/copy`로 복제본을
만들고 그 ID로 Context·추천·피드백을 진행해야 한다. header 없는 legacy 경로에서만
기존 샘플 데모 편집을 유지한다. 이 예외는 Frontend 전환 완료 후 `required=true`로
제거할 수 있다.

## RoomPlan → Web 조사와 handoff

Backend의 RoomPlan 업로드 계약은 `POST /api/rooms/upload` 응답의 `roomId`뿐이다.
Repository, controller, 문서에서 QR·URL·deep link·공유 코드·세션 전달은 발견되지
않았다. 즉 현재 iOS 앱에서 업로드한 legacy Room을 Web이 header UUID로 열 수는 없다.

이번 Backend 변경은 opt-in이므로 RoomPlan 앱을 수정하지 않았고 `required=true`도
설정하지 않는다. 후속 연동 계약은 다음 중 하나를 선택해야 한다.

1. Web이 최초 실행 시 UUID를 생성·보관하고, RoomPlan 실행/업로드에도 동일 UUID를
   전달한다. QR/deep link에는 raw roomId만 넣지 말고 client UUID 전달을 위한 안전한
   handoff payload를 사용한다.
2. 서로 다른 기기라면 서버가 발급하는 짧은 만료의 1회용 transfer code를 추가하고,
   Web이 code를 교환해 Room의 scope를 자신의 UUID로 이전한다. roomId만으로 접근을
   허용하는 우회는 만들지 않는다.

## 통합 검증 범위

`ClientScopeIsolationControllerTest`는 A/B UUID로 Room 생성·목록, cross-client
Room read/update/delete, 샘플 read-only/copy, recommendation Layout read/confirm,
confirmed Layout Draft 생성/update/confirm, feedback 차단을 검증한다. header 없는
legacy 요청, 잘못된·공백·과도하게 긴 header, CORS preflight도 포함한다.

## 운영 관측성

모든 HTTP 응답에는 `X-Request-Id`가 있다. 클라이언트가 안전한 형식의 request ID를
보내면 그대로 반환하고, 아니면 UUID를 발급한다. 오류 body에는 기존 `error.code`와
`error.message`를 유지한 채 additive `error.requestId`가 포함된다. 서버 로그는
request ID, method, path, HTTP status, duration만 기록하며 client ID·API key·prompt·
이미지 원문은 기록하지 않는다. `/health`는 service와 build version
(`ROOMFIT_BUILD_VERSION`, 기본 `unknown`)을 반환한다.

## 배포 smoke 실행

```bash
chmod +x scripts/deployment-smoke.sh
ROOMFIT_BASE_URL=https://your-backend.example \
ROOMFIT_CLIENT_A=<uuid-a> ROOMFIT_CLIENT_B=<uuid-b> \
scripts/deployment-smoke.sh
```

스크립트는 health, A/B Room 생성과 목록 격리, 교차 Room/Layout/confirm 404, A의
recommendation 저장·재조회·confirm, B Room 불변을 확인한다. 안전하지 않은 고아
Layout 삭제를 피하기 위해 생성 ID를 출력하고 자동 삭제하지 않는다.
