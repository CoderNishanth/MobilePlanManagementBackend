package com.examly.springapp.repository;

import com.examly.springapp.model.Plan;
import com.examly.springapp.model.Subscription;
import com.examly.springapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCustomer(User customer);
    List<Subscription> findByPlan(Plan plan);
    List<Subscription> findByStatus(Subscription.Status status);
    
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE'")
    long countActiveSubscriptions();
    
    @Query("SELECT SUM(s.plan.price) FROM Subscription s")
    Double getTotalActiveRevenue();
    
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan = :plan AND s.status = 'ACTIVE'")
    long countActiveSubscriptionsByPlan(@Param("plan") Plan plan);
    
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan = :plan")
    long countTotalSubscriptionsByPlan(@Param("plan") Plan plan);
    
    @Query("SELECT SUM(s.plan.price) FROM Subscription s WHERE s.plan = :plan")
    Double getRevenueByPlan(@Param("plan") Plan plan);
    
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan = :plan AND s.status = 'CANCELLED' AND s.activationDate >= :since")
    long countCancelledSubscriptionsByPlanSince(@Param("plan") Plan plan, @Param("since") LocalDateTime since);
    
    @Query("SELECT s FROM Subscription s WHERE s.activationDate >= :startDate AND s.activationDate <= :endDate")
    List<Subscription> findByActivationDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

