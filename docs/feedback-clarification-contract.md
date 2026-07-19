# Feedback clarification and failure contract

`selectedFurnitureId` is an optional UI selection used only to resolve target ambiguity. The backend accepts it only when it identifies an active furniture item whose canonical type matches the requested target. It never substitutes a different provider target and never resolves reference or product ambiguity.

## Target ambiguity

- Duplicate active items of the same canonical type return `NEEDS_CLARIFICATION` with reason `AMBIGUOUS_TARGET`.
- `requiredField` is `targetFurnitureId` and `candidates` contains user-displayable candidates only for this target ambiguity.
- Retrying the same feedback with a valid `selectedFurnitureId` executes against exactly that target.
- Missing, inactive, or wrong-type selections do not mutate furniture and do not create a Layout.

## Reference and product ambiguity

- `AMBIGUOUS_REFERENCE_TARGET` is separate from target selection. `selectedFurnitureId` is never applied to `referenceTarget`.
- Product failures such as `NO_SAFE_SWAP_CANDIDATE`, `NO_LARGER_PRODUCT_AVAILABLE`, and `NO_SMALLER_PRODUCT_AVAILABLE` are `FAILED`, not target clarifications. They have no target chooser or candidates.

## Composite and snapshot behavior

- Rule-based composite operations keep sentence order and link each later operation to its predecessor.
- Execution is atomic. Any operation failure rolls back earlier operations and preserves the complete source Layout.
- A successful feedback creates a derived Layout. Repeating an identical request against the same source reuses an identical derived snapshot instead of creating duplicates.
- Clarification and failure return the source `layoutId`; successful feedback returns the derived `layoutId`.

## Direction aliases

- `왼쪽`, `좌측`, `왼편` map to `LEFT` or `LEFT_OF`.
- `오른쪽`, `우측`, `오른편` map to `RIGHT` or `RIGHT_OF`.
- `구석`, `모서리`, `코너`, `방 모서리` map to `IN_CORNER`.
- An impossible directional move returns `NO_VALID_MOVE_PLACEMENT`; the executor never tries the opposite direction.
