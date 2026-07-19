# Layout feedback clarification contract

`POST /api/layouts/feedback` accepts `layoutId`, `feedback`, and an optional
`selectedFurnitureId`. The optional ID is an internal retry handle and must be
sent only after the user explicitly chooses a **target** candidate.

## Clarifications

Each clarification has `reasonCode`, `question`, optional `operationId`,
`requiredField`, and `candidates`. A candidate is `{ furnitureId, type, label }`.
`furnitureId` is a request handle only; clients must render `label` (or a safe
ordinal fallback), never the ID.

| Meaning | reasonCode | requiredField | candidates | Client retry |
| --- | --- | --- | --- | --- |
| Ambiguous target | `AMBIGUOUS_TARGET` | `targetFurnitureId` | active matching furniture, max 10 | Send selected `furnitureId` once as `selectedFurnitureId` |
| Ambiguous reference | `AMBIGUOUS_REFERENCE_TARGET` | `referenceTargetFurnitureId` | active matching reference furniture | Do not send `selectedFurnitureId`; ask for a clearer reference expression |
| Other target details needed | `NEEDS_CLARIFICATION` | `targetFurnitureId` | may be empty | Do not infer an ID |
| Product/catalog failure | `NO_MATCHING_PRODUCT`, `NO_RENDERABLE_PRODUCT`, `NO_SAFE_SWAP_CANDIDATE`, `NO_LARGER_PRODUCT_AVAILABLE`, `NO_SMALLER_PRODUCT_AVAILABLE`, or placement failure | absent | absent | Do not treat as target ambiguity or send `selectedFurnitureId` |

`NO_SAFE_SWAP_CANDIDATE` is emitted only after a target has been resolved and
same-type replacement products have been considered, but no candidate can be
placed safely. It is not an ambiguity response; `requiredField` and
`candidates` remain absent. Size-only candidate exhaustion continues to use
`NO_LARGER_PRODUCT_AVAILABLE` or `NO_SMALLER_PRODUCT_AVAILABLE`.

## Layout and operation semantics

- A successful execution persists a new layout and returns its newest `layoutId`.
- Clarification or failure returns the source layout's `layoutId`; no new layout
  is saved and the source layout is unchanged.
- `operationResults[].status` is `APPLIED`, `NEEDS_CLARIFICATION`, `FAILED`, or
  `SKIPPED_DEPENDENCY`. Atomic rollback makes prior tentative operations
  `FAILED` with `reasonCode: ATOMIC_ROLLBACK`; no operation remains applied.
- `feedbackStatus` is `SUCCESS`, `NEEDS_CLARIFICATION`, or `FAILED` for the
  atomic feedback path. Clients must retain the current visual layout unless
  the response contains an applied result and a newer `layoutId`.
