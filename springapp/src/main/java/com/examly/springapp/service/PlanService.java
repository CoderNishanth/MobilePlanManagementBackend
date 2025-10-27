package com.examly.springapp.service;

import com.examly.springapp.model.Plan;
import com.examly.springapp.model.Subscription;
import com.examly.springapp.repository.PlanRepository;
import com.examly.springapp.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    public Plan createPlan(Plan plan) {
        return planRepository.save(plan);
    }
    
    public Plan updatePlan(Plan plan) {
        return planRepository.save(plan);  // save() works for both create and update
    }
    
    public Plan getPlanById(Long id) {
        return planRepository.findById(id).orElse(null);
    }
    
    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }
    
    public List<Plan> getPlansByType(String type) {
        try {
            Plan.PlanType planType = Plan.PlanType.valueOf(type.toUpperCase());
            return planRepository.findByType(planType);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid plan type: " + type);
        }
    }

    public List<Plan> getPlansSortedByPrice() {
        return planRepository.findAllByOrderByPriceDesc();
    }

    public void deletePlan(Long id) {
        planRepository.deleteById(id);
    }

    // Statistics and Analytics Methods
    public Map<String, Object> getPlanStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Plan> allPlans = planRepository.findAll();
        stats.put("totalPlans", allPlans.size());
        
        long activePlans = allPlans.stream()
            .filter(plan -> plan.getCreatedAt() != null)
            .count();
        stats.put("activePlans", activePlans);
        
        long prepaidPlans = allPlans.stream()
            .filter(plan -> plan.getType() == Plan.PlanType.PREPAID)
            .count();
        stats.put("prepaidPlans", prepaidPlans);
        
        long postpaidPlans = allPlans.stream()
            .filter(plan -> plan.getType() == Plan.PlanType.POSTPAID)
            .count();
        stats.put("postpaidPlans", postpaidPlans);
        
        // Calculate average price
        double avgPrice = allPlans.stream()
            .mapToDouble(Plan::getPrice)
            .average()
            .orElse(0.0);
        stats.put("averagePrice", avgPrice);
        
        // Most expensive plan
        Plan mostExpensive = allPlans.stream()
            .max((p1, p2) -> Integer.compare(p1.getPrice(), p2.getPrice()))
            .orElse(null);
        stats.put("mostExpensivePlan", mostExpensive != null ? mostExpensive.getPlanName() : "N/A");
        
        // Most affordable plan
        Plan mostAffordable = allPlans.stream()
            .min((p1, p2) -> Integer.compare(p1.getPrice(), p2.getPrice()))
            .orElse(null);
        stats.put("mostAffordablePlan", mostAffordable != null ? mostAffordable.getPlanName() : "N/A");
        
        return stats;
    }

    public List<Map<String, Object>> getPlanPerformance() {
        List<Map<String, Object>> performance = new ArrayList<>();
        List<Plan> allPlans = planRepository.findAll();
        
        for (Plan plan : allPlans) {
            Map<String, Object> planPerf = new HashMap<>();
            planPerf.put("id", plan.getId());
            planPerf.put("planName", plan.getPlanName());
            planPerf.put("type", plan.getType().toString());
            planPerf.put("price", plan.getPrice());
            planPerf.put("validity", plan.getValidity());
            planPerf.put("dataAllowance", plan.getDataAllowance());
            planPerf.put("callMinutes", plan.getCallMinutes());
            planPerf.put("smsQuota", plan.getSmsQuota());
            
            // Calculate real metrics from subscriptions
            long totalSubscribers = subscriptionRepository.countTotalSubscriptionsByPlan(plan);
            long activeSubscribers = subscriptionRepository.countActiveSubscriptionsByPlan(plan);
            Double planRevenue = subscriptionRepository.getRevenueByPlan(plan);
            
            // Calculate churn rate (cancelled in last 30 days vs total active)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long recentCancellations = subscriptionRepository.countCancelledSubscriptionsByPlanSince(plan, thirtyDaysAgo);
            double churnRate = activeSubscribers > 0 ? (recentCancellations * 100.0) / activeSubscribers : 0;
            
            // Calculate satisfaction score based on plan features and price
            double satisfaction = calculateSatisfactionScore(plan, activeSubscribers);
            
            // Calculate growth rate based on recent subscriptions vs older ones
            double growthRate = calculateGrowthRate(plan);
            
            planPerf.put("subscribers", (int)activeSubscribers);
            planPerf.put("totalSubscribers", (int)totalSubscribers);
            planPerf.put("revenue", planRevenue != null ? planRevenue.intValue() : 0);
            planPerf.put("churnRate", Math.round(churnRate * 100.0) / 100.0);
            planPerf.put("satisfaction", Math.round(satisfaction * 100.0) / 100.0);
            planPerf.put("growthRate", Math.round(growthRate * 100.0) / 100.0);
            
            performance.add(planPerf);
        }
        
        return performance;
    }
    
    private double calculateSatisfactionScore(Plan plan, long activeSubscribers) {
        // Base satisfaction score on value proposition
        double baseScore = 3.0;
        
        // Higher data allowance increases satisfaction
        if (plan.getDataAllowance() != null) {
            if (plan.getDataAllowance().contains("GB")) {
                String dataStr = plan.getDataAllowance().replaceAll("[^0-9.]", "");
                try {
                    double dataGB = Double.parseDouble(dataStr);
                    if (dataGB >= 100) baseScore += 1.5;
                    else if (dataGB >= 50) baseScore += 1.0;
                    else if (dataGB >= 10) baseScore += 0.5;
                } catch (NumberFormatException e) {
                    // Handle parsing errors gracefully
                }
            }
        }
        
        // More subscribers generally indicates higher satisfaction
        if (activeSubscribers > 100) baseScore += 0.5;
        else if (activeSubscribers > 50) baseScore += 0.3;
        
        // Better call minutes increase satisfaction
        if (plan.getCallMinutes() > 1000) baseScore += 0.3;
        else if (plan.getCallMinutes() > 500) baseScore += 0.2;
        
        // Cap at 5.0
        return Math.min(5.0, baseScore);
    }
    
    private double calculateGrowthRate(Plan plan) {
        // Get subscriptions from last 30 days vs previous 30 days
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sixtyDaysAgo = now.minusDays(60);
        
        List<Subscription> recentSubs = subscriptionRepository.findByActivationDateBetween(thirtyDaysAgo, now)
            .stream()
            .filter(sub -> sub.getPlan().getId().equals(plan.getId()))
            .toList();
            
        List<Subscription> previousSubs = subscriptionRepository.findByActivationDateBetween(sixtyDaysAgo, thirtyDaysAgo)
            .stream()
            .filter(sub -> sub.getPlan().getId().equals(plan.getId()))
            .toList();
        
        if (previousSubs.isEmpty()) {
            return recentSubs.isEmpty() ? 0 : 100; // 100% growth if no previous subs but recent ones exist
        }
        
        double growthRate = ((recentSubs.size() - previousSubs.size()) * 100.0) / previousSubs.size();
        return growthRate;
    }
}
