// com.examly.springapp.service.UserService.java
package com.examly.springapp.service;

import com.examly.springapp.exception.UserNotFoundException;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByVerificationToken(String token) {
        return userRepository.findByVerificationToken(token);
    }

    public User findByMobileNumber(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber).orElse(null);
    }

    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        if (user.getVerificationToken() == null || user.getVerificationToken().isEmpty()) {
            user.setVerificationToken(UUID.randomUUID().toString());
        }

        user.setVerified(false);
        return userRepository.save(user);
    }

    public void markUserAsVerified(User user) {
        user.setVerified(true);
        user.setVerificationToken(null); // optional: clear token after verification
        userRepository.save(user);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
    }

    public User updateUser(Long id, User updatedUser) {
        User existing = getUserById(id);

        // Only update allowed fields
        existing.setUsername(updatedUser.getUsername() != null ? updatedUser.getUsername() : existing.getUsername());
        existing.setEmail(updatedUser.getEmail() != null ? updatedUser.getEmail() : existing.getEmail());
        existing.setMobileNumber(updatedUser.getMobileNumber() != null ? updatedUser.getMobileNumber() : existing.getMobileNumber());
        existing.setServiceArea(updatedUser.getServiceArea() != null ? updatedUser.getServiceArea() : existing.getServiceArea());

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        // Admin can change role
        if (updatedUser.getRole() != null) {
            existing.setRole(updatedUser.getRole());
        }

        return userRepository.save(existing);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> getUsersByServiceArea(String serviceArea) {
        return userRepository.findByServiceArea(serviceArea);
    }

    public List<User> searchUsers(String query) {
        // Search by email or username containing the query
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(user -> 
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(query.toLowerCase())) ||
                    (user.getUsername() != null && user.getUsername().toLowerCase().contains(query.toLowerCase()))
                )
                .collect(java.util.stream.Collectors.toList());
    }
}
