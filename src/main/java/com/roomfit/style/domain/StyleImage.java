package com.roomfit.style.domain;

import java.util.List;

public class StyleImage {

    private final Long imageId;
    private final String title;
    private final String imageUrl;
    private final List<String> tags;

    public StyleImage(Long imageId, String title, String imageUrl, List<String> tags) {
        this.imageId = imageId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.tags = List.copyOf(tags);
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
