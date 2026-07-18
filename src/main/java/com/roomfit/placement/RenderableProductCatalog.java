package com.roomfit.placement;

import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.repository.MockProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class RenderableProductCatalog {

    public static final int MAX_PRODUCT_CANDIDATES = 4;

    private static final Set<String> WEB_VARIANT_IDS = Set.of(
            "desk-compact", "desk-storage", "desk-corner", "desk-midcentury-glass"
    );
    private static final Set<String> WEB_LEGACY_TYPES = Set.of(
            "bed", "desk", "chair", "storage", "rug", "lamp",
            "cabinet", "lighting", "sofa", "table", "shelf", "tvStand", "wardrobe",
            "bookshelf", "hanger"
    );

    private final List<MockProduct> products;

    @Autowired
    public RenderableProductCatalog(MockProductRepository repository) {
        this(repository.findAll());
    }

    RenderableProductCatalog(List<MockProduct> products) {
        this.products = List.copyOf(products);
        validateCatalog(this.products);
    }

    public List<MockProduct> findCandidates(FeedbackProductRequirements requirements, FurnitureSize referenceSize) {
        List<MockProduct> candidates = products.stream()
                .filter(this::isRenderable)
                .filter(product -> requirements.furnitureType().equals(product.getType()))
                .filter(product -> !requirements.storagePreferred() || hasStorage(product))
                .filter(product -> containsStyleKeywords(product, requirements.styleKeywords()))
                .sorted(productComparator(requirements.sizePreference(), referenceSize))
                .limit(MAX_PRODUCT_CANDIDATES)
                .toList();
        return List.copyOf(candidates);
    }

    public boolean isRenderable(MockProduct product) {
        if (product.getVariantId() != null) {
            return WEB_VARIANT_IDS.contains(product.getVariantId());
        }
        return WEB_LEGACY_TYPES.contains(product.getType());
    }

    public Set<String> supportedVariantIds() {
        return WEB_VARIANT_IDS;
    }

    private Comparator<MockProduct> productComparator(FeedbackSizePreference preference, FurnitureSize referenceSize) {
        Comparator<MockProduct> sizeComparator = switch (preference) {
            case SMALL -> Comparator.comparingDouble(this::footprintArea);
            case LARGE -> Comparator.comparingDouble(this::footprintArea).reversed();
            case SIMILAR -> Comparator.comparingDouble(product -> sizeDistance(product, referenceSize));
            case ANY -> (first, second) -> 0;
        };
        return sizeComparator.thenComparing(MockProduct::getProductId);
    }

    private double sizeDistance(MockProduct product, FurnitureSize referenceSize) {
        if (referenceSize == null) {
            return 0;
        }
        return Math.abs(product.getWidth() - referenceSize.width())
                + Math.abs(product.getDepth() - referenceSize.depth())
                + Math.abs(product.getHeight() - referenceSize.height());
    }

    private double footprintArea(MockProduct product) {
        return product.getWidth() * product.getDepth();
    }

    private boolean containsStyleKeywords(MockProduct product, List<String> keywords) {
        if (keywords.isEmpty()) {
            return true;
        }
        Set<String> tags = new HashSet<>();
        product.getStyleTags().forEach(tag -> tags.add(tag.toLowerCase(Locale.ROOT)));
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .allMatch(tags::contains);
    }

    private boolean hasStorage(MockProduct product) {
        return product.getStyleTags().stream().anyMatch(tag -> "storage".equalsIgnoreCase(tag))
                || product.getLifestyleTags().stream().anyMatch(tag -> "STORAGE".equalsIgnoreCase(tag))
                || product.getVariantId() != null && product.getVariantId().contains("storage");
    }

    private void validateCatalog(List<MockProduct> catalogProducts) {
        if (catalogProducts == null) {
            throw new IllegalArgumentException("Renderable product catalog must not be null");
        }
        Set<String> productIds = new HashSet<>();
        Set<String> variantIds = new HashSet<>();
        List<String> errors = new ArrayList<>();
        for (MockProduct product : catalogProducts) {
            if (product == null) {
                errors.add("null product");
                continue;
            }
            if (product.getProductId() == null || product.getProductId().isBlank()) {
                errors.add("missing productId");
            } else if (!productIds.add(product.getProductId())) {
                errors.add("duplicate productId: " + product.getProductId());
            }
            if (product.getVariantId() != null && !variantIds.add(product.getVariantId())) {
                errors.add("duplicate variantId: " + product.getVariantId());
            }
            if (product.getType() == null || product.getType().isBlank()) {
                errors.add("invalid type for product: " + product.getProductId());
            }
            if (!positiveFinite(product.getWidth()) || !positiveFinite(product.getDepth())
                    || !positiveFinite(product.getHeight())) {
                errors.add("missing or invalid dimensions for product: " + product.getProductId());
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    private boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0;
    }

    public record FurnitureSize(double width, double depth, double height) {
    }
}
