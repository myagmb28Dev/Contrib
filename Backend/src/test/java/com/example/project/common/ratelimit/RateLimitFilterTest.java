package com.example.project.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.example.project.auth.service.GitHubPrincipal;
import com.example.project.common.config.RequestCorrelationFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFilterTest {

    private RateLimiter rateLimiter;
    private RateLimitProperties properties;
    private ObjectMapper objectMapper;
    private RateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
        properties = new RateLimitProperties();
        objectMapper = new ObjectMapper().findAndRegisterModules();
        filter = new RateLimitFilter(rateLimiter, properties, objectMapper);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsRequestsWhenDisabled() throws ServletException, IOException {
        properties.setEnabled(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/certificates/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void throttlesPublicEndpointWhenLimitExceeded() throws ServletException, IOException {
        properties.setPublicLimit(2);
        properties.setWindowSeconds(60);

        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/certificates/abc");
            request.setRemoteAddr("192.168.1.10");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("GET", "/api/public/certificates/abc");
        blockedRequest.setRemoteAddr("192.168.1.10");
        blockedRequest.addHeader(RequestCorrelationFilter.HEADER_NAME, "req-1234");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        filter.doFilter(blockedRequest, blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getHeader("Retry-After")).isEqualTo("60");
        JsonNode json = objectMapper.readTree(blockedResponse.getContentAsString());
        assertThat(json.path("code").asText()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(json.path("requestId").asText()).isEqualTo("req-1234");
    }

    @Test
    void limitsAuthenticatedAnalysisCreationPerUser() throws ServletException, IOException {
        properties.setAnalysisLimit(1);
        GitHubPrincipal principal = new GitHubPrincipal(UUID.randomUUID(), 12345L, "testuser", "testuser@example.com", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/repositories/repo-1/analyses");
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        filter.doFilter(request1, response1, filterChain);
        assertThat(response1.getStatus()).isEqualTo(200);

        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/repositories/repo-1/analyses");
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        filter.doFilter(request2, response2, filterChain);
        assertThat(response2.getStatus()).isEqualTo(429);
    }

    @Test
    void limitsAiSummaryRegeneration() throws ServletException, IOException {
        properties.setAiSummaryLimit(1);
        GitHubPrincipal principal = new GitHubPrincipal(UUID.randomUUID(), 9999L, "aiuser", "aiuser@example.com", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/analyses/analysis-1/ai-summary");
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        filter.doFilter(request1, response1, filterChain);
        assertThat(response1.getStatus()).isEqualTo(200);

        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/analyses/analysis-1/ai-summary");
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        filter.doFilter(request2, response2, filterChain);
        assertThat(response2.getStatus()).isEqualTo(429);
    }
}
