# Furniture recommendation backend smoke QA

Catalog checked: `2026-07-18.2` (`93` generated variants, `21` canonical types).

This is a backend contract check only. A passing `renderable metadata` column means
that the response has a catalog `productId`/`variantId` and visual footprint; it
does **not** assert WebGL rendering.

| furnitureType | request recognized | catalog candidate | renderable metadata | placement | boundary validation | persistence | confirm/room reload | result | failure classification |
|---|---|---|---|---|---|---|---|---|---|
| bed | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| bookshelf | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| curtain_blind | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| desk | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| desk_chair | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| drawer_chest | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| full_length_mirror | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| hanger | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| media_console | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| monitor | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| mood_lamp | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| multi_table | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| nightstand | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| partition_shelf | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| plant | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| rug | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| side_table | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| sofa | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| sofa_bed | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| tv | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |
| wardrobe | yes | yes | yes | yes | yes | covered by recommendation lifecycle | covered by recommendation lifecycle | PASS | — |

## Deterministic search and failure policy

- Each request instance is evaluated in request order. Existing active furniture can satisfy one matching request without mutating the Room.
- New furniture uses the existing type-specific candidates followed by a bounded 4×4 grid (maximum 19 position attempts per request). There is no unbounded or combinatorial search.
- Every candidate is validated against the already accepted snapshot for collision, wall/boundary, door, window, and path clearance. Failed candidates never enter the snapshot.
- A request with no physical placement is normal API output, not HTTP 500. The result carries one of `NO_VALID_BOUNDARY_PLACEMENT`, `COLLISION_DETECTED`, `DOOR_BLOCKED`, `WINDOW_BLOCKED`, `MOVEMENT_PATH_BLOCKED`, or `NO_VALID_PLACEMENT`.
- Duplicate requested types are supported as independent request instances; IDs use `<canonical-type>-rec-<sequence>` and are collision-checked against earlier additions.

## Room-size matrix

| room | request | expected/result |
|---|---|---|
| large (30m × 30m) | each canonical type individually | `SUCCESS`, one rendered-metadata placement |
| constrained (3m × 2.8m) | bed + sofa | `PARTIAL_SUCCESS`; bed is retained and sofa is returned in stable `unplacedFurniture` order |
| small (1m × 1m) | bed | `FAILED`, `NO_VALID_BOUNDARY_PLACEMENT`, no Layout snapshot |

## Room creation regression

`POST /api/rooms/upload` constructs a new `Room` with a null generated ID and never accepts a request room ID. The regression test creates Room A then Room B, verifies different IDs, and reloads Room A unchanged. There is no sample-room creation endpoint; seeded samples are created only by the backend initializer. A deployed “new sample overwrites an old room” symptom is therefore more consistent with a client retaining an ID and issuing a `PUT` update flow than with this POST endpoint.
