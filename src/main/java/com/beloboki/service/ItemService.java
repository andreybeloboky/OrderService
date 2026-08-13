package com.beloboki.service;

import com.beloboki.dao.ItemDAO;
import com.beloboki.dto.ItemRequest;
import com.beloboki.dto.ItemResponse;
import com.beloboki.exception.ItemNotFoundException;
import com.beloboki.mapper.ItemMapper;
import com.beloboki.model.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "items")
public class ItemService {

    private final ItemDAO itemDAO;
    private final ItemMapper itemMapper;

    @Transactional
    public ItemResponse createItem(ItemRequest request) {
        Item item = itemMapper.toEntity(request);
        item = itemDAO.saveAndFlush(item);
        return itemMapper.toResponse(item);
    }

    @Cacheable(key = "#id")
    public ItemResponse getItemById(Long id) {
        Item item =
                itemDAO.findById(id)
                        .orElseThrow(
                                () -> new ItemNotFoundException("Item not found with id " + id));

        return itemMapper.toResponse(item);
    }

    public Page<ItemResponse> getItems(Pageable pageable) {
        return itemDAO.findAll(pageable).map(itemMapper::toResponse);
    }

    @Transactional
    @CacheEvict(key = "#id")
    public ItemResponse updateItem(Long id, ItemRequest request) {
        Item item =
                itemDAO.findById(id)
                        .orElseThrow(
                                () -> new ItemNotFoundException("Item not found with id " + id));

        Item updatedData = itemMapper.toEntity(request);
        item.setName(updatedData.getName());
        item.setPrice(updatedData.getPrice());

        item = itemDAO.saveAndFlush(item);
        return itemMapper.toResponse(item);
    }

    @Transactional
    @CacheEvict(key = "#id")
    public void deleteItem(Long id) {
        Item item =
                itemDAO.findById(id)
                        .orElseThrow(
                                () -> new ItemNotFoundException("Item not found with id " + id));
        itemDAO.delete(item);
    }
}
