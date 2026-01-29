package com.bookstore.bookstore_api.order.adapter.out.persistence;

import org.mapstruct.Mapper;

import com.bookstore.bookstore_api.order.domain.entity.OrdersEntity;
import com.bookstore.bookstore_api.order.domain.model.Orders;

@Mapper(componentModel = "spring")
public interface OrderConverter {

    /**
     * Orders 모델을 OrdersEntity 엔티티로 변환
     * @param orders Orders 모델
     * @return OrdersEntity 엔티티
     */
    OrdersEntity toEntity(Orders orders);

    /**
     * OrdersEntity 엔티티를 Orders 모델로 변환
     * @param ordersEntity OrdersEntity 엔티티
     * @return Orders 모델
     */
    Orders toModel(OrdersEntity ordersEntity);
}
