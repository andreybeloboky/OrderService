package com.beloboki.specification;

import com.beloboki.model.Order;
import java.time.LocalDateTime;
import java.util.List;

import com.beloboki.model.Status;
import org.springframework.data.jpa.domain.Specification;

public class OrderSpecifications {

    public static Specification<Order> getOrdersByCriteria(
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<Status> statuses) {

        return Specification.where(createdAfter(startDate))
                .and(createdBefore(endDate))
                .and(hasStatuses(statuses));
    }

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

    public static Specification<Order> hasStatuses(List<Status> statuses) {
        return (root, query, criteriaBuilder) ->
                (statuses == null || statuses.isEmpty()) ? null : root.get("status").in(statuses);
    }

    private OrderSpecifications() {}
}
