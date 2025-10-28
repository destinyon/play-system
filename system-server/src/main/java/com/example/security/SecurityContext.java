package com.example.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityContext {

    public static final String REQUEST_ATTRIBUTE = SecurityContext.class.getName() + ".AUTH_USER";

    private static final ThreadLocal<AuthenticatedUser> CURRENT = new ThreadLocal<>();

    public static void setCurrentUser(AuthenticatedUser user) {
        if (user == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(user);
        }
    }

    public static AuthenticatedUser getCurrentUser() {
        return CURRENT.get();
    }

    public static String currentUsername() {
        AuthenticatedUser user = CURRENT.get();
        return user == null ? null : user.username();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static void enrich(Map<String, Object> target) {
        if (target == null) {
            return;
        }
        AuthenticatedUser user = CURRENT.get();
        if (user == null) {
            return;
        }
        target.put("username", user.username());
        if (user.role() != null) {
            target.put("role", user.role());
            target.put("authRole", user.role());
        }
        if (user.userId() != null) {
            target.put("userId", user.userId());
        }
    }

    public record AuthenticatedUser(String username,
                                    String role,
                                    Long userId,
                                    Instant issuedAt,
                                    Instant expiresAt,
                                    String token) {
        public AuthenticatedUser {
            Objects.requireNonNull(username, "username");
        }
    }
}
