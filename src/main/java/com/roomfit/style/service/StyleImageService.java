package com.roomfit.style.service;

import com.roomfit.style.dto.response.StyleImageResponse;
import com.roomfit.style.repository.StyleImageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StyleImageService {

    private final StyleImageRepository styleImageRepository;

    public StyleImageService(StyleImageRepository styleImageRepository) {
        this.styleImageRepository = styleImageRepository;
    }

    public List<StyleImageResponse> getStyleImages() {
        return styleImageRepository.findAll().stream()
                .map(StyleImageResponse::from)
                .toList();
    }
}
