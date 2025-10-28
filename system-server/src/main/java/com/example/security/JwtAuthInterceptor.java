package com.example.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final List<String> WHITELIST = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/user/avatar/**",
            "/uploads/**",
            "/error",
            "/ws/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (shouldBypass(request)) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null) {
            writeUnauthorized(response, "缺少身份凭证");
            return false;
        }

        Optional<JwtUtil.TokenDetails> parsed = jwtUtil.parseToken(token);
        if (parsed.isEmpty() || parsed.get().getUsername() == null) {
            writeUnauthorized(response, "身份凭证无效或已过期");
            return false;
        }

        JwtUtil.TokenDetails details = parsed.get();
        SecurityContext.AuthenticatedUser user = new SecurityContext.AuthenticatedUser(
                details.getUsername(),
                details.getRole(),
                details.getUserId(),
                details.getIssuedAt(),
                details.getExpiresAt(),
                details.getToken()
        );
        request.setAttribute(SecurityContext.REQUEST_ATTRIBUTE, user);
        SecurityContext.setCurrentUser(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityContext.clear();
    }

    private boolean shouldBypass(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        for (String pattern : WHITELIST) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    private String resolveToken(HttpServletRequest request) {
        String token = extractBearer(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token != null) {
            return token;
        }
        token = extractBearer(request.getHeader("authorization"));
        if (token != null) {
            return token;
        }
        String headerToken = request.getHeader("X-Auth-Token");
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken.trim();
        }
        Object attrToken = request.getAttribute("authToken");
        if (attrToken instanceof String str && !str.isBlank()) {
            return str.trim();
        }
        return null;
    }

    private String extractBearer(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String value = header.trim();
        if (value.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            return value.substring(7).trim();
        }
        return value;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> payload = Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "message", message,
                "data", null
        );
        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }
}
