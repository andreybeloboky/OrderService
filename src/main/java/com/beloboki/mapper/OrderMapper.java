package com.beloboki.mapper;

import com.beloboki.dto.OrderRequest;
import com.beloboki.dto.OrderResponse;
import com.beloboki.dto.UserResponse;
import com.beloboki.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {OrderItemMapper.class})
public interface OrderMapper {

    @Mapping(source = "order.id", target = "id")
    @Mapping(source = "order.createdAt", target = "createdAt")
    @Mapping(source = "order.updatedAt", target = "updatedAt")
    @Mapping(source = "user", target = "user")
    OrderResponse toResponse(Order order, UserResponse user);

    Order toEntity(OrderRequest request);
}
