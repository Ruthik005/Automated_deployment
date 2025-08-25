package com.example.login;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/login.html", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")        // Point to your static login.html
                .loginProcessingUrl("/login")    // Process form submissions here
                .defaultSuccessUrl("/", true)    // After login, go to home
                .permitAll()
            )
            .logout(logout -> logout.permitAll());

        return http.build();
    }
}