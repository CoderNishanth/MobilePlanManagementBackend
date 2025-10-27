package com.examly.springapp.controller;

import com.examly.springapp.model.UsageRecord;
import com.examly.springapp.model.User;
import com.examly.springapp.service.UsageService;
import com.examly.springapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UsageRecordController {

    private final UsageService usageService;
    private final UserService userService;

    /**
     * Customer: Get my usage records
     */
    @GetMapping("/my-usage")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyUsage(Authentication authentication) {
        try {
            String email = authentication.getName();
            User customer = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            List<UsageRecord> usageRecords = usageService.getUsageByCustomer(customer);
            return ResponseEntity.ok(usageRecords);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch usage records: " + e.getMessage()));
        }
    }

    /**
     * Plan Manager/Admin: Get usage statistics
     */
    @GetMapping("/statistics/{period}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAN_MANAGER')")
    public ResponseEntity<?> getUsageStatistics(@PathVariable String period) {
        try {
            Map<String, Object> stats = usageService.getUsageStatistics(period);
            return ResponseEntity.ok(stats);
        
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch usage statistics: " + e.getMessage()));
        }
    }
}
