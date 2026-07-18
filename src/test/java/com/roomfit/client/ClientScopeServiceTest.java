package com.roomfit.client;

import com.roomfit.common.CustomException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientScopeServiceTest {

    @Test
    void requiredScope_rejectsMissingHeader() {
        ClientScopeProperties properties = new ClientScopeProperties();
        properties.setEnabled(true);
        properties.setRequired(true);

        assertThatThrownBy(() -> new ClientScopeService(properties).resolve(null))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode().name())
                .isEqualTo("CLIENT_ID_REQUIRED");
    }

    @Test
    void validUuid_isNormalizedAndInvalidValueIsRejected() {
        ClientScopeProperties properties = new ClientScopeProperties();
        ClientScopeService service = new ClientScopeService(properties);

        assertThat(service.resolve("0C1C4A96-7F5B-48B7-9A01-111111111111").id())
                .isEqualTo("0c1c4a96-7f5b-48b7-9a01-111111111111");
        assertThatThrownBy(() -> service.resolve("legacy"))
                .isInstanceOf(CustomException.class);
    }
}
