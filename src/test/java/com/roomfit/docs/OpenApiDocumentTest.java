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
                .contains("/api/rooms/upload")
                .contains("/api/rooms/{roomId}")
                .contains("/api/rooms/{roomId}/furniture")
                .contains("/api/styles/images")
                .contains("/api/products/mock")
                .contains("/api/agent/context")
                .contains("/api/layouts/recommend")
                .contains("/api/layouts/validate")
                .contains("/api/layouts/{layoutId}")
                .contains("/api/layouts/{layoutId}/draft")
                .contains("/api/layouts/{layoutId}/furniture-additions")
                .contains("/api/layouts/rooms/{roomId}/confirmed/latest")
                .contains("/api/layouts/feedback")
                .contains("/api/layouts/{layoutId}/confirm")
                .contains("RoomFit MVP Demo Flow")
                .contains("배치 추천 생성")
                .contains("selectedProductIds: [desk-01, chair-01, lamp-01]")
                .contains("purchaseUrl:")
                .contains("variantId:")
                .contains("$ref: \"#/components/schemas/SelectedProduct\"")
                .contains("프론트 체크리스트 UI용 검증 항목")
                .contains("CommonResponse")
                .contains("recommendedFurniture")
                .contains("sourceLayoutId")
                .contains("confirmedAt")
                .contains("scoreSummary")
                .contains("validationResult")
                .contains("interpretedIntent")
                .contains("RoomUploadRequest")
                .contains("ROOMPLAN")
                .contains("INVALID_REQUEST_BODY");
    }

    @Test
    void openApiDocument_doesNotPlaceSiblingPropertiesNextToReferenceObjects() throws IOException {
        Path path = Path.of("docs/openapi/roomfit-api.yaml");

        String document = Files.readString(path);

        assertThat(document).doesNotContainPattern("nullable: true\\R\\s+\\$ref:");
    }

    @Test
    void openApiDocument_declaresVariantIdContractForEveryResponseSchema() throws IOException {
        String document = Files.readString(Path.of("docs/openapi/roomfit-api.yaml"));

        assertVariantIdContract(schemaSection(document, "Furniture", "Position"));
        assertVariantIdContract(schemaSection(document, "MockProduct", "RequiredClearance"));
        assertVariantIdContract(schemaSection(document, "SelectedProduct", "RecommendRequest"));
    }

    @Test
    void openApiDocument_declaresNullableProductMetadataContract() throws IOException {
        String document = Files.readString(Path.of("docs/openapi/roomfit-api.yaml"));
        String mockProduct = schemaSection(document, "MockProduct", "RequiredClearance");

        assertThat(mockProduct)
                .containsPattern("brand:\\R\\s+type: string\\R\\s+nullable: true")
                .containsPattern("price:\\R\\s+type: integer\\R\\s+nullable: true")
                .containsPattern("imageUrl:\\R\\s+type: string\\R\\s+nullable: true")
                .containsPattern("purchaseUrl:\\R\\s+type: string\\R\\s+format: uri\\R\\s+nullable: true");
    }

    private String schemaSection(String document, String schemaName, String nextSchemaName) {
        String schemaHeader = "    " + schemaName + ":";
        String nextSchemaHeader = "    " + nextSchemaName + ":";
        int start = document.indexOf(schemaHeader);
        int end = document.indexOf(nextSchemaHeader, start + schemaHeader.length());

        assertThat(start).as("schema %s exists", schemaName).isGreaterThanOrEqualTo(0);
        assertThat(end).as("schema following %s exists", schemaName).isGreaterThan(start);
        return document.substring(start, end);
    }

    private void assertVariantIdContract(String schemaSection) {
        assertThat(schemaSection)
                .contains("variantId:")
                .contains("nullable: true")
                .contains("pattern: \"^[a-z0-9]+(?:-[a-z0-9]+)*$\"");
    }
}
