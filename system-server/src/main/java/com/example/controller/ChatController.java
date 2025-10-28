package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.ChatMessageDelivery;
import com.example.security.SecurityContext;
import com.example.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/sessions")
    public Result<?> listSessions(@RequestBody DataRequest request) {
        return chatService.listSessions(request);
    }

    @PostMapping("/history")
    public Result<?> history(@RequestBody DataRequest request) {
        return chatService.loadHistory(request);
    }

    @PostMapping("/markRead")
    public Result<?> markRead(@RequestBody DataRequest request) {
        return chatService.markRead(request);
    }

    @PostMapping("/send")
    public Result<?> send(@RequestBody DataRequest request) {
        Map<String, Object> data = request != null ? request.getData() : null;
        String clientMessageId = extractClientMessageId(data);
        try {
            ChatMessageDelivery delivery = chatService.dispatchMessage(request);
            pushDelivery(delivery, clientMessageId);
            return Result.success(delivery.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Chat send rejected: {}", ex.getMessage());
            sendErrorFrame(SecurityContext.currentUsername(), clientMessageId, ex.getMessage());
            return Result.error(ex.getMessage());
        } catch (Exception ex) {
            log.error("Chat send failed", ex);
            String fallback = "发送失败，请稍后再试";
            sendErrorFrame(SecurityContext.currentUsername(), clientMessageId, fallback);
            return Result.error(fallback);
        }
    }

    private void pushDelivery(ChatMessageDelivery delivery, String clientMessageId) {
        Map<String, Object> base = new HashMap<>();
        base.put("message", delivery.getMessage());
        if (clientMessageId != null) {
            base.put("clientMessageId", clientMessageId);
        }

        Map<String, Object> senderFrame = new HashMap<>(base);
        senderFrame.put("session", delivery.getSenderSession());
        if (delivery.getSenderUsername() != null) {
            messagingTemplate.convertAndSendToUser(delivery.getSenderUsername(), "/queue/chat", senderFrame);
            messagingTemplate.convertAndSend("/topic/chat/" + delivery.getSenderUsername(), senderFrame);
        }

        if (delivery.getReceiverUsername() != null) {
            Map<String, Object> receiverFrame = new HashMap<>(base);
            receiverFrame.put("session", delivery.getReceiverSession());
            messagingTemplate.convertAndSendToUser(delivery.getReceiverUsername(), "/queue/chat", receiverFrame);
            messagingTemplate.convertAndSend("/topic/chat/" + delivery.getReceiverUsername(), receiverFrame);
        }
    }

    private void sendErrorFrame(String username, String clientMessageId, String message) {
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

    private String extractClientMessageId(Map<String, Object> data) {
        if (data == null || !data.containsKey("clientMessageId")) {
            return null;
        }
        Object raw = data.get("clientMessageId");
        return raw == null ? null : raw.toString();
    }
}
