package com.beloboki.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.beloboki.dao.ItemDAO;
import com.beloboki.dao.OrderDAO;
import com.beloboki.dto.OrderItemRequest;
import com.beloboki.dto.OrderRequest;
import com.beloboki.dto.OrderResponse;
import com.beloboki.model.Item;
import com.beloboki.model.Order;
import com.beloboki.model.OrderItem;
import com.beloboki.model.Status;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class OrderControllerIT extends AbstractIT {

    @Autowired private WebTestClient webTestClient;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private OrderDAO orderDAO;

    @Autowired private ItemDAO itemDAO;

    private static final String PATH = "/api/orders";
    private static final String PATH_WITH_ID = "/api/orders/";
    private static final String PATH_WITH_USER = "/api/orders/user/";

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    private Item testItem;
    private Order testOrder;
    private String token;
    private OrderRequest request;

    @BeforeEach
    void setUp() {
        token = generateTestToken("test@test.com", 1L, "ADMIN");

        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM items");

        Item item = new Item();
        item.setName("TestItem");
        item.setPrice(BigDecimal.valueOf(100.0));
        testItem = itemDAO.save(item);

        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(Status.CREATED);
        order.setTotalPrice(BigDecimal.valueOf(100.0));

        OrderItem orderItem = new OrderItem();
        orderItem.setItem(testItem);
        orderItem.setOrder(order);
        orderItem.setQuantity(1);
        order.getOrderItems().add(orderItem);

        testOrder = orderDAO.save(order);
        request =
                new OrderRequest(
                        "update@test.com",
                        Status.PAID,
                        List.of(new OrderItemRequest(testItem.getId(), 3)));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
    }

    private void stubUserByEmail(String email, Long userId) {
        wireMockServer.stubFor(
                get(urlPathMatching("/api/users/email/.*"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                {
                                                  "id": %d,
                                                  "name": "Test",
                                                  "surname": "User",
                                                  "email": "%s",
                                                  "active": true
                                                }
                                                """
                                                        .formatted(userId, email))
                                        .withStatus(200)));
    }

    private void stubUserById(Long userId, String email) {
        wireMockServer.stubFor(
                get(urlPathMatching("/api/users/" + userId))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                {
                                                  "id": %d,
                                                  "name": "Test",
                                                  "surname": "User",
                                                  "email": "%s",
                                                  "active": true
                                                }
                                                """
                                                        .formatted(userId, email))
                                        .withStatus(200)));
    }

    private void stubUsersBatch(List<Long> expectedIds, String email) {
        String expectedRequestBody = expectedIds.toString();

        StringBuilder responseBody = new StringBuilder("[");
        for (int i = 0; i < expectedIds.size(); i++) {
            responseBody.append(
                    """
                    {
                      "id": %d,
                      "name": "Test",
                      "surname": "User",
                      "email": "%s",
                      "active": true
                    }
                    """
                            .formatted(expectedIds.get(i), email));
            if (i < expectedIds.size() - 1) {
                responseBody.append(",");
            }
        }
        responseBody.append("]");

        wireMockServer.stubFor(
                post(urlPathEqualTo("/api/users/batch"))
                        .withRequestBody(equalToJson(expectedRequestBody))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(responseBody.toString())
                                        .withStatus(200)));
    }

    @Test
    void givenOrderRequestAndExistingUser_ShouldCreateOrder() {
        String userEmail = "test@test.com";
        stubUserByEmail(userEmail, 1L);

        OrderRequest request =
                new OrderRequest(
                        userEmail,
                        Status.CREATED,
                        List.of(new OrderItemRequest(testItem.getId(), 2)));

        OrderResponse body =
                webTestClient
                        .post()
                        .uri(PATH)
                        .header(AUTHORIZATION, BEARER + token)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(OrderResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertNotNull(body);
        assertEquals(Status.CREATED, body.status());
        assertEquals(BigDecimal.valueOf(200.0).doubleValue(), body.totalPrice().doubleValue());
        assertEquals(1, body.user().id());
        assertEquals(testItem.getId(), body.orderItems().getFirst().item().id());
    }

    @Test
    void givenOrderRequestAndNonExistingUser_ShouldReturnBadRequest() {
        String userEmail = "notfound@test.com";
        wireMockServer.stubFor(
                get(urlPathMatching("/api/users/email/.*"))
                        .willReturn(aResponse().withStatus(404)));

        OrderRequest request =
                new OrderRequest(
                        userEmail,
                        Status.CREATED,
                        List.of(new OrderItemRequest(testItem.getId(), 2)));

        webTestClient
                .post()
                .uri(PATH)
                .header(AUTHORIZATION, BEARER + token)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ProblemDetail.class)
                .value(p -> assertEquals("User not found or service unavailable", p.getDetail()));
    }

    @Test
    void givenOrderRequestWithMissingStatus_ShouldReturnBadRequest() {
        OrderRequest request =
                new OrderRequest(
                        "test@test.com", null, List.of(new OrderItemRequest(testItem.getId(), 2)));

        webTestClient
                .post()
                .uri(PATH)
                .header(AUTHORIZATION, BEARER + token)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void givenOrderIdAndExistingUser_ShouldReturnOrder() {
        stubUserById(1L, "test@test.com");

        OrderResponse body =
                webTestClient
                        .get()
                        .uri(PATH_WITH_ID + testOrder.getId())
                        .header(AUTHORIZATION, BEARER + token)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(OrderResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertNotNull(body);
        assertEquals(testOrder.getId(), body.id());
        assertEquals(1L, body.user().id());
        assertEquals(BigDecimal.valueOf(100.0).doubleValue(), body.totalPrice().doubleValue());
    }

    @Test
    void givenOrderId_ShouldReturnNotFound() {
        webTestClient
                .get()
                .uri("/api/orders/999")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void givenOrdersRequestAndExistingUser_ShouldReturnOrders() {
        stubUsersBatch(List.of(1L), "test@test.com");

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/api/orders").build())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content[0].id")
                .isEqualTo(testOrder.getId());
    }

    @Test
    void givenUserId_ShouldReturnOrders() {
        stubUserById(1L, "test@test.com");

        List<OrderResponse> orders =
                webTestClient
                        .get()
                        .uri(PATH_WITH_USER + testOrder.getUserId())
                        .header(AUTHORIZATION, BEARER + token)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBodyList(OrderResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertNotNull(orders);
        assertEquals(1, orders.size());
        assertEquals(testOrder.getId(), orders.getFirst().id());
    }

    @Test
    void givenOrderIdAndUpdateRequest_ShouldUpdateOrder() {
        stubUserByEmail("update@test.com", 2L);

        OrderResponse body =
                webTestClient
                        .put()
                        .uri(PATH_WITH_ID + testOrder.getId())
                        .header(AUTHORIZATION, BEARER + token)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(OrderResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertNotNull(body);
        assertEquals(Status.PAID, body.status());
        assertEquals(2L, body.user().id());
        assertEquals(BigDecimal.valueOf(300.0).doubleValue(), body.totalPrice().doubleValue());
    }

    @Test
    void givenOrderIdAndUpdateRequest_ShouldReturnNotFound() {
        stubUserByEmail("update@test.com", 2L);

        webTestClient
                .put()
                .uri("/api/orders/999")
                .header(AUTHORIZATION, BEARER + token)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void givenOrderId_ShouldDeleteOrder() {
        webTestClient
                .delete()
                .uri(PATH_WITH_ID + testOrder.getId())
                .header(AUTHORIZATION, BEARER + token)
                .exchange()
                .expectStatus()
                .isNoContent();

        assertTrue(orderDAO.findById(testOrder.getId()).isEmpty());
    }
}
