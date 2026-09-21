package com.springexample.webchat.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;   // 로그인 ID

    @Column(nullable = false)
    private String password;   // BCrypt 암호화 저장

    protected User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long getId()           { return id; }
    public String getUsername()   { return username; }
    public String getPassword()   { return password; }
}
