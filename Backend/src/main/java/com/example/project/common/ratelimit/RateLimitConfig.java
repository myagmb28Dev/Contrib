package com.example.project.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimitFilter rateLimitFilter(
            RateLimiter rateLimiter,
            RateLimitProperties properties,
            ObjectMapper objectMapper) {
        return new RateLimitFilter(rateLimiter, properties, objectMapper);
    }
}
