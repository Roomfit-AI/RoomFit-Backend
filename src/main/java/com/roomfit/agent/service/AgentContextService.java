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
import com.roomfit.room.Room;
import com.roomfit.room.RoomAccessService;
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
    private final RoomAccessService roomAccessService;
    private final StyleImageRepository styleImageRepository;
    private final MockProductService mockProductService;

    public AgentContextService(AgentContextRepository agentContextRepository,
                               RoomAccessService roomAccessService,
                               StyleImageRepository styleImageRepository,
                               MockProductService mockProductService) {
        this.agentContextRepository = agentContextRepository;
        this.roomAccessService = roomAccessService;
        this.styleImageRepository = styleImageRepository;
        this.mockProductService = mockProductService;
    }

    public AgentContextResponse createContext(AgentContextRequest request) {
        // roomId가 공유 샘플 템플릿을 가리키면 여기서 현재 게스트의 개인 fork로
        // 투명하게 전환된다 — 이 컨텍스트가 그 fork의 실제 id를 갖고 있어야
        // 이후 recommend/feedback 등이 같은 fork를 계속 가리킨다.
        Room room = roomAccessService.resolveAccessibleRoom(request.getRoomId());

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
                room.getId(),
                lifestyleGoal,
                designStyles,
                requiredItems,
                optionalItems,
                selectedImageIds,
                selectedProductIds,
                styleTags
        );

        agentContextRepository.save(context);
        return AgentContextResponse.from(context, selectedProducts);
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
