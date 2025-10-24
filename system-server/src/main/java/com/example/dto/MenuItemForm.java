package com.example.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MenuItemForm {
    private Integer id;
    private String name;
    private String category;
    private BigDecimal price;
    private String imageUrl;
    private String description;
    private String status;
}
