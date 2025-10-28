package com.example.dto;

import lombok.Data;

@Data
public class ChatWebSocketPayload {
    private String sessionId;
    private Integer orderId;
    private Integer receiverId;
    private String receiverRole;
    private String content;
    private String username;
    private String clientMessageId;
}
