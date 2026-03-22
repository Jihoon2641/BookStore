package com.bookstore.bookstore_api.order.application.event.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bookstore.bookstore_api.order.application.event.object.OrderLogCreatedEvent;
import com.bookstore.bookstore_api.order.application.port.out.OrderLogRepository;
import com.bookstore.bookstore_api.order.application.service.OrderLogOutboxService;
import com.bookstore.bookstore_api.order.domain.model.OrderLog;
import com.bookstore.bookstore_api.order.domain.model.OrderLogOutBox;
import com.bookstore.bookstore_api.order.domain.model.OrderLogPayload;
import com.bookstore.bookstore_api.order.application.port.out.OrderLogOutboxRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@AllArgsConstructor
public class OrderedEventListener {

    private final OrderLogOutboxRepository orderLogOutboxRepository;
    private final OrderLogRepository orderLogRepository;
    private final OrderLogOutboxService orderLogOutboxService;

    @Async("logTaskExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderedLogEvent(OrderLogCreatedEvent event) {

        log.info("OrderLog 처리 시작 - OutboxId: {}", event.getOutboxId());

        try {
            // Outbox 조회
            OrderLogOutBox outBox = orderLogOutboxRepository.findById(event.getOutboxId());
            if (outBox == null) {
                log.error("Outbox를 찾을 수 없습니다. OutboxId: {}", event.getOutboxId());
                return;
            }

            // Payload 역직렬화
            OrderLogPayload payload = OrderLogPayload.fromJson(outBox.getPayload());

            // OrderLog 저장
            OrderLog orderLog = OrderLog.builder()
                    .orderId(payload.getOrderId())
                    .userId(payload.getUserId())
                    .previousStatus(payload.getPreviousStatus())
                    .currentStatus(payload.getCurrentStatus())
                    .result(payload.getResult())
                    .failureReason(payload.getFailureReason())
                    .requestId(payload.getRequestId())
                    .build();

            orderLogRepository.save(orderLog);

            // Outbox 상태 업데이트
            orderLogOutboxRepository.markAsSent(event.getOutboxId());

            log.info("OrderLog 저장 성공 - OrderId: {}, RequestId: {}",
                    payload.getOrderId(), payload.getRequestId());

        } catch (Exception e) {
            log.error("OrderLog 저장 실패 - OutboxId: {}, Error: {}",
                    event.getOutboxId(), e.getMessage(), e);
            orderLogOutboxService.markAsFailed(event.getOutboxId(), e.getMessage());
        }
    }
}
