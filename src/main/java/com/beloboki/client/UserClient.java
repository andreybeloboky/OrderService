package com.beloboki.client;

import com.beloboki.dto.UserResponse;
import com.beloboki.exception.UserNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {

    private final WebClient userWebClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "fallback")
    public UserResponse getUserByEmail(String email) {
        return userWebClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/api/users/email/{email}").build(email))
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "fallback")
    public UserResponse getUserById(Long id) {
        return userWebClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/api/users/{id}").build(id))
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();
    }

    public UserResponse fallback(Exception e) {
        log.error("Circuit breaker fallback triggered: {}", e.getMessage());
        throw new UserNotFoundException("User not found or service unavailable");
    }
}
