package com.roomfit.style.dto.response;

import com.roomfit.style.domain.StyleImage;

import java.util.List;

public class StyleImageResponse {

    private final Long imageId;
    private final String title;
    private final String imageUrl;
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
