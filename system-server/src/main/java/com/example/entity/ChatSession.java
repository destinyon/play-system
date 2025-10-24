package com.example.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSession {
    private Integer id;
    private String sessionKey;
    private Integer restaurateurId;
    private Integer peerUserId;
    private String peerRole;
    private Integer orderId;
    private String contextType;
    private Integer contextRef;
    private String title;
    private String lastMessagePreview;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
