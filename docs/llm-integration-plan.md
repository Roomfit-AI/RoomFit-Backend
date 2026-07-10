# LLM Integration Plan

## 1. 목적

RoomFit MVP의 LLM 적용 목적은 AI/SW 경진대회에서 RoomFit의 AI Agent 성격을 강화하는 것이다. LLM은 사용자 자연어 피드백을 구조화된 intent/constraint로 변환하고, 추천 배치에 대한 자연어 설명을 생성하는 데 사용한다.

기존 rule-based layout generation과 validation 안정성은 유지한다. RoomFit의 핵심 좌표 계산과 충돌 검증은 이미 안정적인 rule-based 로직과 validation pipeline이 담당하므로, LLM은 사용자의 의도를 이해하고 추천 결과를 설명하는 보조 Agent 역할에 집중한다.

## 2. 원칙

- LLM이 직접 `x`, `z`, `rotation` 좌표를 생성하지 않는다.
- LLM은 자연어 이해, 의도 분류, 제약조건 추출, 설명 생성에만 사용한다.
- 실제 좌표 계산은 기존 rule-based placement 로직이 담당한다.
- 최종 결과는 항상 기존 `validationResult`를 통해 검증한다.
- LLM 실패 시 기존 rule-based fallback을 사용한다.

## 3. 1차 적용 지점: Feedback Intent Parsing

대상 API:

```http
POST /api/layouts/feedback
```

현재 요청:

```json
{
  "layoutId": 2,
  "feedback": "책상 더 크게"
}
```

LLM 출력 목표 예시:

```json
{
  "intent": "ENLARGE_FURNITURE",
  "targetFurniture": "desk",
  "constraints": {
    "minWidth": 1.4
  },
  "priority": "study_comfort"
}
```

LLM은 `feedback` text를 structured intent JSON으로 변환한다. 기존 feedback rule logic은 fallback으로 유지한다. Layout generation은 이 intent를 참고하되, 새 layout 생성은 기존 알고리즘으로 수행한다.

즉, LLM은 “책상 더 크게”, “수납을 늘리고 싶어”, “방이 답답해 보여” 같은 표현을 기계가 다루기 쉬운 intent와 constraint로 변환하는 역할만 맡는다.

## 4. 2차 적용 지점: Recommendation Reason Generation

대상 후보:

```http
POST /api/layouts/recommend
POST /api/layouts/feedback
```

목표는 추천된 배치에 대해 사용자가 이해하기 쉬운 설명을 생성하는 것이다.

예시:

```text
선택한 미니멀/화이트톤 스타일에 맞춰 책상과 의자를 방 안쪽 벽면에 배치했습니다. 침대와 책상 사이 동선을 확보하고, 조명은 공부 공간 근처에 배치했습니다.
```

이 설명은 `recommendedFurniture`, `scoreSummary`, `validationResult`, 선택한 스타일/제품 정보를 바탕으로 생성한다. 단, 설명 생성은 validation을 대체하지 않으며, 실제 배치의 정합성은 기존 validation 결과를 기준으로 판단한다.

## 5. 적용하지 않을 범위

- LLM이 전체 layout JSON을 생성하지 않는다.
- LLM이 가구 좌표를 직접 결정하지 않는다.
- LLM이 validation을 대체하지 않는다.
- LLM이 제품 DB를 대체하지 않는다.
- 이미지 분석 모델은 지금 단계에서 붙이지 않는다.

## 6. 안정성 전략

- JSON schema 기반 응답 강제
- 파싱 실패 시 fallback
- timeout 설정
- API key는 환경변수로 관리
- LLM 응답 로그는 디버깅용으로만 제한적으로 사용
- 사용자 입력이 이상해도 500이 아니라 기존 error response 또는 fallback으로 처리

LLM 호출은 외부 의존성이므로 실패 가능성을 기본 전제로 둔다. 네트워크 오류, timeout, JSON parsing 실패, schema 불일치가 발생해도 추천 API 전체가 실패하지 않도록 rule-based fallback을 유지한다.

## 7. 향후 구현 구조 제안

예상 클래스:

- `FeedbackIntentParser` interface
- `RuleBasedFeedbackIntentParser`
- `LlmFeedbackIntentParser`
- `FeedbackIntent` DTO
- `RecommendationReasonGenerator`
- `LlmClient` 또는 `OpenAiClient`

예상 흐름:

```text
FeedbackService
-> FeedbackIntentParser
-> Placement/Recommendation Logic
-> Validation
-> LayoutResponse or FeedbackLayoutResponse
```

`FeedbackIntentParser`는 rule-based 구현과 LLM 기반 구현을 교체 가능하게 둔다. 초기에는 rule-based parser를 기본값으로 유지하고, LLM parser는 환경변수 또는 profile 기반으로 활성화하는 방식이 안전하다.

## 8. API 계약 영향

- 1차 구현에서는 기존 request/response 구조를 최대한 유지한다.
- 필요 시 `interpretedIntent` 필드를 확장한다.
- `recommendationReason`은 선택 필드로 추가하는 방안을 검토한다.
- 프론트가 기존 기능을 깨지 않도록 optional field 중심으로 확장한다.

API 응답 확장은 기존 프론트가 무시할 수 있는 optional field로 진행한다. 예를 들어 feedback 응답의 `interpretedIntent` 내부 구조를 풍부하게 하거나, recommend/feedback 응답에 `recommendationReason`을 nullable 또는 optional string으로 추가하는 방식을 우선 검토한다.

## 9. MVP 적용 우선순위

Priority 1:
Feedback Intent Parsing

Priority 2:
Recommendation Reason Generation

Priority 3:
사용자 context summary 생성

Priority 4:
실제 제품/스타일 데이터 기반 고도화

## 10. 결론

RoomFit의 LLM 적용은 “좌표 생성 AI”가 아니라 “사용자 의도 해석 + 설명 가능한 추천 Agent” 방향으로 간다.

이 방향은 데모 안정성과 AI 설득력을 동시에 확보한다. 좌표 생성과 충돌 검증은 deterministic rule-based logic이 맡고, LLM은 자연어를 구조화하고 결과를 설명하는 Agent 역할을 맡는다.
