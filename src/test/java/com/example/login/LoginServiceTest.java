package com.example.login;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginServiceTest {

    private final LoginService loginService = new LoginService();

    @GetMapping("/validate")
    public String validateLogin() {
        boolean result = loginService.validateLogin(
            "john",
            "john@example.com",
            "9876543210",
            "Password@123",
            "1995-08-15"
        );
        return "Login validation result: " + result;
    }
}
