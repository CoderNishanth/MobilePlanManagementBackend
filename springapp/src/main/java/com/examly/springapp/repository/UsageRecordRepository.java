package com.examly.springapp.repository;

import com.examly.springapp.model.UsageRecord;
import com.examly.springapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
    List<UsageRecord> findByCustomer(User customer);
}
