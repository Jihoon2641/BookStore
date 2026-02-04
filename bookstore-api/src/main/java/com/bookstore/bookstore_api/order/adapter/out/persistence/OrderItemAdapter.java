package com.bookstore.bookstore_api.order.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;  

import com.bookstore.bookstore_api.order.application.port.out.OrderItemRepository;
import com.bookstore.bookstore_api.order.domain.model.OrderItem;
import com.bookstore.bookstore_api.order.domain.entity.OrderItemEntity;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderItemAdapter implements OrderItemRepository {

    private final OrderItemMapper orderItemMapper;
    private final OrderItemConverter orderItemConverter;

    @Override
    public int saveAll(List<OrderItem> orderItems) {
        List<OrderItemEntity> entities = orderItemConverter.toEntityList(orderItems);
        return orderItemMapper.saveAll(entities);
    }

    @Override
    public List<OrderItem> findByOrderId(Long orderId) {
        List<OrderItemEntity> entities = orderItemMapper.findByOrderId(orderId);
        return orderItemConverter.toModelList(entities);
    }

}
