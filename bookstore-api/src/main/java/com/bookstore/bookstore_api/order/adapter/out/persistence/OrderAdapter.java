package com.bookstore.bookstore_api.order.adapter.out.persistence;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.bookstore.bookstore_api.order.application.port.out.OrderRepository;
import com.bookstore.bookstore_api.order.domain.model.Orders;
import com.bookstore.bookstore_api.order.domain.entity.OrdersEntity;


@Component
@RequiredArgsConstructor
public class OrderAdapter implements OrderRepository {

    private final OrderMapper orderMapper;
    private final OrderConverter orderConverter;

    @Override
    public Orders save(Orders order) {
        OrdersEntity orderEntity = orderConverter.toEntity(order);

        if (orderEntity == null) {
            throw new RuntimeException("주문 생성에 실패하였습니다.");
        }

        int result = orderMapper.save(orderEntity);

        if (result == 0) {
            throw new RuntimeException("주문 생성에 실패하였습니다.");
        }

        return orderConverter.toModel(orderEntity);
    }

}
