package com.example.project.common.config;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("prod")
public class ProductionEnvironmentValidator implements ApplicationRunner {

    private static final List<String> REQUIRED_PROPERTIES = List.of(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.security.oauth2.client.registration.github.client-id",
            "spring.security.oauth2.client.registration.github.client-secret",
            "app.security.token-encryption-key",
            "app.monitoring.token",
            "app.blockchain.contract-address");

    private final Environment environment;

    public ProductionEnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String property : REQUIRED_PROPERTIES) {
            requireConfigured(property);
        }

        String frontendUrl = requireConfigured("app.frontend-url");
        URI frontendUri = URI.create(frontendUrl);
        if (!"https".equalsIgnoreCase(frontendUri.getScheme()) || frontendUri.getHost() == null) {
            throw new IllegalStateException("FRONTEND_URL must be an absolute HTTPS URL in production");
        }

        if (!environment.getProperty("server.servlet.session.cookie.secure", Boolean.class, false)) {
            throw new IllegalStateException("Secure session cookies must be enabled in production");
        }

        String allowedOrigins = requireConfigured("app.cors.allowed-origins");
        List<String> origins = Arrays.stream(StringUtils.commaDelimitedListToStringArray(allowedOrigins))
                .map(String::trim)
                .toList();
        if (!origins.contains(frontendUrl) || origins.stream().anyMatch(origin -> !isHttpsUrl(origin))) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must use HTTPS and include FRONTEND_URL");
        }

        if (requireConfigured("app.monitoring.token").length() < 32) {
            throw new IllegalStateException("MONITORING_TOKEN must contain at least 32 characters");
        }

        String contractAddress = requireConfigured("app.blockchain.contract-address");
        if (!contractAddress.matches("0x[0-9a-fA-F]{40}")) {
            throw new IllegalStateException("ATTESTATION_CONTRACT_ADDRESS must be a valid EVM address");
        }
    }

    private String requireConfigured(String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank() || value.endsWith("-not-configured")) {
            throw new IllegalStateException(property + " must be configured in production");
        }
        return value;
    }

    private boolean isHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
