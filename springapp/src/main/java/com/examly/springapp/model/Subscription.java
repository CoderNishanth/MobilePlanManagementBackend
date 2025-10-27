package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @JsonIgnoreProperties({"password", "verificationToken", "hibernateLazyInitializer", "handler"})
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    @JsonIgnoreProperties({"planManager", "hibernateLazyInitializer", "handler"})
    private Plan plan;
    
    private LocalDateTime activationDate;
    private LocalDateTime expiryDate;
    
    @Enumerated(EnumType.STRING)
    private Status status;         // ACTIVE, EXPIRED, CANCELLED

    private String paymentMethod;  // e.g., UPI, Card, Wallet
    
    public enum Status {
        ACTIVE,
        EXPIRED,
        CANCELLED
    }
}
