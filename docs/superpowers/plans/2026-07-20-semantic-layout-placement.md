# Semantic Layout Placement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make empty-room recommendations deterministic and relationship-aware, including strict desk-monitor and media-console-TV stacking, anchor-adjacent placement, wall/corner snapping, and inward rotations.

**Architecture:** Keep the existing sequential recommendation pipeline and introduce an internal `PlacementCandidate(position, rotation, relation, order)` model. Semantic candidates run before the existing safe/grid fallback, while a focused support policy is shared by recommendation and 2D collision validation.

**Tech Stack:** Java 21, Spring Boot 3.5, JUnit 5, AssertJ, Gradle

## Global Constraints

- Preserve `requiredItems`/`optionalItems`, partial-success, product selection, generated variant, feedback, validate, update, and confirm contracts.
- Use only `desk -> monitor` and `media_console -> tv` as supported stack pairs.
- A stack exemption requires center x/z equality within `1.0e-6` and complete containment of the dependent rotated footprint in the supporter rotated footprint.
- Do not add a stack relation field to API responses.
- Candidate priority is semantic relation, then wall/corner, then current safe candidates, then the current deterministic 4x4 grid.
- Wall-facing rotations are south `0`, east `90`, north `180`, west `270` degrees in Backend coordinates.
- No random placement.
- `curtain_blind` without a usable window must not enter generic/grid fallback.
- Do not implement lifestyle zones or furniture-to-furniture target-facing rotations.

---

### Task 1: Backend semantic candidate placement and strict support validation

**Files:**
- Create: `src/main/java/com/roomfit/placement/FurnitureSupportPolicy.java`
- Modify: `src/main/java/com/roomfit/placement/RuleBasedPlacementService.java`
- Modify: `src/main/java/com/roomfit/placement/ValidationService.java`
- Test: `src/test/java/com/roomfit/placement/FurnitureSupportPolicyTest.java`
- Test: `src/test/java/com/roomfit/placement/RuleBasedSemanticPlacementTest.java`
- Test: `src/test/java/com/roomfit/placement/ValidationServiceTest.java`

**Interfaces:**
- Consumes: `FurnitureBoundary.footprint(Furniture)`, `FurnitureBoundary.usableBounds(Room)`, `GeneratedFurnitureCatalog.sameType`, existing `ValidationService.validate`.
- Produces: package-private `FurnitureSupportPolicy.isStrictStack(Furniture, Furniture): boolean`; internal `PlacementCandidate(Position, double, PlacementRelation, int)`; recommendation results with semantic x/z and cardinal rotation.

- [ ] **Step 1: Write failing strict-support tests**

Add tests covering both accepted pairs, reversed argument order, partial overlap, wrong pair, moved dependent, and deleted furniture. The assertions must be equivalent to:

```java
@Test
void monitorCenteredAndContainedOnDeskIsStrictStack() {
    Furniture desk = furniture("desk", 1.2, 0.7, 0.75, 2.0, 2.0, 0);
    Furniture monitor = furniture("monitor", 0.5, 0.2, 0.35, 2.0, 2.0, 0);
    assertThat(FurnitureSupportPolicy.isStrictStack(desk, monitor)).isTrue();
    assertThat(FurnitureSupportPolicy.isStrictStack(monitor, desk)).isTrue();
}

@Test
void supportedTypesAreNotAStackAfterIndependentMove() {
    Furniture desk = furniture("desk", 1.2, 0.7, 0.75, 2.0, 2.0, 0);
    Furniture monitor = furniture("monitor", 0.5, 0.2, 0.35, 2.01, 2.0, 0);
    assertThat(FurnitureSupportPolicy.isStrictStack(desk, monitor)).isFalse();
}
```

Extend `ValidationServiceTest` so a strict stack is collision-free while a `0.01m` moved dependent and an oversized dependent remain collisions.

- [ ] **Step 2: Run strict-support tests and verify RED**

Run:

```bash
./gradlew test --tests com.roomfit.placement.FurnitureSupportPolicyTest --tests com.roomfit.placement.ValidationServiceTest
```

Expected: FAIL because `FurnitureSupportPolicy` does not exist and strict supported overlap is currently reported as collision.

- [ ] **Step 3: Implement the minimal strict support policy**

Create a stateless package-private utility with this complete behavior:

```java
final class FurnitureSupportPolicy {
    static final double CENTER_EPSILON = 1.0e-6;

    private FurnitureSupportPolicy() {}

    static boolean isStrictStack(Furniture first, Furniture second) {
        SupportPair pair = SupportPair.resolve(first, second);
        if (pair == null || !active(pair.supporter()) || !active(pair.dependent())) return false;
        if (Math.abs(pair.supporter().getPosition().getX() - pair.dependent().getPosition().getX()) > CENTER_EPSILON
                || Math.abs(pair.supporter().getPosition().getZ() - pair.dependent().getPosition().getZ()) > CENTER_EPSILON) return false;
        FurnitureBoundary.Footprint base = FurnitureBoundary.footprint(pair.supporter());
        FurnitureBoundary.Footprint top = FurnitureBoundary.footprint(pair.dependent());
        return top.minX() >= base.minX() - CENTER_EPSILON
                && top.maxX() <= base.maxX() + CENTER_EPSILON
                && top.minZ() >= base.minZ() - CENTER_EPSILON
                && top.maxZ() <= base.maxZ() + CENTER_EPSILON;
    }
}
```

`SupportPair.resolve` must canonicalize types, accept either argument order, and return only desk/monitor or media_console/tv. Update `ValidationService.checkCollisionFree` to skip a pair only when `isStrictStack` returns true; keep every other overlap unchanged.

