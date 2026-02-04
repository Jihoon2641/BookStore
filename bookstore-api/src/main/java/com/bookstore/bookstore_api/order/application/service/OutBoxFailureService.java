package com.bookstore.bookstore_api.order.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bookstore.bookstore_api.order.domain.model.OrderLogOutBox;
import com.bookstore.bookstore_api.order.utils.OutboxPayloadSerializer;
import com.bookstore.bookstore_api.order.application.port.out.OrderLogOutboxRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutBoxFailureService {

    private final OrderLogOutboxRepository orderLogOutboxRepository;
    private final OutboxPayloadSerializer outboxPayloadSerializer;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailureLog(Long userId, String errorMessage) {
        String payload = outboxPayloadSerializer.serializeFailureLog(userId, errorMessage);
        OrderLogOutBox orderLogOutbox = OrderLogOutBox.failed(null, userId, payload, errorMessage);
        orderLogOutboxRepository.save(orderLogOutbox);
    }
}
