package com.bookstore.bookstore_api.scheduler.order;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bookstore.bookstore_api.order.application.port.out.OrderLogOutboxRepository;
import com.bookstore.bookstore_api.order.domain.model.OrderLogOutBox;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class OrderLogOutboxScheduler {

    private final OrderLogOutboxRepository orderLogOutboxRepository;

    // /**
    // * Pending 상태의 Outbox를 처리
    // */
    // @Scheduled(fixedDelay = 15000)
    // public void processPendingOutbox() {
    // List<OrderLogOutBox> pendingList =
    // orderLogOutboxRepository.findPendingWithLimit(50);

    // int processedCount = 0;

    // for (OrderLogOutBox outBox : pendingList) {
    // try {
    // eventPublisher.publishEvent(new OrderLogCreatedEvent(outBox.getId()));
    // processedCount++;
    // } catch (Exception e) {
    // log.error("OrderLog 저장 실패 - OutboxId: {}, Error: {}",
    // outBox.getId(), e.getMessage(), e);
    // orderLogOutboxRepository.markAsFailed(outBox.getId(), e.getMessage(), 3);
    // }
    // }

    // if (processedCount > 0) {
    // log.info("OrderLog 저장 성공 - {}개", processedCount);
    // }
    // }

    /**
     * FAILED 상태의 Outbox를 처리
     */
    @Scheduled(fixedDelay = 60000)
    public void processFailedOutbox() {
        List<OrderLogOutBox> failedList = orderLogOutboxRepository.findFailedWithLimit(50);

        if (failedList.isEmpty())
            return;

        for (OrderLogOutBox outBox : failedList) {
            try {
                orderLogOutboxRepository.resetFailedToPending(outBox.getId());
                log.info("FAILED → PENDING 변경 완료 - OutboxId: {}", outBox.getId());
            } catch (Exception e) {
                log.error("FAILED → PENDING 변경 실패 - OutboxId: {}, Error: {}", outBox.getId(), e.getMessage(), e);
            }
        }
    }
}
