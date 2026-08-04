package com.beloboki.specification;

import com.beloboki.model.Order;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class OrderSpecifications {

    public static Specification<Order> createdAfter(LocalDateTime startDate) {
        return (root, query, criteriaBuilder) ->
                startDate == null
                        ? null
                        : criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
    }

    public static Specification<Order> createdBefore(LocalDateTime endDate) {
        return (root, query, criteriaBuilder) ->
                endDate == null
                        ? null
                        : criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
    }

    public static Specification<Order> hasStatuses(List<String> statuses) {
        return (root, query, criteriaBuilder) ->
                (statuses == null || statuses.isEmpty()) ? null : root.get("status").in(statuses);
    }

    private OrderSpecifications() {}
}
