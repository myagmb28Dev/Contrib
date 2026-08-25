package com.example.project.auth.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.project.auth.service.GitHubPrincipal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "management.endpoints.web.exposure.include=health,info,prometheus",
        "management.prometheus.metrics.export.enabled=true",
        "app.monitoring.token=test-monitoring-token",
        "app.security.token-encryption-key=" + AuthSecurityIntegrationTest.TEST_KEY
})
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    static final String TEST_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void redirectsGitHubLoginToSpringAuthorizationEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/github"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/oauth2/authorization/github"));
    }

    @Test
    void rejectsCurrentUserRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void protectsPrometheusMetricsWithMonitoringToken() throws Exception {
        mockMvc.perform(get("/actuator").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.prometheus.href").exists());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/prometheus")
                        .header("X-Monitoring-Token", "test-monitoring-token"))
                .andExpect(status().isOk());
    }

    @Test
    void returnsCurrentAuthenticatedUser() throws Exception {
        GitHubPrincipal principal = new GitHubPrincipal(
                java.util.UUID.fromString("958cd0a0-21e5-4f9f-a86e-801347366838"),
                1001L,
                "octocat",
                "octocat@example.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("958cd0a0-21e5-4f9f-a86e-801347366838"))
                .andExpect(jsonPath("$.githubUserId").value(1001L))
                .andExpect(jsonPath("$.githubUsername").value("octocat"));
    }

    @Test
    void exposesCsrfTokenForFrontendRequests() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
