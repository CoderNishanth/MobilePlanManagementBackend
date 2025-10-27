package com.examly.springapp.service;

import com.examly.springapp.model.Transaction;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.TransactionRepository;
import com.examly.springapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public Transaction createTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId).orElse(null);
    }

    public List<Transaction> getTransactionsByCustomer(User customer) {
        return transactionRepository.findByCustomer(customer);
    }

    public List<Transaction> getTransactionsByRetailer(User retailer) {
        return transactionRepository.findByCustomer(retailer);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<Transaction> getTransactionsByStatus(Transaction.Status status) {
        return transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Transaction> getTransactionsByType(Transaction.Type type) {
        return transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getTransactionType() == type)
                .collect(Collectors.toList());
    }

    public List<Transaction> getTransactionsByStatusAndType(Transaction.Status status, Transaction.Type type) {
        return transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getStatus() == status && transaction.getTransactionType() == type)
                .collect(Collectors.toList());
    }

    public List<Transaction> getTransactionsByServiceArea(String serviceArea) {
        List<User> customersInArea = userRepository.findByServiceAreaAndRole(serviceArea, User.Role.CUSTOMER);
        return customersInArea.stream()
                .flatMap(customer -> transactionRepository.findByCustomer(customer).stream())
                .collect(Collectors.toList());
    }

    public boolean hasRefund(Long originalTransactionId) {
        // Check if there's already a refund transaction for this original transaction
        // In a real implementation, you might want to add a reference field to link transactions
        Transaction originalTransaction = getTransactionById(originalTransactionId);
        if (originalTransaction == null) return false;
        
        return transactionRepository.findByCustomer(originalTransaction.getCustomer()).stream()
                .anyMatch(t -> t.getTransactionType() == Transaction.Type.REFUND && 
                              Double.compare(t.getAmount(), originalTransaction.getAmount()) == 0 &&
                              t.getTransactionDate().isAfter(originalTransaction.getTransactionDate()));
    }

    public Map<String, Object> getTransactionStatistics(String period) {
        List<Transaction> allTransactions = transactionRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        // Filter by period
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
                startDate = LocalDateTime.of(2000, 1, 1, 0, 0); // All time
        }

        List<Transaction> filteredTransactions = allTransactions.stream()
                .filter(t -> t.getTransactionDate().isAfter(startDate))
                .collect(Collectors.toList());

        double totalRevenue = filteredTransactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.Type.RECHARGE && t.getStatus() == Transaction.Status.SUCCESS)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalRefunds = filteredTransactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.Type.REFUND && t.getStatus() == Transaction.Status.SUCCESS)
                .mapToDouble(Transaction::getAmount)
                .sum();

        long successfulTransactions = filteredTransactions.stream()
                .filter(t -> t.getStatus() == Transaction.Status.SUCCESS)
                .count();

        long failedTransactions = filteredTransactions.stream()
                .filter(t -> t.getStatus() == Transaction.Status.FAILED)
                .count();

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalTransactions", filteredTransactions.size());
        statistics.put("successfulTransactions", successfulTransactions);
        statistics.put("failedTransactions", failedTransactions);
        statistics.put("totalRevenue", totalRevenue);
        statistics.put("totalRefunds", totalRefunds);
        statistics.put("netRevenue", totalRevenue - totalRefunds);
        statistics.put("successRate", filteredTransactions.isEmpty() ? 0 : 
                      (double) successfulTransactions / filteredTransactions.size() * 100);
        statistics.put("period", period != null ? period : "all");
        statistics.put("generatedAt", LocalDateTime.now());

        return statistics;
    }

    public void markTransactionAsFailed(Long transactionId, String reason) {
        Transaction transaction = getTransactionById(transactionId);
        if (transaction != null) {
            transaction.setStatus(Transaction.Status.FAILED);
            // In a real implementation, you might want to add a reason field to the Transaction model
            transactionRepository.save(transaction);
        }
    }
    
    public Transaction retryFailedTransaction(Long transactionId) {
        Transaction originalTransaction = getTransactionById(transactionId);
        if (originalTransaction == null || originalTransaction.getStatus() != Transaction.Status.FAILED) {
            throw new RuntimeException("Transaction not found or not in failed state");
        }
        
        // Create a new transaction as a retry
        Transaction retryTransaction = Transaction.builder()
                .customer(originalTransaction.getCustomer())
                .amount(originalTransaction.getAmount())
                .transactionType(originalTransaction.getTransactionType())
                .status(Transaction.Status.SUCCESS) // Assuming retry is successful for demo
                .paymentMethod(originalTransaction.getPaymentMethod())
                .transactionDate(LocalDateTime.now())
                .build();

        return transactionRepository.save(retryTransaction);
    }
    
    public Map<String, Object> getMonthlySpendingSummary(User customer, int year, int month) {
        LocalDateTime startOfMonth;
        LocalDateTime endOfMonth;

        if (year == 0 || month == 0) {
            // Use current month if not specified
            YearMonth currentMonth = YearMonth.now();
            startOfMonth = currentMonth.atDay(1).atStartOfDay();
            endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        } else {
            YearMonth targetMonth = YearMonth.of(year, month);
            startOfMonth = targetMonth.atDay(1).atStartOfDay();
            endOfMonth = targetMonth.atEndOfMonth().atTime(23, 59, 59);
        }
        
        List<Transaction> monthlyTransactions = transactionRepository.findByCustomer(customer).stream()
                .filter(t -> t.getTransactionDate().isAfter(startOfMonth) && 
                            t.getTransactionDate().isBefore(endOfMonth))
                .collect(Collectors.toList());

        double totalSpent = monthlyTransactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.Type.RECHARGE && t.getStatus() == Transaction.Status.SUCCESS)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalRefunded = monthlyTransactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.Type.REFUND && t.getStatus() == Transaction.Status.SUCCESS)
                .mapToDouble(Transaction::getAmount)
                .sum();

        long transactionCount = monthlyTransactions.stream()
                .filter(t -> t.getStatus() == Transaction.Status.SUCCESS)
                .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("month", month == 0 ? LocalDateTime.now().getMonthValue() : month);
        summary.put("year", year == 0 ? LocalDateTime.now().getYear() : year);
        summary.put("totalSpent", totalSpent);
        summary.put("totalRefunded", totalRefunded);
        summary.put("netSpending", totalSpent - totalRefunded);
        summary.put("transactionCount", transactionCount);
        summary.put("averageTransactionAmount", transactionCount == 0 ? 0 : totalSpent / transactionCount);
        summary.put("transactions", monthlyTransactions);

        return summary;
    }
}
