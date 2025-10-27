package com.examly.springapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String toEmail, String verificationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Verify Your Email - Telecom Portal");
        message.setText("""
                Welcome to the Telecom Management Portal!

                Please verify your email address by clicking the link below:
                """ + verificationLink + """

                This link will expire in 24 hours.
                """);

        mailSender.send(message);
    }
}
