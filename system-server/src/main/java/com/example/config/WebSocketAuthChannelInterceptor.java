package com.example.config;

import com.example.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);
            if (!StringUtils.hasText(token)) {
                throw new IllegalArgumentException("缺少聊天鉴权 Token");
            }
            Optional<JwtUtil.TokenDetails> parsed = jwtUtil.parseToken(token);
            if (parsed.isEmpty() || !StringUtils.hasText(parsed.get().getUsername())) {
                throw new IllegalArgumentException("聊天鉴权 Token 无效");
            }
            String username = parsed.get().getUsername();

            String requestedUsername = headerFirst(accessor, "username");
            if (!StringUtils.hasText(requestedUsername)) {
                requestedUsername = headerFirst(accessor, "login");
            }
            if (StringUtils.hasText(requestedUsername) && !requestedUsername.trim().equalsIgnoreCase(username)) {
                log.warn("WebSocket 鉴权用户名不匹配: header={}, token={}", requestedUsername, username);
                throw new IllegalArgumentException("身份信息不匹配");
            }

            accessor.getSessionAttributes().put("authToken", token);
            accessor.getSessionAttributes().put("authRole", parsed.get().getRole());
            Principal principal = new WebSocketPrincipal(username.trim());
            accessor.setUser(principal);
        }
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String token = headerFirst(accessor, HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(token)) {
            token = headerFirst(accessor, "authorization");
        }
        if (!StringUtils.hasText(token)) {
            token = headerFirst(accessor, "token");
        }
        if (StringUtils.hasText(token) && token.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            return token.substring(7).trim();
        }
        return StringUtils.hasText(token) ? token.trim() : null;
    }

    private String headerFirst(StompHeaderAccessor accessor, String key) {
        List<String> values = accessor.getNativeHeader(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }
}
