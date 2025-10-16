package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;



@Data
@Entity
@Table(name = "restaurateur")
public class Restaurateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Double income;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToOne(mappedBy = "restaurateur", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Restaurant restaurant;
}
