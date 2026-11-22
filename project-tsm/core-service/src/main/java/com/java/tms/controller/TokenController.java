package com.java.tms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Bridges the OIDC browser session to the JWT resource server: after the UI logs in via
 * Keycloak (OIDC), it calls this endpoint once to obtain the raw access token, then attaches
 * it as "Authorization: Bearer ..." on every /api/** call — same as a direct API client.
 * Answers 401 (not a redirect) when there is no OIDC session, so the UI can show "logged out".
 */
@RestController
@RequiredArgsConstructor
public class TokenController {

    private final OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/ui/token")
    public ResponseEntity<Map<String, Object>> currentAccessToken(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName());
        if (client == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = oauthToken.getPrincipal().getAttribute("preferred_username");
        if (username == null) {
            username = oauthToken.getName();
        }

        return ResponseEntity.ok(Map.of(
                "accessToken", client.getAccessToken().getTokenValue(),
                "username", username
        ));
    }
}
