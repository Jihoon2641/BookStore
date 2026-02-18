package com.bookstore.bookstore_api.scheduler.order;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bookstore.bookstore_api.order.application.event.listener.OrderedEventListener;
import com.bookstore.bookstore_api.order.application.event.object.OrderLogCreatedEvent;
import com.bookstore.bookstore_api.order.application.port.out.OrderLogOutboxRepository;
import com.bookstore.bookstore_api.order.domain.model.OrderLogOutBox;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class OrderLogOutboxScheduler {

    private final OrderLogOutboxRepository orderLogOutboxRepository;
    private final BlockingQueue<Long> orderQueue;
    private final OrderedEventListener eventListener;

    @Scheduled(fixedDelay = 30000)
    public void processQueueOutbox() {
        int processedCount = 0;

        while (!orderQueue.isEmpty() && processedCount < 10) {
            Long outboxId = orderQueue.poll();

            if (outboxId != null) {
                try {
                    log.info("큐 재처리 시작 - OutboxId: {}", outboxId);
                    eventListener.handleOrderedLogEvent(new OrderLogCreatedEvent(outboxId));
                    processedCount++;
                } catch (Exception e) {
                    log.error("큐 재처리 실패 - OutboxId: {}, Error: {}", outboxId, e.getMessage());
                    orderLogOutboxRepository.markAsFailed(outboxId, e.getMessage(), 3);
                }
            }
        }

        if (processedCount > 0) {
            log.info("OrderLog 저장 성공 - {}개", processedCount);
        }
    }

    /**
     * Pending 상태의 Outbox를 처리
     * 
     */
    @Scheduled(fixedDelay = 30000)
    public void processPendingOutbox() {
        List<OrderLogOutBox> pendingList = orderLogOutboxRepository.findPendingWithLimit(50);

        int processedCount = 0;

        for (OrderLogOutBox outBox : pendingList) {
            try {
                eventListener.handleOrderedLogEvent(new OrderLogCreatedEvent(outBox.getId()));
                processedCount++;
            } catch (Exception e) {
                log.error("OrderLog 저장 실패 - OutboxId: {}, Error: {}",
                        outBox.getId(), e.getMessage(), e);
                // 실패 시 FAILED 상태로 변경
                orderLogOutboxRepository.markAsFailed(outBox.getId(), e.getMessage(), 3);
            }
        }

        if (processedCount > 0) {
            log.info("OrderLog 저장 성공 - {}개", processedCount);
        }
    }

    /**
     * FAILED 상태의 Outbox를 처리
     * 
     */
    @Scheduled(fixedDelay = 30000)
    public void processFailedOutbox() {
        List<OrderLogOutBox> failedList = orderLogOutboxRepository.findFailedWithLimit(50);

        int processedCount = 0;

        for (OrderLogOutBox outBox : failedList) {
            try {
                eventListener.handleOrderedLogEvent(new OrderLogCreatedEvent(outBox.getId()));
                processedCount++;
            } catch (Exception e) {
                log.error("OrderLog 저장 실패 - OutboxId: {}, Error: {}",
                        outBox.getId(), e.getMessage(), e);
            }
        }

        if (processedCount > 0) {
            log.info("OrderLog 저장 성공 - {}개", processedCount);
        }
    }
}
