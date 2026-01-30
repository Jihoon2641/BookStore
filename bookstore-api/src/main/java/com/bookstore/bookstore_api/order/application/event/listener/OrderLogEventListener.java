package com.bookstore.bookstore_api.order.application.event.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bookstore.bookstore_api.order.application.event.object.OrderLogEvent;
import com.bookstore.bookstore_api.order.application.port.out.OrderLogRepository;
import com.bookstore.bookstore_api.order.domain.model.OrderLog;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class OrderLogEventListener {

    private final OrderLogRepository orderLogRepository;

    // 주문 로직 성공 시
    @Async("logTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderSuccessLog(OrderLogEvent orderLogEvent) {

        orderLogRepository.save(OrderLog.create(
                orderLogEvent.getOrderId(),
                orderLogEvent.getUserId(),
                orderLogEvent.getPreviousStatus(),
                orderLogEvent.getCurrentStatus(),
                orderLogEvent.getResult(),
                orderLogEvent.getFailureReason()));
    }

    // 주문 로직 실패 시
    @Async("logTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleOrderFailureLog(OrderLogEvent orderLogEvent) {

        orderLogRepository.save(OrderLog.createFailure(
                orderLogEvent.getUserId(),
                orderLogEvent.getFailureReason()));
    }
}
