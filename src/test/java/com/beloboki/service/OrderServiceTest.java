package com.beloboki.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.beloboki.client.UserClient;
import com.beloboki.config.CurrentUser;
import com.beloboki.dao.ItemDAO;
import com.beloboki.dao.OrderDAO;
import com.beloboki.dao.OrderItemDAO;
import com.beloboki.dto.OrderItemRequest;
import com.beloboki.dto.OrderRequest;
import com.beloboki.dto.OrderResponse;
import com.beloboki.dto.UserResponse;
import com.beloboki.exception.OrderNotFoundException;
import com.beloboki.exception.UserNotFoundException;
import com.beloboki.mapper.OrderMapper;
import com.beloboki.model.Item;
import com.beloboki.model.Order;
import com.beloboki.model.Status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authorization.AuthorizationDeniedException;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock private OrderDAO orderDAO;

    @Mock private ItemDAO itemDAO;

    @Mock private OrderMapper orderMapper;

    @Mock private OrderItemDAO orderItemDAO;

    @Mock private UserClient userClient;

    @InjectMocks private OrderService orderService;

    @Test
    void givenOrderRequest_ShouldCreateOrder_WhenUserExists() {
        OrderRequest request =
                new OrderRequest(
                        "test@test.com", Status.CREATED, List.of(new OrderItemRequest(1L, 2)));
        UserResponse user =
                new UserResponse(1L, "Name", "Surname", null, "test@test.com", true, null, null);
        Item item = new Item(1L, "Item1", BigDecimal.TEN);

        Order order = new Order();
        order.setId(10L);
        order.setUserId(1L);
        order.setTotalPrice(BigDecimal.valueOf(20));

        Order mappedOrder = new Order();
        mappedOrder.setStatus(Status.CREATED);
        mappedOrder.setUserEmail("test@test.com");
        com.beloboki.model.OrderItem mappedOrderItem = new com.beloboki.model.OrderItem();
        Item tempItem = new Item();
        tempItem.setId(1L);
        mappedOrderItem.setItem(tempItem);
        mappedOrderItem.setQuantity(2);
        mappedOrder.getOrderItems().add(mappedOrderItem);

        OrderResponse mappedResponse =
                new OrderResponse(
                        10L,
                        user,
                        Status.CREATED,
                        BigDecimal.valueOf(20),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        List.of());

        when(userClient.getUserByEmail(request.userEmail())).thenReturn(user);
        when(orderMapper.toEntity(request)).thenReturn(mappedOrder);
        when(itemDAO.findById(1L)).thenReturn(Optional.of(item));
        when(orderDAO.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class), any(UserResponse.class)))
                .thenReturn(mappedResponse);

        OrderResponse result =
                orderService.createOrder(request, new CurrentUser(1L, "user", "USER"));

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertNotNull(result.user());
        assertEquals(1L, result.user().id());
        verify(orderDAO, times(1)).save(any(Order.class));
    }

    @Test
    void givenOrderRequest_ShouldThrowException_WhenUserNotFound() {
        OrderRequest request =
                new OrderRequest(
                        "test@test.com", Status.CREATED, List.of(new OrderItemRequest(1L, 2)));
        Order mappedOrder = new Order();
        mappedOrder.setUserEmail("test@test.com");
        when(orderMapper.toEntity(request)).thenReturn(mappedOrder);
        when(userClient.getUserByEmail("test@test.com")).thenReturn(null);

        assertThrows(
                UserNotFoundException.class,
                () -> orderService.createOrder(request, new CurrentUser(1L, "user", "USER")));
    }

    @Test
    void givenOrderId_ShouldReturnOrder_WhenOrderExists() {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(1L);

        UserResponse user =
                new UserResponse(1L, "Name", "Surname", null, "test@test.com", true, null, null);
        OrderResponse mappedResponse =
                new OrderResponse(
                        10L,
                        user,
                        Status.CREATED,
                        BigDecimal.valueOf(20),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        List.of());

        when(orderDAO.findById(10L)).thenReturn(Optional.of(order));
        when(userClient.getUserById(1L)).thenReturn(user);
        when(orderMapper.toResponse(any(Order.class), any(UserResponse.class)))
                .thenReturn(mappedResponse);

        OrderResponse result = orderService.getOrderById(10L, new CurrentUser(1L, "user", "USER"));
        assertNotNull(result);
        assertNotNull(result.user());
        assertEquals(10L, result.id());
    }

    @Test
    void givenFilterParameters_ShouldReturnOrders_WhenOrdersExist() {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(1L);

        Page<Order> page = new PageImpl<>(List.of(order));
        UserResponse user =
                new UserResponse(1L, "Name", "Surname", null, "test@test.com", true, null, null);
        OrderResponse mappedResponse =
                new OrderResponse(
                        10L,
                        user,
                        Status.CREATED,
                        BigDecimal.valueOf(20),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        List.of());

        when(orderDAO.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(userClient.getUsersByIds(anyList())).thenReturn(List.of(user));
        when(orderMapper.toResponse(any(Order.class), any(UserResponse.class)))
                .thenReturn(mappedResponse);

        Page<OrderResponse> result =
                orderService.getOrders(null, null, null, PageRequest.of(0, 10));
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertNotNull(result.getContent().getFirst().user());
    }

    @Test
    void givenUserId_ShouldReturnOrders_WhenUserHasOrders() {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(1L);

        UserResponse user =
                new UserResponse(1L, "Name", "Surname", null, "test@test.com", true, null, null);
        OrderResponse mappedResponse =
                new OrderResponse(
                        10L,
                        user,
                        Status.CREATED,
                        BigDecimal.valueOf(20),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        List.of());

        when(orderDAO.findByUserId(1L)).thenReturn(List.of(order));
        when(userClient.getUserById(1L)).thenReturn(user);
        when(orderMapper.toResponse(any(Order.class), any(UserResponse.class)))
                .thenReturn(mappedResponse);

        List<OrderResponse> result =
                orderService.getOrdersByUserId(1L, new CurrentUser(1L, "user", "USER"));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.getFirst().user());
    }

    @Test
    void givenOrderIdAndUpdateRequest_ShouldUpdateOrder_WhenOrderExists() {
        OrderRequest request =
                new OrderRequest(
                        "test@test.com", Status.PAID, List.of(new OrderItemRequest(1L, 2)));
        Order order = new Order();
        order.setId(10L);
        order.setUserId(1L);

        UserResponse user =
                new UserResponse(2L, "Name", "Surname", null, "test@test.com", true, null, null);
        Item item = new Item(1L, "Item1", BigDecimal.TEN);

        Order mappedOrder = new Order();
        mappedOrder.setStatus(Status.PAID);
        mappedOrder.setUserEmail("test@test.com");
        com.beloboki.model.OrderItem mappedOrderItem = new com.beloboki.model.OrderItem();
        Item tempItem = new Item();
        tempItem.setId(1L);
        mappedOrderItem.setItem(tempItem);
        mappedOrderItem.setQuantity(2);
        mappedOrder.getOrderItems().add(mappedOrderItem);

        OrderResponse mappedResponse =
                new OrderResponse(
                        10L,
                        user,
                        Status.PAID,
                        BigDecimal.valueOf(20),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        List.of());

        when(orderDAO.findById(10L)).thenReturn(Optional.of(order));
        when(userClient.getUserByEmail(request.userEmail())).thenReturn(user);
        when(orderMapper.toEntity(request)).thenReturn(mappedOrder);
        when(itemDAO.findById(1L)).thenReturn(Optional.of(item));
        when(orderDAO.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class), any(UserResponse.class)))
                .thenReturn(mappedResponse);

        OrderResponse result =
                orderService.updateOrder(10L, request, new CurrentUser(1L, "admin", "ADMIN"));
        assertNotNull(result);
        assertEquals(2L, result.user().id());
    }

    @Test
    void givenOrderId_ShouldDeleteOrder_WhenOrderExists() {
        Order order = new Order();
        order.setId(10L);
        when(orderDAO.findById(10L)).thenReturn(Optional.of(order));

        orderService.deleteOrder(10L, new CurrentUser(10L, null, null));

        assertTrue(order.getDeleted());
        verify(orderDAO, times(1)).save(order);
    }

    @Test
    void givenOrderId_ShouldThrowException_WhenOrderNotFound() {
        when(orderDAO.findById(10L)).thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(10L, new CurrentUser(1L, "user", "USER")));
    }

    @Test
    void givenOrderId_ShouldThrowException_WhenAccessDenied() {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(2L);

        when(orderDAO.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(
                AuthorizationDeniedException.class,
                () -> orderService.getOrderById(10L, new CurrentUser(1L, "user", "USER")));
    }

    @Test
    void givenOrderId_ShouldThrowException_WhenOrderNotFoundOnUpdate() {
        OrderRequest request = new OrderRequest("test@test.com", Status.PAID, List.of());

        when(orderDAO.findById(10L)).thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.updateOrder(10L, request, new CurrentUser(1L, "user", "USER")));
    }

    @Test
    void givenOrderId_ShouldThrowException_WhenAccessDeniedOnDelete() {
        assertThrows(
                AuthorizationDeniedException.class,
                () -> orderService.deleteOrder(10L, new CurrentUser(1L, "user", "USER")));
    }

    @Test
    void givenOrderId_ShouldThrowException_WhenUserNotFoundOnUpdate() {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(1L);

        OrderRequest request = new OrderRequest("test@test.com", Status.PAID, List.of());
        Order mappedOrder = new Order();
        mappedOrder.setUserEmail("test@test.com");

        when(orderDAO.findById(10L)).thenReturn(Optional.of(order));
        when(orderMapper.toEntity(request)).thenReturn(mappedOrder);
        when(userClient.getUserByEmail("test@test.com")).thenReturn(null);

        assertThrows(
                UserNotFoundException.class,
                () -> orderService.updateOrder(10L, request, new CurrentUser(1L, "user", "USER")));
    }
}
