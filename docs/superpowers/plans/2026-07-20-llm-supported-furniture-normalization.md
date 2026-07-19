# LLM Supported Furniture Normalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure uploaded-room LLM recommendations place monitors on desks and TVs on media consoles before validation and rendering.

**Architecture:** Keep the frontend's strict same-center support contract. Normalize only the LLM-generated candidate list by moving each supported screen to the nearest matching support's `x/z` and rotation before deterministic validation; clarify the same exception in the LLM prompt.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, AssertJ, Mockito

## Global Constraints

- Preserve independent movement after recommendation; only initial LLM output is normalized.
- Do not change frontend support detection.
- Skip the TDD RED cycle per the user's explicit request; run only the focused regression test after implementation.

---

### Task 1: Normalize LLM Support Pairs

**Files:**
- Modify: `src/main/java/com/roomfit/placement/LlmPlacementService.java`
- Test: `src/test/java/com/roomfit/placement/LlmPlacementVariantIdTest.java`

**Interfaces:**
- Consumes: parsed `List<Furniture>` from `toFurnitureList(...)`
- Produces: `normalizeSupportedFurniture(List<Furniture>)`, with monitor/desk and TV/media-console pairs sharing center coordinates and rotation

- [x] **Step 1: Normalize support pairs before validation**

Add a normalization pass immediately after parsing the LLM response. For every active `monitor` or `tv`, find the nearest active `desk` or `media_console`, then copy the support's `Position` and rotation onto the dependent.

- [x] **Step 2: Clarify the LLM placement prompt**

Replace the unconditional no-overlap rule with explicit support-pair rules: `monitor` is centered on `desk`, `tv` is centered on `media_console`, and only those pairs may intentionally overlap in footprint.

- [x] **Step 3: Add a focused regression test**

Feed an LLM response whose monitor and TV are separated from their supports, then assert the returned monitor matches the desk center/rotation and the TV matches the media-console center/rotation. Also assert the prompt describes the support exception.

- [x] **Step 4: Verify the focused test**

Run: `./gradlew test --tests com.roomfit.placement.LlmPlacementVariantIdTest`

Expected: `BUILD SUCCESSFUL` with no failed tests.

- [x] **Step 5: Publish from latest main**

PR #55 is already merged. Stage only the service, test, and this plan; commit with `fix: normalize llm supported furniture`; push `codex/fix-uploaded-room-support-placement` and open a new PR against `main`.
