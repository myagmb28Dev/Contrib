package com.example.project.common.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ProductionEnvironmentValidatorTest {

    @Test
    void acceptsCompleteProductionConfiguration() {
        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(validEnvironment());

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonHttpsFrontend() {
        MockEnvironment environment = validEnvironment()
                .withProperty("app.frontend-url", "http://app.example.com");
        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(environment);

        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    private MockEnvironment validEnvironment() {
        return new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://db.example.com/contribution")
                .withProperty("spring.datasource.username", "contribution")
                .withProperty("spring.datasource.password", "secret")
                .withProperty("spring.security.oauth2.client.registration.github.client-id", "client-id")
                .withProperty("spring.security.oauth2.client.registration.github.client-secret", "client-secret")
                .withProperty("app.security.token-encryption-key", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .withProperty("app.monitoring.token", "0123456789abcdef0123456789abcdef")
                .withProperty("app.blockchain.contract-address", "0x1111111111111111111111111111111111111111")
                .withProperty("app.frontend-url", "https://app.example.com")
                .withProperty("app.cors.allowed-origins", "https://app.example.com")
                .withProperty("server.servlet.session.cookie.secure", "true");
    }
}
