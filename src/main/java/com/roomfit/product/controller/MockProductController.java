package com.roomfit.product.controller;

import com.roomfit.common.CommonResponse;
import com.roomfit.product.dto.response.MockProductResponse;
import com.roomfit.product.service.MockProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "MVP 제품 카드 렌더링용 Mock Product API")
public class MockProductController {

    private final MockProductService mockProductService;

    public MockProductController(MockProductService mockProductService) {
        this.mockProductService = mockProductService;
    }

    @GetMapping("/mock")
    @Operation(summary = "Mock 제품 목록 조회", description = "MVP 데모에서 사용자가 선택할 수 있는 제품 카드 목록을 반환합니다. selectedProductIds로 Agent Context 생성 시 사용됩니다.")
    @ApiResponse(responseCode = "200", description = "Mock 제품 목록 조회 성공")
    public CommonResponse<List<MockProductResponse>> getMockProducts() {
        return CommonResponse.ok(mockProductService.getMockProducts());
    }
}
