package com.example.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessage {

    private Integer id;

    private Integer sessionId;

    private Integer orderId;

    private String contextType;

    private Integer contextRef;

    private Integer senderId;

    private String senderRole;

    private Integer receiverId;

    private String receiverRole;

    private String content;

    private Boolean readFlag;

    private LocalDateTime createdAt;
}
