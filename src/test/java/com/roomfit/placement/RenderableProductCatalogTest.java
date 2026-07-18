package com.roomfit.placement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.domain.RequiredClearance;
import com.roomfit.product.repository.MockProductRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenderableProductCatalogTest {

    @Test
    void productionCatalogOnlyExposesWebRegisteredVariantsOrLegacyProducts() {
        RenderableProductCatalog catalog = new RenderableProductCatalog(new MockProductRepository());

        List<MockProduct> deskCandidates = catalog.findCandidates(
                requirements("desk"), null);

        assertThat(deskCandidates).hasSize(RenderableProductCatalog.MAX_PRODUCT_CANDIDATES);
        assertThat(deskCandidates).allMatch(product -> product.getVariantId() == null
                || catalog.supportedVariantIds().contains(product.getVariantId()));
        assertThat(catalog.supportedVariantIds()).containsExactlyInAnyOrder(
                "desk-compact", "desk-storage", "desk-corner", "desk-midcentury-glass");
    }

    @Test
    void unsupportedFrontendVariantIsExcludedFromCandidates() {
        MockProduct unsupported = product("chair-shell-01", "chair-midcentury-shell", "chair", 0.5, 0.5, 0.8);
        RenderableProductCatalog catalog = new RenderableProductCatalog(List.of(unsupported));

        assertThat(catalog.findCandidates(requirements("chair"), null)).isEmpty();
    }

    @Test
    void duplicateProductOrVariantIdsAreRejected() {
        MockProduct first = product("desk-a", "desk-compact", "desk", 1.2, 0.6, 0.73);
        MockProduct duplicateProduct = product("desk-a", "desk-storage", "desk", 1.4, 0.62, 0.73);
        MockProduct duplicateVariant = product("desk-b", "desk-compact", "desk", 1.3, 0.7, 0.73);

        assertThatThrownBy(() -> new RenderableProductCatalog(List.of(first, duplicateProduct)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate productId: desk-a");
        assertThatThrownBy(() -> new RenderableProductCatalog(List.of(first, duplicateVariant)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate variantId: desk-compact");
    }

    @Test
    void invalidTypeOrMissingDimensionsAreRejected() {
        MockProduct missingType = product("missing-type", null, "", 1, 1, 1);
        MockProduct missingDimensions = product("missing-size", null, "desk", 0, 1, 1);

        assertThatThrownBy(() -> new RenderableProductCatalog(List.of(missingType)))
                .hasMessageContaining("invalid type");
        assertThatThrownBy(() -> new RenderableProductCatalog(List.of(missingDimensions)))
                .hasMessageContaining("invalid dimensions");
    }

    @Test
    void parsesAllAvailableIncomingVariantDocumentsAndAuditsIdsAndDimensions() throws IOException {
        Path incoming = Path.of("..", "_incoming", "roomfit-furniture-json", "furniture");
        Assumptions.assumeTrue(Files.isDirectory(incoming), "workspace incoming catalog is not available");
        ObjectMapper objectMapper = new ObjectMapper();
        Set<String> variantIds = new HashSet<>();
        JsonNode materials = objectMapper.readTree(incoming.resolveSibling("materials.json").toFile());

        assertThat(materials.path("schemaVersion").asText()).isEqualTo("1.0");
        assertThat(materials.path("materials").isObject()).isTrue();
        assertThat(materials.path("materials").size()).isPositive();

        List<Path> documents;
        try (var paths = Files.list(incoming)) {
            documents = paths.filter(path -> path.getFileName().toString().endsWith(".json")).sorted().toList();
        }

        assertThat(documents).hasSize(93);
        for (Path document : documents) {
            JsonNode root = objectMapper.readTree(document.toFile());
            String variantId = root.path("variantId").asText();
            assertThat(variantId).as(document.toString()).isNotBlank();
            assertThat(variantIds.add(variantId)).as("duplicate variantId %s", variantId).isTrue();
            assertThat(root.path("furnitureTypeCode").asText()).as(variantId).isNotBlank();
            assertThat(root.path("dimensions").path("width").asDouble()).as(variantId).isPositive();
            assertThat(root.path("dimensions").path("depth").asDouble()).as(variantId).isPositive();
            assertThat(root.path("dimensions").path("height").asDouble()).as(variantId).isPositive();
        }
    }

    private FeedbackProductRequirements requirements(String type) {
        return new FeedbackProductRequirements(type, FeedbackSizePreference.ANY, false, List.of());
    }

    private MockProduct product(String productId, String variantId, String type,
                                double width, double depth, double height) {
        return new MockProduct(productId, variantId, type, productId, null,
                width, depth, height, (Integer) null, List.of("minimal"), null, null,
                new RequiredClearance(0.2, 0.1));
    }
}
