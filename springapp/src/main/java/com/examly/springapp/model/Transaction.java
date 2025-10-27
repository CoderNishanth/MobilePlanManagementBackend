package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @JsonIgnoreProperties({"password", "verificationToken", "hibernateLazyInitializer", "handler"})
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_id")
    @JsonIgnoreProperties({"password", "verificationToken", "hibernateLazyInitializer", "handler"})
    private User retailer; // The retailer who facilitated this transaction

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Plan plan; // The plan being subscribed to (for subscription transactions)

    private double amount;

    @Enumerated(EnumType.STRING)
    private Type transactionType;   // RECHARGE, REFUND, etc.
    
    @Enumerated(EnumType.STRING)
    private Status status;          // SUCCESS, FAILED

    private String paymentMethod;
    private LocalDateTime transactionDate;

    public enum Type { RECHARGE, REFUND, SUBSCRIPTION, PLAN_PURCHASE }
    public enum Status { SUCCESS, FAILED, PENDING, CANCELLED }
}
