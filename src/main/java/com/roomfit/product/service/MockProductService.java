package com.roomfit.product.service;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.dto.response.MockProductResponse;
import com.roomfit.product.repository.MockProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockProductService {

    private final MockProductRepository mockProductRepository;

    public MockProductService(MockProductRepository mockProductRepository) {
        this.mockProductRepository = mockProductRepository;
    }

    public List<MockProductResponse> getMockProducts() {
        return mockProductRepository.findAll().stream()
                .map(MockProductResponse::from)
                .toList();
    }

    public MockProduct findByProductId(String productId) {
        return mockProductRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public List<MockProduct> findByProductIds(List<String> productIds) {
        return productIds.stream()
                .map(this::findByProductId)
                .toList();
    }
}
