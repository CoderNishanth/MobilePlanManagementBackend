package com.examly.springapp.repository;

import com.examly.springapp.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByType(Plan.PlanType type);
    List<Plan> findAllByOrderByPriceDesc();
    
    @Query("SELECT COUNT(p) FROM Plan p WHERE p.type = :type")
    long countByType(@Param("type") Plan.PlanType type);
    
    @Query("SELECT AVG(p.price) FROM Plan p")
    Double getAveragePrice();
    
    @Query("SELECT p FROM Plan p ORDER BY p.price DESC")
    List<Plan> findTopByOrderByPriceDesc();
    
    @Query("SELECT p FROM Plan p ORDER BY p.price ASC")
    List<Plan> findTopByOrderByPriceAsc();
}
