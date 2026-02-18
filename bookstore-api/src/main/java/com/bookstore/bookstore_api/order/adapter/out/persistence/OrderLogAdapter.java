package com.bookstore.bookstore_api.order.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

import com.bookstore.bookstore_api.order.application.port.out.OrderLogRepository;
import com.bookstore.bookstore_api.order.domain.model.OrderLog;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.bookstore.bookstore_api.order.domain.entity.OrderLogEntity;

@Repository
@RequiredArgsConstructor
public class OrderLogAdapter implements OrderLogRepository {

    private final OrderLogMapper orderLogMapper;
    private final OrderLogConverter orderLogConverter;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderLog save(OrderLog orderLog) {
        OrderLogEntity orderLogEntity = orderLogConverter.toEntity(orderLog);

        if (orderLogEntity == null) {
            throw new RuntimeException("주문 로그 생성에 실패하였습니다.");
        }

        int result = orderLogMapper.save(orderLogEntity);

        if (result == 0) {
            throw new RuntimeException("주문 로그 저장에 실패하였습니다.");
        }

        return orderLogConverter.toModel(orderLogEntity);
    }

    @Override
    public boolean existsByRequestId(String requestId) {
        return orderLogMapper.existsByRequestId(requestId);
    }

}
