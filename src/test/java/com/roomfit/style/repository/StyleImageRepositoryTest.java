package com.roomfit.style.repository;

import com.roomfit.style.domain.StyleImage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StyleImageRepositoryTest {

    private final StyleImageRepository repository = new StyleImageRepository();

    @Test
    void findAll_returnsTheFixedFiveStyleImageContract() {
        List<StyleImage> images = repository.findAll();

        assertThat(images).hasSize(5);
        assertThat(images).extracting(StyleImage::getImageId).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(images).extracting(StyleImage::getTags).containsExactly(
                List.of("minimal"),
                List.of("natural"),
                List.of("modern"),
                List.of("classic"),
                List.of("midcentury")
        );
        assertThat(images).allSatisfy(image -> {
            assertThat(image.getTags()).hasSize(1);
            assertThat(image.getTags()).allMatch(Set.of(
                    "minimal", "modern", "classic", "natural", "midcentury")::contains);
        });
    }
}
