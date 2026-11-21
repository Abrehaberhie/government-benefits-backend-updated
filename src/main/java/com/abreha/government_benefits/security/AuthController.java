package com.abreha.government_benefits.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public CurrentUserResponse currentUser(Authentication authentication) {
        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .toList();

        return new CurrentUserResponse(
                authentication.getName(),
                roles
        );
    }

    public record CurrentUserResponse(
            String email,
            List<String> roles
    ) {}


    @GetMapping("/csrf")
    public java.util.Map<String, String> csrf(
            org.springframework.security.web.csrf.CsrfToken token
    ) {
        return java.util.Map.of(
                "headerName", token.getHeaderName(),
                "token", token.getToken()
        );
    }
}
