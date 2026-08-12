package com.beloboki.client;

import com.beloboki.dto.UserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {

    private final WebClient userWebClient;
    private static final String UNKNOWN = "UNKNOWN";

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    public UserResponse getUserByEmail(String email) {
        return userWebClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/api/users/email/{email}").build(email))
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserResponse getUserById(Long id) {
        return userWebClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/api/users/{id}").build(id))
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUsersByIdsFallback")
    public List<UserResponse> getUsersByIds(List<Long> ids) {
        return userWebClient
                .post()
                .uri("/api/users/batch")
                .bodyValue(ids)
                .retrieve()
                .bodyToFlux(UserResponse.class)
                .collectList()
                .block();
    }

    public UserResponse getUserByEmailFallback(String email, Throwable t) {
        log.error("Fallback for getUserByEmail: {}", t.getMessage());
        return new UserResponse(
                null,
                UNKNOWN,
                UNKNOWN,
                null,
                email,
                false,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    public UserResponse getUserByIdFallback(Long id, Throwable t) {
        log.error("Fallback for getUserById: {}", t.getMessage());
        return new UserResponse(
                id,
                UNKNOWN,
                UNKNOWN,
                null,
                UNKNOWN,
                false,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    public List<UserResponse> getUsersByIdsFallback(List<Long> ids, Throwable t) {
        log.error("Fallback for getUsersByIds: {}", t.getMessage());
        return ids.stream()
                .map(
                        id ->
                                new UserResponse(
                                        id,
                                        UNKNOWN,
                                        UNKNOWN,
                                        null,
                                        UNKNOWN,
                                        false,
                                        LocalDateTime.now(),
                                        LocalDateTime.now()))
                .toList();
    }
}
