package com.bookstore.bookstore_api.order.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bookstore.bookstore_api.order.application.port.out.OrderLogOutboxRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderLogOutboxService {

    private static final Logger log = LoggerFactory.getLogger(OrderLogOutboxService.class);

    private final OrderLogOutboxRepository orderLogOutboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long outboxId, String errorMessage) {
        try {
            int updated = orderLogOutboxRepository.markAsFailed(outboxId, errorMessage, 3);
            if (updated > 0) {
                log.info("Outbox FAILED 변경 완료 - OutboxId: {}", outboxId);
            } else {
                log.warn("Outbox 업데이트 실패 - OutboxId: {}", outboxId);
            }
        } catch (Exception e) {
            log.error("Outbox 상태 변경 실패 - OutboxId: {}, Error: {}", outboxId, e.getMessage(), e);
        }
    }
}
