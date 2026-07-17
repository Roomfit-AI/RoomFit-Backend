package com.roomfit.style.repository;

import com.roomfit.style.domain.StyleImage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StyleImageRepository {

    private final List<StyleImage> styleImages = List.of(
            new StyleImage(
                    1L,
                    "미니멀 원룸 인테리어",
                    "/images/styles/minimal-1.jpg",
                    List.of("minimal")
            ),
            new StyleImage(
                    2L,
                    "내추럴 원룸 인테리어",
                    "/images/styles/natural-1.jpg",
                    List.of("natural")
            ),
            new StyleImage(
                    3L,
                    "모던 원룸 인테리어",
                    "/images/styles/modern-1.jpg",
                    List.of("modern")
            ),
            new StyleImage(
                    4L,
                    "클래식 원룸 인테리어",
                    "/images/styles/classic-1.jpg",
                    List.of("classic")
            ),
            new StyleImage(
                    5L,
                    "미드센추리 원룸 인테리어",
                    "/images/styles/midcentury-1.jpg",
                    List.of("midcentury")
            )
    );

    public List<StyleImage> findAll() {
        return styleImages;
    }

    public Optional<StyleImage> findById(Long imageId) {
        return styleImages.stream()
                .filter(styleImage -> styleImage.getImageId().equals(imageId))
                .findFirst();
    }
}
