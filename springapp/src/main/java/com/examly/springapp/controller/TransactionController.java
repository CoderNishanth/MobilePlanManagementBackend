package com.examly.springapp.controller;

import com.examly.springapp.model.Transaction;
import com.examly.springapp.model.User;
import com.examly.springapp.service.TransactionService;
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
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    
    private final TransactionService transactionService;
    private final UserService userService;



    /**
     * Customer: Get my transaction history
     */
    @GetMapping("/my-transactions")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('RETAILER')")
    public ResponseEntity<?> getMyTransactions(Authentication authentication) {
        try {
            String email = authentication.getName();
            User customer = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            List<Transaction> transactions = transactionService.getTransactionsByCustomer(customer);
            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch transactions: " + e.getMessage()));
        }
    }



    /**
     * Admin/Telecom Manager: Get all transactions
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAN_MANAGER')")
    public ResponseEntity<?> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        try {
            List<Transaction> transactions;

            if (status != null && type != null) {
                Transaction.Status transactionStatus = Transaction.Status.valueOf(status.toUpperCase());
                Transaction.Type transactionType = Transaction.Type.valueOf(type.toUpperCase());
                transactions = transactionService.getTransactionsByStatusAndType(transactionStatus, transactionType);
            } else if (status != null) {
                Transaction.Status transactionStatus = Transaction.Status.valueOf(status.toUpperCase());
                transactions = transactionService.getTransactionsByStatus(transactionStatus);
            } else if (type != null) {
                Transaction.Type transactionType = Transaction.Type.valueOf(type.toUpperCase());
                transactions = transactionService.getTransactionsByType(transactionType);
            } else {
                transactions = transactionService.getAllTransactions();
            }

            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch transactions: " + e.getMessage()));
        }
    }





    /**
     * Retailer: Get transactions facilitated by the retailer
     */
    @GetMapping("/retailer/my-transactions")
    @PreAuthorize("hasRole('RETAILER')")
    public ResponseEntity<?> getRetailerTransactions(Authentication authentication) {
        try {
            String email = authentication.getName();
            User retailer = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));
            
            // Get all transactions created by this retailer
            List<Transaction> transactions = transactionService.getTransactionsByRetailer(retailer);
                    
            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch retailer transactions: " + e.getMessage()));
        }
    }
    


    /**
     * Admin: Get transaction statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAN_MANAGER')")
    public ResponseEntity<?> getTransactionStatistics(@RequestParam(required = false) String period) {
        try {
            Map<String, Object> stats = transactionService.getTransactionStatistics(period);
            return ResponseEntity.ok(stats);
        
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch transaction statistics: " + e.getMessage()));
        }
    }




}
