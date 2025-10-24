package com.example.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String username = headerFirst(accessor, "username");
            if (username == null || username.isBlank()) {
                username = headerFirst(accessor, "login");
            }
            if (username != null && !username.isBlank()) {
                Principal principal = new WebSocketPrincipal(username.trim());
                accessor.setUser(principal);
            }
        }
        return message;
    }

    private String headerFirst(StompHeaderAccessor accessor, String key) {
        List<String> values = accessor.getNativeHeader(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }
}
