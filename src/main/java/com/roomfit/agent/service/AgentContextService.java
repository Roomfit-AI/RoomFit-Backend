package com.roomfit.agent.service;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.agent.dto.request.AgentContextRequest;
import com.roomfit.agent.dto.response.AgentContextResponse;
import com.roomfit.agent.repository.AgentContextRepository;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.service.MockProductService;
import com.roomfit.room.RoomRepository;
import com.roomfit.style.domain.StyleImage;
import com.roomfit.style.repository.StyleImageRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AgentContextService {

    private static final Set<String> FURNITURE_TYPES = Set.of("bed", "desk", "chair", "storage", "rug", "lamp");

    private final AgentContextRepository agentContextRepository;
    private final RoomRepository roomRepository;
    private final StyleImageRepository styleImageRepository;
    private final MockProductService mockProductService;

    public AgentContextService(AgentContextRepository agentContextRepository,
                               RoomRepository roomRepository,
                               StyleImageRepository styleImageRepository,
                               MockProductService mockProductService) {
        this.agentContextRepository = agentContextRepository;
        this.roomRepository = roomRepository;
        this.styleImageRepository = styleImageRepository;
        this.mockProductService = mockProductService;
    }

    public AgentContextResponse createContext(AgentContextRequest request) {
        roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (request.getRequiredItems() == null || request.getRequiredItems().isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_ITEM_EMPTY);
        }
        if (request.getSelectedImageIds() == null || request.getSelectedImageIds().isEmpty()) {
            throw new CustomException(ErrorCode.STYLE_IMAGE_EMPTY);
        }

        LifestyleGoal lifestyleGoal = parseLifestyleGoal(request.getLifestyleGoal());
        List<DesignStyle> designStyles = parseDesignStyles(request.getDesignStyle());
        List<String> requiredItems = validateFurnitureTypes(request.getRequiredItems());
        List<String> optionalItems = validateFurnitureTypes(nullToEmpty(request.getOptionalItems()));
        List<Long> selectedImageIds = List.copyOf(request.getSelectedImageIds());
        List<String> selectedProductIds = nullToEmpty(request.getSelectedProductIds());

        List<StyleImage> selectedImages = selectedImageIds.stream()
                .map(imageId -> styleImageRepository.findById(imageId)
                        .orElseThrow(() -> new CustomException(ErrorCode.STYLE_IMAGE_NOT_FOUND)))
                .toList();

        List<MockProduct> selectedProducts = mockProductService.findByProductIds(selectedProductIds);

        List<String> styleTags = collectStyleTags(selectedImages, selectedProducts);

        AgentContext context = new AgentContext(
                request.getRoomId(),
                lifestyleGoal,
                designStyles,
                requiredItems,
                optionalItems,
                selectedImageIds,
                selectedProductIds,
                styleTags,
                selectedProducts
        );

        agentContextRepository.save(context);
        return AgentContextResponse.from(context);
    }

    private LifestyleGoal parseLifestyleGoal(String rawLifestyleGoal) {
        try {
            return LifestyleGoal.valueOf(rawLifestyleGoal);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_LIFESTYLE_GOAL);
        }
    }

    private List<DesignStyle> parseDesignStyles(List<String> rawDesignStyles) {
        if (rawDesignStyles == null) {
            throw new CustomException(ErrorCode.INVALID_DESIGN_STYLE);
        }
        try {
            return rawDesignStyles.stream()
                    .map(DesignStyle::valueOf)
                    .toList();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_DESIGN_STYLE);
        }
    }

    private List<String> validateFurnitureTypes(List<String> furnitureTypes) {
        for (String furnitureType : furnitureTypes) {
            if (!FURNITURE_TYPES.contains(furnitureType)) {
                throw new CustomException(ErrorCode.INVALID_FURNITURE_TYPE);
            }
        }
        return List.copyOf(furnitureTypes);
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }

    private List<String> collectStyleTags(List<StyleImage> selectedImages, List<MockProduct> selectedProducts) {
        Set<String> tags = new LinkedHashSet<>();
        selectedImages.forEach(image -> tags.addAll(image.getTags()));
        selectedProducts.forEach(product -> tags.addAll(product.getStyleTags()));
        return new ArrayList<>(tags);
    }
}
