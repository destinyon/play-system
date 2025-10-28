package com.example.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageDto {
    private Integer id;
    private String sessionId;
    private Integer orderId;
    private Integer senderId;
    private String senderRole;
    private Integer receiverId;
    private String receiverRole;
    private String content;
    private Boolean read;
    private LocalDateTime createdAt;
    private String clientMessageId;
}
