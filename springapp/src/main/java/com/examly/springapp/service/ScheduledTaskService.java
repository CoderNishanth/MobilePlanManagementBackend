package com.examly.springapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {
    
    private final SubscriptionService subscriptionService;

    /**
     * Automatically expire subscriptions that have passed their expiry date
     * Runs every hour
     * TEMPORARILY DISABLED to fix circular reference issues
     */
    // @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 milliseconds
    public void processExpiredSubscriptions() {
        try {
            log.info("Starting automatic subscription expiry check...");
            subscriptionService.processExpiredSubscriptions();
            log.info("Completed automatic subscription expiry check");
        } catch (Exception e) {
            log.error("Error processing expired subscriptions: {}", e.getMessage(), e);
        }
    }

    /**
     * Alternative: Run every 30 minutes during business hours (9 AM to 9 PM)
     * Uncomment this method and comment the above one if you prefer this schedule
     */
    // @Scheduled(cron = "0 */30 9-21 * * *") // Every 30 minutes between 9 AM and 9 PM
    // public void processExpiredSubscriptionsDuringBusinessHours() {
    //     try {
    //         log.info("Starting business hours subscription expiry check...");
    //         subscriptionService.processExpiredSubscriptions();
    //         log.info("Completed business hours subscription expiry check");
    //     } catch (Exception e) {
    //         log.error("Error processing expired subscriptions: {}", e.getMessage(), e);
    //     }
    // }
}