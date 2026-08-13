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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class ItemControllerIT extends AbstractIT {

    @Autowired private WebTestClient webTestClient;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private ItemDAO itemDAO;

    private Item testItem;
    private String adminToken;
    private String userToken;
    private ItemRequest request;
    private String specificUserToken;

    private static final String PATH = "/api/items";
    private static final String PATH_WITH_ID = "/api/items/";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    @BeforeEach
    void setUp() {
        adminToken = generateTestToken("admin@test.com", 1L, "ADMIN");
        userToken = generateTestToken("user@test.com", 2L, "USER");
        request = new ItemRequest("Item", BigDecimal.valueOf(150.0));

        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM items");

        Item item = new Item();
        item.setName("TestItem");
        item.setPrice(BigDecimal.valueOf(100.0));
        testItem = itemDAO.save(item);

        specificUserToken = generateTestToken("user@test.com", testItem.getId(), "USER");
    }

    @Test
    void givenItemRequestAndAdminToken_ShouldCreateItem() {
        ItemResponse body =
                webTestClient
                        .post()
                        .uri(PATH)
                        .header(AUTHORIZATION, BEARER + adminToken)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(ItemResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertNotNull(body);
        assertEquals("Item", body.name());
        assertEquals(BigDecimal.valueOf(150.0).doubleValue(), body.price().doubleValue());
    }

    @Test
    void givenItemRequestAndUserToken_ShouldReturnForbidden() {
        webTestClient
                .post()
                .uri(PATH)
                .header(AUTHORIZATION, BEARER + userToken)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void givenItemIdAndMatchingUserToken_ShouldReturnItem() {
        ItemResponse body =
                webTestClient
                        .get()
                        .uri(PATH_WITH_ID + testItem.getId())
                        .header(AUTHORIZATION, BEARER + specificUserToken)
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
    void givenPageRequestAndUserToken_ShouldReturnItems() {
        webTestClient
                .get()
                .uri(PATH)
                .header(AUTHORIZATION, BEARER + userToken)
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
        ItemResponse body =
                webTestClient
                        .put()
                        .uri(PATH_WITH_ID + testItem.getId())
                        .header(AUTHORIZATION, BEARER + adminToken)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(ItemResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertNotNull(body);
        assertEquals("Item", body.name());
        assertEquals(BigDecimal.valueOf(150.0).doubleValue(), body.price().doubleValue());
    }

    @Test
    void givenItemIdAndAdminToken_ShouldDeleteItem() {
        webTestClient
                .delete()
                .uri(PATH_WITH_ID + testItem.getId())
                .header(AUTHORIZATION, BEARER + adminToken)
                .exchange()
                .expectStatus()
                .isNoContent();

        webTestClient
                .get()
                .uri(PATH_WITH_ID + testItem.getId())
                .header(AUTHORIZATION, BEARER + adminToken)
                .exchange()
                .expectStatus()
                .isNotFound();
    }
}
