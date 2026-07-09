package com.roomfit.product.service;

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
}
