package com.pswied.core.security;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/session")
@Profile("local")
public class MockSessionController {

    @GetMapping
    public SessionView getSession(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().substring("ROLE_".length()))
                .sorted()
                .toList();
        return new SessionView(authentication.getName(), roles, true);
    }

    public record SessionView(String username, List<String> roles, boolean mockIdentity) {
    }
}
