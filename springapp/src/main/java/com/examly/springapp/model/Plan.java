package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String planName;
    public enum PlanType { PREPAID, POSTPAID }
    @Enumerated(EnumType.STRING)
    private PlanType type;           // e.g., Prepaid/Postpaid
    private int price;
    private int validity;          // in days
    private String dataAllowance;  // e.g., "5GB"
    private int callMinutes;
    private int smsQuota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_manager_id")
    @JsonIgnoreProperties({"password", "verificationToken", "createdAt", "lastLogin", "hibernateLazyInitializer", "handler"})
    private User planManager;      // The Plan Manager who created this plan
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
