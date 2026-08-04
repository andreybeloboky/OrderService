package com.beloboki.mapper;

import com.beloboki.dto.OrderItemRequest;
import com.beloboki.dto.OrderItemResponse;
import com.beloboki.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {ItemMapper.class})
public interface OrderItemMapper {

    OrderItemResponse toResponse(OrderItem orderItem);

    @Mapping(source = "itemId", target = "item.id")
    OrderItem toEntity(OrderItemRequest request);
}
