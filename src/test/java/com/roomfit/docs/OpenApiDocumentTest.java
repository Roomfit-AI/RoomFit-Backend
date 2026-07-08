package com.roomfit.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiDocumentTest {

    @Test
    void openApiDocument_containsImplementedEndpointsAndKeyFields() throws IOException {
        Path path = Path.of("docs/openapi/roomfit-api.yaml");

        assertThat(path).exists();

        String document = Files.readString(path);

        assertThat(document)
                .contains("/api/rooms/samples")
                .contains("/api/rooms/{roomId}")
                .contains("/api/rooms/{roomId}/furniture")
                .contains("/api/styles/images")
                .contains("/api/products/mock")
                .contains("/api/agent/context")
                .contains("/api/layouts/recommend")
                .contains("/api/layouts/validate")
                .contains("/api/layouts/{layoutId}")
                .contains("/api/layouts/feedback")
                .contains("/api/layouts/{layoutId}/confirm")
                .contains("CommonResponse")
                .contains("recommendedFurniture")
                .contains("scoreSummary")
                .contains("validationResult")
                .contains("interpretedIntent")
                .contains("INVALID_REQUEST_BODY");
    }
}
