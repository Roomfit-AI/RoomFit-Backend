# Mock Products API Design

## Scope

Implement only `GET /api/products/mock` as the first independent RoomFit AI backend slice. The endpoint returns selectable MVP mock product cards for all supported furniture types: `bed`, `desk`, `chair`, `storage`, `rug`, and `lamp`.

This slice does not implement style images, Agent Context creation, recommendation, validation, update, feedback, confirmation, authentication, real shopping integrations, RoomPlan upload, or LLM calls.

## Source Of Truth

The final RoomFit AI backend API specification v1.0 is authoritative for endpoint, response wrapper, field names, and allowed furniture type values. The referenced Dcom intranet server repository is used only as a folder-structure reference. Its relevant pattern is domain-first packages with role subpackages such as `controller`, `domain`, `dto/request`, `dto/response`, `repository`, and `service`.

## Package Structure

Create a new product domain under `com.roomfit.product`:

- `product/controller`: Spring MVC endpoint classes.
- `product/domain`: product model objects such as `MockProduct` and `RequiredClearance`.
- `product/dto/response`: response DTOs exposed to the API.
- `product/repository`: in-memory mock product lookup.
- `product/service`: read-only product query service.

No existing `room`, `agent`, or `placement` package should receive product-specific responsibilities in this slice.

## API Contract

Endpoint:

`GET /api/products/mock`

Success:

- HTTP status: `200`
- Body: `CommonResponse<List<MockProductResponse>>`
- `success`: `true`
- `data`: list of mock products
- `error`: `null`

Each product response includes only the spec fields:

- `productId`
- `type`
- `name`
- `brand`
- `width`
- `depth`
- `height`
- `price`
- `styleTags`
- `imageUrl`
- `requiredClearance.front`
- `requiredClearance.side`

The spec does not define an error case for this read-only list endpoint, so no endpoint-specific error code is added.

## Mock Data

Seed six products in memory, one per MVP furniture type. Preserve the specification examples for:

- `desk-01`
- `chair-01`
- `lamp-01`

Add minimal spec-shaped entries for:

- `bed`
- `storage`
- `rug`

The added products use only existing allowed furniture type values and only fields defined in the API specification.

## Data Flow

The controller delegates to the service. The service reads all products from the repository and maps domain objects to response DTOs. The controller wraps the response with `CommonResponse.ok(data)`.

The repository is read-only for this slice and may store data as an immutable in-memory list. A `findById` method is not required yet unless the Agent Context slice needs it.

## Testing

Add focused MVC tests for `GET /api/products/mock`:

- returns HTTP 200
- returns `success=true`
- returns `error=null`
- returns all six supported furniture types
- includes the required nested `requiredClearance.front` and `requiredClearance.side` fields
- preserves example product ids `desk-01`, `chair-01`, and `lamp-01`

Run the full test suite after implementation.

## Open Decisions

None. The user approved using MVP-wide mock data coverage for all six furniture types.
