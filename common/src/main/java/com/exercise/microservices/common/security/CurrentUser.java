package com.exercise.microservices.common.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        Object claim = jwt().getClaim(JwtSupport.USER_ID);
        if (claim instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("JWT does not contain a user id");
    }

    public static boolean isAdmin() {
        return authentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public static void assertOwnerOrAdmin(Long ownerId) {
        if (isAdmin() || id().equals(ownerId)) {
            return;
        }
        throw new AccessDeniedException("You can only access your own orders");
    }

    public static String tokenValue() {
        return jwt().getTokenValue();
    }

    private static Jwt jwt() {
        if (authentication() instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken();
        }
        throw new IllegalStateException("Expected a JWT authentication");
    }

    private static Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }
        return authentication;
    }
}
