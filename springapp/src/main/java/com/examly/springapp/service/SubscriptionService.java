package com.examly.springapp.service;

import com.examly.springapp.model.Subscription;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.SubscriptionRepository;
import com.examly.springapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public Subscription createSubscription(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    public Subscription updateSubscription(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    public Subscription getSubscriptionById(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId).orElse(null);
    }

    public List<Subscription> getSubscriptionsByCustomer(User customer) {
        return subscriptionRepository.findByCustomer(customer);
    }

    public List<Subscription> getActiveSubscriptionsByCustomer(User customer) {
        return subscriptionRepository.findByCustomer(customer).stream()
                .filter(subscription -> subscription.getStatus() == Subscription.Status.ACTIVE)
                .collect(Collectors.toList());
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public List<Subscription> getSubscriptionsByStatus(Subscription.Status status) {
        return subscriptionRepository.findByStatus(status);
    }
    
    public List<Subscription> getSubscriptionsByServiceArea(String serviceArea) {
        List<User> customersInArea = userRepository.findByServiceAreaAndRole(serviceArea, User.Role.CUSTOMER);
        return customersInArea.stream()
                .flatMap(customer -> subscriptionRepository.findByCustomer(customer).stream())
                .collect(Collectors.toList());
    }

    public Map<String, Object> getSubscriptionStatistics() {
        // Use repository methods that don't load related entities
        long activeCount = subscriptionRepository.countActiveSubscriptions();
        long totalCount = subscriptionRepository.count();
        long expiredCount = totalCount - activeCount - 
                subscriptionRepository.findByStatus(Subscription.Status.CANCELLED).size();
        long cancelledCount = subscriptionRepository.findByStatus(Subscription.Status.CANCELLED).size();

        // Get total revenue from active subscriptions using repository method
        Double subscriptionRevenue = subscriptionRepository.getTotalActiveRevenue();
        double totalRevenue = subscriptionRevenue != null ? subscriptionRevenue : 0.0;
        
        // Calculate monthly growth: compare current month vs previous month subscriptions
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfPreviousMonth = startOfCurrentMonth.minusMonths(1);
        
        // Get subscriptions created in current month
        long currentMonthSubscriptions = subscriptionRepository.findAll().stream()
                .filter(sub -> sub.getActivationDate() != null && 
                              sub.getActivationDate().isAfter(startOfCurrentMonth))
                .count();
                
        // Get subscriptions created in previous month
        long previousMonthSubscriptions = subscriptionRepository.findAll().stream()
                .filter(sub -> sub.getActivationDate() != null && 
                              sub.getActivationDate().isAfter(startOfPreviousMonth) &&
                              sub.getActivationDate().isBefore(startOfCurrentMonth))
                .count();
                
        // Calculate growth percentage: ((current - previous) / previous) * 100
        double monthlyGrowth = previousMonthSubscriptions > 0 
            ? ((double) (currentMonthSubscriptions - previousMonthSubscriptions) / previousMonthSubscriptions) * 100
            : (currentMonthSubscriptions > 0 ? 100.0 : 0.0); // 100% growth if previous was 0 but current > 0
        
        // Calculate churn rate: (cancelled subscriptions / total subscriptions) * 100
        double churnRate = totalCount > 0 ? (double) cancelledCount / totalCount * 100 : 0.0;

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalSubscriptions", totalCount);
        statistics.put("activeSubscriptions", activeCount);
        statistics.put("expiredSubscriptions", expiredCount);
        statistics.put("cancelledSubscriptions", cancelledCount);
        statistics.put("totalActiveRevenue", totalRevenue);
        statistics.put("monthlyRevenue", totalRevenue); // Same as totalActiveRevenue for monthly billing
        statistics.put("monthlyGrowth", Math.round(monthlyGrowth * 100.0) / 100.0);
        statistics.put("churnRate", Math.round(churnRate * 100.0) / 100.0);
        statistics.put("generatedAt", LocalDateTime.now());
        
        return statistics;
    }

    public void extendSubscription(Long customerId, int days) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        List<Subscription> activeSubscriptions = getActiveSubscriptionsByCustomer(customer);
        
        for (Subscription subscription : activeSubscriptions) {
            subscription.setExpiryDate(subscription.getExpiryDate().plusDays(days));
            subscriptionRepository.save(subscription);
        }
    }

    public void reactivateSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        if (subscription.getStatus() == Subscription.Status.EXPIRED || 
            subscription.getStatus() == Subscription.Status.CANCELLED) {
            
            subscription.setStatus(Subscription.Status.ACTIVE);
            // Extend expiry date if it's in the past
            if (subscription.getExpiryDate().isBefore(LocalDateTime.now())) {
                subscription.setExpiryDate(LocalDateTime.now().plusDays(subscription.getPlan().getValidity()));
            }
            subscriptionRepository.save(subscription);
        } else {
            throw new RuntimeException("Only expired or cancelled subscriptions can be reactivated");
        }
    }

    public void processExpiredSubscriptions() {
        List<Subscription> activeSubscriptions = getSubscriptionsByStatus(Subscription.Status.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        int expiredCount = 0;
        
        for (Subscription subscription : activeSubscriptions) {
            if (subscription.getExpiryDate() != null && subscription.getExpiryDate().isBefore(now)) {
                subscription.setStatus(Subscription.Status.EXPIRED);
                subscriptionRepository.save(subscription);
                expiredCount++;
            }
        }
        
        if (expiredCount > 0) {
            System.out.println("Expired " + expiredCount + " subscriptions at " + now);
        }
    }
}
