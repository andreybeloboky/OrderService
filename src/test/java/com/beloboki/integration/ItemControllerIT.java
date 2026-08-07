package com.beloboki.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.beloboki.dao.ItemDAO;
import com.beloboki.dto.ItemRequest;
import com.beloboki.dto.ItemResponse;
import com.beloboki.model.Item;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;

public class ItemControllerIT extends AbstractIT {

    @Autowired private WebTestClient webTestClient;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private ItemDAO itemDAO;

    private Item testItem;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = generateTestToken("admin@test.com", 1L, "ADMIN");
        userToken = generateTestToken("user@test.com", 2L, "USER");

        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM items");

        Item item = new Item();
        item.setName("TestItem");
        item.setPrice(BigDecimal.valueOf(100.0));
        testItem = itemDAO.save(item);
    }

    @Test
    void givenItemRequestAndAdminToken_ShouldCreateItem() {
        ItemRequest request = new ItemRequest("NewItem", BigDecimal.valueOf(50.0));

        ItemResponse body =
                webTestClient
                        .post()
                        .uri("/api/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(ItemResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertNotNull(body);
        assertEquals("NewItem", body.name());
        assertEquals(BigDecimal.valueOf(50.0).doubleValue(), body.price().doubleValue());
    }

    @Test
    void givenItemRequestAndUserToken_ShouldReturnForbidden() {
        ItemRequest request = new ItemRequest("NewItem", BigDecimal.valueOf(50.0));

        webTestClient
                .post()
                .uri("/api/items")
                .header("Authorization", "Bearer " + userToken)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void givenItemIdAndMatchingUserToken_ShouldReturnItem() {
        String specificUserToken = generateTestToken("user@test.com", testItem.getId(), "USER");
        ItemResponse body =
                webTestClient
                        .get()
                        .uri("/api/items/" + testItem.getId())
                        .header("Authorization", "Bearer " + specificUserToken)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(ItemResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertNotNull(body);
        assertEquals(testItem.getId(), body.id());
        assertEquals("TestItem", body.name());
    }

    @Test
    void givenItemIdAndDifferentUserToken_ShouldReturnForbidden() {
        String specificUserToken =
                generateTestToken("user@test.com", testItem.getId() + 999, "USER");

        webTestClient
                .get()
                .uri("/api/items/" + testItem.getId())
                .header("Authorization", "Bearer " + specificUserToken)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void givenPageRequestAndUserToken_ShouldReturnItems() {
        webTestClient
                .get()
                .uri("/api/items")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content.length()")
                .isEqualTo(1)
                .jsonPath("$.content[0].name")
                .isEqualTo("TestItem");
    }

    @Test
    void givenItemIdAndUpdateRequestWithAdminToken_ShouldUpdateItem() {
        ItemRequest request = new ItemRequest("UpdatedItem", BigDecimal.valueOf(150.0));

        ItemResponse body =
                webTestClient
                        .put()
                        .uri("/api/items/" + testItem.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(ItemResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertNotNull(body);
        assertEquals("UpdatedItem", body.name());
        assertEquals(BigDecimal.valueOf(150.0).doubleValue(), body.price().doubleValue());
    }

    @Test
    void givenItemIdAndAdminToken_ShouldDeleteItem() {
        webTestClient
                .delete()
                .uri("/api/items/" + testItem.getId())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus()
                .isNoContent();

        webTestClient
                .get()
                .uri("/api/items/" + testItem.getId())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus()
                .isNotFound();
    }
}
