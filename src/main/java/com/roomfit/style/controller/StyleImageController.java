package com.roomfit.style.controller;

import com.roomfit.common.CommonResponse;
import com.roomfit.style.dto.response.StyleImageResponse;
import com.roomfit.style.service.StyleImageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/styles")
public class StyleImageController {

    private final StyleImageService styleImageService;

    public StyleImageController(StyleImageService styleImageService) {
        this.styleImageService = styleImageService;
    }

    @GetMapping("/images")
    public CommonResponse<List<StyleImageResponse>> getStyleImages() {
        return CommonResponse.ok(styleImageService.getStyleImages());
    }
}
