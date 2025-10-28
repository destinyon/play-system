package com.example.controller;

import com.example.common.DataRequest;
import com.example.dto.ChatMessageDelivery;
import com.example.dto.ChatWebSocketPayload;
import com.example.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/send")
    public void handleSend(ChatWebSocketPayload payload, Principal principal) {
        if (payload == null) {
            return;
        }
        String username = principal != null ? principal.getName() : payload.getUsername();
        if (username == null || username.isBlank()) {
            log.warn("Rejecting chat send: missing username");
            return;
        }

        String clientMessageId = payload.getClientMessageId();
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", payload.getOrderId());
        data.put("receiverId", payload.getReceiverId());
        data.put("receiverRole", payload.getReceiverRole());
        data.put("content", payload.getContent());
        data.put("username", username);
        if (clientMessageId != null) {
            data.put("clientMessageId", clientMessageId);
        }

        DataRequest request = new DataRequest();
        request.setData(data);

        try {
            ChatMessageDelivery delivery = chatService.dispatchMessage(request);
            Map<String, Object> base = new HashMap<>();
            base.put("message", delivery.getMessage());
            if (clientMessageId != null) {
                base.put("clientMessageId", clientMessageId);
            }

            Map<String, Object> senderFrame = new HashMap<>(base);
            senderFrame.put("session", delivery.getSenderSession());
            messagingTemplate.convertAndSendToUser(delivery.getSenderUsername(), "/queue/chat", senderFrame);
            messagingTemplate.convertAndSend("/topic/chat/" + delivery.getSenderUsername(), senderFrame);

            if (delivery.getReceiverUsername() != null) {
                Map<String, Object> receiverFrame = new HashMap<>(base);
                receiverFrame.put("session", delivery.getReceiverSession());
                messagingTemplate.convertAndSendToUser(delivery.getReceiverUsername(), "/queue/chat", receiverFrame);
                messagingTemplate.convertAndSend("/topic/chat/" + delivery.getReceiverUsername(), receiverFrame);
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to send chat message: {}", ex.getMessage());
            sendError(username, ex.getMessage(), clientMessageId);
        } catch (Exception ex) {
            log.error("Unexpected error when sending chat message", ex);
            sendError(username, "发送失败，请稍后再试", clientMessageId);
        }
    }

    @MessageExceptionHandler
    public void handleException(Throwable ex, Principal principal) {
        String username = principal == null ? null : principal.getName();
        if (username != null) {
            sendError(username, ex.getMessage(), null);
        }
    }

    private void sendError(String username, String message, String clientMessageId) {
        if (username == null || username.isBlank()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ERROR");
        payload.put("message", message);
        if (clientMessageId != null) {
            payload.put("clientMessageId", clientMessageId);
        }
        messagingTemplate.convertAndSendToUser(username, "/queue/chat", payload);
    }
}
