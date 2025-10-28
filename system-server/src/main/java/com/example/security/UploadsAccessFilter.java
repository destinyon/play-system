package com.example.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class UploadsAccessFilter extends OncePerRequestFilter {

    private static final String PREFIX = "/uploads/";
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!HttpMethod.GET.matches(request.getMethod()) && !HttpMethod.HEAD.matches(request.getMethod())) {
            reject(response, HttpStatus.METHOD_NOT_ALLOWED, "仅允许 GET/HEAD 访问上传资源");
            return;
        }

        String normalized = uri.replace('\\', '/');
        if (normalized.contains("..") || normalized.contains("//") || normalized.contains("\\\\")) {
            reject(response, HttpStatus.BAD_REQUEST, "非法的资源路径");
            return;
        }

        String extension = StringUtils.getFilenameExtension(normalized);
        if (extension != null) {
            String lower = extension.toLowerCase(Locale.ROOT);
            if (!isAllowedExtension(lower)) {
                reject(response, HttpStatus.FORBIDDEN, "不支持的文件类型访问");
                return;
            }
        }

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "public, max-age=31536000");
        filterChain.doFilter(request, response);
    }

    private boolean isAllowedExtension(String ext) {
        return switch (ext) {
            case "png", "jpg", "jpeg", "gif", "webp", "svg", "avif" -> true;
            default -> false;
        };
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> payload = Map.of(
                "status", status.value(),
                "message", message,
                "data", null
        );
        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }
}
