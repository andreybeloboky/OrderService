package com.beloboki.kafka.consumer;

import com.beloboki.dao.OrderDAO;
import com.beloboki.kafka.event.PaymentEvent;
import com.beloboki.model.PaymentStatus;
import com.beloboki.model.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentEventConsumer {

    private OrderDAO orderDAO;

    @KafkaListener(
            topics = "${kafka.topics.payment-events:payment-events}",
            groupId = "${spring.kafka.consumer.group-id:order-group}"
    )
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received PaymentEvent for orderId: {}, status: {}", event.orderId(), event.status());

        if (event.status() == PaymentStatus.SUCCESS) {
            log.info("Payment succeeded for order {}. Updating order status to PAID.", event.orderId());
            orderDAO.findById(event.orderId()).ifPresent(order -> {
                order.setStatus(Status.PAID);
                orderDAO.save(order);
            });
        } else {
            log.warn("Payment failed for order {}. Updating order status to CANCELLED/FAILED.", event.orderId());
            orderDAO.findById(event.orderId()).ifPresent(order -> {
                order.setStatus(Status.CANCELLED);
                orderDAO.save(order);
            });
        }
    }
}
