# RoomPlan scan import policy

## Reproduction and original failure

The instruction described a 3.39m × 3.42m scan with an east wall at 3.36m,
zero-thickness walls, near-wall furniture, and an existing chair/storage
overlap. The raw JSON was not included with the attachment, so
`RoomPlanImportControllerTest` recreates those measurements without a
thumbnail. Before this change the first failure was
`RoomService.validateFurnitureWithinRoom → FurnitureBoundary.isInside`, which
returned `400 INVALID_FURNITURE_POSITION`. Upload did not perform collision,
opening, or path validation before that boundary failure.

## Final upload-only policy

`RoomPlanImportValidator` applies only to `POST /api/rooms/upload`. It never
changes the normal AI recommendation, feedback, Draft, direct-edit, or confirm
validators.

Each imported `EXISTING` furniture item is processed in request order. Earlier
strict-safe items are fixed. For the next item, the deterministic finite search
tries its original position, its strict interior clamp, a bounded 0.10m local
spiral (radius 1–8), then a bounded whole-room grid. Original rotation is tried
first; only then are 0/90/180/270 alternatives considered. Every candidate is
checked by the existing strict collision, boundary, door/window, and path
validator. The cap is `MAX_CANDIDATE_SEARCHES=4000` per rotation.

No size/type/id/dimension is changed. If no rotation and position can fit, the
single furniture is omitted from the active Room and reported as
`FURNITURE_UNPLACED`; the Room still uploads. Non-finite data, non-positive
Room dimensions, invalid IDs, and unsupported types remain rejected.

The canonical boundary remains `room.width`/`room.depth`; wall endpoints are
preserved scan geometry. A small mismatch is reported as
`ROOM_WALL_DIMENSION_NORMALIZED`. Explicit zero wall thickness is also
preserved (it is not overwritten by a default) and reported as
`ZERO_WALL_THICKNESS_ACCEPTED`.

## Response and strict lifecycle

`RoomResponse` has additive `importStatus` (`ACCEPTED` or
`ACCEPTED_WITH_WARNINGS`) and `importWarnings`. Warning data includes code,
entity/furniture ID, type, original/normalized position and rotation, movement
meters, and message. Current placement codes are `FURNITURE_REPOSITIONED`,
`FURNITURE_ROTATED_AND_REPOSITIONED`, and `FURNITURE_UNPLACED`.

There is no baseline exception persisted after upload. Stored active furniture
already passes strict validation, so recommendation, feedback, add furniture,
direct move/rotate, Draft, and confirm continue to use the unchanged strict
policy. Client UUID ownership is unchanged; header-less RoomPlan uploads stay
in legacy scope until RoomPlan and Web share the same UUID.
