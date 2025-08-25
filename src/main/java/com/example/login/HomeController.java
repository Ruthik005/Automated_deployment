package com.example.login;

import org.springframework.stereotype.Controller;  // Changed from @RestController
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller  // Changed this!
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/index.html"; 
    }

    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }
}