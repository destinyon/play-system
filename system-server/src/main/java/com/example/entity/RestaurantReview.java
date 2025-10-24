package com.example.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RestaurantReview {
    private Integer id;
    private Restaurant restaurant;
    private User user;
    private Integer orderId;
    private Integer rating;
    private String content;
    private String detail;
    private Integer likes;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
