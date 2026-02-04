package com.bookstore.bookstore_api.order.application.event.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bookstore.bookstore_api.order.adapter.out.kafka.OrderLogKafkaProducer;
import com.bookstore.bookstore_api.order.application.event.message.OrderLogMessage;
import com.bookstore.bookstore_api.order.application.event.object.OrderLogEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLogEventListener {

    private final OrderLogKafkaProducer orderLogKafkaProducer;

    // 주문 로직 성공 시 - Kafka로 메시지 발행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderSuccessLog(OrderLogEvent orderLogEvent) {
        log.info("주문 성공 이벤트 수신 - orderId: {}, userId: {}",
                orderLogEvent.getOrderId(),
                orderLogEvent.getUserId());

        OrderLogMessage message = OrderLogMessage.success(
                orderLogEvent.getOrderId(),
                orderLogEvent.getUserId(),
                orderLogEvent.getPreviousStatus(),
                orderLogEvent.getCurrentStatus(),
                orderLogEvent.getResult(),
                orderLogEvent.getFailureReason());

        orderLogKafkaProducer.send(message);
    }

    // 주문 로직 실패 시 - Kafka로 메시지 발행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleOrderFailureLog(OrderLogEvent orderLogEvent) {
        log.info("주문 실패 이벤트 수신 - userId: {}, reason: {}",
                orderLogEvent.getUserId(),
                orderLogEvent.getFailureReason());

        OrderLogMessage message = OrderLogMessage.failure(
                orderLogEvent.getUserId(),
                orderLogEvent.getFailureReason());

        orderLogKafkaProducer.send(message);
    }
}
