package com.bookstore.bookstore_api.order.adapter.out.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bookstore.bookstore_api.order.domain.entity.OrderItemEntity;
import com.bookstore.bookstore_api.order.domain.model.OrderItem;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderItemConverter {

    /**
     * OrderItem 모델을 OrderItemEntity 엔티티로 변환
     */
    OrderItemEntity toEntity(OrderItem orderItem);

    /**
     * OrderItem 모델 리스트를 OrderItemEntity 엔티티 리스트로 변환
     */
    List<OrderItemEntity> toEntityList(List<OrderItem> orderItems);

    /**
     * OrderItemEntity 엔티티를 OrderItem 모델로 변환
     */
    @Mapping(target = "withOrderId", ignore = true)
    OrderItem toModel(OrderItemEntity orderItemEntity);

    /**
     * OrderItemEntity 엔티티 리스트를 OrderItem 모델 리스트로 변환
     */
    List<OrderItem> toModelList(List<OrderItemEntity> orderItemEntities);

}
