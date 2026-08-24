package com.example.project.common.security;

import java.util.Arrays;
import java.util.List;

import com.example.project.auth.service.GitHubOAuth2UserService;
import com.example.project.common.config.RequestCorrelationFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final GitHubOAuth2UserService gitHubOAuth2UserService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String frontendUrl;
    private final List<String> allowedOrigins;
    private final boolean secureCookie;
    private final String sameSiteCookie;

    public SecurityConfig(
            GitHubOAuth2UserService gitHubOAuth2UserService,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            OAuth2AuthorizedClientService authorizedClientService,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.cors.allowed-origins:${app.frontend-url}}") String allowedOrigins,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookie,
            @Value("${server.servlet.session.cookie.same-site:lax}") String sameSiteCookie) {
        this.gitHubOAuth2UserService = gitHubOAuth2UserService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authorizedClientService = authorizedClientService;
        this.frontendUrl = frontendUrl;
        this.allowedOrigins = Arrays.stream(StringUtils.commaDelimitedListToStringArray(allowedOrigins))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
        this.secureCookie = secureCookie;
        this.sameSiteCookie = sameSiteCookie;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie
                .secure(secureCookie)
                .sameSite(sameSiteCookie));

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.migrateSession()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/error",
                                "/api/health",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/prometheus",
                                "/livez",
                                "/readyz",
                                "/oauth2/authorization/**",
                                "/api/auth/github",
                                "/api/auth/github/callback/**",
                                "/api/public/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint))
                .oauth2Login(oauth2 -> oauth2
                        .authorizedClientService(authorizedClientService)
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/api/auth/github/callback/*"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(gitHubOAuth2UserService))
                        .successHandler((request, response, authentication) -> response
                                .sendRedirect(frontendUrl + "/dashboard"))
                        .failureHandler((request, response, exception) -> response
                                .sendRedirect(frontendUrl + "/login?error=oauth")))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessHandler((request, response, authentication) -> response
                                .setStatus(HttpStatus.NO_CONTENT.value())));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN"));
        configuration.setExposedHeaders(List.of(RequestCorrelationFilter.HEADER_NAME));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
