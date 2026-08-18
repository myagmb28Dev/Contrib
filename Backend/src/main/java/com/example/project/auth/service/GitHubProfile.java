package com.example.project.auth.service;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public record GitHubProfile(long id, String username, String email) {

    private static final String INVALID_USER_INFO = "invalid_user_info_response";

    public static GitHubProfile from(Map<String, Object> attributes) {
        long id = parseId(attributes.get("id"));
        String username = requiredString(attributes.get("login"), "login");
        String email = optionalString(attributes.get("email"));
        return new GitHubProfile(id, username, email);
    }

    private static long parseId(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                throw invalidUserInfo("GitHub user id is not numeric");
            }
        }
        throw invalidUserInfo("GitHub user id is missing");
    }

    private static String requiredString(Object value, String attributeName) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw invalidUserInfo("GitHub " + attributeName + " is missing");
    }

    private static String optionalString(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private static OAuth2AuthenticationException invalidUserInfo(String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(INVALID_USER_INFO), description);
    }
}
