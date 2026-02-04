package com.bookstore.bookstore_api.order.adapter.out.persistence;

import org.mapstruct.Mapper;

import com.bookstore.bookstore_api.order.domain.entity.OrderLogOutboxEntity;
import com.bookstore.bookstore_api.order.domain.model.OrderLogOutBox;

@Mapper(componentModel = "spring")
public interface OrderLogOutboxConverter {

    /**
     * OrderLogOutBox 도메인 모델을 OrderLogOutboxEntity로 변환
     * 
     * @param orderLogOutBox 도메인 모델
     * @return OrderLogOutboxEntity
     */
    OrderLogOutboxEntity toEntity(OrderLogOutBox orderLogOutBox);

    /**
     * OrderLogOutboxEntity를 OrderLogOutBox 도메인 모델로 변환
     * 
     * @param entity OrderLogOutboxEntity
     * @return OrderLogOutBox 도메인 모델
     */
    OrderLogOutBox toModel(OrderLogOutboxEntity entity);
}
