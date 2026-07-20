package com.roomfit.placement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.agent.repository.AgentContextRepository;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import com.roomfit.room.RoomRepository;
import com.roomfit.room.RoomSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FurnitureAdditionGuardrailControllerTest {

    private static final List<String> EIGHT_TYPES = List.of(
            "mood_lamp", "plant", "bookshelf", "nightstand",
            "side_table", "desk_chair", "full_length_mirror", "hanger");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @MockitoSpyBean
    private LayoutRepository layoutRepository;

    @Autowired
    private AgentContextRepository agentContextRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private DeterministicFeedbackExecutor feedbackExecutor;

    @MockitoSpyBean
    private RenderableProductCatalog renderableProductCatalog;

    @MockitoSpyBean
    private ScoreService scoreService;

    static Stream<Arguments> largeRoomSuccessCases() {
        return Stream.of(
                Arguments.of(0, 2),
                Arguments.of(0, 6),
                Arguments.of(0, 8),
                Arguments.of(4, 8)
        );
    }

    @ParameterizedTest(name = "large room: existing {0} + new {1}")
    @MethodSource("largeRoomSuccessCases")
    void largeRoomAddsUpToEightAndReachesTheExpectedActiveTotal(int existingCount, int requestedCount)
            throws Exception {
        TestDraft testDraft = createDraft(30.0, 30.0, existingCount, EIGHT_TYPES.subList(0, requestedCount));
        List<FurnitureSnapshot> before = snapshot(testDraft.layoutId());

        long started = System.nanoTime();
        mockMvc.perform(post("/api/layouts/{layoutId}/furniture-additions", testDraft.layoutId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextBody(testDraft.contextId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendedFurniture.length()")
                        .value(existingCount + requestedCount));
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        Layout updated = freshLayout(testDraft.layoutId());
        assertThat(active(updated.getFurniture())).hasSize(existingCount + requestedCount);
        assertExistingUnchanged(before, updated.getFurniture());
        assertThat(updated.getFurniture()).extracting(Furniture::getId).doesNotHaveDuplicates();
        long healthMillis = healthElapsedMillis();
        System.out.printf("PERF large existing=%d requested=%d total=%d status=200 elapsedMs=%d healthMs=%d preserved=true%n",
                existingCount, requestedCount, existingCount + requestedCount, elapsedMillis, healthMillis);
    }

    @ParameterizedTest(name = "realistic room: existing {0} + new {1}")
    @MethodSource("largeRoomSuccessCases")
    void realisticRoomReturnsOnlySuccessOrSafeDomainFailure(int existingCount, int requestedCount)
            throws Exception {
        TestDraft testDraft = createDraft(5.8, 5.4, existingCount, EIGHT_TYPES.subList(0, requestedCount));
        List<FurnitureSnapshot> before = snapshot(testDraft.layoutId());

        long started = System.nanoTime();
        MvcResult result = mockMvc.perform(post("/api/layouts/{layoutId}/furniture-additions", testDraft.layoutId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextBody(testDraft.contextId())))
                .andReturn();
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        int placedCount = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("placedFurnitureCount").asInt();
        Layout updated = freshLayout(testDraft.layoutId());
        assertThat(active(updated.getFurniture())).hasSize(existingCount + placedCount);
        assertExistingUnchanged(before, updated.getFurniture());
        assertThat(elapsedMillis).isLessThan(10_000);
        long healthMillis = healthElapsedMillis();
        System.out.printf("PERF realistic existing=%d requested=%d total=%d status=%d elapsedMs=%d healthMs=%d preserved=true%n",
                existingCount, requestedCount, existingCount + requestedCount,
                result.getResponse().getStatus(), elapsedMillis, healthMillis);
    }

    @ParameterizedTest(name = "preflight rejects existing {0} + new {1}")
    @MethodSource("overLimitCases")
    void overLimitRequestsReturnExact422BeforeExecutorOrCatalog(int existingCount, int requestedCount)
            throws Exception {
        List<String> requestedTypes = IntStream.range(0, requestedCount)
                .mapToObj(index -> "desk")
                .toList();
        TestDraft testDraft = createDraft(30.0, 30.0, existingCount, requestedTypes);
        List<FurnitureSnapshot> before = snapshot(testDraft.layoutId());
        reset(feedbackExecutor, renderableProductCatalog);

        long started = System.nanoTime();
        mockMvc.perform(post("/api/layouts/{layoutId}/furniture-additions", testDraft.layoutId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextBody(testDraft.contextId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value((Object) null))
                .andExpect(jsonPath("$.error.code").value("FURNITURE_ADDITION_FAILED"))
                .andExpect(jsonPath("$.error.message")
                        .value("선택한 추가 가구를 안전하게 배치할 수 없습니다."));
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;

        verifyNoInteractions(feedbackExecutor, renderableProductCatalog);
        assertThat(snapshot(testDraft.layoutId())).containsExactlyElementsOf(before);
        long healthMillis = healthElapsedMillis();
        System.out.printf("PERF preflight existing=%d requested=%d status=422 elapsedMs=%d healthMs=%d preserved=true%n",
                existingCount, requestedCount, elapsedMillis, healthMillis);
    }

    static Stream<Arguments> overLimitCases() {
        return Stream.of(
                Arguments.of(0, 9)
        );
    }

    @Test
    void twentyRepeatedOverLimitRequestsStayFastHealthyAndAtomic() throws Exception {
        TestDraft testDraft = createDraft(30.0, 30.0, 5,
                IntStream.range(0, 9).mapToObj(index -> "desk").toList());
        List<FurnitureSnapshot> before = snapshot(testDraft.layoutId());
        List<Long> rejectionMillis = new ArrayList<>();
        List<Long> healthMillis = new ArrayList<>();
        reset(feedbackExecutor, renderableProductCatalog);
        System.gc();
        long heapBefore = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        int threadsBefore = ManagementFactory.getThreadMXBean().getThreadCount();

        for (int attempt = 0; attempt < 20; attempt++) {
            long rejectionStarted = System.nanoTime();
            mockMvc.perform(post("/api/layouts/{layoutId}/furniture-additions", testDraft.layoutId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(contextBody(testDraft.contextId())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code").value("FURNITURE_ADDITION_FAILED"));
            rejectionMillis.add((System.nanoTime() - rejectionStarted) / 1_000_000);

            long healthStarted = System.nanoTime();
            mockMvc.perform(get("/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
            healthMillis.add((System.nanoTime() - healthStarted) / 1_000_000);
        }

        verifyNoInteractions(feedbackExecutor, renderableProductCatalog);
        assertThat(snapshot(testDraft.layoutId())).containsExactlyElementsOf(before);
        assertThat(rejectionMillis).allMatch(millis -> millis < 2_000);
        assertThat(healthMillis).allMatch(millis -> millis < 2_000);
        System.gc();
        long heapAfter = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        int threadsAfter = ManagementFactory.getThreadMXBean().getThreadCount();
        long heapDelta = heapAfter - heapBefore;
        assertThat(heapDelta).isLessThan(64L * 1024 * 1024);
        assertThat(threadsAfter).isLessThanOrEqualTo(threadsBefore + 4);
        System.out.printf("PERF repeat count=20 rejectionAvgMs=%.2f rejectionMaxMs=%d healthAvgMs=%.2f healthMaxMs=%d heapDeltaBytes=%d threadsBefore=%d threadsAfter=%d preserved=true%n",
                rejectionMillis.stream().mapToLong(Long::longValue).average().orElse(0),
                rejectionMillis.stream().mapToLong(Long::longValue).max().orElse(0),
                healthMillis.stream().mapToLong(Long::longValue).average().orElse(0),
                healthMillis.stream().mapToLong(Long::longValue).max().orElse(0),
                heapDelta, threadsBefore, threadsAfter);
    }

    @Test
    void duplicateDeskAndBookshelfSelectionsCreateSeparateFurniture() throws Exception {
        TestDraft testDraft = createDraft(30.0, 30.0, 0,
                List.of("desk", "desk", "bookshelf", "bookshelf"));

        mockMvc.perform(post("/api/layouts/{layoutId}/furniture-additions", testDraft.layoutId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextBody(testDraft.contextId())))
                .andExpect(status().isOk());

        Layout updated = freshLayout(testDraft.layoutId());
        assertThat(updated.getFurniture().stream().filter(item -> "desk".equals(item.getType())))
                .hasSize(2);
        assertThat(updated.getFurniture().stream().filter(item -> "bookshelf".equals(item.getType())))
                .hasSize(2);
    }

    @Test
    void placementFailureReturnsNormalFailedOutcomeWithoutChangingDraft() throws Exception {
        TestDraft testDraft = createDraft(1.0, 1.0, 0, List.of("bed"));
        List<FurnitureSnapshot> before = snapshot(testDraft.layoutId());

        mockMvc.perform(post("/api/layouts/{layoutId}/furniture-additions", testDraft.layoutId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextBody(testDraft.contextId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendationStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.placedFurnitureCount").value(0));

        assertThat(snapshot(testDraft.layoutId())).containsExactlyElementsOf(before);
    }

    @Test
    void activeLimitKeepsTheFirstSafeAdditionAndReportsTheRest() throws Exception {
        TestDraft testDraft = createDraft(30.0, 30.0, 11, List.of("plant", "mood_lamp"));

        mockMvc.perform(post("/api/layouts/{layoutId}/furniture-additions", testDraft.layoutId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextBody(testDraft.contextId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendationStatus").value("PARTIAL_SUCCESS"))
                .andExpect(jsonPath("$.data.placedFurnitureCount").value(1))
                .andExpect(jsonPath("$.data.unplacedFurniture.length()").value(1))
                .andExpect(jsonPath("$.data.unplacedFurniture[0].reasonCode")
                        .value("ACTIVE_FURNITURE_LIMIT"));

        assertThat(active(freshLayout(testDraft.layoutId()).getFurniture())).hasSize(12);
    }

    @Test
    void scoreFailureOccursBeforeSaveAndLeavesLayoutUnchanged() throws Exception {
        TestDraft testDraft = createDraft(30.0, 30.0, 0, List.of("mood_lamp", "plant"));
        List<FurnitureSnapshot> before = snapshot(testDraft.layoutId());
        reset(layoutRepository, scoreService);
        doThrow(new IllegalStateException("score failure"))
                .when(scoreService).calculate(any(), any(), any());

        mockMvc.perform(post("/api/layouts/{layoutId}/furniture-additions", testDraft.layoutId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contextBody(testDraft.contextId())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));

        verify(layoutRepository, never()).save(any(Layout.class));
        reset(scoreService);
        assertThat(snapshot(testDraft.layoutId())).containsExactlyElementsOf(before);
    }

    private TestDraft createDraft(double width, double depth, int existingCount, List<String> requestedTypes) {
        String suffix = java.util.UUID.randomUUID().toString();
        List<Furniture> existing = existingFurniture(width, depth, existingCount);
        Room room = roomRepository.save(new Room(null, "Guardrail " + suffix,
                width, depth, 2.7, "meter", List.of(), List.of(),
                existing, RoomSource.ROOMPLAN, LocalDateTime.now(), null));
        AgentContext context = agentContextRepository.save(new AgentContext(
                room.getId(), LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                requestedTypes, List.of(), List.of(1L), List.of(), List.of("minimal")));
        Layout layout = layoutRepository.save(new Layout(room.getId(), context.getId(),
                deepCopy(existing)));
        return new TestDraft(layout.getId(), context.getId());
    }

    private List<Furniture> existingFurniture(double width, double depth, int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> {
                    int column = index % 4;
                    int row = index / 4;
                    return new Furniture("existing-" + index, "desk", "Existing " + index,
                            0.4, 0.4, 0.7,
                            new Position(width / 2.0 + (column - 1.5) * 0.6,
                                    depth / 2.0 + (row - 1.0) * 0.6),
                            index * 5.0, FurnitureStatus.EXISTING,
                            "desk-compact-01", List.of("minimal"), "desk-compact");
                })
                .toList();
    }

    private List<Furniture> deepCopy(List<Furniture> furniture) {
        return furniture.stream().map(item -> new Furniture(item.getId(), item.getType(), item.getLabel(),
                item.getWidth(), item.getDepth(), item.getHeight(),
                new Position(item.getPosition().getX(), item.getPosition().getZ()),
                item.getRotation(), item.getStatus(), item.getProductId(), item.getStyleTags(), item.getVariantId()))
                .toList();
    }

    private Layout freshLayout(Long layoutId) {
        return layoutRepository.findById(layoutId).orElseThrow();
    }

    private List<Furniture> active(List<Furniture> furniture) {
        return furniture.stream().filter(FurnitureAdditionPolicy::active).toList();
    }

    private List<FurnitureSnapshot> snapshot(Long layoutId) {
        return freshLayout(layoutId).getFurniture().stream().map(FurnitureSnapshot::from).toList();
    }

    private void assertExistingUnchanged(List<FurnitureSnapshot> before, List<Furniture> after) {
        for (FurnitureSnapshot expected : before) {
            Furniture actual = after.stream().filter(item -> expected.id().equals(item.getId()))
                    .findFirst().orElseThrow();
            assertThat(FurnitureSnapshot.from(actual)).isEqualTo(expected);
        }
    }

    private String contextBody(Long contextId) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("contextId", contextId));
    }

    private long healthElapsedMillis() throws Exception {
        long started = System.nanoTime();
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        return (System.nanoTime() - started) / 1_000_000;
    }

    private record TestDraft(Long layoutId, Long contextId) {
    }

    private record FurnitureSnapshot(String id, String type, String productId, String variantId,
                                     double x, double z, double rotation, FurnitureStatus status) {
        static FurnitureSnapshot from(Furniture furniture) {
            return new FurnitureSnapshot(furniture.getId(), furniture.getType(), furniture.getProductId(),
                    furniture.getVariantId(), furniture.getPosition().getX(), furniture.getPosition().getZ(),
                    furniture.getRotation(), furniture.getStatus());
        }
    }
}
