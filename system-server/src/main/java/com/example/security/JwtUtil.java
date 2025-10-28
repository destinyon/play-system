package com.example.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class JwtUtil {

    private static final int MIN_KEY_BYTES = 32;

    private final String configuredSecret;
    private final String issuer;
    private final long expirationSeconds;

    private SecretKey secretKey;

    public JwtUtil(
            @Value("${security.jwt.secret:}") String secret,
            @Value("${security.jwt.issuer:play-system}") String issuer,
            @Value("${security.jwt.expiration-seconds:86400}") long expirationSeconds) {
        this.configuredSecret = secret == null ? "" : secret.trim();
        this.issuer = issuer == null || issuer.isBlank() ? "play-system" : issuer.trim();
        this.expirationSeconds = expirationSeconds > 0 ? expirationSeconds : 86400;
    }

    @PostConstruct
    void init() {
        if (configuredSecret.isEmpty()) {
            byte[] random = new byte[MIN_KEY_BYTES];
            new SecureRandom().nextBytes(random);
            this.secretKey = Keys.hmacShaKeyFor(random);
            log.warn("security.jwt.secret 未配置，已生成临时密钥（服务重启后所有 Token 失效）");
            return;
        }

        byte[] keyBytes = decodeSecret(configuredSecret);
        if (keyBytes.length < MIN_KEY_BYTES) {
            keyBytes = padToMinLength(keyBytes);
            log.warn("security.jwt.secret 长度不足，已通过 SHA-256 补强处理");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] decodeSecret(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] padToMinLength(byte[] original) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(original);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            byte[] padded = new byte[MIN_KEY_BYTES];
            System.arraycopy(original, 0, padded, 0, Math.min(original.length, MIN_KEY_BYTES));
            if (original.length < MIN_KEY_BYTES) {
                byte[] random = new byte[MIN_KEY_BYTES - original.length];
                new SecureRandom().nextBytes(random);
                System.arraycopy(random, 0, padded, original.length, random.length);
            }
            return padded;
        }
    }

    public TokenDetails generateToken(String username, String role, Long userId, Map<String, Object> additionalClaims) {
        if (secretKey == null) {
            throw new IllegalStateException("JWT secret key 未初始化，请检查配置");
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        Map<String, Object> claims = new HashMap<>();
        if (additionalClaims != null) {
            claims.putAll(additionalClaims);
        }
        claims.put("role", role);
        if (userId != null) {
            claims.put("uid", userId);
        }

        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .addClaims(claims);

        String token = builder.compact();
        return new TokenDetails(token, username, role, userId, now, expiresAt);
    }

    public Optional<TokenDetails> parseToken(String token) {
        if (token == null || token.isBlank() || secretKey == null) {
            return Optional.empty();
        }
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .requireIssuer(issuer)
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            Claims claims = jws.getBody();
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            Long userId = null;
            Object uidObj = claims.get("uid");
            if (uidObj instanceof Number number) {
                userId = number.longValue();
            } else if (uidObj instanceof String str) {
                try {
                    userId = Long.parseLong(str);
                } catch (NumberFormatException ignored) {
                }
            }
            Instant issuedAt = claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : null;
            Instant expiresAt = claims.getExpiration() != null ? claims.getExpiration().toInstant() : null;
            return Optional.of(new TokenDetails(token, username, role, userId, issuedAt, expiresAt));
        } catch (Exception ex) {
            log.debug("解析 JWT 失败: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Getter
    public static class TokenDetails {
        private final String token;
        private final String username;
        private final String role;
        private final Long userId;
        private final Instant issuedAt;
        private final Instant expiresAt;

        TokenDetails(String token, String username, String role, Long userId, Instant issuedAt, Instant expiresAt) {
            this.token = token;
            this.username = username;
            this.role = role;
            this.userId = userId;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }
    }
}
