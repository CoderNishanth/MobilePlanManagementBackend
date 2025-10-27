package com.examly.springapp.service;

import com.examly.springapp.model.Subscription;
import com.examly.springapp.model.UsageRecord;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UsageRecordRepository;
import com.examly.springapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsageService {

    private final UsageRecordRepository usageRecordRepository;
    private final UserRepository userRepository;

    public UsageRecord logUsage(UsageRecord usageRecord) {
        return usageRecordRepository.save(usageRecord);
    }

    public List<UsageRecord> getUsageByCustomer(User customer) {
        return usageRecordRepository.findByCustomer(customer);
    }

    public List<UsageRecord> getUsageBySubscription(Subscription subscription) {
        return usageRecordRepository.findAll().stream()
                .filter(usage -> usage.getSubscription().getId().equals(subscription.getId()))
                .collect(Collectors.toList());
    }

    public List<UsageRecord> getAllUsageRecords() {
        return usageRecordRepository.findAll();
    }

    public List<UsageRecord> getUsageByServiceArea(String serviceArea) {
        List<User> customersInArea = userRepository.findByServiceAreaAndRole(serviceArea, User.Role.CUSTOMER);
        return customersInArea.stream()
                .flatMap(customer -> usageRecordRepository.findByCustomer(customer).stream())
                .collect(Collectors.toList());
    }

    public Map<String, Object> getMonthlyUsageSummary(User customer, int year, int month) {
        LocalDateTime startOfMonth;
        LocalDateTime endOfMonth;

        if (year == 0 || month == 0) {
            YearMonth currentMonth = YearMonth.now();
            startOfMonth = currentMonth.atDay(1).atStartOfDay();
            endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        } else {
            YearMonth targetMonth = YearMonth.of(year, month);
            startOfMonth = targetMonth.atDay(1).atStartOfDay();
            endOfMonth = targetMonth.atEndOfMonth().atTime(23, 59, 59);
        }

        List<UsageRecord> monthlyUsage = usageRecordRepository.findByCustomer(customer).stream()
                .filter(usage -> usage.getRecordDate().isAfter(startOfMonth) && 
                                usage.getRecordDate().isBefore(endOfMonth))
                .collect(Collectors.toList());

        int totalDataUsed = monthlyUsage.stream().mapToInt(UsageRecord::getDataUsed).sum();
        int totalCallsUsed = monthlyUsage.stream().mapToInt(UsageRecord::getCallsUsed).sum();
        int totalSmsUsed = monthlyUsage.stream().mapToInt(UsageRecord::getSmsUsed).sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("month", month == 0 ? LocalDateTime.now().getMonthValue() : month);
        summary.put("year", year == 0 ? LocalDateTime.now().getYear() : year);
        summary.put("totalDataUsed", totalDataUsed);
        summary.put("totalCallsUsed", totalCallsUsed);
        summary.put("totalSmsUsed", totalSmsUsed);
        summary.put("recordCount", monthlyUsage.size());
        summary.put("averageDailyData", monthlyUsage.isEmpty() ? 0 : totalDataUsed / YearMonth.now().lengthOfMonth());
        summary.put("usageRecords", monthlyUsage);

        return summary;
    }

    public Map<String, Object> getUsageStatistics(String period) {
        List<UsageRecord> allUsage = usageRecordRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        switch (period != null ? period.toLowerCase() : "all") {
            case "today":
                startDate = now.toLocalDate().atStartOfDay();
                break;
            case "week":
                startDate = now.minusWeeks(1);
                break;
            case "month":
                startDate = now.minusMonths(1);
                break;
            case "year":
                startDate = now.minusYears(1);
                break;
            default:
                startDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        }
        
        List<UsageRecord> filteredUsage = allUsage.stream()
                .filter(usage -> usage.getRecordDate().isAfter(startDate))
                .collect(Collectors.toList());
        
        int totalDataUsed = filteredUsage.stream().mapToInt(UsageRecord::getDataUsed).sum();
        int totalCallsUsed = filteredUsage.stream().mapToInt(UsageRecord::getCallsUsed).sum();
        int totalSmsUsed = filteredUsage.stream().mapToInt(UsageRecord::getSmsUsed).sum();

        long uniqueUsers = filteredUsage.stream()
                .map(usage -> usage.getCustomer().getId())
                .distinct()
                .count();

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRecords", filteredUsage.size());
        statistics.put("uniqueUsers", uniqueUsers);
        statistics.put("totalDataUsed", totalDataUsed);
        statistics.put("totalCallsUsed", totalCallsUsed);
        statistics.put("totalSmsUsed", totalSmsUsed);
        statistics.put("averageDataPerUser", uniqueUsers == 0 ? 0 : totalDataUsed / uniqueUsers);
        statistics.put("averageCallsPerUser", uniqueUsers == 0 ? 0 : totalCallsUsed / uniqueUsers);
        statistics.put("averageSmsPerUser", uniqueUsers == 0 ? 0 : totalSmsUsed / uniqueUsers);
        statistics.put("period", period != null ? period : "all");
        statistics.put("generatedAt", LocalDateTime.now());

        return statistics;
    }

    public List<Map<String, Object>> getHeavyUsers(String type, int limit) {
        List<UsageRecord> allUsage = usageRecordRepository.findAll();
        
        Map<Long, Integer> userUsage = new HashMap<>();
        
        // Aggregate usage by customer
        for (UsageRecord record : allUsage) {
            Long customerId = record.getCustomer().getId();
            int usage = 0;
            
            switch (type.toLowerCase()) {
                case "data":
                    usage = record.getDataUsed();
                    break;
                case "calls":
                    usage = record.getCallsUsed();
                    break;
                case "sms":
                    usage = record.getSmsUsed();
                    break;
            }
            
            userUsage.merge(customerId, usage, Integer::sum);
        }
        
        // Sort by usage and get top users
        return userUsage.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    User user = userRepository.findById(entry.getKey()).orElse(null);
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("customerId", entry.getKey());
                    userInfo.put("customerName", user != null ? user.getUsername() : "Unknown");
                    userInfo.put("customerEmail", user != null ? user.getEmail() : "Unknown");
                    userInfo.put("totalUsage", entry.getValue());
                    userInfo.put("usageType", type);
                    return userInfo;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getCustomerUsagePatterns(User customer) {
        List<UsageRecord> customerUsage = usageRecordRepository.findByCustomer(customer);
        
        if (customerUsage.isEmpty()) {
            return Map.of("message", "No usage data available for this customer");
        }

        // Calculate patterns
        Map<String, Object> patterns = new HashMap<>();
        
        // Daily averages
        double avgDailyData = customerUsage.stream().mapToInt(UsageRecord::getDataUsed).average().orElse(0);
        double avgDailyCalls = customerUsage.stream().mapToInt(UsageRecord::getCallsUsed).average().orElse(0);
        double avgDailySms = customerUsage.stream().mapToInt(UsageRecord::getSmsUsed).average().orElse(0);
        
        patterns.put("averageDailyData", avgDailyData);
        patterns.put("averageDailyCalls", avgDailyCalls);
        patterns.put("averageDailySms", avgDailySms);
        
        // Peak usage day
        UsageRecord peakDataDay = customerUsage.stream()
                .max(Comparator.comparing(UsageRecord::getDataUsed))
                .orElse(null);
        
        patterns.put("peakDataUsage", peakDataDay != null ? Map.of(
                "date", peakDataDay.getRecordDate(),
                "dataUsed", peakDataDay.getDataUsed()
        ) : null);
        
        // Usage trend (increasing/decreasing)
        List<UsageRecord> recentUsage = customerUsage.stream()
                .sorted(Comparator.comparing(UsageRecord::getRecordDate))
                .collect(Collectors.toList());
        
        if (recentUsage.size() >= 2) {
            int recentDataAvg = recentUsage.subList(Math.max(0, recentUsage.size() - 7), recentUsage.size())
                    .stream().mapToInt(UsageRecord::getDataUsed).sum() / 7;
            int olderDataAvg = recentUsage.subList(0, Math.min(7, recentUsage.size()))
                    .stream().mapToInt(UsageRecord::getDataUsed).sum() / 7;
            
            String trend = recentDataAvg > olderDataAvg ? "increasing" : 
                          recentDataAvg < olderDataAvg ? "decreasing" : "stable";
            patterns.put("usageTrend", trend);
        }
        
        patterns.put("totalRecords", customerUsage.size());
        patterns.put("analysisDate", LocalDateTime.now());
        
        return patterns;
    }

    public List<Map<String, Object>> getQuotaRemaining(User customer) {
        // Get customer's active subscriptions and their usage
        List<Subscription> activeSubscriptions = customer.getId() != null ? 
                Arrays.asList() : Arrays.asList(); // This would need SubscriptionService integration
        
        List<Map<String, Object>> quotaInfo = new ArrayList<>();
        
        // For now, create a simplified version
        List<UsageRecord> recentUsage = usageRecordRepository.findByCustomer(customer).stream()
                .filter(usage -> usage.getRecordDate().isAfter(LocalDateTime.now().minusDays(30)))
                .collect(Collectors.toList());
        
        int totalDataUsed = recentUsage.stream().mapToInt(UsageRecord::getDataUsed).sum();
        int totalCallsUsed = recentUsage.stream().mapToInt(UsageRecord::getCallsUsed).sum();
        int totalSmsUsed = recentUsage.stream().mapToInt(UsageRecord::getSmsUsed).sum();
        
        Map<String, Object> overallQuota = new HashMap<>();
        overallQuota.put("dataUsed", totalDataUsed);
        overallQuota.put("callsUsed", totalCallsUsed);
        overallQuota.put("smsUsed", totalSmsUsed);
        overallQuota.put("period", "Last 30 days");
        overallQuota.put("lastUpdated", LocalDateTime.now());
        
        quotaInfo.add(overallQuota);
        
        return quotaInfo;
    }
}
