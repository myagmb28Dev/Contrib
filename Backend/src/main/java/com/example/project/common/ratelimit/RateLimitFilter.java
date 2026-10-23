package com.example.project.common.ratelimit;

import java.io.IOException;
import java.util.regex.Pattern;

import com.example.project.auth.service.GitHubPrincipal;
import com.example.project.common.config.RequestCorrelationFilter;
import com.example.project.common.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final Pattern PUBLIC_PATH = Pattern.compile("^/api/public/.*");
    private static final Pattern ANALYSIS_CREATE_PATH = Pattern.compile("^/api/repositories/[^/]+/analyses$");
    private static final Pattern ANALYSIS_RETRY_PATH = Pattern.compile("^/api/analysis-jobs/[^/]+/retry$");
    private static final Pattern AI_SUMMARY_PATH = Pattern.compile("^/api/analyses/[^/]+/ai-summary$");

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (PUBLIC_PATH.matcher(path).matches()) {
            String clientIp = resolveClientIp(request);
            String key = "public:" + clientIp;
            if (!rateLimiter.tryAcquire(key, properties.getPublicLimit(), properties.getWindowSeconds())) {
                reject(request, response, "공개 API 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
                return;
            }
        } else if ("POST".equalsIgnoreCase(method)) {
            if (AI_SUMMARY_PATH.matcher(path).matches()) {
                String subjectKey = resolveSubjectKey(request);
                String key = "ai-summary:" + subjectKey;
                if (!rateLimiter.tryAcquire(key, properties.getAiSummaryLimit(), properties.getWindowSeconds())) {
                    reject(request, response, "AI 요약 생성 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
                    return;
                }
            } else if (ANALYSIS_CREATE_PATH.matcher(path).matches() || ANALYSIS_RETRY_PATH.matcher(path).matches()) {
                String subjectKey = resolveSubjectKey(request);
                String key = "analysis:" + subjectKey;
                if (!rateLimiter.tryAcquire(key, properties.getAnalysisLimit(), properties.getWindowSeconds())) {
                    reject(request, response, "분석 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int commaIndex = forwarded.indexOf(',');
            return commaIndex > -1 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private String resolveSubjectKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof GitHubPrincipal principal) {
            return "user:" + principal.getUserId();
        }
        return "ip:" + resolveClientIp(request);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        String requestId = response.getHeader(RequestCorrelationFilter.HEADER_NAME);
        if (requestId == null) {
            requestId = request.getHeader(RequestCorrelationFilter.HEADER_NAME);
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(properties.getWindowSeconds()));

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "TOO_MANY_REQUESTS",
                message,
                request.getRequestURI(),
                requestId);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
