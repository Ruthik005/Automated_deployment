package com.example.login;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Deployment Successful 🚀 (Spring Boot is running)";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
