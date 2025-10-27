package com.examly.springapp.repository;

import com.examly.springapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationToken(String token);
    Optional<User> findByMobileNumber(String mobileNumber);
    List<User> findByRole(User.Role role);
    List<User> findByServiceArea(String serviceArea);
    List<User> findByServiceAreaAndRole(String serviceArea, User.Role role);
}