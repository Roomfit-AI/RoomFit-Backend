# Partial Furniture Addition Implementation Plan

> **For Codex:** Execute this plan with `superpowers:executing-plans`. The user explicitly waived the TDD RED step; add focused regression tests after implementation.

**Goal:** Let uploaded-room drafts keep every safely placeable requested item, place screens only on valid supports, and score/validate only violations introduced by the current draft change.

**Architecture:** Add change-relative validation to `ValidationService`, reuse it in placement/execution/layout responses, and change the add workflow from one atomic composite plan to ordered single-item operations. Extend deterministic candidates with center-out interior positions while preserving wall-first behavior.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Gradle.

### Task 1: Support-aware and change-relative validation

**Files:**
- Modify: `src/main/java/com/roomfit/placement/FurnitureSupportPolicy.java`
- Modify: `src/main/java/com/roomfit/placement/ValidationService.java`
- Test: `src/test/java/com/roomfit/placement/ValidationServiceTest.java`

1. Treat a monitor/desk or TV/media-console overlap as a valid support pair when the dependent center is inside the rotated top footprint.
2. Add `validateChange(room, baseline, candidate)` and `isSafeAddition(...)`.
3. Evaluate collisions, bounds, and openings only for new/changed active items; compare path quality against the baseline; warn when pre-existing validation debt is excluded.
4. Add focused tests for support-footprint tolerance and unchanged baseline collision exclusion.

### Task 2: Broaden safe placement candidates

**Files:**
- Modify: `src/main/java/com/roomfit/placement/FeedbackPlacementCandidateGenerator.java`
- Modify: `src/main/java/com/roomfit/placement/DeterministicFeedbackExecutor.java`
- Modify: `src/main/java/com/roomfit/placement/RuleBasedPlacementService.java`
- Modify: `src/main/java/com/roomfit/placement/LlmPlacementService.java`

1. Keep wall candidates first, then append a center-out 4x4 interior grid.
2. Require an existing support for monitor/TV additions and place them at the support center.
3. Replace full-layout acceptance with change-relative safety checks.

### Task 3: Preserve partial successes and return relative score/validation

**Files:**
- Modify: `src/main/java/com/roomfit/placement/LayoutService.java`
- Test: the closest existing layout addition service/controller test.

1. Sort support furniture before dependents.
2. Execute one ADD operation per requested item and retain prior successes.
3. Return SUCCESS, PARTIAL_SUCCESS, or FAILED with unplaced item details; do not throw for normal no-space outcomes.
4. Use change-relative validation for draft recommendation/update/add/feedback/confirmation scoring.

### Task 4: Minimal verification

1. Run the focused placement tests.
2. Run backend compilation and `git diff --check`.
3. Commit, push, and open a ready PR.
