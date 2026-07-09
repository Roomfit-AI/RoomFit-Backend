package com.roomfit.style.dto.response;

import com.roomfit.style.domain.StyleImage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "스타일 이미지 응답. Product API의 styleTags와 달리 스타일 이미지는 tags 필드를 사용합니다.")
public class StyleImageResponse {

    @Schema(description = "스타일 이미지 ID", example = "1")
    private final Long imageId;
    @Schema(description = "스타일 이미지 제목", example = "화이트톤 미니멀 원룸")
    private final String title;
    @Schema(description = "스타일 이미지 URL", example = "/images/styles/minimal-white-1.jpg")
    private final String imageUrl;
    @Schema(description = "스타일 이미지 태그 목록. 필드명은 styleTags가 아니라 tags입니다.", example = "[\"minimal\", \"white_tone\", \"open_space\"]")
    private final List<String> tags;

    private StyleImageResponse(Long imageId, String title, String imageUrl, List<String> tags) {
        this.imageId = imageId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.tags = tags;
    }

    public static StyleImageResponse from(StyleImage styleImage) {
        return new StyleImageResponse(
                styleImage.getImageId(),
                styleImage.getTitle(),
                styleImage.getImageUrl(),
                styleImage.getTags()
        );
    }

    public Long getImageId() {
        return imageId;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getTags() {
        return tags;
    }
}
