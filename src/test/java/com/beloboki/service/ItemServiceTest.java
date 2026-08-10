package com.beloboki.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.beloboki.dao.ItemDAO;
import com.beloboki.dto.ItemRequest;
import com.beloboki.dto.ItemResponse;
import com.beloboki.exception.ItemNotFoundException;
import com.beloboki.mapper.ItemMapper;
import com.beloboki.model.Item;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock private ItemDAO itemDAO;

    @Mock private ItemMapper itemMapper;

    @InjectMocks private ItemService itemService;

    @Test
    void givenItemRequest_ShouldCreateItem_WhenValidRequest() {
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
    void givenItemId_ShouldReturnItem_WhenItemExists() {
        Item item = new Item(1L, "Test", BigDecimal.TEN);
        ItemResponse response = new ItemResponse(1L, "Test", BigDecimal.TEN);
        when(itemDAO.findById(1L)).thenReturn(Optional.of(item));
        when(itemMapper.toResponse(any(Item.class))).thenReturn(response);

        ItemResponse result = itemService.getItemById(1L);
        assertEquals(1L, result.id());
    }

    @Test
    void givenItemIdAndDifferentUser_ShouldThrowException_WhenAccessDenied() {
        assertThrows(ItemNotFoundException.class, () -> itemService.getItemById(1L));
    }

    @Test
    void givenPageRequest_ShouldReturnItems_WhenItemsExist() {
        Item item = new Item(1L, "Test", BigDecimal.TEN);
        ItemResponse response = new ItemResponse(1L, "Test", BigDecimal.TEN);
        Page<Item> page = new PageImpl<>(List.of(item));

        when(itemDAO.findAll(any(PageRequest.class))).thenReturn(page);
        when(itemMapper.toResponse(any(Item.class))).thenReturn(response);

        Page<ItemResponse> result = itemService.getItems(PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void givenItemIdAndUpdateRequest_ShouldUpdateItem_WhenItemExists() {
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
    void givenItemId_ShouldDeleteItem_WhenItemExists() {
        Item item = new Item(1L, "Test", BigDecimal.TEN);
        when(itemDAO.findById(1L)).thenReturn(Optional.of(item));

        itemService.deleteItem(1L);

        verify(itemDAO, times(1)).delete(item);
    }

    @Test
    void givenItemId_ShouldThrowException_WhenItemNotFoundOnUpdate() {
        ItemRequest request = new ItemRequest("UpdatedName", BigDecimal.valueOf(100));

        when(itemDAO.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.updateItem(10L, request));
    }

    @Test
    void givenItemId_ShouldThrowException_WhenItemNotFoundOnDelete() {
        when(itemDAO.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemService.deleteItem(10L));
    }
}
