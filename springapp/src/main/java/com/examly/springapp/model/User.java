package com.examly.springapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String mobileNumber;
    private String serviceArea;

    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    
    @Builder.Default
    private boolean isVerified = false;
    @JsonIgnore
    private String verificationToken;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        
        if (this.verificationToken == null) {
            this.verificationToken = UUID.randomUUID().toString();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.lastLogin = LocalDateTime.now();
    }
    
    public enum Role {
        ADMIN,
        PLAN_MANAGER,
        RETAILER,
        CUSTOMER
    }
}
