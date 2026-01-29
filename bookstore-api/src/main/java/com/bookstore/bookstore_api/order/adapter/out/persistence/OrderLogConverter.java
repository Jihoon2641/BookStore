package com.bookstore.bookstore_api.order.adapter.out.persistence;

import org.mapstruct.Mapper;

import com.bookstore.bookstore_api.order.domain.entity.OrderLogEntity;
import com.bookstore.bookstore_api.order.domain.model.OrderLog;

@Mapper(componentModel = "spring")
public interface OrderLogConverter {

    /**
     * OrderLog 모델을 OrderLogEntity 엔티티로 변환
     * @param orderLog OrderLog 모델
     * @return OrderLogEntity 엔티티
     */
    OrderLogEntity toEntity(OrderLog orderLog);

    /**
     * OrderLogEntity 엔티티를 OrderLog 모델로 변환
     * @param orderLogEntity OrderLogEntity 엔티티
     * @return OrderLog 모델
     */
    OrderLog toModel(OrderLogEntity orderLogEntity);

}
