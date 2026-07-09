# OpenAPI Documentation Design

## Goal

Document the implemented RoomFit AI MVP backend APIs in a static OpenAPI 3.0 file without adding runtime Swagger dependencies.

## Scope

- Include all implemented API endpoints.
- Include request/response schemas and representative examples.
- Keep response wrappers aligned with `CommonResponse<T>`.
- Exclude Swagger UI runtime integration and springdoc dependency setup.

## Output

- `docs/openapi/roomfit-api.yaml`

## Validation

A lightweight test checks that the OpenAPI file exists and contains the implemented paths plus key frontend-sensitive fields:

- `recommendedFurniture`
- `validationResult`
- `scoreSummary`
- `interpretedIntent`
- `CommonResponse`