- [ ] **Step 4: Run strict-support tests and verify GREEN**

Run the Step 2 command. Expected: PASS.

- [ ] **Step 5: Write failing semantic recommendation tests**

Create a service-level test fixture using catalog products and an empty rectangular Room. Cover:

```java
@Test void requestOrderDoesNotPreventDeskMonitorStack();
@Test void requestOrderDoesNotPreventMediaConsoleTvStack();
@Test void nightstandUsesBedSideCandidate();
@Test void sideTableUsesSofaSideCandidate();
@Test void plantUsesUsableCornerCandidate();
@Test void bulkyFurnitureUsesWallCandidateAndFacesInward();
@Test void openingBlockedWallFallsThroughToAnotherWall();
@Test void curtainBlindWithoutWindowIsUnplacedInsteadOfUsingGrid();
@Test void blockedSemanticCandidateFallsBackToDeterministicGrid();
```

For stack tests, locate results by canonical type and assert exact x/z equality plus supporter-first behavior independent of request order. For wall tests, assert the selected center touches one usable-bound side after applying its rotated footprint and that rotation matches the side table in Global Constraints. For anchor-side tests, rotate the anchor through at least `0` and `90` degrees and assert the dependent is outside the anchor footprint on its local left/right/front vector.

- [ ] **Step 6: Run semantic tests and verify RED**

Run:

```bash
./gradlew test --tests com.roomfit.placement.RuleBasedSemanticPlacementTest
```

Expected: FAIL because the service returns position-only generic candidates and rotation `0`.

- [ ] **Step 7: Implement candidate ordering, rotation, semantic anchors, wall/corner candidates, and fallback**

Inside `RuleBasedPlacementService`, replace `List<Position>` candidate flow with:

```java
private enum PlacementRelation { SUPPORT, ANCHOR_SIDE, ANCHOR_FRONT, WALL, CORNER, SAFE, GRID, WINDOW }
private record PlacementCandidate(Position position, double rotation,
                                  PlacementRelation relation, int order) {}
```

Stable-sort requested canonical types by dependency rank: anchors `0`, ordinary items `1`, dependents `2`; preserve original order within a rank. Anchors are bed, desk, sofa, sofa_bed, media_console. Dependents are nightstand, desk_chair, monitor, side_table, tv, mood_lamp.

Build candidates in this exact order:

```java
List<PlacementCandidate> candidates = new ArrayList<>();
candidates.addAll(relationshipCandidates(itemType, spec, placed));
candidates.addAll(environmentCandidates(itemType, spec, room));
candidates.addAll(safeCandidates(itemType, spec, room, placed));
candidates.addAll(gridCandidates(spec, room));
```

For each candidate, create the prototype with candidate rotation before `FurnitureBoundary.clamp`, then recreate the validated candidate with the clamped position and the same rotation. Deduplicate by x/z/rotation, not x/z alone.

Relationship vectors for cardinal anchor rotation `r` are:

```java
Position forward(double r) { return switch ((int) normalize(r)) {
    case 90 -> new Position(-1, 0); case 180 -> new Position(0, -1);
    case 270 -> new Position(1, 0); default -> new Position(0, 1); } }
Position right(double r) { return switch ((int) normalize(r)) {
    case 90 -> new Position(0, -1); case 180 -> new Position(-1, 0);
    case 270 -> new Position(0, 1); default -> new Position(1, 0); } }
```

- monitor and tv: exact anchor center and anchor rotation; if anchor absent, continue to safe/grid fallback.
- nightstand and side_table: right then left of anchor with `0.15m` gap and anchor rotation.
- desk_chair: anchor forward with `0.25m` gap and `(anchor.rotation + 180) % 360`.
- mood_lamp: right then left with `0.15m` gap, searching desk then bed then sofa/sofa_bed.

Use `FurnitureBoundary.usableBounds` plus the rotated prototype footprint for wall/corner centers. Wall candidate order is south, east, north, west with rotations `0,90,180,270`; emit center then the two quarter positions along each wall. Apply wall candidates to bed, desk, sofa, sofa_bed, media_console, wardrobe, bookshelf, drawer_chest, hanger, partition_shelf, full_length_mirror, and storage. Plant gets four corners. Curtain/blind gets candidates derived from Room windows and returns only those candidates—when there is no window, return an empty candidate list and do not append safe/grid candidates.

Keep the current type-specific safe positions and 4x4 grid as the last two tiers. Remove the obsolete position-only helper paths only after all semantic tests pass.

- [ ] **Step 8: Run focused semantic and feasibility tests**

Run:

```bash
./gradlew test --tests com.roomfit.placement.RuleBasedSemanticPlacementTest --tests com.roomfit.placement.ValidationServiceTest --tests com.roomfit.placement.RecommendationFeasibilityTest --tests com.roomfit.placement.RuleBasedPlacementVariantIdTest
```

Expected: PASS.

- [ ] **Step 9: Run Backend full verification**

Run:

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL` with no non-LLM test failures.

- [ ] **Step 10: Commit Backend implementation**

```bash
git add src/main/java/com/roomfit/placement/FurnitureSupportPolicy.java \
  src/main/java/com/roomfit/placement/RuleBasedPlacementService.java \
  src/main/java/com/roomfit/placement/ValidationService.java \
  src/test/java/com/roomfit/placement/FurnitureSupportPolicyTest.java \
  src/test/java/com/roomfit/placement/RuleBasedSemanticPlacementTest.java \
  src/test/java/com/roomfit/placement/ValidationServiceTest.java
git commit -m "feat: add semantic furniture placement rules"
```
