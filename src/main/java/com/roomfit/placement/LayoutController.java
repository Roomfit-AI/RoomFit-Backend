package com.roomfit.placement;

import com.roomfit.common.CommonResponse;
import com.roomfit.placement.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/layouts")
@Tag(name = "Layouts", description = "추천 배치 생성, 드래그 검증, 수정 저장, 피드백, 확정 API")
public class LayoutController {

    private final LayoutService layoutService;

    public LayoutController(LayoutService layoutService) {
        this.layoutService = layoutService;
    }

    @PostMapping("/recommend")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "배치 추천 생성", description = "Agent Context를 기반으로 기존 가구와 선택 제품을 고려한 추천 배치를 생성합니다. recommendedFurniture, validationResult, scoreSummary를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "추천 생성 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 contextId")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "추천에 사용할 Agent Context ID",
            required = true,
            content = @Content(examples = @ExampleObject(name = "Recommend from context", value = """
                    {
                      "contextId": 1
                    }
                    """)))
    public CommonResponse<LayoutResponse> recommend(@RequestBody RecommendRequest request) {
        return CommonResponse.ok(layoutService.recommend(request));
    }

    @PostMapping("/validate")
    @Operation(summary = "현재 배치 검증", description = "사용자가 가구를 드래그하는 중 현재 화면의 가구 배치가 충돌/경계/문/창문/동선 조건을 만족하는지 검증합니다. 저장은 수행하지 않습니다. furniture 배열에는 현재 layoutId에 포함된 전체 furniture id 목록을 전달해야 합니다. 각 item은 full furniture object가 아니라 id, position, rotation, status 중심의 compact update item입니다. width/depth/height/productId/styleTags 등은 요청 필드가 아니라 백엔드 추천 결과 메타데이터입니다. 일부 가구 id만 전달하면 FURNITURE_ARRAY_MISMATCH가 발생할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배치 검증 성공"),
            @ApiResponse(responseCode = "400", description = "요청 furniture 배열이 기존 배치와 일치하지 않음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 layoutId")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "검증할 현재 가구 배치 전체 배열. 전체 furniture id를 포함하되 full furniture object는 보내지 않습니다.",
            required = true,
            content = @Content(examples = @ExampleObject(name = "Validate current layout", value = """
                    {
                      "layoutId": 1,
                      "furniture": [
                        { "id": "bed-1", "position": { "x": 0.8, "z": 1.4 }, "rotation": 0, "status": "EXISTING" },
                        { "id": "desk-rec-1", "position": { "x": 2.3, "z": 1.0 }, "rotation": 0, "status": "USER_MODIFIED" },
                        { "id": "chair-rec-1", "position": { "x": 2.3, "z": 1.8 }, "rotation": 0, "status": "USER_MODIFIED" }
                      ]
                    }
                    """)))
    public CommonResponse<ValidationResult> validate(@RequestBody ValidateRequest request) {
        return CommonResponse.ok(layoutService.validateOnly(request));
    }

    @PutMapping("/{layoutId}")
    @Operation(summary = "수정 배치 저장", description = "사용자가 수정한 최종 가구 배치를 저장하고 validationResult와 scoreSummary를 다시 계산합니다. furniture 배열에는 현재 layout의 전체 furniture id 목록을 compact update item 형태로 전달합니다. width/depth/height/productId/styleTags 등은 요청 필드가 아니라 백엔드 추천 결과 메타데이터입니다. 이미 확정된 layout은 수정할 수 없으며 409 ALREADY_CONFIRMED가 반환됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 배치 저장 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 가구 배열, 위치, 회전 또는 status"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 layoutId"),
            @ApiResponse(responseCode = "409", description = "이미 확정된 배치(ALREADY_CONFIRMED)")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "저장할 최종 가구 배치 전체 배열. 전체 furniture id를 포함하되 full furniture object는 보내지 않습니다.",
            required = true,
            content = @Content(examples = @ExampleObject(name = "Save modified layout", value = """
                    {
                      "furniture": [
                        { "id": "bed-1", "position": { "x": 0.8, "z": 1.4 }, "rotation": 0, "status": "EXISTING" },
                        { "id": "desk-rec-1", "position": { "x": 2.3, "z": 1.0 }, "rotation": 0, "status": "USER_MODIFIED" },
                        { "id": "chair-rec-1", "position": { "x": 2.3, "z": 1.8 }, "rotation": 0, "status": "USER_MODIFIED" }
                      ]
                    }
                    """)))
    public CommonResponse<LayoutResponse> updateLayout(@PathVariable Long layoutId,
                                                         @RequestBody LayoutUpdateRequest request) {
        return CommonResponse.ok(layoutService.updateLayout(layoutId, request));
    }

    @PostMapping("/{layoutId}/confirm")
    @Operation(summary = "최종 배치 확정", description = "사용자가 추천 또는 수정한 배치를 최종 확정합니다. 이미 확정된 layout은 다시 확정할 수 없으며 409 ALREADY_CONFIRMED가 반환됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "최종 배치 확정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 layoutId"),
            @ApiResponse(responseCode = "409", description = "이미 확정된 배치(ALREADY_CONFIRMED)")
    })
    public CommonResponse<ConfirmResponse> confirmLayout(@PathVariable Long layoutId) {
        return CommonResponse.ok(layoutService.confirmLayout(layoutId));
    }

    @PostMapping("/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "사용자 피드백 기반 재추천", description = "사용자의 자연어 피드백을 바탕으로 기존 배치를 조정하거나 재추천합니다. MVP에서는 제한적인 규칙 기반 피드백을 지원합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "피드백 기반 재추천 성공"),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 피드백"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 layoutId")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "지원 피드백: 책상 더 크게, 수납 늘려줘, 방이 넓어 보이게",
            required = true,
            content = @Content(examples = @ExampleObject(name = "Larger desk feedback", value = """
                    {
                      "layoutId": 1,
                      "feedback": "책상 더 크게"
                    }
                    """)))
    public CommonResponse<FeedbackResponse> feedback(@RequestBody FeedbackRequest request) {
        return CommonResponse.ok(layoutService.feedback(request));
    }
}
