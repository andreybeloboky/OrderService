package com.beloboki.service;

import com.beloboki.config.CurrentUser;
import com.beloboki.dao.ItemDAO;
import com.beloboki.dto.ItemRequest;
import com.beloboki.dto.ItemResponse;
import com.beloboki.mapper.ItemMapper;
import com.beloboki.model.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock private ItemDAO itemDAO;

    @Mock private ItemMapper itemMapper;

    @InjectMocks private ItemService itemService;

    @Test
    void testCreateItem() {
        ItemRequest request = new ItemRequest("Test", BigDecimal.TEN);
        Item item = new Item(null, "Test", BigDecimal.TEN);
        Item savedItem = new Item(1L, "Test", BigDecimal.TEN);
        ItemResponse response = new ItemResponse(1L, "Test", BigDecimal.TEN);

        when(itemMapper.toEntity(request)).thenReturn(item);
        when(itemDAO.save(any(Item.class))).thenReturn(savedItem);
        when(itemMapper.toResponse(any(Item.class))).thenReturn(response);

        ItemResponse result = itemService.createItem(request);
        assertEquals(1L, result.id());
    }

    @Test
    void testGetItemById() {
        Item item = new Item(1L, "Test", BigDecimal.TEN);
        ItemResponse response = new ItemResponse(1L, "Test", BigDecimal.TEN);
        when(itemDAO.findById(1L)).thenReturn(Optional.of(item));
        when(itemMapper.toResponse(any(Item.class))).thenReturn(response);

        ItemResponse result = itemService.getItemById(1L, new CurrentUser(1L, "user", "USER"));
        assertEquals(1L, result.id());
    }

    @Test
    void testGetItemByIdAccessDenied() {
        assertThrows(
                AuthorizationDeniedException.class,
                () -> itemService.getItemById(1L, new CurrentUser(2L, "user", "USER")));
    }

    @Test
    void testGetItems() {
        Item item = new Item(1L, "Test", BigDecimal.TEN);
        ItemResponse response = new ItemResponse(1L, "Test", BigDecimal.TEN);
        Page<Item> page = new PageImpl<>(List.of(item));

        when(itemDAO.findAll(any(PageRequest.class))).thenReturn(page);
        when(itemMapper.toResponse(any(Item.class))).thenReturn(response);

        Page<ItemResponse> result = itemService.getItems(PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testUpdateItem() {
        ItemRequest request = new ItemRequest("Test2", BigDecimal.valueOf(20));
        Item item = new Item(1L, "Test", BigDecimal.TEN);
        Item mappedItem = new Item(null, "Test2", BigDecimal.valueOf(20));
        Item savedItem = new Item(1L, "Test2", BigDecimal.valueOf(20));
        ItemResponse response = new ItemResponse(1L, "Test2", BigDecimal.valueOf(20));

        when(itemDAO.findById(1L)).thenReturn(Optional.of(item));
        when(itemMapper.toEntity(request)).thenReturn(mappedItem);
        when(itemDAO.save(any(Item.class))).thenReturn(savedItem);
        when(itemMapper.toResponse(any(Item.class))).thenReturn(response);

        ItemResponse result = itemService.updateItem(1L, request);
        assertEquals("Test2", result.name());
    }

    @Test
    void testDeleteItem() {
        Item item = new Item(1L, "Test", BigDecimal.TEN);
        when(itemDAO.findById(1L)).thenReturn(Optional.of(item));

        itemService.deleteItem(1L);

        verify(itemDAO, times(1)).delete(item);
    }
}
