package com.roomfit.style.controller;

import com.roomfit.common.CommonResponse;
import com.roomfit.style.dto.response.StyleImageResponse;
import com.roomfit.style.service.StyleImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/styles")
@Tag(name = "Styles", description = "스타일 이미지 선택용 API")
public class StyleImageController {

    private final StyleImageService styleImageService;

    public StyleImageController(StyleImageService styleImageService) {
        this.styleImageService = styleImageService;
    }

    @GetMapping("/images")
    @Operation(summary = "스타일 이미지 목록 조회", description = "사용자가 선호 스타일을 선택할 수 있도록 스타일 이미지 목록을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "스타일 이미지 목록 조회 성공")
    public CommonResponse<List<StyleImageResponse>> getStyleImages() {
        return CommonResponse.ok(styleImageService.getStyleImages());
    }
}
