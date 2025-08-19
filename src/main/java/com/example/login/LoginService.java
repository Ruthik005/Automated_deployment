package com.example.login;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;

@Service
public class LoginService {

    // Main login validation
    public boolean validateLogin(String username, String email, String mobile, String password, String dob) {
        return isValidUsername(username) &&
               isValidEmail(email) &&
               isValidMobile(mobile) &&
               isValidPassword(password) &&
               isValidDOB(dob);
    }

    // Username must be at least 3 characters
    public boolean isValidUsername(String username) {
        return username != null && username.length() >= 3;
    }

    // Simple email regex
    public boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    // Mobile must be exactly 10 digits
    public boolean isValidMobile(String mobile) {
        return mobile != null && mobile.matches("^\\d{10}$");
    }

    // Password must contain at least 1 digit, 1 lowercase, 1 uppercase, 1 special char, and be at least 8 chars
    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        return password.matches(".*\\d.*") &&       // at least one digit
               password.matches(".*[a-z].*") &&    // at least one lowercase
               password.matches(".*[A-Z].*") &&    // at least one uppercase
               password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"); // at least one special char
    }

    // Date of Birth must be in yyyy-MM-dd format
    public boolean isValidDOB(String dob) {
        if (dob == null) return false;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate.parse(dob, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
