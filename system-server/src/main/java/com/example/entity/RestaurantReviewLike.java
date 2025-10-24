package com.example.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RestaurantReviewLike {
    private Integer id;
    private Integer reviewId;
    private Integer userId;
    private LocalDateTime createdAt;
}
