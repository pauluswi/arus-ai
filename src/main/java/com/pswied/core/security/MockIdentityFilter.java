package com.pswied.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Profile("local")
public class MockIdentityFilter extends OncePerRequestFilter {

    private static final String DEFAULT_USERNAME = "demo-operator";
    private static final String DEFAULT_ROLE = MockRole.PAYMENT_OPERATOR.name();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String username = request.getHeader("X-Mock-User");
        if (username == null) {
            username = DEFAULT_USERNAME;
        }
        if (username.isBlank() || username.length() > 120) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-Mock-User must contain 1 to 120 characters.");
            return;
        }

        String rolesHeader = request.getHeader("X-Mock-Roles");
        List<String> roles = rolesHeader == null
                ? List.of(DEFAULT_ROLE)
                : Arrays.stream(rolesHeader.split(","))
                        .map(String::strip)
                        .filter(role -> !role.isEmpty())
                        .toList();
        if (roles.isEmpty() || roles.stream().anyMatch(role -> !isKnownRole(role))) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "X-Mock-Roles must contain comma-separated ARUS role names.");
            return;
        }

        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean isKnownRole(String role) {
        try {
            MockRole.valueOf(role);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
