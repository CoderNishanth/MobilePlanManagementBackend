package com.examly.springapp.controller;

import com.examly.springapp.model.User;
import com.examly.springapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * Admin/Telecom Manager: Get all users with filtering
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TELECOM_MANAGER')")
    public ResponseEntity<?> getAllUsers(
        @RequestParam(name = "page", required = false, defaultValue = "0") int page,
        @RequestParam(name = "size", required = false, defaultValue = "10") int size
    ) {
    try {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<User> usersPage = userService.getAllUsers(pageable);

        List<Map<String, Object>> safeUsers = usersPage.getContent().stream()
            .map(this::createSafeUserResponse)
            .collect(Collectors.toList());

        Map<String, Object> paged = Map.of(
            "content", safeUsers,
            "page", usersPage.getNumber(),
            "size", usersPage.getSize(),
            "totalElements", usersPage.getTotalElements(),
            "totalPages", usersPage.getTotalPages()
        );

        return ResponseEntity.ok(paged);

    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
    }
    }

    /**
     * Admin/Customer Service: Get user by ID
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TELECOM_MANAGER') or hasRole('CUSTOMER_SERVICE')")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            Map<String, Object> safeUser = createSafeUserResponse(user);
            return ResponseEntity.ok(safeUser);
        
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found: " + e.getMessage()));
        }
    }
    
    /**
     * Any authenticated user: Get own profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELECOM_MANAGER', 'PLAN_MANAGER', 'RETAILER', 'CUSTOMER', 'CUSTOMER_SERVICE')")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> profile = createSafeUserResponse(user);
            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch profile: " + e.getMessage()));
        }
    }
    
    /**
     * Any authenticated user: Update own profile
     */
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELECOM_MANAGER', 'PLAN_MANAGER', 'RETAILER', 'CUSTOMER', 'CUSTOMER_SERVICE')")
    public ResponseEntity<?> updateMyProfile(@RequestBody Map<String, Object> updates, Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create update object with only allowed fields for self-update
            User updateUser = new User();
            
            if (updates.containsKey("username")) {
                updateUser.setUsername(updates.get("username").toString());
            }
            if (updates.containsKey("mobileNumber")) {
                updateUser.setMobileNumber(updates.get("mobileNumber").toString());
            }
            if (updates.containsKey("serviceArea")) {
                updateUser.setServiceArea(updates.get("serviceArea").toString());
            }
            if (updates.containsKey("password")) {
                updateUser.setPassword(updates.get("password").toString());
            }

            User updatedUser = userService.updateUser(currentUser.getId(), updateUser);
            Map<String, Object> safeUser = createSafeUserResponse(updatedUser);

            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully",
                    "user", safeUser
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update profile: " + e.getMessage()));
        }
    }

    /**
     * Admin: Update any user
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody User updatedUser) {
        try {
            User user = userService.updateUser(userId, updatedUser);
            Map<String, Object> safeUser = createSafeUserResponse(user);

            return ResponseEntity.ok(Map.of(
                    "message", "User updated successfully",
                    "user", safeUser
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update user: " + e.getMessage()));
        }
    }

    /**
     * Admin: Delete user (with self-deletion prevention)
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, Authentication authentication) {
        try {
            String currentUserEmail = authentication.getName();
            User currentUser = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            // Prevent self-deletion
            if (currentUser.getId().equals(userId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "You cannot delete your own account"));
            }

            userService.deleteUser(userId);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete user: " + e.getMessage()));
        }
    }



    /**
     * Admin: Create new user (staff/admin users)
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody User newUser) {
        try {
            // Validate required fields
            if (newUser.getEmail() == null || newUser.getPassword() == null || newUser.getRole() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email, password, and role are required"));
            }

            // Check if user already exists
            if (userService.findByEmail(newUser.getEmail()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User with this email already exists"));
            }

            User createdUser = userService.registerUser(newUser);
            Map<String, Object> safeUser = createSafeUserResponse(createdUser);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "User created successfully",
                            "user", safeUser
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create user: " + e.getMessage()));
        }
    }

    /**
     * Admin: Get user statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TELECOM_MANAGER')")
    public ResponseEntity<?> getUserStatistics() {
        try {
            List<User> allUsers = userService.getAllUsers();
            
            Map<User.Role, Long> roleCount = allUsers.stream()
                    .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
            
            Map<String, Long> serviceAreaCount = allUsers.stream()
                    .filter(user -> user.getServiceArea() != null)
                    .collect(Collectors.groupingBy(User::getServiceArea, Collectors.counting()));

            long verifiedUsers = allUsers.stream()
                    .filter(User::isVerified)
                    .count();
            long newUserstoday = allUsers.stream()
                    .filter(user -> user.getCreatedAt() != null &&
                            user.getCreatedAt().toLocalDate().isEqual(java.time.LocalDate.now()))
                    .count();
            long newThismonth = allUsers.stream()
                    .filter(user -> user.getCreatedAt() != null) 
                    .filter(user -> {
                        java.time.LocalDate createdDate = user.getCreatedAt().toLocalDate();
                        java.time.LocalDate now = java.time.LocalDate.now();
                        return createdDate.getYear() == now.getYear() &&
                               createdDate.getMonth() == now.getMonth();
                    })
                    .count();

        

            Map<String, Object> statistics = Map.of(
                    "totalUsers", allUsers.size(),
                    "verifiedUsers", verifiedUsers,
                    "unverifiedUsers", allUsers.size() - verifiedUsers,
                    "usersByRole", roleCount,
                    "usersByServiceArea", serviceAreaCount,
                    "newUsersToday", newUserstoday,
                    "newUsersThisMonth", newThismonth,
                    "generatedAt", java.time.LocalDateTime.now()
            );

            
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch user statistics: " + e.getMessage()));
        }
    }



    /**
     * Retailer: Search customer by mobile number
     */
    @GetMapping("/search-by-mobile")
    @PreAuthorize("hasRole('RETAILER') or hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    public ResponseEntity<?> searchCustomerByMobile(@RequestParam String mobileNumber, Authentication authentication) {
        try {
            // Find customer by mobile number
            User customer = userService.findByMobileNumber(mobileNumber);
            if (customer == null) {
                return ResponseEntity.notFound().build();
            }

            // Check if customer is actually a customer (not admin, retailer, etc.)
            if (customer.getRole() != User.Role.CUSTOMER) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Mobile number does not belong to a customer"));
            }

            Map<String, Object> safeCustomer = createSafeUserResponse(customer);
            return ResponseEntity.ok(safeCustomer);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to search customer: " + e.getMessage()));
        }
    }



    /**
     * Helper method to create safe user response (without sensitive data)
     * Service area is included for all users who have it
     */
    private Map<String, Object> createSafeUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername() != null ? user.getUsername() : "");
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("mobileNumber", user.getMobileNumber() != null ? user.getMobileNumber() : "");
        response.put("isVerified", user.isVerified());
        response.put("createdAt", user.getCreatedAt());
        response.put("lastLogin", user.getLastLogin());
        
        // Include serviceArea for all users who have it
        if (user.getServiceArea() != null) {
            response.put("serviceArea", user.getServiceArea());
        }
        
        return response;
    }
}
