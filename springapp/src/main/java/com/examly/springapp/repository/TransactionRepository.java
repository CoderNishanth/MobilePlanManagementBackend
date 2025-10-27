package com.examly.springapp.repository;

import com.examly.springapp.model.Transaction;
import com.examly.springapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCustomer(User customer);
    List<Transaction> findByStatus(Transaction.Status status);
    List<Transaction> findByTransactionType(Transaction.Type type);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.status = 'SUCCESS' AND t.transactionType = 'RECHARGE'")
    Double getTotalSuccessfulRecharges();
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.status = 'SUCCESS' AND t.transactionType = 'RECHARGE' AND t.transactionDate >= :since")
    Double getTotalRechargesSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'SUCCESS' AND t.transactionDate >= :since")
    long countSuccessfulTransactionsSince(@Param("since") LocalDateTime since);
}
