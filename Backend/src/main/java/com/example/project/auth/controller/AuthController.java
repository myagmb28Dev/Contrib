package com.example.project.auth.controller;

import java.net.URI;

import com.example.project.auth.service.GitHubPrincipal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/github")
    public ResponseEntity<Void> githubLogin() {
        return ResponseEntity.status(302)
                .location(URI.create("/oauth2/authorization/github"))
                .build();
    }

    @GetMapping("/me")
    public AuthMeResponse me(@AuthenticationPrincipal GitHubPrincipal principal) {
        return AuthMeResponse.from(principal);
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return CsrfTokenResponse.from(csrfToken);
    }
}
