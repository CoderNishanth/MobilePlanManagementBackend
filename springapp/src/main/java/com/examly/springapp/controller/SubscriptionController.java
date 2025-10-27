package com.examly.springapp.controller;

import com.examly.springapp.model.Plan;
import com.examly.springapp.model.Subscription;
import com.examly.springapp.model.Transaction;
import com.examly.springapp.model.User;
import com.examly.springapp.service.PlanService;
import com.examly.springapp.service.SubscriptionService;
import com.examly.springapp.service.TransactionService;
import com.examly.springapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final PlanService planService;
    private final TransactionService transactionService;

    /**
     * Customer: Subscribe to a new plan
     */
    @PostMapping("/subscribe")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> subscribeToPlan(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User customer = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            Long planId = Long.valueOf(request.get("planId").toString());
            String paymentMethod = request.get("paymentMethod").toString();
            Double amount = Double.valueOf(request.get("amount").toString());
            
            Plan plan = planService.getPlanById(planId);
            if (plan == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Plan not found"));
            }

            // Validate amount matches plan price
            if (Math.abs(amount - plan.getPrice()) > 0.01) {
                return ResponseEntity.badRequest().body(Map.of("error", "Amount does not match plan price"));
            }

            // Check if customer already has an active subscription for this plan
            List<Subscription> existingSubscriptions = subscriptionService.getActiveSubscriptionsByCustomer(customer);
            boolean hasActivePlan = existingSubscriptions.stream()
                    .anyMatch(sub -> sub.getPlan().getId().equals(planId) && 
                             sub.getStatus() == Subscription.Status.ACTIVE);

            if (hasActivePlan) {
                return ResponseEntity.badRequest().body(Map.of("error", "You already have an active subscription for this plan"));
            }

            // Step 1: Create transaction for payment
            Transaction transaction = Transaction.builder()
                    .customer(customer)
                    .amount(amount)
                    .transactionType(Transaction.Type.RECHARGE)
                    .status(Transaction.Status.SUCCESS) // In real app, this would be pending initially
                    .paymentMethod(paymentMethod)
                    .transactionDate(LocalDateTime.now())
                    .build();

            Transaction savedTransaction = transactionService.createTransaction(transaction);

            // Step 2: Create subscription only after successful payment
            Subscription subscription = Subscription.builder()
                    .customer(customer)
                    .plan(plan)
                    .activationDate(LocalDateTime.now())
                    .expiryDate(LocalDateTime.now().plusDays(plan.getValidity()))
                    .status(Subscription.Status.ACTIVE)
                    .paymentMethod(paymentMethod)
                    .build();

            Subscription savedSubscription = subscriptionService.createSubscription(subscription);

            return ResponseEntity.ok(Map.of(
                    "message", "Subscription created successfully",
                    "subscriptionId", savedSubscription.getId(),
                    "transactionId", savedTransaction.getId(),
                    "planName", plan.getPlanName(),
                    "activationDate", savedSubscription.getActivationDate(),
                    "expiryDate", savedSubscription.getExpiryDate(),
                    "amount", amount
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create subscription: " + e.getMessage()));
        }
    }

    /**
     * Customer: Get my subscriptions
     */
    @GetMapping("/my-subscriptions")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMySubscriptions(Authentication authentication) {
        try {
            String email = authentication.getName();
            User customer = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            
            List<Subscription> subscriptions = subscriptionService.getSubscriptionsByCustomer(customer);
            return ResponseEntity.ok(subscriptions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch subscriptions: " + e.getMessage()));
        }
    }

    /**
     * Customer: Get active subscriptions only
     */
    @GetMapping("/my-active-subscriptions")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyActiveSubscriptions(Authentication authentication) {
        try {
            String email = authentication.getName();
            User customer = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            List<Subscription> activeSubscriptions = subscriptionService.getActiveSubscriptionsByCustomer(customer);
            return ResponseEntity.ok(activeSubscriptions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch active subscriptions: " + e.getMessage()));
        }
    }

    /**
     * Customer: Cancel a subscription
     */
    @PutMapping("/{subscriptionId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> cancelSubscription(@PathVariable Long subscriptionId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User customer = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            Subscription subscription = subscriptionService.getSubscriptionById(subscriptionId);
            if (subscription == null) {
                return ResponseEntity.notFound().build();
            }

            // Verify subscription belongs to the customer
            if (!subscription.getCustomer().getId().equals(customer.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only cancel your own subscriptions"));
            }

            if (subscription.getStatus() != Subscription.Status.ACTIVE) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Only active subscriptions can be cancelled"));
            }

            subscription.setStatus(Subscription.Status.CANCELLED);
            subscriptionService.updateSubscription(subscription);

            return ResponseEntity.ok(Map.of("message", "Subscription cancelled successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cancel subscription: " + e.getMessage()));
        }
    }

    /**
     * Admin/Telecom Manager: Get all subscriptions
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAN_MANAGER')")
    public ResponseEntity<?> getAllSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        try {
            List<Subscription> subscriptions;
            
            if (status != null && !status.isEmpty()) {
                Subscription.Status subscriptionStatus = Subscription.Status.valueOf(status.toUpperCase());
                subscriptions = subscriptionService.getSubscriptionsByStatus(subscriptionStatus);
            } else {
                subscriptions = subscriptionService.getAllSubscriptions();
            }

            // Convert to safe DTOs to avoid circular references
            List<Map<String, Object>> safeSubscriptions = subscriptions.stream()
                    .map(this::createSafeSubscriptionResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(safeSubscriptions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch subscriptions: " + e.getMessage()));
        }
    }

    /**
     * Admin/Retailer: Get subscriptions by customer ID
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAN_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('RETAILER')")
    public ResponseEntity<?> getSubscriptionsByCustomerId(@PathVariable Long customerId, Authentication authentication) {
        try {
            User customer = userService.getUserById(customerId);
            if (customer.getRole() != User.Role.CUSTOMER) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid customer ID"));
            }

            List<Subscription> subscriptions = subscriptionService.getSubscriptionsByCustomer(customer);
            return ResponseEntity.ok(subscriptions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch customer subscriptions: " + e.getMessage()));
        }
    }



    /**
     * Admin: Get subscription statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAN_MANAGER')")
    public ResponseEntity<?> getSubscriptionStatistics() {
        try {
            Map<String, Object> stats = subscriptionService.getSubscriptionStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch subscription statistics: " + e.getMessage()));
        }
    }

    /**
     * Retailer: Create subscription for customer
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('RETAILER') or hasRole('ADMIN')")
    public ResponseEntity<?> createSubscriptionForCustomer(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User retailer = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            Long customerId = Long.valueOf(request.get("customerId").toString());
            Long planId = Long.valueOf(request.get("planId").toString());
            
            User customer = userService.getUserById(customerId);
            if (customer.getRole() != User.Role.CUSTOMER) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid customer ID"));
            }

            Plan plan = planService.getPlanById(planId);
            if (plan == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Plan not found"));
            }

            // Check if customer already has an active subscription for this plan
            List<Subscription> existingSubscriptions = subscriptionService.getActiveSubscriptionsByCustomer(customer);
            boolean hasActivePlan = existingSubscriptions.stream()
                    .anyMatch(sub -> sub.getPlan().getId().equals(planId) && 
                             sub.getStatus() == Subscription.Status.ACTIVE);

            if (hasActivePlan) {
                return ResponseEntity.badRequest().body(Map.of("error", "Customer already has an active subscription for this plan"));
            }

            // Create transaction record for retailer-initiated subscription
            Transaction transaction = Transaction.builder()
                    .customer(customer)
                    .retailer(retailer)
                    .plan(plan)
                    .amount(plan.getPrice())
                    .transactionType(Transaction.Type.SUBSCRIPTION)
                    .status(Transaction.Status.SUCCESS)
                    .paymentMethod("RETAILER_TRANSACTION")
                    .transactionDate(LocalDateTime.now())
                    .build();

            Transaction savedTransaction = transactionService.createTransaction(transaction);

            // Create subscription
            Subscription subscription = Subscription.builder()
                    .customer(customer)
                    .plan(plan)
                    .activationDate(LocalDateTime.now())
                    .expiryDate(LocalDateTime.now().plusDays(plan.getValidity()))
                    .status(Subscription.Status.ACTIVE)
                    .paymentMethod("RETAILER_TRANSACTION")
                    .build();

            Subscription savedSubscription = subscriptionService.createSubscription(subscription);

            return ResponseEntity.ok(Map.of(
                    "message", "Subscription created successfully for customer",
                    "subscriptionId", savedSubscription.getId(),
                    "transactionId", savedTransaction.getId(),
                    "customerName", customer.getUsername(),
                    "planName", plan.getPlanName(),
                    "activationDate", savedSubscription.getActivationDate(),
                    "expiryDate", savedSubscription.getExpiryDate(),
                    "amount", plan.getPrice(),
                    "createdBy", retailer.getUsername()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create subscription: " + e.getMessage()));
        }
    }



    /**
     * Admin/Plan Manager: Manually process expired subscriptions
     */
    @PostMapping("/process-expired")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAN_MANAGER')")
    public ResponseEntity<?> processExpiredSubscriptions() {
        try {
            subscriptionService.processExpiredSubscriptions();
            return ResponseEntity.ok(Map.of(
                    "message", "Expired subscriptions processed successfully",
                    "processedAt", LocalDateTime.now()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process expired subscriptions: " + e.getMessage()));
        }
    }

    /**
     * Helper method to create safe subscription response without circular references
     */
    private Map<String, Object> createSafeSubscriptionResponse(Subscription subscription) {
        return Map.of(
                "id", subscription.getId(),
                "customer", Map.of(
                        "id", subscription.getCustomer().getId(),
                        "username", subscription.getCustomer().getUsername(),
                        "email", subscription.getCustomer().getEmail(),
                        "mobileNumber", subscription.getCustomer().getMobileNumber()
                ),
                "plan", Map.of(
                        "id", subscription.getPlan().getId(),
                        "planName", subscription.getPlan().getPlanName(),
                        "type", subscription.getPlan().getType(),
                        "price", subscription.getPlan().getPrice(),
                        "dataAllowance", subscription.getPlan().getDataAllowance()
                ),
                "activationDate", subscription.getActivationDate(),
                "expiryDate", subscription.getExpiryDate(),
                "status", subscription.getStatus(),
                "paymentMethod", subscription.getPaymentMethod() != null ? subscription.getPaymentMethod() : ""
                // Temporarily removing customer and plan info to avoid circular loading
                // These would need to be fetched separately if needed
        );
    }
}
