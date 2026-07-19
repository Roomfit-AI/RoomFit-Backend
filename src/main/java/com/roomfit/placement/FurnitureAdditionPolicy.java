package com.roomfit.placement;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FurnitureAdditionPolicy {

    public static final int MAX_NEW_ADDITIONS = 8;
    public static final int MAX_ACTIVE_FURNITURE = 12;

    public void validate(List<Furniture> existingFurniture, List<String> requestedTypes) {
        int requestedCount = requestedTypes.size();
        long activeExistingCount = existingFurniture.stream().filter(FurnitureAdditionPolicy::active).count();
        if (requestedCount > MAX_NEW_ADDITIONS
                || activeExistingCount + requestedCount > MAX_ACTIVE_FURNITURE) {
            throw new CustomException(ErrorCode.FURNITURE_ADDITION_FAILED);
        }
    }

    static boolean active(Furniture furniture) {
        return furniture.getStatus() != FurnitureStatus.DELETED;
    }
}
