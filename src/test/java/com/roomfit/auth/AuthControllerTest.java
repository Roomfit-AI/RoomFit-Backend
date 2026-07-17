package com.roomfit.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void issueGuestSession_returnsGuestIdAndToken() throws Exception {
        mockMvc.perform(post("/api/auth/guest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.guestId", notNullValue()))
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    void issueGuestSession_twoCallsReturnDifferentGuests() throws Exception {
        String first = mockMvc.perform(post("/api/auth/guest"))
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/auth/guest"))
                .andReturn().getResponse().getContentAsString();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void roomEndpoint_withBlankAuthorizationHeader_returnsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/rooms/samples").header("Authorization", ""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void roomEndpoint_withGarbageToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/rooms/samples").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }
}
