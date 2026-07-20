# Relative Furniture Addition and Support Placement Design

## Goal

Allow every safely placeable furniture request to be added to an uploaded room even when unchanged scanned furniture already has validation problems. Keep monitor/TV support height stable, and make the displayed score and validation result describe whether the recommendation introduced new problems.

## Scope

- Backend initial recommendations and re-edit additions.
- Backend score, validation, snapshot, update, feedback, and confirmation responses.
- Frontend monitor/TV support-height inference.
- No database schema changes and no automatic movement of scanned furniture.

## Relative validation

`ValidationService.validateChange(room, baseline, candidate)` compares the final candidate layout with the room's persisted furniture baseline.

- New or position/rotation/status/shape-modified active furniture is evaluated.
- Unchanged baseline furniture may retain pre-existing collision, boundary, opening-clearance, or path warnings without failing a new recommendation.
- Evaluated furniture must stay inside the room and outside door/window clearance zones.
- A collision fails when at least one member of the colliding pair is evaluated. Rug overlays and valid support pairs remain exempt.
- If the baseline path was valid, the final path must remain valid. If it was already invalid, evaluated furniture must not independently create a path blocker.
- When baseline problems are excluded, the response warning states that unchanged existing issues were excluded from the new-placement evaluation.

Recommendation, draft update, snapshot, feedback, and confirmation responses use this relative result for `validationResult` and `scoreSummary`. This makes the existing frontend refresh path show the current result and allows a safe change to be rated `양호` without claiming that scanned geometry was repaired.

## Candidate search and partial success

Re-edit additions execute one requested furniture operation at a time instead of one atomic composite operation.

- Requests are dependency ordered: desk/media console before monitor/TV.
- Each normal furniture product searches semantic wall candidates first and then a center-out 4×4 interior grid at supported rotations.
- Monitor requires an active desk; TV requires an active media console. Their first placement is centered on the support and uses its rotation. They are not placed on the floor when the support is absent.
- Successful operations stay in the draft when a later request fails.
- The response reports `SUCCESS`, `PARTIAL_SUCCESS`, or `FAILED`, requested/placed counts, and per-item failure details.
- `FAILED` persists no change; partial and successful additions persist their resulting draft.

Initial rule-based recommendations use the same safe-addition predicate so unchanged uploaded-room violations do not force 0/N.

## Support placement

A desk supports a monitor and a media console supports a TV when the dependent center lies within the rotated top footprint of the active supporter.

- Backend collision validation and frontend height rendering use this same center-on-top rule.
- Supported render height is `supporter.height + dependent.height / 2`.
- Initial placement remains centered.
- Independent movement within the top footprint remains supported; movement outside it drops the item back to floor height.
- Deleting the supporter removes support.

## Error handling

- A request with no safely placeable item returns a normal failed recommendation result instead of a server exception.
- A partially placeable request returns the placed furniture plus failure details for the rest.
- Existing validation debt remains visible as a warning but does not block unrelated safe furniture.
- Actual new collisions, boundary violations, opening blockage, or new path blockage remain hard failures.

## Verification

Focused backend tests cover:

- an uploaded baseline with a pre-existing violation accepting one non-colliding chair;
- a four-item request persisting only placeable items;
- a genuinely colliding addition being rejected;
- relative validation driving score and confirmation;
- monitor/TV support pairs and missing-support failures.

Focused frontend tests cover:

- monitor and TV height within their supporter footprint;
- slight independent movement on the top surface;
- movement outside the footprint returning to floor height;
- deleted supporters.

Per the user's request, the TDD RED cycle is skipped and only focused verification commands are run.
