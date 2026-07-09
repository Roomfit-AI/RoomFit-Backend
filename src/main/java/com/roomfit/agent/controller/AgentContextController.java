package com.roomfit.agent.controller;

import com.roomfit.agent.dto.request.AgentContextRequest;
import com.roomfit.agent.dto.response.AgentContextResponse;
import com.roomfit.agent.service.AgentContextService;
import com.roomfit.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent", description = "사용자 목표/스타일/선택 제품 기반 추천 Context 생성 API")
public class AgentContextController {

    private final AgentContextService agentContextService;

    public AgentContextController(AgentContextService agentContextService) {
        this.agentContextService = agentContextService;
    }

    @PostMapping("/context")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agent Context 생성", description = "방 ID, 생활 목표, 디자인 스타일, 필수/선택 가구, 선택 제품 정보를 바탕으로 추천에 사용할 Context를 생성합니다. 이후 POST /api/layouts/recommend에 contextId를 전달합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Context 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 lifestyleGoal/designStyle/productId 또는 요청 본문"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 roomId 또는 styleImageId")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "추천 조건 Context 생성 요청",
            required = true,
            content = @Content(examples = @ExampleObject(name = "Study focused context", value = """
                    {
                      "roomId": 1,
                      "lifestyleGoal": "STUDY_FOCUSED",
                      "designStyle": ["MINIMAL", "WHITE_TONE"],
                      "requiredItems": ["bed", "desk", "chair"],
                      "optionalItems": ["storage", "rug", "lamp"],
                      "selectedImageIds": [1, 3],
                      "selectedProductIds": ["desk-01", "chair-01", "lamp-01"]
                    }
                    """)))
    public CommonResponse<AgentContextResponse> createContext(@RequestBody AgentContextRequest request) {
        return CommonResponse.ok(agentContextService.createContext(request));
    }
}
