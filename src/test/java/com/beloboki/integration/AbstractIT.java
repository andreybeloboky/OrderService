package com.beloboki.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.redis.testcontainers.RedisContainer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public abstract class AbstractIT {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine");

    private static final RedisContainer REDIS = new RedisContainer("redis:7-alpine");

    protected static WireMockServer wireMockServer;

    @Autowired protected CacheManager cacheManager;

    private static final String TEST_SECRET_STRING = "my_ultra_secure_test_secret_key!!";

    protected String generateTestToken(String subject, Long userId, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("userId", userId)
                .claim("role", role)
                .signWith(
                        Keys.hmacShaKeyFor(TEST_SECRET_STRING.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @BeforeAll
    static void initWireMock() {
        if (wireMockServer == null) {
            wireMockServer = new WireMockServer(0);
            wireMockServer.start();
        }
    }

    @AfterAll
    static void destroyWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        registry.add("jwt.secret", () -> TEST_SECRET_STRING);

        registry.add("user-service.url", () -> wireMockServer.baseUrl());
    }
}
