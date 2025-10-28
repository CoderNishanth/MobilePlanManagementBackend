package com.examly.springapp.controller;

import com.examly.springapp.model.User;
import com.examly.springapp.service.EmailService;
import com.examly.springapp.service.JwtService;
import com.examly.springapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;
    private final EmailService emailService;
    
    // ✅ Register new user and send verification email
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user, HttpServletRequest request) {
        // Validate required fields
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }
        
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        if (user.getRole() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role is required"));
        }

        if (userService.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        try {
            User saved = userService.registerUser(user);

            String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String verifyLink = appUrl + "/api/auth/verify?token=" + saved.getVerificationToken();
            System.out.println("Verification link: " + verifyLink); // For debugging
            // emailService.sendVerificationEmail(saved.getEmail(), verifyLink);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Registration successful! Please verify your email to activate your account."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
    // ✅ Verify email link
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token, HttpServletResponse response) throws IOException {
        return userService.findByVerificationToken(token)
                .map(user -> {
                    userService.markUserAsVerified(user);
                    try {
                        // Redirect to frontend success page after verification
                        response.sendRedirect("https://mobileplanmanagement.web.app/email-verified");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
                })
                .orElseGet(() -> {
                    try {
                        // Redirect to frontend error/expired page
                        response.sendRedirect("http://localhost:5173/email-verification-failed");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired verification token"));
                });
    }
// ✅ Login user (only if verified)
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        
        User user = userService.findByEmail(email).orElse(null);
        if (user == null || !userService.verifyPassword(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        
        if (!user.isVerified()) {
            return ResponseEntity.status(403).body(Map.of("error", "Email not verified. Please check your inbox."));
        }
        
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "email", user.getEmail(),
                "role", user.getRole() != null ? user.getRole() : "USER"
        ));
    }

    // ✅ Get logged-in user info
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestAttribute(value = "userEmail", required = false) String email) {
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized or missing token"));
        }

        return userService.findByEmail(email)
                .map(user -> {
                    String role = user.getRole() != null ? user.getRole().toString() : "USER";
                    Map<String, Object> response = new java.util.HashMap<>();
                    response.put("id", user.getId());
                    response.put("username", user.getUsername());
                    response.put("email", user.getEmail());
                    response.put("role", role);
                    response.put("mobileNumber", user.getMobileNumber());
                    // Only include serviceArea if not null
                    if (user.getServiceArea() != null) {
                        response.put("serviceArea", user.getServiceArea());
                    }
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "User not found")));
    }
}
