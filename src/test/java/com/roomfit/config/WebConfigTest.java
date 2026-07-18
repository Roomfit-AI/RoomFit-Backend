package com.roomfit.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "CORS_ALLOWED_ORIGINS= http://localhost:5173 , https://roomfit-1eg55x26g-roomfit.vercel.app "
})
@AutoConfigureMockMvc
class WebConfigTest {

    private static final String DEPLOYED_FRONTEND_ORIGIN =
            "https://roomfit-1eg55x26g-roomfit.vercel.app";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void parseAllowedOrigins_withoutConfiguration_usesLocalDevelopmentDefaults() {
        assertThat(WebConfig.parseAllowedOrigins(null))
                .containsExactly(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:5175"
                );
    }

    @Test
    void getRequest_fromConfiguredDeploymentOrigin_returnsCorsHeader() throws Exception {
        mockMvc.perform(get("/api/rooms/samples")
                        .header(HttpHeaders.ORIGIN, DEPLOYED_FRONTEND_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        DEPLOYED_FRONTEND_ORIGIN
                ))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void recentUploadsRequest_fromConfiguredDeploymentOrigin_returnsCorsHeader() throws Exception {
        mockMvc.perform(get("/api/rooms/uploads/recent")
                        .param("limit", "10")
                        .header(HttpHeaders.ORIGIN, DEPLOYED_FRONTEND_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        DEPLOYED_FRONTEND_ORIGIN
                ))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void preflight_fromConfiguredDeploymentOrigin_allowsConfiguredMethodsAndHeaders() throws Exception {
        mockMvc.perform(options("/api/rooms/uploads/recent")
                        .header(HttpHeaders.ORIGIN, DEPLOYED_FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,X-RoomFit-Test,X-RoomFit-Client-Id"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        DEPLOYED_FRONTEND_ORIGIN
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        allOf(
                                containsString("GET"),
                                containsString("POST"),
                                containsString("PUT"),
                                containsString("PATCH"),
                                containsString("DELETE"),
                                containsString("OPTIONS")
                        )
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        allOf(containsString("Content-Type"), containsString("X-RoomFit-Test"),
                                containsString("X-RoomFit-Client-Id"))
                ))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }
}
