package com.beloboki.service;

import com.beloboki.client.UserClient;
import com.beloboki.config.CurrentUser;
import com.beloboki.dao.ItemDAO;
import com.beloboki.dao.OrderDAO;
import com.beloboki.dao.OrderItemDAO;
import com.beloboki.dto.OrderRequest;
import com.beloboki.dto.OrderResponse;
import com.beloboki.dto.UserResponse;
import com.beloboki.exception.ItemNotFoundException;
import com.beloboki.exception.OrderNotFoundException;
import com.beloboki.exception.UserNotFoundException;
import com.beloboki.mapper.OrderMapper;
import com.beloboki.model.*;
import com.beloboki.specification.OrderSpecifications;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "orders")
public class OrderService {

    private final OrderDAO orderDAO;
    private final ItemDAO itemDAO;
    private final OrderItemDAO orderItemDAO;
    private final OrderMapper orderMapper;
    private final UserClient userClient;

    @Transactional
    public OrderResponse createOrder(OrderRequest request, CurrentUser currentUser) {
        Order order = orderMapper.toEntity(request);

        UserResponse user = userClient.getUserByEmail(order.getUserEmail());
        if (user == null || user.id() == null) {
            throw new UserNotFoundException("User not found or service unavailable");
        }

        validate(currentUser.userId(), user.id(), currentUser.role());

        order.setUserId(user.id());

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem orderItem : order.getOrderItems()) {
            Item item =
                    itemDAO.findById(orderItem.getItem().getId())
                            .orElseThrow(
                                    () ->
                                            new ItemNotFoundException(
                                                    "Item not found with id "
                                                            + orderItem.getItem().getId()));

            orderItem.setOrder(order);
            orderItem.setItem(item);

            total =
                    total.add(
                            item.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }

        order.setTotalPrice(total);
        order = orderDAO.save(order);
        orderItemDAO.saveAll(order.getOrderItems());

        return orderMapper.toResponse(order, user);
    }

    @Cacheable(key = "#id")
    public OrderResponse getOrderById(Long id, CurrentUser currentUser) {
        Order order =
                orderDAO.findById(id)
                        .orElseThrow(
                                () -> new OrderNotFoundException("Order not found with id " + id));

        validate(currentUser.userId(), order.getUserId(), currentUser.role());

        UserResponse user = userClient.getUserById(order.getUserId());
        return orderMapper.toResponse(order, user);
    }

    public Page<OrderResponse> getOrders(
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<Status> statuses,
            Pageable pageable) {
        return orderDAO.findAll(
                        OrderSpecifications.getOrdersByCriteria(startDate, endDate, statuses),
                        pageable)
                .map(
                        order -> {
                            UserResponse user = userClient.getUserById(order.getUserId());
                            return orderMapper.toResponse(order, user);
                        });
    }

    public List<OrderResponse> getOrdersByUserId(Long userId, CurrentUser currentUser) {
        validate(currentUser.userId(), userId, currentUser.role());
        return orderDAO.findByUserId(userId).stream()
                .map(
                        order -> {
                            UserResponse user = userClient.getUserById(order.getUserId());
                            return orderMapper.toResponse(order, user);
                        })
                .toList();
    }

    @Transactional
    @CacheEvict(key = "#id")
    public OrderResponse updateOrder(Long id, OrderRequest request, CurrentUser currentUser) {
        Order order =
                orderDAO.findById(id)
                        .orElseThrow(
                                () -> new OrderNotFoundException("Order not found with id " + id));

        validate(currentUser.userId(), order.getUserId(), currentUser.role());

        Order updatedData = orderMapper.toEntity(request);

        UserResponse user = userClient.getUserByEmail(updatedData.getUserEmail());
        if (user == null || user.id() == null) {
            throw new UserNotFoundException("User not found or service unavailable");
        }

        validate(currentUser.userId(), user.id(), currentUser.role());

        order.setUserId(user.id());
        order.setStatus(updatedData.getStatus());
        orderItemDAO.deleteById(order.getId());
        order.getOrderItems().clear();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem orderItem : updatedData.getOrderItems()) {
            Item item =
                    itemDAO.findById(orderItem.getItem().getId())
                            .orElseThrow(
                                    () ->
                                            new ItemNotFoundException(
                                                    "Item not found with id "
                                                            + orderItem.getItem().getId()));

            orderItem.setOrder(order);
            orderItem.setItem(item);

            order.getOrderItems().add(orderItem);

            total =
                    total.add(
                            item.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }

        order.setTotalPrice(total);
        order = orderDAO.save(order);
        orderItemDAO.saveAll(order.getOrderItems());

        return orderMapper.toResponse(order, user);
    }

    @Transactional
    @CacheEvict(key = "#id")
    public void deleteOrder(Long id) {
        Order order =
                orderDAO.findById(id)
                        .orElseThrow(
                                () -> new OrderNotFoundException("Order not found with id " + id));
        orderDAO.delete(order);
    }

    private void validate(Long currentUserId, Long targetUserId, String role) {
        if (!Objects.equals(currentUserId, targetUserId)
                && (!Objects.equals(role, String.valueOf(Role.ADMIN)))) {
            throw new AuthorizationDeniedException("Access denied");
        }
    }
}
