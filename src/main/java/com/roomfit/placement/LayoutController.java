package com.roomfit.placement;

import com.roomfit.common.CommonResponse;
import com.roomfit.placement.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/layouts")
public class LayoutController {

    private final LayoutService layoutService;

    public LayoutController(LayoutService layoutService) {
        this.layoutService = layoutService;
    }

    @PostMapping("/recommend")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<LayoutResponse> recommend(@RequestBody RecommendRequest request) {
        return CommonResponse.ok(layoutService.recommend(request));
    }

    @PostMapping("/validate")
    public CommonResponse<ValidationResult> validate(@RequestBody ValidateRequest request) {
        return CommonResponse.ok(layoutService.validateOnly(request));
    }

    @PutMapping("/{layoutId}")
    public CommonResponse<LayoutResponse> updateLayout(@PathVariable Long layoutId,
                                                         @RequestBody LayoutUpdateRequest request) {
        return CommonResponse.ok(layoutService.updateLayout(layoutId, request));
    }

    @PostMapping("/{layoutId}/confirm")
    public CommonResponse<ConfirmResponse> confirmLayout(@PathVariable Long layoutId) {
        return CommonResponse.ok(layoutService.confirmLayout(layoutId));
    }

    // TODO: POST /api/layouts/feedback (선택 구현) - 자연어 피드백 기반 재추천
}
