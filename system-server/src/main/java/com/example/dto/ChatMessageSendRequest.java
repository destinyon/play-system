package com.example.dto;

import lombok.Data;

@Data
public class ChatMessageSendRequest {
    private Integer orderId;
    private String content;
    private String senderUsername;
    private String senderRole;
    private Integer targetId;
    private String targetRole;
}
