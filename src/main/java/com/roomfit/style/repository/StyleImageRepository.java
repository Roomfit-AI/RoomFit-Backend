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
                    "화이트톤 미니멀 원룸",
                    "/images/styles/minimal-white-1.jpg",
                    List.of("minimal", "white_tone", "open_space")
            ),
            new StyleImage(
                    2L,
                    "내추럴 우드톤 원룸",
                    "/images/styles/natural-wood-1.jpg",
                    List.of("natural", "wood_tone", "cozy")
            ),
            new StyleImage(
                    3L,
                    "공부형 원룸 인테리어",
                    "/images/styles/study-focused-1.jpg",
                    List.of("study", "desk_zone", "minimal")
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
