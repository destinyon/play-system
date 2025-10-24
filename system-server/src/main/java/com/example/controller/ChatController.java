package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.ChatMessageDelivery;
import com.example.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @PostMapping("/sessions")
    public Result listSessions(@RequestBody DataRequest request) {
        return chatService.listSessions(request);
    }

    @PostMapping("/history")
    public Result history(@RequestBody DataRequest request) {
        return chatService.loadHistory(request);
    }

    @PostMapping("/markRead")
    public Result markRead(@RequestBody DataRequest request) {
        return chatService.markRead(request);
    }

    @PostMapping("/send")
    public Result send(@RequestBody DataRequest request) {
        // Use the same dispatch logic as WebSocket to ensure receiver gets realtime push
        ChatMessageDelivery delivery = chatService.dispatchMessage(request);

        try {
            java.util.Map<String, Object> base = new java.util.HashMap<>();
            base.put("message", delivery.getMessage());

            java.util.Map<String, Object> senderFrame = new java.util.HashMap<>(base);
            senderFrame.put("session", delivery.getSenderSession());
            if (delivery.getSenderUsername() != null) {
                messagingTemplate.convertAndSendToUser(delivery.getSenderUsername(), "/queue/chat", senderFrame);
            }

            if (delivery.getReceiverUsername() != null) {
                java.util.Map<String, Object> receiverFrame = new java.util.HashMap<>(base);
                receiverFrame.put("session", delivery.getReceiverSession());
                messagingTemplate.convertAndSendToUser(delivery.getReceiverUsername(), "/queue/chat", receiverFrame);
            }
        } catch (Exception ignored) {
            // Even if push fails, we still return the message to the REST caller
        }

        return Result.success(delivery.getMessage());
    }
}
