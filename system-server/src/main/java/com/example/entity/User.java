package com.example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 主键，自增

    @Column(unique = true, nullable = false)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Column(nullable = false)
    @NotBlank(message = "密码不能为空")
    private String password;

    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;

    private String address;

    @Email(message = "邮箱格式不正确")
    private String email;

    // 手机号验证（大陆手机号）
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String avatarUrl;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Deliveryman deliveryman;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Restaurateur restaurateur;
}
