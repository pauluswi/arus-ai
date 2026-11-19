package com.pswied.core.security;

import com.pswied.core.ArusAiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ArusAiApplication.class,
        properties = "spring.datasource.url=jdbc:h2:mem:arus-ai-security-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MockIdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void providesDefaultLocalMockIdentity() throws Exception {
        mockMvc.perform(get("/api/v1/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo-operator"))
                .andExpect(jsonPath("$.roles[0]").value("PAYMENT_OPERATOR"))
                .andExpect(jsonPath("$.mockIdentity").value(true));
    }

    @Test
    void acceptsConfiguredMockRoles() throws Exception {
        mockMvc.perform(get("/api/v1/session")
                        .header("X-Mock-User", "demo-manager")
                        .header("X-Mock-Roles", "OPERATIONS_MANAGER,RECONCILIATION_OFFICER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo-manager"))
                .andExpect(jsonPath("$.roles[0]").value("OPERATIONS_MANAGER"))
                .andExpect(jsonPath("$.roles[1]").value("RECONCILIATION_OFFICER"));
    }

    @Test
    void rejectsUnknownMockRoles() throws Exception {
        mockMvc.perform(get("/api/v1/session").header("X-Mock-Roles", "ROOT"))
                .andExpect(status().isBadRequest());
    }
}
