package com.roomfit.product.controller;

import com.roomfit.common.CommonResponse;
import com.roomfit.product.dto.response.MockProductResponse;
import com.roomfit.product.service.MockProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class MockProductController {

    private final MockProductService mockProductService;

    public MockProductController(MockProductService mockProductService) {
        this.mockProductService = mockProductService;
    }

    @GetMapping("/mock")
    public CommonResponse<List<MockProductResponse>> getMockProducts() {
        return CommonResponse.ok(mockProductService.getMockProducts());
    }
}
