package com.bookstore.bookstore_api.order.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.bookstore.bookstore_api.order.application.port.out.OrderLogOutboxRepository;
import com.bookstore.bookstore_api.order.domain.entity.OrderLogOutboxEntity;
import com.bookstore.bookstore_api.order.domain.model.OrderLogOutBox;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderLogOutboxAdapter implements OrderLogOutboxRepository {

    private final OrderLogOutboxMapper orderLogOutboxMapper;
    private final OrderLogOutboxConverter orderLogOutboxConverter;

    @Override
    public OrderLogOutBox save(OrderLogOutBox orderLogOutBox) {
        OrderLogOutboxEntity entity = orderLogOutboxConverter.toEntity(orderLogOutBox);
        orderLogOutboxMapper.save(entity);
        return orderLogOutboxConverter.toModel(entity);
    }

    @Override
    public List<OrderLogOutBox> findPendingWithLimit(int limit) {
        List<OrderLogOutboxEntity> entities = orderLogOutboxMapper.findPendingWithLimit(limit);
        return entities.stream()
                .map(orderLogOutboxConverter::toModel)
                .toList();
    }

    @Override
    public List<OrderLogOutBox> findFailedWithLimit(int limit) {
        List<OrderLogOutboxEntity> entities = orderLogOutboxMapper.findFailedWithLimit(limit);
        return entities.stream()
                .map(orderLogOutboxConverter::toModel)
                .toList();
    }

    @Override
    public int markAsSent(Long id) {
        return orderLogOutboxMapper.updateStatusToSent(id, LocalDateTime.now());
    }

    @Override
    public int markAsFailed(Long id, String lastError, int maxRetry) {
        return orderLogOutboxMapper.updateRetryAndError(id, lastError, maxRetry);
    }

    @Override
    public OrderLogOutBox findById(Long id) {
        OrderLogOutboxEntity entity = orderLogOutboxMapper.findById(id);
        return orderLogOutboxConverter.toModel(entity);
    }

    @Override
    public OrderLogOutBox update(OrderLogOutBox orderLogOutBox) {
        OrderLogOutboxEntity entity = orderLogOutboxConverter.toEntity(orderLogOutBox);
        orderLogOutboxMapper.update(entity);
        return orderLogOutboxConverter.toModel(entity);
    }
}
