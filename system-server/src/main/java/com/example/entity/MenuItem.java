package com.example.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MenuItem {

    private Integer id;

    private Integer restaurateurId;

    private String name;

    private String category;

    private BigDecimal price;

    private String imageUrl;

    private String description;

    private String status;

    private Boolean deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
